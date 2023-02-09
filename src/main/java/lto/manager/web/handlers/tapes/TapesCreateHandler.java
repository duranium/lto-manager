package lto.manager.web.handlers.tapes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.List;

import org.xmlet.htmlapifaster.EnumTypeButtonType;
import org.xmlet.htmlapifaster.EnumTypeInputType;

import com.sun.net.httpserver.HttpExchange;

import htmlflow.DynamicHtml;
import lto.manager.common.database.Database;
import lto.manager.common.database.tables.TableManufacturer.RecordManufacturer;
import lto.manager.common.database.tables.TableTape.RecordTape;
import lto.manager.common.database.tables.TableTapeType.RecordTapeType;
import lto.manager.web.handlers.BaseHandler;
import lto.manager.web.handlers.templates.TemplatePage;
import lto.manager.web.handlers.templates.TemplatePage.SelectedPage;
import lto.manager.web.handlers.templates.TemplatePage.TemplatePageModel;
import lto.manager.web.handlers.templates.models.BodyModel;
import lto.manager.web.handlers.templates.models.HeadModel;
import lto.manager.web.resource.Asset;
import lto.manager.web.resource.CSS;

public class TapesCreateHandler extends BaseHandler {
	public static final String PATH = "/tapes/new";
	public static DynamicHtml<BodyModel> view = DynamicHtml.view(TapesCreateHandler::body);

	private static final String SERIAL = "serial";
	private static final String TAPETYPE = "type";
	private static final String MANU = "manu";
	private static final String BARCODE = "barcode";
	private static final String WORM = "worm";

	static void body(DynamicHtml<BodyModel> view, BodyModel model) {
		List<RecordManufacturer> m = null;
		List<RecordTapeType> t = null;
		String er = null;
		boolean s = false;

		final String barcode = model.getQueryNoNull(BARCODE);
		final String serial = model.getQueryNoNull(SERIAL);
		final String manu = model.getQueryNoNull(MANU);
		final String type = model.getQueryNoNull(TAPETYPE);
		final String worm = model.getQueryNoNull(WORM);

		final int manuIndex = manu.equals("") ? -1 : Integer.valueOf(manu);
		final int typeIndex = type.equals("") ? -1 : Integer.valueOf(type);

		try {
			m = Database.getAllTapeManufacturers();
			t = Database.getAllTapeTypes();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		final List<RecordTapeType> typesList = t;
		final List<RecordManufacturer> manuList = m;

		if (model.hasQuery()) {
			try {
				var tape = RecordTape.of(null, manuIndex, typeIndex, barcode, serial, 0, 0, null);
				Database.addTape(tape);
				s = true;
			} catch (Exception e) {
				e.printStackTrace();
				er = e.getMessage();
			}
		}

		final String errorMessage = er;
		final boolean addedTapeSuccess = s;


		try {
			view
				.div()
					.form()
						.b().attrStyle("width:150px;display:inline-block").text("LTO Tape Type: ").__()
						.select().attrId("select-type").attrName(TAPETYPE).dynamic(select -> {
							select.attrOnchange("onSelectType()")
								.option().attrValue("").attrSelected(typeIndex == -1).attrDisabled(true).text("Select").__();
							for (RecordTapeType item: typesList) {
								if (item.getID() == typeIndex) {
									select.option()
										.addAttr("data-des", item.getDesignation())
										.addAttr("data-worm", item.getDesignationWORM())
										.attrSelected(true).attrValue(String.valueOf(item.getID())).text(item.getType())
									.__();
								} else {
									select.option()
										.addAttr("data-des", item.getDesignation())
										.addAttr("data-worm", item.getDesignationWORM())
										.attrValue(String.valueOf(item.getID())).text(item.getType())
									.__();
								}
							}
						}).__().br().__()

						.b().attrStyle("width:150px;display:inline-block").text("LTO Manufacturer: ").__()
						.select().attrName(MANU).dynamic(select -> {
							select.option().attrValue("").attrSelected(manuIndex == -1).attrDisabled(true).text("Select").__();
							for (RecordManufacturer item: manuList) {
								if (item.getID() == manuIndex) {
									select.option().attrSelected(true).attrValue(String.valueOf(item.getID())).text(item.getManufacturer()).__();
								} else {
									select.option().attrValue(String.valueOf(item.getID())).text(item.getManufacturer()).__();
								}
							}
						}).__().br().__()

						.b().attrStyle("width:150px;display:inline-block").text("Serial Number: ").__()
						.input().attrType(EnumTypeInputType.TEXT).attrName(SERIAL).dynamic(input -> input.attrValue(serial)).__().br().__()

						.b().attrStyle("width:150px;display:inline-block").text("Barcode: ").__()
						.input().attrType(EnumTypeInputType.TEXT).attrName(BARCODE).dynamic(input -> input.attrValue(barcode)).__()
						.input().attrType(EnumTypeInputType.TEXT).attrId("des").__().br().__()

						.b().attrStyle("width:150px;display:inline-block").text("WORM: ").__()
						.input().attrId(WORM).attrOnclick("onSelectType(this)").attrType(EnumTypeInputType.CHECKBOX).attrName(WORM).dynamic(input -> input.attrValue(worm)).__().br().__()

						.button().attrClass(CSS.BUTTON).attrType(EnumTypeButtonType.SUBMIT).text("Submit").__()

						.p().dynamic(p -> {
							if (model.hasQuery()) {
								if (errorMessage == null) {
									if (addedTapeSuccess) p.text("Tape added successfully");
								} else {
									p.text("Failed to add tape: " + errorMessage);
								}
							}
						}).__()
					.__()
				.__(); // div
		} catch (Exception e) {
			view = DynamicHtml.view(TapesCreateHandler::body);
			throw e;
		}
	}

	@Override
	public void requestHandle(HttpExchange he) throws IOException, SQLException {
		try {
			HeadModel thm = HeadModel.of("Tapes");
			thm.AddScript(Asset.JS_ADD_TAPE);
			TemplatePageModel tepm = TemplatePageModel.of(view, thm, SelectedPage.Tapes, BodyModel.of(he, null));
			String response = TemplatePage.view.render(tepm);

			he.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length());
			OutputStream os = he.getResponseBody();
			os.write(response.getBytes());
			os.close();
		} catch (Exception e) {
			view = DynamicHtml.view(TapesCreateHandler::body);
			throw e;
		}
	}
}
