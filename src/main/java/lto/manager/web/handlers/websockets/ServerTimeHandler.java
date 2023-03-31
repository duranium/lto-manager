package lto.manager.web.handlers.websockets;

import java.io.IOException;

import lto.manager.common.ExternalProcess;

public class ServerTimeHandler extends BaseWebsocketHandler {
	public static final String PATH = "/time";

	private ExternalProcess ep;

	@Override
	public boolean start() {
		if (ep != null) return false;
		ep = new ExternalProcess() {

			@Override
			public void onProcessExit() {

			}
		};

		try {
			ep.start("repeat.sh");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}


}