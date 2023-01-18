package lto.manager.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import lto.manager.common.database.Options;
import lto.manager.common.database.tables.TableOptions;

public abstract class ExternalProcess {
	protected List<String> stdout = new ArrayList<String>();
	protected List<String> stderr = new ArrayList<String>();

	protected Process process;
	protected ExecutorService service;
	protected AtomicBoolean inProgress = new AtomicBoolean();
	protected Integer exitCode;

	public boolean start(String... commands) throws IOException {
		if (inProgress.get()) return false;
		inProgress.set(true);

		stdout.clear();
		stderr.clear();
		exitCode = null;
		ProcessBuilder builder = new ProcessBuilder(commands);
		process = builder.start();
		service = Executors.newFixedThreadPool(2);
		BufferedReader stdOutBuffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader stdErrBuffer = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		Semaphore semaphore = new Semaphore(2, true);
		semaphore.drainPermits();

		new Thread(() -> {
			try {
				process.waitFor();
				exitCode = process.exitValue();
				inProgress.set(false);
				semaphore.acquire(2);
				stop();
				onProcessExit();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();

		service.submit(() -> { // stdout
			StringBuilder str = new StringBuilder();
			while (true) {
				try {
					if (stdOutBuffer.ready()) {
						char tmp = (char) stdOutBuffer.read();
						if (tmp != '\n') str.append(tmp);
						else {
							String line = str.toString();
							if (Options.getBool(TableOptions.INDEX_ENABLE_LOG_EXTERNAL_PROCESS))
								System.out.println("stdout:" + line);
							stdout.add(line);
							str = new StringBuilder();
						}
					} else if (!inProgress.get()) {
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
			semaphore.release();
			if (Options.getBool(TableOptions.INDEX_ENABLE_LOG_EXTERNAL_PROCESS)) System.out.println("stdout: EXIT");
	    });

		service.submit(() -> { // stderr
			StringBuilder str = new StringBuilder();
			while (true) {
				try {
					if (stdErrBuffer.ready()) {
						char tmp = (char) stdErrBuffer.read();
						if (tmp != '\n') str.append(tmp);
						else {
							String line = str.toString();
							if (Options.getBool(TableOptions.INDEX_ENABLE_LOG_EXTERNAL_PROCESS))
								System.out.println("stderr:" + line);
							stdout.add(line);
							str = new StringBuilder();
						}
					} else if (!inProgress.get()) {
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
			semaphore.release();
			if (Options.getBool(TableOptions.INDEX_ENABLE_LOG_EXTERNAL_PROCESS)) System.out.println("stderr: EXIT");
	    });

		return true;
	}

	public boolean stop() {
		inProgress.set(false);
		if (service != null) service.shutdownNow();
		if (service != null) process.destroyForcibly();
		return true;
	}

	public boolean operationInProgress() {
		if (process == null) return false;
		return process.isAlive();
	}

	public boolean completed() {
		return exitCode != null;
	}

	public String getLatestError() {
		int index = stderr.size();
		if (index > 0) return stderr.get(index - 1);
		else return "NONE";
	}

	public Integer getExitCode() { return exitCode; }

	public abstract void onProcessExit();

}
