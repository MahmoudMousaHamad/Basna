package com.example.testdriverapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.testdriverapp.helpers.FireBaseHelper;
import com.example.testdriverapp.helpers.UiHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class BackgroundService extends Service {

    private LocationListener locationListener;
    private LocationManager locationManager;
    public static final int LOCATION_PENDING_INTENT_REQUEST_CODE = 1209;
    public static final int STICKY_LOCATION_NOTIFICATION_ID = 345;
    public static final String CHANNEL_ID = "location_notification_channel_id";
    private FusedLocationProviderClient locationProviderClient;
    private LocationRequest locationRequest;
    private UiHelper uiHelper;
    private FireBaseHelper fireBaseHelper = new FireBaseHelper(MainActivity.DRIVER_ID);

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();

            if (location == null)
                return;

            Intent i = new Intent("location_update");

            i.putExtra("longitude", location.getLongitude());
            i.putExtra("latitude", location.getLatitude());

            sendBroadcast(i);

            Log.i("Location Changed", "Service");
            }
        };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        uiHelper = new UiHelper(this);

        locationRequest = uiHelper.getLocationRequest();

        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            notificationManager.createNotificationChannel(serviceChannel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                LOCATION_PENDING_INTENT_REQUEST_CODE, notificationIntent, 0);

        Notification stickyNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location tracking is on!")
                .setContentText("Tap to stop sharing your location")
                .setSmallIcon(R.drawable.driver_icon)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(STICKY_LOCATION_NOTIFICATION_ID, stickyNotification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (locationManager != null){
            locationManager.removeUpdates(locationListener);
        }
        MainActivity.setDriverOnlineFlag(false);
        fireBaseHelper.deleteDriver();
        stopSelf();
    }
}