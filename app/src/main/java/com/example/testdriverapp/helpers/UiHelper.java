package com.example.testdriverapp.helpers;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

import com.example.testdriverapp.R;
import com.example.testdriverapp.inrerfaces.IPositiveNegativeListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.LocationRequest;

public class UiHelper {

    private final Context context;

    public UiHelper(Context context) {
        this.context = context;
    }

    public boolean isPlayServicesAvailable(Context context)
    {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(context);
        return ConnectionResult.SUCCESS == status;
    }

    public boolean hasLocationPermission()
    {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public LocationRequest getLocationRequest()
    {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3000);
        return locationRequest;
    }

    public boolean isLocationProviderEnabled()
    {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void showPositiveDialogWithListener(Activity activity
            , String title
            , String content
            , final IPositiveNegativeListener positiveNegativeDialogListener
            , String positiveText, boolean cancelable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(content)
                .setTitle(title)
                .setPositiveButton(positiveText, ((dialog, which) -> positiveNegativeDialogListener.onPositive()));
        AlertDialog dialog = builder.create();
    }
}
