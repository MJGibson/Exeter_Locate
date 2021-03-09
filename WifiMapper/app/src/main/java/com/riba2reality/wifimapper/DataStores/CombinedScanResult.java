package com.riba2reality.wifimapper.DataStores;

import android.location.Location;

public class CombinedScanResult {

    public String dateTime;

    //public ArrayList<WifiResult> wifiResult = new ArrayList<>();
    public WifiScanResult wifiScanResult;

    public Location location;

    public SensorResult magSensorResult;

    public SensorResult accelSensorResult;

    public String message = "None";

    public float[] matrix_R;
    public float[] matrix_I;


}// end of class CombinedScanResult
