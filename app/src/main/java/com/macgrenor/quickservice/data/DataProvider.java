package com.macgrenor.quickservice.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

public class DataProvider {

    private static final String DATABASE_NAME = "quickservice.db";
    private static final int DATABASE_VERSION = 1;
    
    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE 'autocomplete' ('id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'field_name' TEXT NOT NULL, 'value' TEXT NOT NULL, 'date' INT NOT NULL DEFAULT 0);");
            db.execSQL("CREATE TABLE 'service' ('id' INTEGER PRIMARY KEY NOT NULL, 'name' TEXT NOT NULL, 'category' TEXT NOT NULL, 'subcategory' TEXT NOT NULL, 'tags' TEXT NOT NULL, 'use_count' INTEGER NOT NULL DEFAULT 0 , 'bookmarked' BOOLEAN NOT NULL DEFAULT 0 , 'service_version' INTEGER NOT NULL, 'promoted' BOOLEAN NOT NULL DEFAULT 0 , 'last_use_id' INTEGER NOT NULL DEFAULT 0 , 'last_use_date' INT, 'reminder_date' INT, 'markup' TEXT NOT NULL DEFAULT '', 'tag' TEXT NOT NULL DEFAULT '');");
            db.execSQL("CREATE TABLE 'service_usage' ('id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'service_id' INTEGER NOT NULL, 'channel' TEXT NOT NULL DEFAULT '', 'date_used' INT, 'service_version' INTEGER NOT NULL DEFAULT '', 'data' TEXT NOT NULL DEFAULT '');");
            db.execSQL("CREATE TABLE 'service_sub_usage' ('id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'service_id' INTEGER NOT NULL, 'channel' TEXT NOT NULL DEFAULT '', 'date_used' INT, 'service_sub_id' TEXT NOT NULL DEFAULT '', 'data' TEXT NOT NULL DEFAULT '');");
            db.execSQL("CREATE UNIQUE INDEX unique_field_value on autocomplete('field_name', 'value');");
            db.execSQL("CREATE INDEX filter_tag on service('tag');");
            
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	
        }
    }

    private DatabaseHelper mOpenHelper;

    private DataProvider(Context cxt) {
        mOpenHelper = new DatabaseHelper(cxt);
    }
    
    private static DataProvider mDataProvider = null;
    
    public static synchronized void initDataProvider(Context cxt) {
    	mDataProvider = new DataProvider(cxt);
    }

    public static DataProvider getDataProvider() {
    	return mDataProvider;
    }

    public Cursor query(SQLiteQueryBuilder qb, String[] projection, String selection, 
    		String[] selectionArgs, String sortOrder, String limit) {        
        return query(qb, projection, selection, selectionArgs, sortOrder, limit, null, null);
    }
    
    public Cursor query(SQLiteQueryBuilder qb, String[] projection, String selection, 
    		String[] selectionArgs, String sortOrder, String limit, String groupby, String having) {        
        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupby, having, sortOrder, limit);
        return c;
    }

    public Cursor rawQuery(String statement, String[] selectionArgs) {
    	SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        return db.rawQuery(statement, selectionArgs);
    }

    public void execSQL(String statement, String[] bindArgs) {
    	SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        db.execSQL(statement, bindArgs);
    }
    
    public long insert(String tbl, ContentValues values) {     	
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();        
        return db.insertOrThrow(tbl, null, values);
    }

    public long delete(String tbl, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        return db.delete(tbl, where, whereArgs);
    }

    public int update(String tbl, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        return db.update(tbl, values, where, whereArgs);
    }
    
    public void close() {
    	mOpenHelper.close();
    }
}
