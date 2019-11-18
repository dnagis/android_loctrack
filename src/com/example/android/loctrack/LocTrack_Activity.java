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
import android.util.Log;

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
	

	
    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "LocTrack_Activity onCreate");
        super.onCreate(savedInstanceState);

        // Set the layout for this activity.  You can find it res/layout/hello_activity.xml
        View view = getLayoutInflater().inflate(R.layout.hello_activity, null);
        setContentView(view);
        
        final Button btn_start = findViewById(R.id.btn_start);
        final Button btn_stop = findViewById(R.id.btn_stop);
                

        /*Au départ j'avais ça: ça lançait automatiquement dans onCreate() mais qu'une première fois, pas aux passages ultérieurs
         * marchait très bien. Après je suis passé au lancement avec un bouton.  
        https://stackoverflow.com/questions/456211/activity-restart-on-rotation-android
        if(savedInstanceState == null){
			Log.d(TAG, "savedInstanceState est null , on lance...");
            launch_le_bousin();
        }*/

        



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
		mLocationManager.removeUpdates(this);
		mAlarmManager.cancel(mAlarmSender);
		stopService(new Intent(this, ForegroundService.class));
		
		//passer SENT=2 quand SENT=0 sinon prochaine fois j'aurais des SENT=0 de la session d'avant dans mes bdd query
		maBDD.forgetUnsent(); 
		
		runningSession = false;
	}
	
	
	/**
	 *
	 * MainActivity implements LocationListener --> il faut les 4 méthodes 
	 * 
	 * 
	 **/    
    @Override	
    public void onLocationChanged(Location location) {
        //Log.d(TAG, location.getLatitude() + ",  " + location.getLongitude() + ",  " + location.getAccuracy() + ",  " + location.getAltitude() + ",  " + location.getTime());
        maBDD.logFix(location.getTime()/1000, location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getAltitude());   
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
}

