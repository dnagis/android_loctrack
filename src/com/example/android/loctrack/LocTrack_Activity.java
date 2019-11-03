/**
 * 
 * 
adb uninstall com.example.android.loctrack
adb install out/target/product/mido/system/app/LocTrack/LocTrack.apk


pm grant com.example.android.loctrack android.permission.ACCESS_FINE_LOCATION

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



public class LocTrack_Activity extends Activity implements LocationListener {
	
	public LocationManager mLocationManager;		
	private static final int MIN_TIME = 10 * 1000; //long: minimum time interval between location updates, in milliseconds
    private static final int MIN_DIST = 0; //float: minimum distance between location updates, in meters

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout for this activity.  You can find it res/layout/hello_activity.xml
        View view = getLayoutInflater().inflate(R.layout.hello_activity, null);
        setContentView(view);
        
        final Button button = findViewById(R.id.button_id);
        
        
        
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, this);
		
		
		//foreground service pour importance (am package-importance com.example.android.hellogps) à 125
		startForegroundService(new Intent(this, ForegroundService.class));


    }
    
    public void ActionPressBouton(View v) {
		Log.d("LocTrack", "press bouton");
	}
	
	
		/**
	 *
	 * MainActivity implements LocationListener --> il faut les 4 méthodes 
	 * 
	 * 
	 **/    
    @Override	
    public void onLocationChanged(Location location) {
        Log.d("LocTrack", location.getLatitude() + ",  " + location.getLongitude() + ",  " + location.getAccuracy() + ",  " + location.getAltitude() + ",  " + location.getTime());
        //maBDD.logFix(location.getTime()/1000, location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getAltitude());   
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

