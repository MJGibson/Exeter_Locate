package com.riba2reality.exeterlocatecore.DataStores;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {

    public static final int LOCATION_SERVICE_ID = 175;

    public static final int BLE_CHANNEL_ID = 4200;

    public static final String ACTION_START_LOCATION_SERVICE = "startLocationService";
    public static final String ACTION_STOP_LOCATION_SERVICE = "stopLocationService";
    public static final String ACTION_SINGLE_SCAN = "performSingleScan";
    public static final String ACTION_POST_ALL = "postALL";
    public static final String ACTION_REQUEST_UPDATE = "requestUpdate";
    public static final String ACTION_REQUEST_GEOFENCE_UPDATE = "geoFenceUpdate";


    public static final String ACTION_TERMS_ACCEPTED = "termsAccept";



    public static final String database = "beta";
    public static final String port = "";
    public static final String address = "api.riba2reality.com";
    public static final boolean useSSL = true;

    // Test settings
    //public static final String database = "devTest";
    //public static final String port = ":5000";
    //public static final String address = "10.0.2.2";
    //public static final boolean useSSL = false;


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


    public static final Map<String, LatLng> Fiducal_locations;
    static {

        Map<String, LatLng> aMap = new HashMap<>();


        aMap.put("R", new LatLng(50.738424,-3.5317571));
        aMap.put("C", new LatLng(50.73768997,-3.530740284));
        aMap.put("T", new LatLng(50.73730422,-3.531939122));
        aMap.put("B", new LatLng(50.7371656,-3.533398));
        aMap.put("F", new LatLng(50.73797821,-3.53571484));
        aMap.put("G", new LatLng(50.7387514,-3.5345678));
        aMap.put("Q", new LatLng(50.73765382,-3.537118091));
        aMap.put("P", new LatLng(50.7367062,-3.5366569));
        aMap.put("O", new LatLng(50.7360569,-3.5359555));
        aMap.put("N", new LatLng(50.7349409,-3.5356282));
        aMap.put("M", new LatLng(50.73385775,-3.5353731));
        aMap.put("I", new LatLng(50.73393,-3.53444));
        aMap.put("S", new LatLng(50.7347765,-3.5338147));
        aMap.put("L", new LatLng(50.7359418,-3.5341125));
        aMap.put("A", new LatLng(50.73612391,-3.534879267));
        aMap.put("K", new LatLng(50.7355964,-3.5336706));
        aMap.put("H", new LatLng(50.735541,-3.532460633));
        aMap.put("E", new LatLng(50.7362221,-3.5315151));
        aMap.put("J", new LatLng(50.7357271,-3.5300814));
        aMap.put("D", new LatLng(50.73655856,-3.530341785));
        aMap.put("U", new LatLng(50.73671711,-3.5296201));
        aMap.put("V", new LatLng(50.737946,-3.5299956));

        Fiducal_locations = Collections.unmodifiableMap(aMap);

    }



}// end of class
