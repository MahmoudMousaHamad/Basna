package com.example.testdriverapp.helpers;

import android.util.Log;

import com.example.testdriverapp.Model.Driver;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FireBaseHelper {

    private final String ONLINE_DRIVERS = "online_drivers";

    private DatabaseReference onlineDriverDatabaseReference;

    public FireBaseHelper(String driverId)
    {
        onlineDriverDatabaseReference = FirebaseDatabase
                .getInstance()
                .getReference()
                .child(ONLINE_DRIVERS)
                .child(driverId);
        onlineDriverDatabaseReference
                .onDisconnect()
                .removeValue();
    }

    public void updateDriver(Driver driver)
    {
        onlineDriverDatabaseReference
                .setValue(driver);
        Log.e("Driver info", "Updated");
    }

    public void deleteDriver()
    {
        onlineDriverDatabaseReference
                .removeValue();
    }


}
