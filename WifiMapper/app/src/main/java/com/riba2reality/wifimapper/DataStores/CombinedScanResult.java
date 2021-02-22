package com.riba2reality.wifimapper.DataStores;

import android.location.Location;

import java.util.ArrayList;

public class CombinedScanResult {

    public String dateTime;

    //public ArrayList<WifiResult> wifiResult = new ArrayList<>();
    public WifiScanResult wifiScanResult;

    public Location location;

    public MagSensorResult magSensorResult;


}// end of class CombinedScanResult
