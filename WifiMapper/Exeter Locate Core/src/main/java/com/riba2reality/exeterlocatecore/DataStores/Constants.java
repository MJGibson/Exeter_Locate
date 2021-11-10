package com.riba2reality.exeterlocatecore.DataStores;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.List;

public class Constants {

    public static final int LOCATION_SERVICE_ID = 175;

    public static final int BLE_CHANNEL_ID = 4200;

    public static final String ACTION_START_LOCATION_SERVICE = "startLocationService";
    public static final String ACTION_STOP_LOCATION_SERVICE = "stopLocationService";
    public static final String ACTION_SINGLE_SCAN = "performSingleScan";
    public static final String ACTION_POST_ALL = "postALL";
    public static final String ACTION_REQUEST_UPDATE = "requestUpdate";
    public static final String ACTION_REQUEST_GEOFENCE_UPDATE = "geoFenceUpdate";



    public static final String database = "alpha";

    //public static final String port = ":5000";
    public static final String port = "";


    public static final List<LatLng> stethamCampusPolygon = Arrays.asList(


            new LatLng(50.73907184,-3.530426842),
            new LatLng(50.73746824,-3.529541272),
            new LatLng(50.73666644,-3.529230125),
            new LatLng(50.73574496,-3.529385699),
            new LatLng(50.73536202,-3.529840451),
            new LatLng(50.73471579,-3.530438809),
            new LatLng(50.73514661,-3.531240609),
            new LatLng(50.73523038,-3.531755197),
            new LatLng(50.73497907,-3.532233884),
            new LatLng(50.73345924,-3.533023717),
            new LatLng(50.73299252,-3.533669944),
            new LatLng(50.73240613,-3.534794857),
            new LatLng(50.73225055,-3.535441084),
            new LatLng(50.73327375,-3.537284028),
            new LatLng(50.73362678,-3.537595174),
            new LatLng(50.73570009,-3.537138926),
            new LatLng(50.73613689,-3.538211483),
            new LatLng(50.73647047,-3.537898841),
            new LatLng(50.73824161,-3.538192036),
            new LatLng(50.73958119,-3.536509902),
            new LatLng(50.74010176,-3.533972863),
            new LatLng(50.73935381,-3.533009506),
            new LatLng(50.73959914,-3.531890576),
            new LatLng(50.73889906,-3.531549512),
            new LatLng(50.73907184,-3.530426842)


    );

    public static final LatLng stethamCampusCenterPoint =
            new LatLng(50.73627862858465,-3.536197682802033);



}// end of class
