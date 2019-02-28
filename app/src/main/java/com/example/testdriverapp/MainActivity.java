package com.example.testdriverapp;

import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.Nullable;
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
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;


import com.example.testdriverapp.Model.Driver;
import com.example.testdriverapp.helpers.FireBaseHelper;
import com.example.testdriverapp.helpers.GoogleMapHelper;
import com.example.testdriverapp.helpers.MarkerAnimationHelper;
import com.example.testdriverapp.helpers.UiHelper;
import com.example.testdriverapp.inrerfaces.IPositiveNegativeListener;
import com.example.testdriverapp.inrerfaces.LatLngInterpolator;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import com.google.android.gms.maps.GoogleMap;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public MainActivity() {
    }

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String DRIVER_ID = UUID.randomUUID().toString();   // Id must be unique for every driver.


    private GoogleMap googleMap;
    // The main entry point for interacting with the fused location provider.
    private FusedLocationProviderClient locationProviderClient;
    //  data object that contains quality of service parameters for requests to the
    // FusedLocationProviderApi
    private LocationRequest locationRequest;
    private boolean locationFlag = true;
    private boolean driverOnlineFlag = false;
    private @Nullable Marker currentLocationMarker;
    private GoogleMapHelper googleMapHelper = new GoogleMapHelper();
    private FireBaseHelper fireBaseHelper = new FireBaseHelper(DRIVER_ID);
    private MarkerAnimationHelper markerAnimationHelper = new MarkerAnimationHelper();
    private UiHelper uiHelper;
    private Marker currentPositionMarker;



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
            if (driverOnlineFlag)
                fireBaseHelper.updateDriver(new Driver(location.getLatitude(), location.getLongitude(), DRIVER_ID));
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

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = uiHelper.getLocationRequest();

        if (!uiHelper.isPlayServicesAvailable(this))
        {
            Toast.makeText(this, "Play Services is Not Installed!", Toast.LENGTH_SHORT).show();
            finish();
        }

        else
        {
            requestLocationUpdates();
        }

        final TextView driverStatusTextView = findViewById(R.id.driverStatusTextView);

        SwitchCompat switchCompat = findViewById(R.id.driverStatusSwitch);
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                driverOnlineFlag = isChecked;
                if (driverOnlineFlag)
                    driverStatusTextView.setText("Online");
                else
                {
                    driverStatusTextView.setText("Offline");
                    fireBaseHelper.deleteDriver();
                }
            }
        });
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

}
