package com.macgrenor.quickservice.data;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;

import com.macgrenor.quickservice.QSApplication;
import com.macgrenor.quickservice.service.ServiceUsage;

public class DataAccess {
	public final static String autocomplete_table = "autocomplete";
	public final static String service_table = "service";
	public final static String service_usage_table = "service_usage";
	public final static String service_sub_usage_table = "service_sub_usage";
	
	private final static HashMap<String, String> sortCriteria = new  HashMap<String, String>(10);
	
	static {
		sortCriteria.put("Alphabetical", "promoted DESC, name");
		sortCriteria.put("Recently Used", "last_use_date");
		sortCriteria.put("Most Used", "use_count");
		sortCriteria.put("Recently Added", "id");
	}
	
	

	public static void saveAutoComplete(String field_name, String value) {
		
		if (value.trim().length() < 2) return;
		
		DataProvider qsdb = DataProvider.getDataProvider();
		ContentValues values = new ContentValues(3);
		values.put("field_name", field_name);
		values.put("value", value);
		values.put("date", QSApplication.getUnixTimeStamp());
		
		try {
			qsdb.insert(autocomplete_table,	values);
		}
		catch (SQLiteConstraintException ce) {
			//the unique index thingy.
		}
		
		QSApplication.ServiceDBInstance.addAutoCompleteValues(field_name, value);
	}
	

	public static ArrayList<String> getAutoComplete(String field_name) {
		ArrayList<String> list = new ArrayList<String>(20);
		DataProvider qsdb = DataProvider.getDataProvider();
		SQLiteQueryBuilder qb1 = new SQLiteQueryBuilder();
		qb1.setTables(DataAccess.autocomplete_table);
		String whereStatement = "field_name = ?";
		
		Cursor srv = qsdb.query(qb1, new String[] {"value"}, 
				whereStatement, new String[] {field_name}, "id DESC", "20");
		
		while (srv.moveToNext()) {
			list.add(srv.getString(0));
		}

		return list;
	}
	
	public static int saveUsage(int usage_id, int service_id, String channel, boolean completed, int service_version, HashMap<String, String> data, int use_count) {
		
		if (!completed) {
			ServiceUsage sf = QSApplication.ServiceDBInstance.getServiceDraft(service_id);
			if (sf == null) sf = new ServiceUsage();
			sf.id = 0; //na draft na
			sf.channel = channel;
			sf.service_version = service_version;
			sf.date_used = QSApplication.getUnixTimeStamp();
			sf.data = data;
			QSApplication.ServiceDBInstance.addServiceDraft(service_id, sf);
			return usage_id;
		}
		
		DataProvider qsdb = DataProvider.getDataProvider();
		ContentValues values = new ContentValues(10);
		values.put("service_id", service_id);
		values.put("channel", channel);
		values.put("date_used", QSApplication.getUnixTimeStamp());
		values.put("service_version", service_version);
		values.put("data", (new JSONObject(data)).toString());
		
		if (usage_id == 0) {
			//new entry
			usage_id = (int) qsdb.insert(service_usage_table, values);
			ServiceListAdapter.setLastUsage(service_id, usage_id);
		}
		else {
			qsdb.update(service_usage_table, values, "ID = ?", new String[] {String.valueOf(usage_id)});
		}
		
		ContentValues service_update = new ContentValues(2);
		service_update.put("last_use_id", usage_id);
		service_update.put("last_use_date", QSApplication.getUnixTimeStamp());
		service_update.put("use_count", use_count);
		qsdb.update(service_table, service_update, "ID = ?", new String[] {String.valueOf(service_id)});
		
		QSApplication.ServiceDBInstance.removeServiceDraft(service_id);
		
		return usage_id;
	}
	
	public static int saveSubUsage(int service_id, String channel, String field_name, boolean saved_before, HashMap<String, String> data) {
		DataProvider qsdb = DataProvider.getDataProvider();
		
		ContentValues values = new ContentValues(10);
		values.put("service_id", service_id);
		values.put("channel", channel);
		values.put("date_used", QSApplication.getUnixTimeStamp());
		values.put("service_sub_id", field_name);
		values.put("data", (new JSONObject(data)).toString());
		
		int sub_usage_id = (int) qsdb.insert(service_sub_usage_table, values);
		
		if (!saved_before) {
			qsdb.execSQL("UPDATE " + service_table + " SET use_count = use_count + 1, last_use_date = " +
					QSApplication.getUnixTimeStamp() + " WHERE ID = ?", new String[] {String.valueOf(service_id)});
		}
		
		return sub_usage_id;
	}
	

	
	public static void bookmark(int service_id, boolean bookmarked) {
		DataProvider qsdb = DataProvider.getDataProvider();
		ContentValues service_update = new ContentValues(2);
		service_update.put("bookmarked", bookmarked);
		qsdb.update(DataAccess.service_table, service_update, "ID = ?", new String[] {String.valueOf(service_id)});
	}
	
