package com.example.android.loctrack;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

import android.content.ContentValues;


//sqlite3 /data/data/com.example.android.loctrack/databases/loc.db "select datetime(FIXTIME, 'unixepoch', 'localtime'), LAT, LONG, ACC, ALT, SENT from loc;"

public class BaseDeDonnees extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "loc.db";
    private static final int DATABASE_VERSION = 1;
    //private static final String CREATE_BDD = "CREATE TABLE loc (ID INTEGER PRIMARY KEY AUTOINCREMENT, TIME INTEGER NOT NULL, CELLID INTEGER NOT NULL, MCC INTEGER NOT NULL, MNC INTEGER NOT NULL, LAC INTEGER NOT NULL, RADIO TEXT NOT NULL)";
    private static final String CREATE_BDD = "CREATE TABLE loc (ID INTEGER PRIMARY KEY AUTOINCREMENT, FIXTIME INTEGER NOT NULL, LAT REAL NOT NULL, LONG REAL NOT NULL, ACC REAL NOT NULL, ALT REAL NOT NULL, SENT INTEGER DEFAULT 0)";
    
    private SQLiteDatabase bdd;

    public BaseDeDonnees(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BDD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
    
    public void logFix(long fixtime, double lat, double lng, float acc, double alt){
		bdd = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("FIXTIME", fixtime);
		values.put("LAT", lat);
		values.put("LONG", lng);
		values.put("ACC", acc);
		values.put("ALT", alt);
		bdd.insert("loc", null, values);
	}
	
	public int get_number_of_rows() {
		bdd = this.getWritableDatabase();
		Cursor cursor = bdd.query("loc", null, "SENT = 0", null, null, null, null);
		return cursor.getCount();
	}
}
