package com.example.testdriverapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.testdriverapp.Model.Driver;
import com.example.testdriverapp.helpers.FireBaseHelper;
import com.example.testdriverapp.helpers.UiHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.example.testdriverapp.MainActivity;

import java.util.UUID;

public class LocationService extends Service {

    public static final String CHANNEL_ID = "location_notification_channel_id";
    public static FusedLocationProviderClient locationProviderClient;
    public static UiHelper uiHelper;
    public static LocationRequest locationRequest;
    public static FireBaseHelper fireBaseHelper;
    public static boolean driverOnlineFlag = false;
    public static final int LOCATION_PENDING_INTENT_REQUEST_CODE = 1209;
    public static final int STICKY_LOCATION_NOTIFICATION_ID = 345;

    private final LocationServiceBinder binder = new LocationServiceBinder();


    public static LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            Location location = locationResult.getLastLocation();

            if (location == null) return;

            if (driverOnlineFlag){
                //FIXME
//                fireBaseHelper.updateDriver(new
//                        Driver(location.getLatitude()
//                        , location.getLongitude()
//                        , MainActivity.DRIVER_ID));
//                Log.e("Updated Driver: ", "LocationService");
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        uiHelper = new UiHelper(this);

        locationRequest = uiHelper.getLocationRequest();

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //FIXME
        //fireBaseHelper = new FireBaseHelper(MainActivity.DRIVER_ID);

        if (!uiHelper.isPlayServicesAvailable(this))
        {
            Toast.makeText(this, "Play Services is Not Installed!", Toast.LENGTH_LONG).show();
            stopForeground(true);
        }

        else
        {
            requestLocationUpdates();
        }

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

        requestLocationUpdates();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //FIXME
        //MainActivity.setDriverOnlineFlag(false);
        fireBaseHelper.deleteDriver();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent){
        return binder;
    }


    @SuppressLint("MissingPermission")
    public void requestLocationUpdates()
    {
        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    public class LocationServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

}
