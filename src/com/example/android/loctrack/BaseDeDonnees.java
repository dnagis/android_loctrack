package com.example.android.loctrack;

import android.content.Context;
import android.util.Log;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
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

import java.io.File;
import android.os.Environment;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;


//sqlite3 /data/data/com.example.android.loctrack/databases/loc.db "select datetime(FIXTIME, 'unixepoch', 'localtime'), LAT, LONG, ACC, ALT, ALTACC, SENT from loc;"
//sqlite3 /data/data/com.example.android.loctrack/databases/loc.db "select ASYNCID, datetime(STARTTIME/1000, 'unixepoch', 'localtime'), datetime(ENDTIME/1000, 'unixepoch', 'localtime'), ENDTIME - STARTTIME, HTTPREPLY, NLOCS from net;"

public class BaseDeDonnees extends SQLiteOpenHelper {
	
	private static final String TAG = "LocTrack";

    private static final String DATABASE_NAME = "loc.db";
    private static final int DATABASE_VERSION = 1;
    //private static final String CREATE_BDD = "CREATE TABLE loc (ID INTEGER PRIMARY KEY AUTOINCREMENT, TIME INTEGER NOT NULL, CELLID INTEGER NOT NULL, MCC INTEGER NOT NULL, MNC INTEGER NOT NULL, LAC INTEGER NOT NULL, RADIO TEXT NOT NULL)";
    private static final String CREATE_BDD_MAIN = "CREATE TABLE loc (ID INTEGER PRIMARY KEY AUTOINCREMENT, FIXTIME INTEGER NOT NULL, LAT REAL NOT NULL, LONG REAL NOT NULL, ACC REAL NOT NULL, ALT REAL NOT NULL, ALTACC REAL NOT NULL, SENT INTEGER DEFAULT 0)";
    //cette table "net" sert à monitorer le network, uniquement
    private static final String CREATE_BDD_NET = "CREATE TABLE net (ID INTEGER PRIMARY KEY AUTOINCREMENT, ASYNCID INTEGER NOT NULL, STARTTIME INTEGER NOT NULL, ENDTIME INTEGER NOT NULL, HTTPREPLY INTEGER NOT NULL, NLOCS INTEGER NOT NULL, LAT REAL NOT NULL, LONG REAL NOT NULL)";
    
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
    
    public void logFix(long fixtime, double lat, double lng, float acc, double alt, float altacc){
		bdd = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("FIXTIME", fixtime);
		values.put("LAT", lat);
		values.put("LONG", lng);
		values.put("ACC", acc);
		values.put("ALT", alt);
		values.put("ALTACC", altacc);
		bdd.insert("loc", null, values);
	}
	

	public JSONArray getJsonOfLocs() {
		bdd = this.getWritableDatabase();
		
		//Récupérer le plus grand fixtime de la bdd
		long greatest_fixtime = 0;
		Cursor cursor_fxtm = bdd.query("loc", null, null, null, null, null, "FIXTIME DESC", null);
		if (cursor_fxtm != null) {
			if (cursor_fxtm.moveToNext()) greatest_fixtime = cursor_fxtm.getLong(1);
			}	
		//Log.d(TAG, "fixtimeLePlusRecent=" + greatest_fixtime);
		int age_maximum_des_fixtimes = 120; //en secondes
		long oldest_fixtime_wanted = greatest_fixtime - age_maximum_des_fixtimes;
		String[] maSelectionArgs = new String[1];
		maSelectionArgs[0] = String.valueOf(oldest_fixtime_wanted);
		
		//Query loc sent=0 et fixtime pas plus vieux que age_maximum_des_fixtimes
		Cursor cursor = bdd.query("loc", null, "SENT = 0 AND FIXTIME > ?", maSelectionArgs, null, null, null, null);
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
	
	public void deleteAll(){
			bdd = this.getWritableDatabase();
			bdd.delete("loc", null, null);
			bdd.delete("net", null, null);		
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
	
	public void logNet(int async_id, long starttime, long endtime, double lat, double lng, int nlocs, String error_code){

		int cellid = -1; //par défault
        int lac = -1;
        int mnc = -1;
        int mcc = -1;
        String radio = "unknown";
        
		bdd = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		
		//Log.d(TAG, "mabdd logNet");
		
		//Partie GSM
		//opencellid gmail, mon token = 3105a1d662ebac max 100/jour https://unwiredlabs.com/api#documentation
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
		
		Log.d(TAG, "Ma bdd logNet " + "cellid=" + cellid + "  mnc=" + mnc + "   mcc=" + mcc + "  lac=" + lac + "  radio=" + radio);
		
		//int http_result = error_code.equals("OK") ? 1 : 0;
	
		values.put("ASYNCID", async_id);
		values.put("STARTTIME", starttime);
		values.put("ENDTIME", endtime);
		values.put("HTTPREPLY", error_code.equals("OK") ? 1 : 0);		
		values.put("NLOCS", nlocs);
		values.put("LAT", lat);
		values.put("LONG", lng);
		bdd.insert("net", null, values);
	}
	
	
	//il faut aller bricoler la permission dans les paramètres pendant que l'appli tourne... ligne commande inutile
	//il faut créer le dir de destination à la mano
	//sur le motorola blanc Z play XT1635-02 aller dans fichiers et dans paramètres: "afficher mém. stock. int."
	//
	public long exporteBD() {
		Log.d(TAG, "bdd exportdb");
		long rt = 0;
		 try {
                File sd = Environment.getExternalStorageDirectory();
                File data = Environment.getDataDirectory();
				
				Log.d(TAG, "bdd exportdb sd="+sd.getPath());
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy_HHmmss");
				
				
				String currentDBPath= "/data/com.example.android.loctrack/databases/loc.db"; 
				String backupDBPath  = "/loctrack/loc"+ sdf.format(new Date()) +".db";
				File currentDB = new File(data, currentDBPath);
				File backupDB = new File(sd, backupDBPath);

				FileChannel src = new FileInputStream(currentDB).getChannel();
				FileChannel dst = new FileOutputStream(backupDB).getChannel();
				dst.transferFrom(src, 0, src.size());
				Log.d(TAG, "bdd exportdb size="+src.size());
				rt = src.size(); 
				src.close();
				dst.close();
				Log.d(TAG, "exporteBD="+backupDB.toString());
                   
            } catch (Exception e) {
				Log.d(TAG, "erreur exporteBD="+e.toString());
				rt = 0;
            }
            return rt;
	}
	
	

	
	
}
