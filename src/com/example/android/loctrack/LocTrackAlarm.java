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
	
 
    @Override
    public void onCreate() {
		Log.d(TAG, "onCreate dans LocTrackAlarm");	
		maBDD = new BaseDeDonnees(this);		
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.d(TAG, "OnStartCommand dans LocTrackAlarm");
		
		//SQLiteDatabase bdd = maBDD.getWritableDatabase();	
		
		JSONArray leJson = maBDD.getJsonOfLocs();		
		
		//POST Request, déporté dans AsyncTask sinon erreuur runtime android.os.NetworkOnMainThreadException
		new PostRequestTask().execute(leJson);	
				
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
		
    
	    @Override
	    protected JSONArray doInBackground(JSONArray... params) {
			String error_code = "HTTP_REPLY_NON_INITIALISEE";
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
	        
	        if (error_code.equals("OK")) {
				return mon_json;
			} else { 
				JSONArray un_json_vide = new JSONArray();
				return un_json_vide;
			}
	    }
	    
	    @Override
	    protected void onPostExecute(JSONArray le_json)	{
			//Log.d(TAG, "onPostExecute -- le_json passé = " + le_json.toString());
			endTime = System.currentTimeMillis();
			//Log.d(TAG, "PostRequestTask startTime=" + startTime + "   endTime=" + endTime);
			if (le_json.length() != 0) 
				{
					maBDD.markAsSent(le_json);
					maBDD.logNet(startTime, endTime, le_json);
					
				}
		}
	}
	

	
	
	

	
	
}



