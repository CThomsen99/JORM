package jormCore.DBConnection;
import java.lang.reflect.Field;
import java.sql.ResultSet;

import jormCore.JormApplication;
import jormCore.PersistentObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class DatabaseConnection {

	protected String connectionString;
	
	public DatabaseConnection(String connectionSting) 
	{
		connectionString = connectionSting;
	}
	
	protected void initDatabase() throws SQLException {
		
		LinkedList<String> createStatements = new LinkedList<>();
		
		
		List<Class<? extends PersistentObject>> types = JormApplication.getApplication().getTypeList();
		for(Class<? extends PersistentObject> type : types)
		{
			String createStatement = generateCreateTypeStatement(type);
			
			if(createStatement != null)
				createStatements.add(createStatement);
		}
		
		
		
		for (String statement : createStatements) {
			execute(statement);
		}
		
		updateSchema();		
	}

	public abstract ResultSet getTable(String name);
	public abstract void update(PersistentObject obj);
	public abstract void delete(PersistentObject obj);
	public abstract void create(PersistentObject obj);
	public abstract void execute(String statement) throws SQLException;
	
	public abstract String generateCreateTypeStatement(Class<? extends PersistentObject> type);
	public abstract void createSchema();
	public abstract void updateSchema();
}
