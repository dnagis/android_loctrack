package com.example.android.loctrack;

import android.content.Context;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

import android.content.ContentValues;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


//sqlite3 /data/data/com.example.android.loctrack/databases/loc.db "select datetime(FIXTIME, 'unixepoch', 'localtime'), LAT, LONG, ACC, ALT, SENT from loc;"

public class BaseDeDonnees extends SQLiteOpenHelper {
	
	private static final String TAG = "LocTrack";

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
	

	public JSONArray getJsonOfLocs() {
		bdd = this.getWritableDatabase();
		Cursor cursor = bdd.query("loc", null, "SENT = 0", null, null, null, null, null);
		JSONArray jsonFinal = new JSONArray();
       
       if (cursor != null) { 
        while (cursor.moveToNext()) {
            try {
                JSONObject unJsonIntermediaire = new JSONObject();
                unJsonIntermediaire.put("id",cursor.getLong(0));
                unJsonIntermediaire.put("fixtime",cursor.getLong(1));
                unJsonIntermediaire.put("lat",cursor.getDouble(2));
                unJsonIntermediaire.put("long",cursor.getDouble(3));
                //unJsonIntermediaire.put("ACCURACY",cursor.getFloat(4));                
                //unJsonIntermediaire.put("ALTITUDE",cursor.getLong(5));
                
                //Log.d(TAG, "getJsonOfLocs: JSON intermediaire=" + unJsonIntermediaire.toString());
                jsonFinal.put(unJsonIntermediaire);
                //Log.d(TAG, "getJsonOfLocs: JSON final après put=" + jsonFinal.toString());
                unJsonIntermediaire = null;
            } catch (JSONException e) { }
        }
        
        //Log.d(TAG, "getJsonOfLocs: tronche de ton JSON Final="+jsonFinal.toString());
		}
		return jsonFinal;
	}
	
	public void markAsSent(JSONArray le_json) {
		//Log.d(TAG, "markAsSent le json = " + le_json.toString());
		int i;
		for(i=0;i<le_json.length();i++){
                try {
                    JSONObject uneLoc = new JSONObject(le_json.getString(i));
                    //Log.d(TAG, "markAsSent: Loop sur un item du JSON=" + uneLoc.toString());
                    ContentValues newValues = new ContentValues();
                    newValues.put("SENT", "1");
                    String strFilter = "ID=" + uneLoc.get("id");
                    bdd.update("loc", newValues, strFilter, null);
                } catch (JSONException e) { }
            }		
	}
	
	//passer les SENT=0 à SENT=2 (arrêt de session, pour ne pas de retrouver dans la session d'après avec des SENT=0)
	public void forgetUnsent() {
                    ContentValues newValues = new ContentValues();
                    newValues.put("SENT", "2");
                    String strFilter = "SENT=0";
                    bdd.update("loc", newValues, strFilter, null);
	}
	
	
}
