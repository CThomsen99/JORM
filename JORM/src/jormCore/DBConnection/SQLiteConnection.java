package jormCore.DBConnection;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jormCore.Annotaions.*;
import jormCore.JormApplication;
import jormCore.PersistentObject;

public class SQLiteConnection extends DatabaseConnection {

	private Connection _connection;

	public SQLiteConnection(String connectionSting) throws SQLException {
		super(connectionSting);
		_connection = DriverManager.getConnection(connectionSting);
	}

	@Override
	public ResultSet getTable(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(PersistentObject obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(PersistentObject obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public void create(PersistentObject obj) {

		Map<Field, Object> mapping = obj.getPersistentPropertiesWithValues();

		String result = "INSERT OR REPLACE INTO " + obj.getClass().getSimpleName();
		String typePart = "( ";
		String valuePart = "VALUES ( ";

		for (Entry<Field, Object> i : mapping.entrySet()) {
		}

	}

	@Override
	public void execute(String statement) throws SQLException {
		_connection.createStatement().execute(statement);
	}

	@Override
	public String generateCreateTypeStatement(Class<? extends PersistentObject> type) {
		List<Field> props = PersistentObject.getPersistentProperties(type);

		String result = "CREATE TABLE IF NOT EXISTS " + type.getSimpleName() + " (";

		for (int i = 0; i < props.size(); i++) {
			Field field = props.get(i);

			FieldWrapper wr = WrapField(field);

			result += generateFieldDefinition(wr);

			if (i < props.size() - 1)
				result += " ,";
		}

		result += " )";

		return result;
	}

	public String generateFieldDefinition(FieldWrapper wr) {
		String result = "";

		result += wr.get_name();
		result += " ";
		result += wr.get_type();

		if (wr.is_isPrimaryKey())
			result += " PRIMARY KEY ";
		if (wr.is_autoincrement())
			result += " AUTOINCREMENT ";
		if (wr.is_canNotBeNull())
			result += " NOT NULL ";

		return result;
	}

	private FieldWrapper WrapField(Field field) {
		String name = field.getName();
		String type = ParseFieldType(field);
		boolean isPrimaryKey = field.isAnnotationPresent(PrimaryKey.class);
		boolean canNotBeNull = field.isAnnotationPresent(CanNotBeNull.class);
		boolean autoincrement = field.isAnnotationPresent(Autoincrement.class);

		return new FieldWrapper(name, type, isPrimaryKey, canNotBeNull, autoincrement);
	}

	private String ParseFieldType(Field field) {
		Class<?> type = field.getType();

		if (type == String.class || type == char.class)
			return "TEXT";

		if (type == int.class)
			return "INTEGER";

		return "TEXT";
	}

	@Override
	public void createSchema()
	{
		for (Class<? extends PersistentObject> cl : JormApplication.getApplication().getTypeList()) {
			try {
				execute(generateCreateTypeStatement(cl));
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void updateSchema() {
		ArrayList<String> updateStatements = new ArrayList<>();
		
		for (Class<? extends PersistentObject> cl : JormApplication.getApplication().getTypeList()) {

			String getTypeSchemaStatement = "PRAGMA table_info(" + cl.getSimpleName() + ")";
			ArrayList<String> persistentColumns = new ArrayList<>();
			
			List<Field> runtimeFields = new ArrayList<>();

			// collect persistentColumns
			try {
				ResultSet resultSet = _connection.createStatement().executeQuery(getTypeSchemaStatement);
				while (resultSet.next()) {
					persistentColumns.add(resultSet.getString("name"));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// collect runtimeColumns (also columns which are already persistent)
			runtimeFields = PersistentObject.getPersistentProperties(cl);

			// set runtimeColumns to ONLY runtimeColumns
			runtimeFields.removeIf(field -> (persistentColumns.contains(field.getName())));

			for (Field nonPersistentField : runtimeFields) {
				updateStatements.add(generateAddColumnToTableStatement(cl, nonPersistentField));
			}

		}
		
		for (String statement : updateStatements) {
			try {
				execute(statement);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String generateAddColumnToTableStatement(Class<? extends PersistentObject> cl, Field nonPersistentField) {
		FieldWrapper wr = WrapField(nonPersistentField);

		return "ALTER TABLE " + cl.getSimpleName() + " ADD " + generateFieldDefinition(wr);
	}
}
