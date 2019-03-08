package com.example.testdriverapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityManagerCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;


import com.example.testdriverapp.Model.Driver;
import com.example.testdriverapp.helpers.FireBaseHelper;
import com.example.testdriverapp.helpers.GoogleMapHelper;
import com.example.testdriverapp.helpers.LocationRequestHelper;
import com.example.testdriverapp.helpers.LocationResultHelper;
import com.example.testdriverapp.helpers.LocationUpdatesBroadcastReceiver;
import com.example.testdriverapp.helpers.MarkerAnimationHelper;
import com.example.testdriverapp.helpers.UiHelper;
import com.example.testdriverapp.inrerfaces.IPositiveNegativeListener;
import com.example.testdriverapp.inrerfaces.LatLngInterpolator;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;


public class MainActivity extends AppCompatActivity {

    private FloatingActionButton fabStart;
    private FloatingActionButton fabStop;
    private BroadcastReceiver broadcastReceiver;
    private FireBaseHelper fireBaseHelper;
    public static String DRIVER_ID;
    public static boolean driverOnlineFlag;
    private UiHelper uiHelper;
    private boolean isDriver;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private static final int VERIFICATION_REQUEST_CODE = 1010;
    private @Nullable Marker currentLocationMarker;
    private GoogleMapHelper googleMapHelper = new GoogleMapHelper();
    private MarkerAnimationHelper markerAnimationHelper = new MarkerAnimationHelper();
    private Marker currentPositionMarker;
    private GoogleMap googleMap;


    @Override
    protected void onResume() {
        super.onResume();

        if(broadcastReceiver == null) broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Location location = new Location("");

                location.setLatitude(Double.parseDouble(intent.getExtras().get("latitude").toString()));
                location.setLongitude(Double.parseDouble(intent.getExtras().get("longitude").toString()));

                Driver tempDriver = new Driver(location.getLatitude(), location.getLongitude(), DRIVER_ID);
                if (driverOnlineFlag) {
                    fireBaseHelper.updateDriver(tempDriver);
                    Log.e("Updated Driver ", "MainActivity");
                }

                animateCamera(location);
                showOrAnimateMarker(location);
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("location_update"));

        driverOnlineFlag = false;
        fireBaseHelper.deleteDriver();
        Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
        stopService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }

        fireBaseHelper.deleteDriver();
        driverOnlineFlag = false;
        Intent intent = new Intent(this, BackgroundService.class);
        stopService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!driverOnlineFlag){
            fireBaseHelper.deleteDriver();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fabStart = findViewById(R.id.fab_start);
        fabStop = findViewById(R.id.fab_stop);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.supportMap);

        mapFragment.getMapAsync(googleMap -> this.googleMap = googleMap);

        preferences = getPreferences(MODE_PRIVATE);

        editor = preferences.edit();

        isDriver = preferences.getBoolean("is_driver", false);

        if (!checkRuntimePermissions())
        {
            enableButton();
        }

        if (DRIVER_ID == null){
            // Give driver their unique id if they don't have one already
            setDriverId();
        }

        fireBaseHelper = new FireBaseHelper(DRIVER_ID);

        uiHelper = new UiHelper(this);

        if (!uiHelper.isPlayServicesAvailable(this))
        {
            Toast.makeText(this, "Play Services is Not Installed!", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void setDriverId() {
        boolean driverIdExists = preferences.getBoolean("has_driver_id", false);

        if (!driverIdExists) {
            int randomNum = (int) (Math.random() * 10000 + 1);
            DRIVER_ID = String.valueOf(randomNum);
            editor.putString("driver_id", DRIVER_ID);
            editor.commit();
        }
        else {
            DRIVER_ID = preferences.getString("driver_id", "0000");
            editor.putBoolean("has_driver_id", true);
            editor.commit();
        }
    }

    private void enableButton() {
        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDriver){
                    startActivityForResult
                            (ScanQrCodeActivity.getScanQrCodeActivity
                                    (getApplicationContext()), VERIFICATION_REQUEST_CODE);
                }

                driverOnlineFlag = true;
                Intent i = new Intent(getApplicationContext(), BackgroundService.class);
                startService(i);
                fabStop.setVisibility(View.VISIBLE);
                fabStart.setVisibility(View.GONE);
            }
        });

        fabStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), BackgroundService.class);
                stopService(i);
                fabStart.setVisibility(View.VISIBLE);
                fabStop.setVisibility(View.GONE);
                fireBaseHelper.deleteDriver();
            }
        });
    }

    private boolean checkRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}
            ,100);
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                enableButton();
            } else{
                checkRuntimePermissions();
            }
        }
    }

    private void switchFabs(boolean isChecked){
        if(isChecked){
            // set start button visibility to gone
            fabStart.setVisibility(View.GONE);

            // set stop button visibility to visible
            fabStop.setVisibility(View.VISIBLE);
        } else{
            // set start button visibility to gone
            fabStart.setVisibility(View.VISIBLE);

            // set stop button visibility to visible
            fabStop.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        boolean isDriverResult = false;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VERIFICATION_REQUEST_CODE && (data != null))
        {
            isDriverResult = data.getExtras().getBoolean("isDriver");
            editor.putBoolean("is_driver", isDriverResult);
            editor.commit();
        }

        if (isDriverResult)
        {
            Toast.makeText(this, R.string.driver_verified, Toast.LENGTH_LONG).show();

            // if user is verified as driver, make them online
            driverOnlineFlag = true;

            switchFabs(true);

            enableButton();
        }
        else
            Toast.makeText(this, "Driver not Verified", Toast.LENGTH_LONG).show();
    }

    private void showOrAnimateMarker(Location location)
    {
        if (currentPositionMarker == null)
            currentPositionMarker = googleMap
                    .addMarker(googleMapHelper.getDriverMarkerOptions(new LatLng(location.getLatitude(), location.getLongitude())));
        else
            MarkerAnimationHelper.animateMarkerToGB(
                    currentPositionMarker,
                    new LatLng(location.getLatitude(),
                            location.getLongitude()),
                    new LatLngInterpolator.Spherical());
    }

    private void animateCamera(Location location)
    {
        CameraUpdate cameraUpdate = googleMapHelper.buildCameraUpdate(new LatLng(location.getLatitude(), location.getLongitude()));
        googleMap.animateCamera(cameraUpdate);
    }

    public static void setDriverOnlineFlag(boolean bool){
        driverOnlineFlag = bool;
    }
}


