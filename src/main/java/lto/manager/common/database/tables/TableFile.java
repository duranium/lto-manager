package lto.manager.common.database.tables;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.CreateTableQuery;
import com.healthmarketscience.sqlbuilder.InsertQuery;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;

import lto.manager.common.database.Database;

public class TableFile {
	public static DbTable table = getSelf();
	public static final String TABLE_NAME = "table_file";
	public static final String COLUMN_NAME_ID = "id_file";
	public static final String COLUMN_NAME_FILE_NAME = "file_name";
	public static final String COLUMN_NAME_FILE_PATH = "file_path";
	public static final String COLUMN_NAME_FILE_SIZE = "file_size";
	public static final String COLUMN_NAME_FILE_DATE_CREATE = "file_created";
	public static final String COLUMN_NAME_FILE_DATE_MODIFY = "file_modified";
	public static final String COLUMN_NAME_FILE_TAPE_LOC = "file_tape_id";
	public static final String COLUMN_NAME_FILE_CRC32 = "file_crc32";

	public static final int COLUMN_INDEX_ID = 0;
	public static final int COLUMN_INDEX_FILE_NAME = 1;
	public static final int COLUMN_INDEX_FILE_PATH = 2;
	public static final int COLUMN_INDEX_FILE_SIZE = 3;
	public static final int COLUMN_INDEX_FILE_DATE_CREATE = 4;
	public static final int COLUMN_INDEX_FILE_DATE_MODFIY = 5;
	public static final int COLUMN_INDEX_FILE_TAPE_LOC = 6;
	public static final int COLUMN_INDEX_FILE_CRC32 = 7;

	public static class RecordFile {
		private int id;
		private String fileName;
		private String filePath;
		private int size;
		private LocalDateTime created;
		private LocalDateTime modified;
		private int tapeID;
		private int crc32;

		public RecordFile(int id, String fileName, String filePath, int size, LocalDateTime created, LocalDateTime modified, int tapeID, int crc32) {
			this.id = id;
			this.fileName = fileName;
			this.filePath = filePath;
			this.size = size;
			this.created = created;
			this.modified = modified;
			this.tapeID = tapeID;
			this.crc32 = crc32;
		}

		public static RecordFile of(int id, String fileName, String filePath, int size, LocalDateTime created, LocalDateTime modified, int tapeID, int crc32) {
			return new RecordFile(id, fileName, filePath, size, created, modified, tapeID, crc32);
		}

		public int getID() { return id;	}
		public void setID(int id) { this.id = id; }
		public String getFileName() { return fileName; }
		public void setFileName(String fileName) { this.fileName = fileName; }
		public String getFilePath() { return filePath; }
		public void setFilePath(String filePath) { this.filePath = filePath; }
		public int getFileSize() { return size;	}
		public void setFileSize(int size) { this.size = size; }
		public LocalDateTime getCreatedDateTime() { return created;	}
		public void setCreatedDateTime(LocalDateTime created) { this.created = created; }
		public LocalDateTime getModifiedDateTime() { return modified;	}
		public void setModifiedDateTime(LocalDateTime modified) { this.modified = modified; }
		public int getTapeID() { return tapeID;	}
		public void setTapeID(int tapeID) { this.tapeID = tapeID; }
		public int getCRC32() { return crc32; }
		public void setCRC32(int crc32) { this.crc32 = crc32; }
		public String getCRC32StrHex() { return Integer.toHexString(crc32); }
	}

	static DbTable getSelf() {
		DbSchema schema = Database.schema;
		DbTable table = schema.addTable(TABLE_NAME);

		DbColumn id = table.addColumn(COLUMN_NAME_ID, Types.INTEGER, null);
		//id.primaryKey();
		id.unique();
		id.notNull();

		String key[] = new String[] { COLUMN_NAME_ID};
		table.primaryKey(COLUMN_NAME_ID, key);
		table.addColumn(COLUMN_NAME_FILE_NAME, Types.VARCHAR, 256);
		table.addColumn(COLUMN_NAME_FILE_PATH, Types.VARCHAR, 256);
		table.addColumn(COLUMN_NAME_FILE_SIZE, Types.INTEGER, null);
		table.addColumn(COLUMN_NAME_FILE_DATE_CREATE, Types.TIMESTAMP, null);
		table.addColumn(COLUMN_NAME_FILE_DATE_MODIFY, Types.TIMESTAMP, null);

		DbColumn tapeTypeForegnColumn = table.addColumn(COLUMN_NAME_FILE_TAPE_LOC, Types.INTEGER, null);
		DbColumn columns[] = new DbColumn[] { tapeTypeForegnColumn};
		DbTable tableTape =  TableTape.table;
		DbColumn columnsRef[] = new DbColumn[] { tableTape.getColumns().get(TableTape.COLUMN_INDEX_ID)};
		table.foreignKey(TableTape.COLUMN_NAME_ID, columns, tableTape, columnsRef);

		table.addColumn(COLUMN_NAME_FILE_CRC32, Types.INTEGER, null);
		return table;
	}

	public static boolean createTable(Connection con) throws SQLException {
		String q = new CreateTableQuery(TableFile.table, true).validate().toString();
		q = q.replace(COLUMN_NAME_ID + ")", COLUMN_NAME_ID + " AUTOINCREMENT)"); // TODO better way of autoincrement

		var statment = con.createStatement();

		if (!statment.execute(q)) {
			return true;
		}

		return false;
	}

	public static boolean addFiles(Connection con, int tapeID, List<File> files) throws SQLException, IOException {
		var statment = con.createStatement();

		for (File f: files) {
			InsertQuery iq = new InsertQuery(table);
			iq.addColumn(table.getColumns().get(COLUMN_INDEX_FILE_NAME), f.getName());
			iq.addColumn(table.getColumns().get(COLUMN_INDEX_FILE_PATH), f.getPath());
			iq.addColumn(table.getColumns().get(COLUMN_INDEX_FILE_SIZE), Files.size(f.toPath()));
			iq.addColumn(table.getColumns().get(COLUMN_INDEX_FILE_DATE_CREATE), "");
			iq.addColumn(table.getColumns().get(COLUMN_INDEX_FILE_DATE_MODFIY), "");
			iq.addColumn(table.getColumns().get(COLUMN_INDEX_FILE_TAPE_LOC), tapeID);
			iq.addColumn(table.getColumns().get(COLUMN_INDEX_FILE_CRC32), 0);
			String sql = iq.validate().toString();
			if (statment.execute(sql)) {
				return false;
			}
		}

		return true;
	}

	public static List<File> getFilesOnTape(Connection con, int tapeID) throws SQLException, IOException {
		var statment = con.createStatement();

		List<File> files = new ArrayList<File>();

		SelectQuery uq = new SelectQuery();
		uq.addAllTableColumns(table);
		uq.addCondition(BinaryCondition.equalTo(table.getColumns().get(COLUMN_INDEX_FILE_TAPE_LOC), tapeID));
		uq.addOrderings(table.getColumns().get(COLUMN_INDEX_ID));
		String sql = uq.validate().toString();

		ResultSet result = statment.executeQuery(sql);

		while (result.next()) {
			String path = result.getString(COLUMN_INDEX_FILE_PATH);
			files.add(new File(path));
		}

		return files;
	}

}
