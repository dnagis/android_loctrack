

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





public class AlarmHttp extends Service {
	
	private static final String TAG = "LocTrack";
	
	public static long launchTimestamp;
	
 
    @Override
    public void onCreate() {
		Log.d(TAG, "onCreate dans AlarmHttp");			
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.d(TAG, "OnStartCommand dans AlarmHttp");				
		
		//POST Request, déporté dans AsyncTask sinon erreuur runtime android.os.NetworkOnMainThreadException
		//new PostRequestTask().execute();	
				
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
	
	

	
	
}



