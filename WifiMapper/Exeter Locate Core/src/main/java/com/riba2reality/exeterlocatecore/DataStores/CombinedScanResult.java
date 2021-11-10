package com.riba2reality.exeterlocatecore.DataStores;

import android.location.Location;

public class CombinedScanResult {

    public long dateTime;

    //public ArrayList<WifiResult> wifiResult = new ArrayList<>();
    public WifiScanResult wifiScanResult;

    public Location location;

    public SensorResult magSensorResult;

    public SensorResult accelSensorResult;

    public String message = "None";

    public float[] matrix_R;
    public float[] matrix_I;


}// end of class CombinedScanResult
