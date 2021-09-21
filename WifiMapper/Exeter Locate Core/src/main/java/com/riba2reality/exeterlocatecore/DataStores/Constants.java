package com.riba2reality.exeterlocatecore.DataStores;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.List;

public class Constants {

    public static final int LOCATION_SERVICE_ID = 175;

    public static final String ACTION_START_LOCATION_SERVICE = "startLocationService";
    public static final String ACTION_STOP_LOCATION_SERVICE = "stopLocationService";
    public static final String ACTION_SINGLE_SCAN = "performSingleScan";
    public static final String ACTION_POST_ALL = "postALL";
    public static final String ACTION_REQUEST_UPDATE = "requestUpdate";
    public static final String ACTION_REQUEST_GEOFENCE_UPDATE = "geoFenceUpdate";



    public static final String database = "alpha";

    //public static final String verificationCode =
    // "aaz0p3DuHxgxqNOk40XA4csgjeEgJzC7AUEb40gTZXgtAM5TtpleDwdGkbXQICmKwCxuO2WXawQQiobWd3nggGH9plwgJHyERBF9";

    //public static final String port = ":5000";
    public static final String port = "";


    public static final List<LatLng> stethamCampusPolygon = Arrays.asList(
            new LatLng(50.7312680719997,-3.53966720088312),
            new LatLng(50.7338931104215,-3.54301986398646),
            new LatLng(50.7410129829002,-3.5459503398842),
            new LatLng(50.7420659405892,-3.54269701494688),
            new LatLng(50.7392999115892,-3.54143045333006),
            new LatLng(50.7381054394667,-3.53835097018329),
            new LatLng(50.7395670919734,-3.53651321411183),
            new LatLng(50.7401014481688,-3.53398009087819),
            new LatLng(50.7393470611796,-3.53298670921794),
            new LatLng(50.7396142412947,-3.53189398939167),
            new LatLng(50.7389069964904,-3.53154630581058),
            new LatLng(50.73907987954,-3.5304287514428),
            new LatLng(50.7374296062181,-3.52950987340707),
            new LatLng(50.736973806195,-3.5284171535808),
            new LatLng(50.7365808715801,-3.52491548322842),
            new LatLng(50.7362036512484,-3.52382276340214),
            new LatLng(50.7334844314737,-3.52516382864348),
            new LatLng(50.7332486535356,-3.52672840475837),
            new LatLng(50.7334844314737,-3.52754794462808),
            new LatLng(50.7340502936828,-3.52916218982598),
            new LatLng(50.7351348438093,-3.531223456771),
            new LatLng(50.7352134334223,-3.53176981668414),
            new LatLng(50.7351191258708,-3.53199332755769),
            new LatLng(50.7349462281999, -3.53224167297276),
            new LatLng(50.7345689947025,-3.5324155147633),
            new LatLng(50.7334529944838,-3.53303637830095),
            new LatLng(50.7329657184433,-3.5336324072971),
            new LatLng(50.7326827816419,-3.53417876721024),
            new LatLng(50.7324155619825,-3.53474996166488),
            new LatLng(50.732195497589,-3.53564400515911),
            new LatLng(50.7320697460428,-3.53775494118714),
            new LatLng(50.7320540270758,-3.53830130110028),
            new LatLng(50.7312680719997,-3.53966720088312)
    );



}// end of class
