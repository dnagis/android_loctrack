package com.example.android.loctrack;

import android.content.Context;
import android.util.Log;
import java.util.List;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

import android.content.ContentValues;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.telephony.SignalStrength;


//sqlite3 /data/data/com.example.android.loctrack/databases/loc.db "select datetime(FIXTIME, 'unixepoch', 'localtime'), LAT, LONG, ACC, ALT, SENT from loc;"
//sqlite3 /data/data/com.example.android.loctrack/databases/loc.db "select datetime(STARTTIME/1000, 'unixepoch', 'localtime'), datetime(ENDTIME/1000, 'unixepoch', 'localtime'), ENDTIME - STARTTIME, NLOCS from net;"

public class BaseDeDonnees extends SQLiteOpenHelper {
	
	private static final String TAG = "LocTrack";

    private static final String DATABASE_NAME = "loc.db";
    private static final int DATABASE_VERSION = 1;
    //private static final String CREATE_BDD = "CREATE TABLE loc (ID INTEGER PRIMARY KEY AUTOINCREMENT, TIME INTEGER NOT NULL, CELLID INTEGER NOT NULL, MCC INTEGER NOT NULL, MNC INTEGER NOT NULL, LAC INTEGER NOT NULL, RADIO TEXT NOT NULL)";
    private static final String CREATE_BDD_MAIN = "CREATE TABLE loc (ID INTEGER PRIMARY KEY AUTOINCREMENT, FIXTIME INTEGER NOT NULL, LAT REAL NOT NULL, LONG REAL NOT NULL, ACC REAL NOT NULL, ALT REAL NOT NULL, SENT INTEGER DEFAULT 0)";
    private static final String CREATE_BDD_NET = "CREATE TABLE net (ID INTEGER PRIMARY KEY AUTOINCREMENT, STARTTIME INTEGER NOT NULL, ENDTIME INTEGER NOT NULL, NLOCS INTEGER NOT NULL, LAT REAL NOT NULL, LONG REAL NOT NULL)";
    
    private SQLiteDatabase bdd;
    private TelephonyManager telphMgr;

    public BaseDeDonnees(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        telphMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BDD_MAIN);
        db.execSQL(CREATE_BDD_NET);
        
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
             if (bdd != null)  { //au début avant les premières location: bdd pas encore remplies, si on lance stop ici: ça plante                  
                    ContentValues newValues = new ContentValues();
                    newValues.put("SENT", "2");
                    String strFilter = "SENT=0";
                    bdd.update("loc", newValues, strFilter, null);
				}
	}
	
	public void logNet(long starttime, long endtime, JSONArray le_json){
		double lat = 0.0 , lng = 0.0;

		int cellid = -1; //par défault
        int lac = -1;
        int mnc = -1;
        int mcc = -1;
        String radio = "unknown";
        
		bdd = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		
		//Log.d(TAG, "mabdd logNet");
		
		//Partie GSM
		//opencellid mon token 3105a1d662ebac 100/jour max https://unwiredlabs.com/api#documentation
		//SignalStrength forceSignal = telphMgr.getSignalStrength(); //api 29 only...		
	    List<CellInfo> cellinfo = telphMgr.getAllCellInfo();
	   //dans le métro quand cartes sims activées mais aucun signal ça plante... check (cellinfo != null)-pas suffisant finalement size marche bien
		if (cellinfo.size() > 0) {
			CellInfo cell0 = cellinfo.get(0);

			if (cell0 instanceof CellInfoGsm) {
				cellid = ((CellInfoGsm) cell0).getCellIdentity().getCid();
				lac = ((CellInfoGsm) cell0).getCellIdentity().getLac();
				mnc = ((CellInfoGsm) cell0).getCellIdentity().getMnc();
				mcc = ((CellInfoGsm) cell0).getCellIdentity().getMcc();
				radio = "GSM";
			} else if (cell0 instanceof CellInfoCdma) { //2g ??
				cellid = ((CellInfoCdma) cell0).getCellIdentity().getBasestationId();
				radio = "CDMA";
			} else if (cell0 instanceof CellInfoLte) { //4G??
				cellid = ((CellInfoLte) cell0).getCellIdentity().getCi();
				mnc = ((CellInfoLte) cell0).getCellIdentity().getMnc();
				mcc = ((CellInfoLte) cell0).getCellIdentity().getMcc();
				lac = ((CellInfoLte) cell0).getCellIdentity().getTac();
				radio = "LTE";
			} else if (cell0 instanceof CellInfoWcdma) { //3G? UMTS?
				cellid = ((CellInfoWcdma) cell0).getCellIdentity().getCid();
				lac = ((CellInfoWcdma) cell0).getCellIdentity().getLac();
				mnc = ((CellInfoWcdma) cell0).getCellIdentity().getMnc();
				mcc = ((CellInfoWcdma) cell0).getCellIdentity().getMcc();
				radio = "UMTS";
			}

		} else {
			cellid = 0; //code pour se rappeler que cellid est vide (genre métro)
		}
		
		Log.d(TAG, "mabdd logNet " + "cellid=" + cellid + "  mnc=" + mnc + "   mcc=" + mcc + "  lac=" + lac + "  radio=" + radio);
		
		
		
		int index = idx_of_biggest_fixtime(le_json);
		
		try {
			JSONObject locMaxFixtime = le_json.getJSONObject(index);		
			lat = locMaxFixtime.getDouble("lat");
			lng = locMaxFixtime.getDouble("long");
		} catch (JSONException e) { }
		
		
		values.put("STARTTIME", starttime);
		values.put("ENDTIME", endtime);
		values.put("NLOCS", le_json.length());
		values.put("LAT", lat);
		values.put("LONG", lng);
		bdd.insert("net", null, values);
	}
	
	
	/*Quel est l'index dans le JSONArray de la loc avec le plus grand fixtime (donc le plus récent au moment de l'envoi, pour savoir où ça s'est passé*/
	public int idx_of_biggest_fixtime(JSONArray le_json) {
			int index_biggest_fixtime = 0;
			long max_fixtime = 0;
			
			//Log.d(TAG, "mabdd idx_of_biggest_fixtime");
			
			for (int i=0 ; i<le_json.length() ; i++) {
				try {
					JSONObject oneItem = le_json.getJSONObject(i);
					long current_fixtime = oneItem.getLong("fixtime");
					if (current_fixtime > max_fixtime) 
						{	
							max_fixtime = current_fixtime;
							index_biggest_fixtime = i;
						}
					} catch (JSONException e) { }
			}
			return index_biggest_fixtime;			
	}
	
	
}
