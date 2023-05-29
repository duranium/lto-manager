package lto.manager.web.handlers.http.pages.files;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.xmlet.htmlapifaster.Div;
import org.xmlet.htmlapifaster.EnumBorderType;

import com.sun.net.httpserver.HttpExchange;

import lto.manager.common.Util;
import lto.manager.common.database.Database;
import lto.manager.common.database.tables.TableFile;
import lto.manager.common.database.tables.records.RecordFile;
import lto.manager.web.handlers.http.BaseHTTPHandler;
import lto.manager.web.handlers.http.templates.TemplatePage.SelectedPage;
import lto.manager.web.handlers.http.templates.TemplatePage.TemplatePageModel;
import lto.manager.web.handlers.http.templates.models.BodyModel;
import lto.manager.web.handlers.http.templates.models.HeadModel;
import lto.manager.web.resource.Asset;
import lto.manager.web.resource.CSS;

public class FilesBrowserHandler extends BaseHTTPHandler {
	public static final String PATH = "/files/browser";

	public static final String DIR = "dir";

	static Void content(Div<?> view, BodyModel model) {
		final String dirQ = model.getQueryNoNull(DIR);

		final List<RecordFile> filesList = new ArrayList<RecordFile>();
		try {
			final String location = dirQ.equals("") ? "/" : dirQ;
			filesList.addAll(TableFile.getFilesInDir(Database.connection, location));
		} catch (SQLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Supplier<String> getDirD = () -> {
			if (!dirQ.equals("")) {
				return dirQ;
		    }
		    return "/";
		};
		final String dirD = getDirD.get();

		view
			.div()
				.p().text("Path " + dirQ).__()
				.div().of(div -> {
					final String complete = dirD;
					int startIndex = 0;
					int endIndex = 0;
					String crumb = "";
					while (true) {
						String name = complete.substring(startIndex, endIndex + 1);
						crumb += Util.encodeUrl(name);
						startIndex = endIndex + 1;
						endIndex = complete.indexOf("/", startIndex);
						div
							.a()
								.attrClass(CSS.BUTTON + (endIndex < 0 ? CSS.BACKGROUND_ACTIVE : ""))
								.attrHref("?" + DIR + "=" + crumb)
								.text(name)
							.__();
						if (endIndex < 0) break;
					}

				}).__()
				.table().attrClass(CSS.TABLE).of(table -> {
					table.attrBorder(EnumBorderType._1).tr()
						.th().text("Filename").__()
						.th().text("Size").__()
					.__();
					for (RecordFile f: filesList) {
						table.tr()
							.td().of(td -> {
								if (f.isDirectory()) {
									var path = f.getAbsolutePath();
									var link = Util.encodeUrl(path);
									var name = f.getFileNameTrim();
									td.b().a().attrHref("?" + DIR + "=" + link).text(name).__().__();
								} else {
									td.text(f.getFileName());
								}
							}).__()
							.td().text(f.getFileSize())
						.__();
					}
					if (filesList.size() == 0) table.tr().td().text("Empty").__().__();
				}).__()
			.__(); // div
		return null;
	}

	@Override
	public void requestHandle(HttpExchange he) throws IOException, InterruptedException, ExecutionException {
		HeadModel thm = HeadModel.of("Files Browser");
		thm.AddCSS(Asset.CSS_FILE_VIEW);
		thm.AddScript(Asset.JS_FILE_VIEW);
		TemplatePageModel tpm = TemplatePageModel.of(FilesBrowserHandler::content, thm, SelectedPage.Files, BodyModel.of(he, null));
		requestHandleCompletePage(he, tpm);
	}
}
