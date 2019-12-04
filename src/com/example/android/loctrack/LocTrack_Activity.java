/**
 * 
 * 
adb uninstall com.example.android.loctrack
adb install out/target/product/mido/system/app/LocTrack/LocTrack.apk

pm grant com.example.android.loctrack android.permission.ACCESS_FINE_LOCATION
 
sqlite3 /data/data/com.example.android.loctrack/databases/loc.db "select datetime(FIXTIME, 'unixepoch', 'localtime'), LAT, LONG, ACC, ALT, SENT from loc;"
 
am stop-service com.example.android.loctrack/.ForegroundService

 */

package com.example.android.loctrack;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import java.text.SimpleDateFormat;
import java.lang.System;


import android.content.Context;
import android.content.Intent;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.SystemClock;





public class LocTrack_Activity extends Activity implements LocationListener {
	
	private static final String TAG = "LocTrack";
	
	public LocationManager mLocationManager;		
	private static final int LOC_MIN_TIME = 10 * 1000; //long: minimum time interval between location updates, in milliseconds
    private static final int LOC_MIN_DIST = 0; //float: minimum distance between location updates, in meters
	private BaseDeDonnees maBDD;
	
    //en dessous de 60s: W AlarmManager: Suspiciously short interval 30000 millis; expanding to 60 seconds
    private static final long ALRM_SYNC_INTVL_MS = 60 * 1000; //60 secondes possible
    private PendingIntent mAlarmSender;
    private AlarmManager mAlarmManager;
    
    private boolean runningSession; //default to false
    private Drawable default_btn;
	private Button btn_start, btn_stop;
	TextView textview_1, textview_2;
	static TextView textview_3;
	
