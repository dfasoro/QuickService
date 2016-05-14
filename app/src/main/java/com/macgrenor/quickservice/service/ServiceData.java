package com.macgrenor.quickservice.service;

import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;

import com.macgrenor.quickservice.data.DataAccess;
import com.macgrenor.quickservice.data.DataProvider;

public class ServiceData {
	public int id;
	public String name;
	public int use_count;
	public boolean bookmarked;
	public String markup;
	public int last_use_id;
	public long last_use_date;
	public long reminder_date;
	
	public ServiceData(int id) throws Exception {
		//this can be gotten from saved entries into related db table or associated json_data in same table.
		DataProvider sqdb = DataProvider.getDataProvider();
		SQLiteQueryBuilder qb1 = new SQLiteQueryBuilder();
		qb1.setTables(DataAccess.service_table);
		String whereStatement = "ID = ?";
		
		Cursor srv = sqdb.query(qb1, new String[] {"name", "use_count", "bookmarked", "markup",
				"last_use_id", "last_use_date", "reminder_date"}, 
				whereStatement, new String[] {String.valueOf(id)}, null, null);
		if (!srv.moveToFirst()) throw new Exception("Cannot find service. It has been deleted.");
		
		this.id = id;
		this.name = srv.getString(0);
		this.use_count = srv.getInt(1);
		this.bookmarked = srv.getInt(2) == 0 ? false : true;
		this.markup = srv.getString(3);
		this.last_use_id = srv.getInt(4);
		try { this.last_use_date = srv.getInt(5); } catch (Exception e) {}
		try { this.reminder_date = srv.getInt(6); } catch (Exception e) {}
		
		srv.close();
	}
}
