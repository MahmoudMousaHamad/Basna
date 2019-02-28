package com.example.testdriverapp.Model;

public class Driver {

    private double lat;
    private double lng;
    private String driverId;

    public Driver(double _lat, double _lng)
    {
        lat = _lat;
        lng = _lng;
        driverId = "0000";
    }

    public Driver(double _lat, double _lng, String _driverId){
        lat = _lat;
        lng = _lng;
        driverId = _driverId;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }
}