    /**
     * manifest attribut d'activity pour prevent passage ici quand rotation:
     * android:configChanges="orientation|screenLayout|screenSize"
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "LocTrack_Activity onCreate");
        super.onCreate(savedInstanceState);

        // Set the layout for this activity.  You can find it res/layout/hello_activity.xml
        View view = getLayoutInflater().inflate(R.layout.loctrack_activity, null);
        setContentView(view);
        
        btn_start = findViewById(R.id.btn_start);
        default_btn = btn_start.getBackground();
        btn_stop = findViewById(R.id.btn_stop);
        textview_1 = findViewById(R.id.textview_1);       
        textview_2 = findViewById(R.id.textview_2); 
        textview_3 = findViewById(R.id.textview_3);
 

        /*Au départ j'avais ça: ça lançait automatiquement dans onCreate() mais qu'une première fois, pas aux passages ultérieurs
         * marchait très bien. Après je suis passé au lancement avec un bouton.  
        https://stackoverflow.com/questions/456211/activity-restart-on-rotation-android
        if(savedInstanceState == null){
			Log.d(TAG, "savedInstanceState est null , on lance...");
            launch_le_bousin();
        }*/
    }
    
    /*@Override
    public void onResume() {
		super.onResume();
	}*/
	
	/**Oui: cette méthode qui update une TextView est appelée depuis LocTrackAlarm. Le mot clé c'est static, et l'astuce c'est que la textview
	 * à update doit être static elle aussi! **/
	public static void updateSent(long last_succesful_send) {
		//Log.d(TAG, "onResume dernier succesful send = " + LocTrackAlarm.last_succesful_send);
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm:ss");	
		textview_3.setText("LAST SENT: "+ sdf.format(last_succesful_send));
	}
	
	
	
    public void launch_le_bousin() {
		
		// Si c'est la première fois qu'on passe on récup une handle vers le location manager
		if(mLocationManager == null) mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOC_MIN_TIME, LOC_MIN_DIST, this);
		
		maBDD = new BaseDeDonnees(this);
		
		/**Create a PendingIntent to trigger a startService()
		 * Attention mec! le nom de la classe à démarrer doit être déclarée dans le manifest.xml! <service android:name=".NomDeLaClasse" />
		 * L'AlrmManager ne se fend pas d'une erreur s'il ne trouve pas la classe!
		 ***/ 
        mAlarmSender = PendingIntent.getService(  // set up an intent for a call to a service (voir dev guide intents à "Using a pending intent")
            this,  // the current context
            0,  // request code (not used)
            new Intent (this, LocTrackAlarm.class),  // A new Service intent 'c'est un intent explicite'
            0   // flags (none are required for a service)
        );
        
        // Si c'est la première fois qu'on passe on récup une handle vers l alarm manager
        if(mAlarmManager == null) mAlarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);        
        long firstAlarmTime = SystemClock.elapsedRealtime();        
        mAlarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, // based on time since last wake up
                firstAlarmTime,  // sends the first alarm immediately
                ALRM_SYNC_INTVL_MS,  // repeats every XX
                mAlarmSender  // when the alarm goes off, sends this Intent
            );	
		
		/**foreground service pour importance (am package-importance com.example.android.hellogps) à 125
		ne pas oublier l'entrée <service android:name=".ForegroundService" /> dans le manifest**/
		startForegroundService(new Intent(this, ForegroundService.class));
		
		runningSession = true;
		btn_start.setBackgroundColor(Color.BLUE);
		
	}
	
	//bouton start
    public void ActionPressBouton_start(View v) {
		Log.d(TAG, "press bouton start");
		if (!runningSession) launch_le_bousin();
	}
    
    //bouton stop
    public void ActionPressBouton_stop(View v) {
		Log.d(TAG, "press bouton stop");
		//On arrête tout le background...
		if(mLocationManager != null) mLocationManager.removeUpdates(this);
		if(mAlarmManager != null) mAlarmManager.cancel(mAlarmSender);
		stopService(new Intent(this, ForegroundService.class));
		
		//passer SENT=2 quand SENT=0 sinon prochaine fois j'aurais des SENT=0 de la session d'avant dans mes bdd query
		if(maBDD != null) maBDD.forgetUnsent(); 
		
		runningSession = false;
		btn_start.setBackgroundDrawable(default_btn);
	}
	    
	//bouton exportdb
    public void ActionPressBouton_exportdb(View v) {
		//Log.d(TAG, "press bouton exportdb");
		if(maBDD != null) maBDD.exporteBD();
		}
	

	
	/**
	 *
	 * MainActivity implements LocationListener --> il faut les 4 méthodes 
	 * 
	 * 
	 **/    
    @Override	
    public void onLocationChanged(Location location) {
        Log.d(TAG, location.getLatitude() + ",  " + location.getLongitude() + ",  " + location.getAccuracy() + ",  " + location.getAltitude() + ",  " + location.getVerticalAccuracyMeters() + ",  "  + location.getTime());
        maBDD.logFix(location.getTime()/1000, location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getAltitude(), location.getVerticalAccuracyMeters());   
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm:ss");
        textview_1.setText("LAST LOC: "+ sdf.format(location.getTime()));
        textview_2.setText(""+location.getLatitude()+" , "+location.getLongitude()+"\n"+getFormattedLocationInDegree(location.getLatitude(), location.getLongitude()));
    }
        
	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
	public static String getFormattedLocationInDegree(double latitude, double longitude) {
        int latSeconds = (int) Math.round(latitude * 3600);
        int latDegrees = latSeconds / 3600;
        latSeconds = Math.abs(latSeconds % 3600);
        int latMinutes = latSeconds / 60;
        latSeconds %= 60;

        int longSeconds = (int) Math.round(longitude * 3600);
        int longDegrees = longSeconds / 3600;
        longSeconds = Math.abs(longSeconds % 3600);
        int longMinutes = longSeconds / 60;
        longSeconds %= 60;
        String latDegree = latDegrees >= 0 ? "N" : "S";
        String lonDegrees = longDegrees >= 0 ? "E" : "W";

        return  Math.abs(latDegrees) + "°" + latMinutes + "'" + latSeconds
                + "\"" + latDegree +" "+ Math.abs(longDegrees) + "°" + longMinutes
                + "'" + longSeconds + "\"" + lonDegrees;

	}
}

