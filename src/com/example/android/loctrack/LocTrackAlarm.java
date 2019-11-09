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
		
		//int unsent_rows_n = maBDD.get_number_of_rows();			
		//Log.d(TAG, "nombre d'unsent rows = " + unsent_rows_n);	
		
		//maBDD.dummy_get_rows();
		
		JSONObject unJson = maBDD.dummy_get_rows();
		
		
		//POST Request, déporté dans AsyncTask sinon erreuur runtime android.os.NetworkOnMainThreadException
		new PostRequestTask().execute(unJson);	
				
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
	
	
	private class PostRequestTask extends AsyncTask<JSONObject,Void,Void> {
		//https://alvinalexander.com/android/asynctask-examples-parameters-callbacks-executing-canceling
    
	    @Override
	    protected Void doInBackground(JSONObject... params) {
			String error_code = "HTTP_REPLY_NON_INITIALISEE";
			
			
			try {	
			URL url = new URL("http://5.135.183.126:8050/post_loc");
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setDoOutput(true);
			urlConnection.setRequestMethod("POST");
			urlConnection.setRequestProperty("Content-Type","application/json");
			urlConnection.setRequestProperty("charset", "utf-8");
			DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
			//out.write(jsonEnvoi.toString().getBytes("iso-8859-15"));
//			String mon_json = "{\"lat\":\"" + lastLat + "\",\"long\":\"" + lastLong + "\",\"fixtime\":\"" +   fixtime/1000 +          "\"}";
//			String mon_json = "{\"lat\":\"43.1111\",\"long\":\"3.1111\",\"fixtime\":\"1573289999\"}";
			JSONObject mon_json = params[0];
			Log.d(TAG, "le json qu'on send= " + mon_json.toString());
			out.writeBytes(mon_json.toString());
			out.flush();
			out.close();
			error_code = urlConnection.getResponseMessage();
			Log.d(TAG, "Vincent reponse getResponseMessage= " + error_code);	
	
	        } catch (MalformedURLException me) {
	            Log.d(TAG, "MalformedURLException: " + me);
	
	        } catch (IOException ioe) {
	            Log.d(TAG, "IOException: " + ioe);
	        }
			return null;
	    }

	}
	
	

	
	
}



