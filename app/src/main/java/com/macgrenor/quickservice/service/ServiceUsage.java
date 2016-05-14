package com.macgrenor.quickservice.service;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;

import com.macgrenor.quickservice.data.DataAccess;
import com.macgrenor.quickservice.data.DataProvider;

public class ServiceUsage {
	public int id;
	public String channel;
	public int service_version;
	public HashMap<String, String> data;
	public long date_used;
	
	public ServiceUsage() {
		//this can be gotten from saved entries into related db table or associated json_data in same table.
		
	}
	
	public ServiceUsage(int id) throws JSONException {
		//this can be gotten from saved entries into related db table or associated json_data in same table.
		DataProvider sqdb = DataProvider.getDataProvider();
		SQLiteQueryBuilder qb1 = new SQLiteQueryBuilder();
		qb1.setTables(DataAccess.service_usage_table);
		String whereStatement = "ID = ?";
		
		Cursor srv = sqdb.query(qb1, new String[] {"channel", "service_version", "data", "date_used"}, 
				whereStatement, new String[] {String.valueOf(id)}, null, null);
		srv.moveToFirst();
		
		this.id = id;
		this.channel = srv.getString(0);
		this.service_version = srv.getInt(1);
		this.data = Service.ObjectToStringMap(new JSONObject(srv.getString(2)));
		this.date_used = srv.getLong(3);
		
		srv.close();
	}
}
