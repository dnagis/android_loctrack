package com.example.android.loctrack;

import android.app.Service;
import android.util.Log;
import android.os.IBinder;
import android.content.Intent;
//DownloadManager
import android.app.DownloadManager;
import android.net.Uri;
//Broadcast Receiver
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import android.os.Environment;
import java.io.File;

import android.os.AsyncTask;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.database.sqlite.SQLiteDatabase;







public class LocTrackAlarm extends Service {
	
	private static final String TAG = "LocTrack";
	
	public static long launchTimestamp;
	
	private BaseDeDonnees maBDD;
	
	private int async_id; //identifier each asynctask
	

	
 
    @Override
    public void onCreate() {
		Log.d(TAG, "onCreate dans LocTrackAlarm");	
		maBDD = new BaseDeDonnees(this);
		async_id = 0;	
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.d(TAG, "OnStartCommand dans LocTrackAlarm");
		
		//SQLiteDatabase bdd = maBDD.getWritableDatabase();	
		
		JSONArray leJson = maBDD.getJsonOfLocs();		
		
		//POST Request, déporté dans AsyncTask sinon erreur runtime android.os.NetworkOnMainThreadException
		if (leJson.length() != 0) {
			async_id++;
			new PostRequestTask().execute(leJson);	
		}
				
		return START_NOT_STICKY;
	}

    @Override
    public void onDestroy() {		
		Log.d(TAG, "OnDestroy");
		stopSelf();		
	 }
	 
	  @Override
	public IBinder onBind(Intent intent) {
      // We don't provide binding, so return null
      return null;
	}
	
	//AsyncTask<JSONArray,Void,String> --> Le premier: JSONArray: ce qui est passé à doInBackground le dernier: String: ce qui est passé à onPostExecute 
	private class PostRequestTask extends AsyncTask<JSONArray,Void,JSONArray> {
		//https://alvinalexander.com/android/asynctask-examples-parameters-callbacks-executing-canceling
		long startTime, endTime;
		double lat, lng;
		String error_code = "HTTP_REPLY_NON_INITIALISEE";
		
	    @Override
	    protected JSONArray doInBackground(JSONArray... params) {
			
			JSONArray mon_json = params[0];
			startTime = System.currentTimeMillis();
			Log.d(TAG, "PostRequestTask doInBackground le json qu'on send= " + mon_json.toString());
			
			
			try {	
			URL url = new URL("http://5.135.183.126:8050/post_loc");
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setDoOutput(true);
			urlConnection.setConnectTimeout(10000); //ms https://stackoverflow.com/questions/2799938/httpurlconnection-timeout-settings 
			urlConnection.setReadTimeout(10000); //https://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#setReadTimeout(int)
			urlConnection.setRequestMethod("POST");
			urlConnection.setRequestProperty("Content-Type","application/json");
			urlConnection.setRequestProperty("charset", "utf-8");
			DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
			//out.write(jsonEnvoi.toString().getBytes("iso-8859-15"));
//			String mon_json = "{\"lat\":\"" + lastLat + "\",\"long\":\"" + lastLong + "\",\"fixtime\":\"" +   fixtime/1000 +          "\"}";
//			String mon_json = "{\"lat\":\"43.1111\",\"long\":\"3.1111\",\"fixtime\":\"1573289999\"}";
			
			
			out.writeBytes(mon_json.toString());
			out.flush();
			out.close();
			error_code = urlConnection.getResponseMessage();
			//Log.d(TAG, "Vincent reponse getResponseMessage= " + error_code);	
	
	        } catch (MalformedURLException me) {
	            Log.d(TAG, "MalformedURLException: " + me);
	
	        } catch (IOException ioe) {
	            Log.d(TAG, "IOException: " + ioe);
	        }
	        
	        //on récup la dernière latlng pour monitorer le network
	        int index = idx_of_biggest_fixtime(mon_json);
			try {
				JSONObject locMaxFixtime = mon_json.getJSONObject(index);		
				lat = locMaxFixtime.getDouble("lat");
				lng = locMaxFixtime.getDouble("long");
			} catch (JSONException e) { }
		        
	        if (error_code.equals("OK")) {
				return mon_json;
			} else { 
				JSONArray un_json_vide = new JSONArray();
				return un_json_vide;
			}
	    }
	    
	    //récupère ce qui est returned par doInBackground
	    @Override
	    protected void onPostExecute(JSONArray le_json)	{
			//Log.d(TAG, "onPostExecute -- le_json returned par doInBackground = " + le_json.toString());
			endTime = System.currentTimeMillis();
			maBDD.logNet(async_id, startTime, endTime, lat, lng, le_json.length(), error_code);
			//Log.d(TAG, "PostRequestTask startTime=" + startTime + "   endTime=" + endTime);
			if (le_json.length() != 0) {
				maBDD.markAsSent(le_json);	
				LocTrack_Activity.updateSent(endTime);
			}
		}
	}
	
	/*Quel est l'index dans le JSONArray de la loc avec le plus grand fixtime (donc le plus récent au moment de l'envoi, pour savoir où ça s'est passé, pour monitorer le network*/
	public int idx_of_biggest_fixtime(JSONArray le_json) {
			int index_biggest_fixtime = 0;
			long max_fixtime = 0;
			
			//Log.d(TAG, "idx_of_biggest_fixtime");
			
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



