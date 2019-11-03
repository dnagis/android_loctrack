/**
 * 
 * 
adb uninstall com.example.android.loctrack
adb install out/target/product/mido/system/app/LocTrack/LocTrack.apk

 */

package com.example.android.loctrack;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import android.widget.Button;
import android.util.Log;




public class LocTrack_Activity extends Activity {
	


    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout for this activity.  You can find it
        // in res/layout/hello_activity.xml
        View view = getLayoutInflater().inflate(R.layout.hello_activity, null);
        setContentView(view);
        
        final Button button = findViewById(R.id.button_id);


    }
    
    public void ActionPressBouton(View v) {
		Log.d("LocTrack", "press bouton hello world");

	}
}

