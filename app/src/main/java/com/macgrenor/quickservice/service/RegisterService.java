package com.macgrenor.quickservice.service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;

import com.macgrenor.quickservice.QSApplication;
import com.macgrenor.quickservice.R;
import com.macgrenor.quickservice.data.DataAccess;
import com.macgrenor.quickservice.data.DataProvider;

//@SuppressWarnings("unused")
public class RegisterService {
	private int id;
	private long timestamp;

	private String name; //check Glo Call Balance
	private String category; //Health/Edu/Unsubscribe/Banking/Telco/Balance/
	private String subcategory; // Gtb/Stanbic/Empty/etc...
	private String tags; //string delimited linda, konga, orisirisi
	
	private int service_version;
	private boolean promoted;
	private String tag;
	
	private String markup;
	private boolean deleted;

	public String _mode;
	
	public RegisterService(String markup) throws JSONException {
		this.markup = markup;
		
		JSONObject config = new JSONObject(markup);
		this.id = config.getInt("id");
		this.timestamp = config.getLong("timestamp");
		this.service_version = config.getInt("service_version");
		this.deleted = config.optBoolean("deleted", false);
		
		if (!this.deleted) {
			this.name = config.getString("name");
			this.category = config.getString("category");
			this.subcategory = config.getString("subcategory");
			this.tags = config.getString("tags");
			
			this.promoted = config.getBoolean("promoted");
			this.tag = config.getString("tag");
		}
	}
	
	public long save() {
		DataProvider qsdb = DataProvider.getDataProvider();
		
		if (this.deleted) {
			wipeOne(this.id);
			_mode = "deleted";
		}
		else {
		
			SQLiteQueryBuilder qb1 = new SQLiteQueryBuilder();
			qb1.setTables(DataAccess.service_table);
			String whereStatement = "ID = ?";
			
			Cursor srv = qsdb.query(qb1, new String[] {"service_version"}, 
					whereStatement, new String[] {String.valueOf(id)}, null, null);
			
			ContentValues save = new ContentValues(10);
			save.put("id", id);
			save.put("name", name);
			save.put("category", category);
			save.put("subcategory", subcategory);
			save.put("tags", tags);
			save.put("service_version", service_version);
			save.put("promoted", promoted);
			save.put("markup", markup);
			save.put("tag", tag);
			
			if (!srv.moveToFirst()) {
				//new entry
				qsdb.insert(DataAccess.service_table, save);
				_mode = "new";
			}
			else {
				if (srv.getInt(0) >= service_version) {
					
					//same version, no update here, how did we even get here.
				}
				else {
					qsdb.update(DataAccess.service_table, save, whereStatement, new String[] {String.valueOf(id)});
					_mode = "update";
				}
			}
			
			srv.close();
			
			QSApplication.ServiceDBInstance.addCategory(category, subcategory);
		}
		
		return timestamp;
	}
	
	public static void wipeAll() {
		DataProvider qsdb = DataProvider.getDataProvider();
		qsdb.delete(DataAccess.service_table, null, null);
	}
	
	public static void wipeOne(int id) {
		DataProvider qsdb = DataProvider.getDataProvider();
		qsdb.delete(DataAccess.service_table, "ID = ?", new String[] {String.valueOf(id)});
	}
	
	public static long initServicesFromResource() throws JSONException {
		//put a try catch after tests.
		String content = readRawTextFile(R.raw.service1, "\n");
		long maxTimeStamp = 0;
		for (String sMarkup : TextUtils.split(content, "\n")) {
			if (TextUtils.isEmpty(sMarkup)) continue;
			
			maxTimeStamp = Math.max(maxTimeStamp, (new RegisterService(sMarkup)).save());
		}
		return maxTimeStamp;
	}

	public static int lastServiceCount;
	public static int lastNewServiceCount;
	public synchronized static long initServicesFromServer(String content) throws JSONException {
		//put a try catch after tests.
		long maxTimeStamp = 0;
		int services = 0, newServices = 0;
		//first line contains timestamp
		for (String sMarkup : TextUtils.split(content, "\n")) {
			if (TextUtils.isEmpty(sMarkup)) continue;
			if (maxTimeStamp == 0) { //first line
				maxTimeStamp = Long.parseLong(sMarkup);
				continue;
			}

			RegisterService rs = new RegisterService(sMarkup);
			maxTimeStamp = Math.max(maxTimeStamp, rs.save());
			if ("new".equals(rs._mode)) newServices++;
			services++;
		}
		lastServiceCount = services;
		lastNewServiceCount = newServices;
		return maxTimeStamp;
	}
	
	public static String readRawTextFile(int resId, String newLine)
	{
	    InputStream inputStream = QSApplication.app.getResources().openRawResource(resId);

	    InputStreamReader inputreader = new InputStreamReader(inputStream);
	    BufferedReader buffreader = new BufferedReader(inputreader);
	    String line;
	    StringBuilder text = new StringBuilder();

	    try {
	        while (( line = buffreader.readLine()) != null) {
	            if (text.length() != 0) text.append(newLine);
	        	text.append(line);
	        }
	    } catch (IOException e) {
	        return null;
	    }
	    return text.toString();
	}
}