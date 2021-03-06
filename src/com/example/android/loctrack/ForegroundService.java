package com.example.android.loctrack;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;



import android.util.Log;


public class ForegroundService extends Service {
	
	private static final String TAG = "LocTrack";
	
	Notification mNotification;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    
    @Override
    public void onCreate() {
		//Log.d(TAG, "ForegroundService onCreate");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "ForegroundService onStartCommand");
		
		//https://developer.android.com/training/notify-user/channels
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        String CHANNEL_ID = "LA_CHAN_ID";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "ma_channel", importance);
        channel.setDescription("android_fait_chier_avec_sa_channel");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
		
		// Build the notification object.
        mNotification = new Notification.Builder(this, CHANNEL_ID)  //  The builder requires the context
                .setSmallIcon(R.drawable.icon)  // the status icon
                .setTicker("NotifText")  // the status text
                .setContentTitle("com.example.android.loctrack")  // the label of the entry
                .setContentText("LocTrack")  // the contents of the entry
                .build();
		
		
        startForeground(1001,mNotification);
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
    }
    
    
    
    
}