/*
public class MainActivity extends AppCompatActivity {

    public MainActivity() {
    }

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    public static String DRIVER_ID;   // Id must be unique for every driver.


    private GoogleMap googleMap;

    // The main entry point for interacting with the fused location provider.
    private FusedLocationProviderClient locationProviderClient;

    //  data object that contains quality of service parameters for requests to the
    // FusedLocationProviderApi
    private LocationRequest locationRequest;
    private boolean locationFlag = true;
    private static boolean driverOnlineFlag = false;
    private @Nullable Marker currentLocationMarker;
    private GoogleMapHelper googleMapHelper = new GoogleMapHelper();

    private FireBaseHelper fireBaseHelper;

    private MarkerAnimationHelper markerAnimationHelper = new MarkerAnimationHelper();
    private UiHelper uiHelper;
    private Marker currentPositionMarker;

    private static final int VERIFICATION_REQUEST_CODE = 1010;

    private SharedPreferences preferences;

    SharedPreferences.Editor prefEditor;

    private TextView driverStatusTextView;

    SwitchCompat switchCompat;

    private static boolean isDriver;

    private FloatingActionButton fabStart;

    private FloatingActionButton fabStop;

    private LocationService locationService;


    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();
            if (location == null) return;
            if (locationFlag) {
                locationFlag = true;
                animateCamera(location);
            }
            if (driverOnlineFlag) {
                fireBaseHelper.updateDriver(new Driver(location.getLatitude(), location.getLongitude(), DRIVER_ID));
                Log.e("Updated Driver: ", "MainActivity");
            }
            showOrAnimateMarker(location);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.supportMap);
        assert mapFragment != null;

        uiHelper = new UiHelper(this);

        mapFragment.getMapAsync(googleMap -> this.googleMap = googleMap);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = uiHelper.getLocationRequest();

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        preferences = getPreferences(MODE_PRIVATE);

        prefEditor = preferences.edit();

        if (DRIVER_ID == null){
            // Give driver their unique id if they don't have one already
            setDriverId();
        }

        fireBaseHelper = new FireBaseHelper(DRIVER_ID);

        if (!uiHelper.isPlayServicesAvailable(this))
        {
            Toast.makeText(this, "Play Services is Not Installed!", Toast.LENGTH_SHORT).show();
            finish();
        }

        else
        {
            requestLocationUpdates();
        }

        driverStatusTextView = findViewById(R.id.driverStatusTextView);

        switchCompat = findViewById(R.id.driverStatusSwitch);

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                driverOnlineFlag = isChecked;

                // check if the switch is on
                if (isChecked)
                {
                    isDriver = preferences.getBoolean("is_driver", false);
                    // check if the user is a driver
                    if (isDriver)
                    {
                        // if user is verified as driver, make them online
                        driverOnlineFlag = true;

                        // set UI status to online
                        driverStatusTextView.setText("Online");
                    }
                    // if we don't know if the user is a driver, then start QR scan activity to check
                    else {
                        switchCompat.setChecked(false);
                        startActivityForResult(ScanQrCodeActivity.getScanQrCodeActivity(getApplicationContext()), VERIFICATION_REQUEST_CODE);
                    }

                }
                else
                {
                    driverStatusTextView.setText("Offline");
                    driverOnlineFlag = false;
                    fireBaseHelper.deleteDriver();
                }
            }
        });

        fabStart = findViewById(R.id.fab_start);

        fabStop = findViewById(R.id.fab_stop);

        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                driverOnlineFlag = true;

                isDriver = preferences.getBoolean("is_driver", false);
                // check if the user is a driver
                if (isDriver)
                {
                    // if user is verified as driver, make them online
                    driverOnlineFlag = true;

                    // set UI status to online
                    driverStatusTextView.setText("Online");

                    // set start button visibility to gone
                    fabStart.setVisibility(View.GONE);

                    // set stop button visibility to visible
                    fabStop.setVisibility(View.VISIBLE);
                }
                // if we don't know if the user is a driver, then start QR scan activity to check
                else {
                    switchFabs(false);
                    startActivityForResult(ScanQrCodeActivity.getScanQrCodeActivity(getApplicationContext()), VERIFICATION_REQUEST_CODE);
                }
            }
        });

        fabStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                driverStatusTextView.setText("Offline");
                driverOnlineFlag = false;
                fireBaseHelper.deleteDriver();

                // set start button visibility to gone
                fabStart.setVisibility(View.VISIBLE);

                // set stop button visibility to visible
                fabStop.setVisibility(View.GONE);
            }
        });

    }

    @Override
    protected void onPause()
    {
        super.onPause();

        fireBaseHelper.deleteDriver();

        if (driverOnlineFlag && isDriver)
        {
            Intent serviceIntent = new Intent(this, LocationService.class);
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        driverOnlineFlag = false;

        fireBaseHelper.deleteDriver();

        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        switchFabs(false);

        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        boolean isDriverResult = false;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VERIFICATION_REQUEST_CODE && (data != null))
        {
            isDriverResult = data.getExtras().getBoolean("isDriver");
            prefEditor.putBoolean("is_driver", isDriverResult);
            prefEditor.commit();
        }

        if (isDriverResult)
        {
            Toast.makeText(this, R.string.driver_verified, Toast.LENGTH_LONG).show();

            // if user is verified as driver, make them online
            driverOnlineFlag = true;

            // set UI status to online
            driverStatusTextView.setText("Online");

            switchFabs(true);

            switchCompat.setChecked(true);
        }
        else
            Toast.makeText(this, "Driver not Verified", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates()
    {
        if(!uiHelper.hasLocationPermission())
        {
            ActivityCompat.requestPermissions(this
                    , new String[] {Manifest.permission.ACCESS_FINE_LOCATION}
                    , MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        if (uiHelper.isLocationProviderEnabled())
        {
            //FIXME
            uiHelper.showPositiveDialogWithListener(this
                    , "Need Location"
                    , "We need access to your current Location"
                    , () -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    , "Turn On", false);
            locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }

        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void showOrAnimateMarker(Location location)
    {
        if (currentPositionMarker == null)
            currentPositionMarker = googleMap
                    .addMarker(googleMapHelper.getDriverMarkerOptions(new LatLng(location.getLatitude(), location.getLongitude())));
        else
            MarkerAnimationHelper.animateMarkerToGB(
                    currentPositionMarker,
                    new LatLng(location.getLatitude(),
                            location.getLongitude()),
                    new LatLngInterpolator.Spherical());
    }

    private void animateCamera(Location location)
    {
        CameraUpdate cameraUpdate = googleMapHelper.buildCameraUpdate(new LatLng(location.getLatitude(), location.getLongitude()));
        googleMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        {
            int value = grantResults[0];

            if (value == PackageManager.PERMISSION_DENIED)
            {
                Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
            } else if (value == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Location Permission Granted!", Toast.LENGTH_SHORT).show();
                requestLocationUpdates();
        }
    }

    private void setDriverId(){

        boolean driverIdExists = preferences.getBoolean("has_driver_id", false);

        if (!driverIdExists) {
            int randomNum = (int) (Math.random() * 10000 + 1);
            DRIVER_ID = String.valueOf(randomNum);
            prefEditor.putString("driver_id", DRIVER_ID);
            prefEditor.commit();
        }
        else {
            DRIVER_ID = preferences.getString("driver_id", "0000");
            prefEditor.putBoolean("has_driver_id", true);
            prefEditor.commit();
        }
    }

    private void switchFabs(boolean isChecked){
        if(isChecked){
            // set start button visibility to gone
            fabStart.setVisibility(View.GONE);

            // set stop button visibility to visible
            fabStop.setVisibility(View.VISIBLE);
        } else{
            // set start button visibility to gone
            fabStart.setVisibility(View.VISIBLE);

            // set stop button visibility to visible
            fabStop.setVisibility(View.GONE);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            locationService = ((LocationService.LocationServiceBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationService = null;
        }
    };

    public static String getDriverId() {
        return DRIVER_ID;
    }

    public static boolean getDriverOnlineFlag(){
        return driverOnlineFlag;
    }

    public static void setDriverOnlineFlag(boolean bool){
        driverOnlineFlag = bool;
    }

    public static boolean getIsDriver(){
        return isDriver;
    }

}
*/