	private static boolean loadedCategories = false; 
	public static void loadCategories() {
		if (loadedCategories) return;
		DataProvider sqdb = DataProvider.getDataProvider();
		SQLiteQueryBuilder qb1 = new SQLiteQueryBuilder();
		qb1.setTables(DataAccess.service_table);
		
		Cursor c = sqdb.query(qb1, new String[] {"category", "subcategory"}, 
				"tag = ?", new String[] {QSApplication.Preferences.TAG}, 
				"category asc, subcategory asc", null, "category, subcategory", null);
		
		String current = null;
		ArrayList<String> subcategories = null;
		while (c.moveToNext()) {
			if (!c.getString(c.getColumnIndexOrThrow("category")).equalsIgnoreCase(current)) {
				current = c.getString(c.getColumnIndexOrThrow("category"));
				subcategories = QSApplication.ServiceDBInstance.getSubCategories(current);
				subcategories.clear();
			}
			
			subcategories.add(c.getString(c.getColumnIndexOrThrow("subcategory")));
		}
		loadedCategories = true;
	}
	

	  
	  public static Cursor getServiceCursor(Context ctx, String category, String subcategory, String filter, String order,
			  String orderDirection, boolean favoriteMode) {
		  DataProvider sqdb = DataProvider.getDataProvider();
			SQLiteQueryBuilder qb1 = new SQLiteQueryBuilder();
			qb1.setTables(DataAccess.service_table);
			
			ArrayList<String> where = new ArrayList<String>(6);
			ArrayList<String> whereParams = new ArrayList<String>(6);

			//always filter to active tag.
			where.add("tag = ?");
			whereParams.add(QSApplication.Preferences.TAG);
			
			if (favoriteMode) {
				where.add("bookmarked != ?");
				whereParams.add("0");
			}
			else {
				if (!TextUtils.isEmpty(category)) {
					where.add("category = ?");
					whereParams.add(category);
				}
				if (!TextUtils.isEmpty(subcategory)) {
					where.add("subcategory = ?");
					whereParams.add(subcategory);
				}
				if (!TextUtils.isEmpty(filter) && filter.length() >= 2) {
					where.add("(name LIKE ? OR tags LIKE ?)");
					filter = filter.replace("%", "");
					filter = "%" + filter + "%";
					whereParams.add(filter);
					whereParams.add(filter);
				}
			}
			
			if ("Ascending".equalsIgnoreCase(orderDirection)) orderDirection = "ASC";
			else if ("Descending".equalsIgnoreCase(orderDirection)) orderDirection = "DESC";
			else if (null == orderDirection) orderDirection = "";
					
			
			String whereStr = TextUtils.join(" AND ", where);
			order = TextUtils.isEmpty(order) ? order : (sortCriteria.get(order) + " " + orderDirection);
			
			return sqdb.query(qb1, new String[] {"id _id", "id", "name", "tags", "bookmarked", "promoted", "last_use_id"},
					whereStr, whereParams.toArray(new String[] {}), order, null);
	  }
	  
	  public static Cursor getUnsyncedUsage() {
		  DataProvider sqdb = DataProvider.getDataProvider();
			SQLiteQueryBuilder qb1 = new SQLiteQueryBuilder();
			qb1.setTables(DataAccess.service_usage_table);
		    qb1.setTables(DataAccess.service_table + " AS ST INNER JOIN " + DataAccess.service_usage_table + " AS SUT ON (ST.id = SUT.service_id)");

		  ArrayList<String> where = new ArrayList<String>(1);
			ArrayList<String> whereParams = new ArrayList<String>(1);

			//always filter to active tag.
			where.add("SUT.id > ?");
			whereParams.add(String.valueOf(QSApplication.Preferences.lastUsageSync));
			
			String whereStr = TextUtils.join(" AND ", where);
			String order = "SUT.id ASC";
			String limit = "100";
			
			return sqdb.query(qb1, new String[] {"SUT.id", "SUT.service_id", "SUT.channel", "SUT.date_used", "SUT.service_version", "ST.bookmarked"},
					whereStr, whereParams.toArray(new String[] {}), order, limit);
	  }
	  
	  public static Cursor getUnsyncedSubUsage() {
		  DataProvider sqdb = DataProvider.getDataProvider();
			SQLiteQueryBuilder qb1 = new SQLiteQueryBuilder();
			qb1.setTables(DataAccess.service_table + " AS ST INNER JOIN " + DataAccess.service_sub_usage_table + " AS SSUT ON (ST.id = SSUT.service_id)");
			
			ArrayList<String> where = new ArrayList<String>(3);
			ArrayList<String> whereParams = new ArrayList<String>(3);

			where.add("SSUT.id > ?");
			whereParams.add(String.valueOf(QSApplication.Preferences.lastSubUsageSync));
			
			String whereStr = TextUtils.join(" AND ", where);
			String order = "SSUT.id ASC";
			String limit = "100";
			
			return sqdb.query(qb1, new String[] {"SSUT.id", "SSUT.service_id", "SSUT.channel", "SSUT.date_used", "SSUT.service_sub_id", "ST.bookmarked"},
					whereStr, whereParams.toArray(new String[] {}), order, limit);
	  }
	  
}
