package com.riba2reality.wifimapper;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.Editable;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.riba2reality.wifimapper.DataStores.CombinedScanResult;
import com.riba2reality.wifimapper.DataStores.Constants;
import com.riba2reality.wifimapper.DataStores.MagSensorResult;
import com.riba2reality.wifimapper.DataStores.ServerMessage;
import com.riba2reality.wifimapper.DataStores.WifiResult;
import com.riba2reality.wifimapper.DataStores.WifiScanResult;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackerScannerSingle extends Service implements LocationListener {



    //----------------------------------------------------------------------------------------------

    private final Queue<CombinedScanResult> combinedScanResultQueue = new ConcurrentLinkedQueue<>();

    private CombinedScanResult combinedScanResult = null;

    //----------------------------------------------------------------------------------------------
    // class constants

    static final public String TRACKERSCANNER_RESULT = "com.riba2reality.wifimapper.TrackerScannerSingle.REQUEST_PROCESSED";
    static final public String TRACKERSCANNER_MESSAGE = "com.riba2reality.wifimapper.TrackerScannerSingle.TRACKERSCANNER_MSG";



    //----------------------------------------------------------------------------------------------
    // class variables

    private SensorManager sensorManager;
    private Sensor sensorMag;

    private WifiManager wifiManager;


    ////public ArrayList<String> arrayList = null;

    ////private PostToServer post;

    LocalBroadcastManager broadcaster;


    PowerManager pm;
    PowerManager.WakeLock wl;

    LocationManager locationManager;

    // private final int intervalSeconds = 1;

    // private final boolean locationScanned = false;
    private boolean running = false;
    private boolean wifi_scan_in_queue = false;

    //private SensorEvent lastEvent = null;
    private long LastTimeStamp = Long.MAX_VALUE;



    final Handler handler = new Handler(Looper.getMainLooper());

    //----------------------------------------------------------------------------------------------



    //private TextView logTextView;
    private Button scanButton;
    private Button postButton;

    private Context appContext;


    //----------------------------------------------------------------------------------------------




    //##############################################################################################
    // class functions




    //==============================================================================================
//    @Override
//    public void onCreate() {
//        super.onCreate();
    public TrackerScannerSingle(Activity activity){

        broadcaster = LocalBroadcastManager.getInstance(this);

        locationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);

        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

        //----

        //logTextView = activity.findViewById(R.id.log);
        scanButton =  activity.findViewById(R.id.manualScanButton);
        postButton =  activity.findViewById(R.id.manualPostButton);

        appContext = activity.getApplication().getApplicationContext();

        wifiManager = (WifiManager)
                activity.getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);


    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wl.isHeld())
            wl.release();

    }// end of onDestroy
    //==============================================================================================


    //==============================================================================================
//    public void sendResult(String message) {
//        Intent intent = new Intent(TRACKERSCANNER_RESULT);
//        if (message != null)
//            intent.putExtra(TRACKERSCANNER_MESSAGE, message);
//        broadcaster.sendBroadcast(intent);
//    }
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onProviderEnabled(String s) {

    }
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onProviderDisabled(String s) {

    }
    //==============================================================================================

    //==============================================================================================
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    //==============================================================================================


    //##############################################################################################
    // scan functions


    //==============================================================================================
    public void scanAll(){


        scanButton.setEnabled(false);


        Log.d("Trace", "TrackerScannerSingle.scanAll()");

        combinedScanResult = new CombinedScanResult();

        requestlocation();

        scanWifi();

        ScanMag();


    }// end of scan all
    //==============================================================================================


    //##############################################################################################
    // location functions

    //==============================================================================================
    private void requestlocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        String provider = locationManager.getBestProvider(criteria, true);

        //SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(appContext);

        int gpsInterval = appContext.getResources().getInteger(R.integer.defaultVal_gps);
        // interval needs to be in milliseconds -- scale accordingly
        long interval = SP.getInt("interval_gps", gpsInterval) * 1000;

        // TODO: Check if permission available
//        locationManager.requestLocationUpdates(
//                provider,
//                interval,
//                0,
//                this
//        );


        locationManager.requestSingleUpdate(provider, this, Looper.getMainLooper());


    }//end of requestlocation
    //==============================================================================================


    //==============================================================================================
    @Override
    public void onLocationChanged(@NonNull Location location) {

        if (location == null)
            return;

        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String message = "Time:" + currentTime + "\nLat:" + location.getLatitude() + "\nLong:" + location.getLongitude();
        System.out.println(message);

        ////// Location lastLocation = new Location(location);

        Log.d("Trace", "TrackerScannerSingle.onLocationChanged()");

        //-------------------------------------------------------------

        this.combinedScanResult.location = new Location(location);

        //-------------------------------------------------------------

        checkAllScansCompleted();

        // check if all scans completed

//        CombinedScanResult combinedScanResult = new CombinedScanResult();
//        combinedScanResult.location = new Location(location);
//
//        combinedScanResultQueue.add(combinedScanResult);
//
//        this.sendResult("GPS: Location updated.");
//
//        //
//        scanWifi();

    }// end of onLocationChanged
    //==============================================================================================





    //##############################################################################################
    // wifi functions

    //==============================================================================================
    final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> results = wifiManager.getScanResults();
            appContext.unregisterReceiver(wifiReceiver);

            String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());
            WifiScanResult result = new WifiScanResult();
            result.dateTime = currentTime;




            //arrayList = new ArrayList<>();
            for (ScanResult scanResult : results) {


                //arrayList.add(scanResult.SSID + " -" + scanResult.BSSID);


                WifiResult wifiResult = new WifiResult();
                wifiResult.macAddress = scanResult.BSSID;
                wifiResult.signalStrength = scanResult.level;


                result.wifiResult.add(wifiResult);


            }

            //sendResult("WiFi: Scan complete.");

            //Log.d("WIFI_UPDATE: ", String.valueOf(arrayList.size()));
            Log.d("Trace", "TrackerScannerSingle.wifiReceiver.event()");



            //-------------------------------------------------------------


            //wifiScanResultQueue.add(result);
            combinedScanResult.wifiScanResult = result;




            //-------------------------------------------------------------


            checkAllScansCompleted();

            // check if all scans completed

//            CombinedScanResult combinedScanResult = combinedScanResultQueue.peek();
//
//            if (combinedScanResult != null && combinedScanResultQueue.peek().dateTime == null) {
//                combinedScanResult.dateTime = currentTime;
//                combinedScanResult.wifiResult = result.wifiResult;
//            }



        }
    };
    //==============================================================================================

    //==============================================================================================
    public void scanWifi() {
        appContext.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }
    //==============================================================================================




    //##############################################################################################
    // magnetic sensor function

    //==============================================================================================
    private SensorEventListener magSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            Log.d("Trace", "TrackerScannerSingle.magSensorListener.event()");

            String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());
            MagSensorResult result = new MagSensorResult();
            result.dateTime = currentTime;

            result.X = event.values[0];
            result.Y = event.values[1];
            result.Z = event.values[2];


            long timeDelay = Long.MAX_VALUE;
            if(LastTimeStamp != Long.MAX_VALUE){

                timeDelay = event.timestamp - LastTimeStamp;
                //timeDelay = LastTimeStamp;
            }

            //LastTimeStamp = event.timestamp;
            // only perform a single scan
            sensorManager.unregisterListener(magSensorListener);


            //-------------------------------------------------------------

            combinedScanResult.magSensorResult = result;

            //-------------------------------------------------------------

            checkAllScansCompleted();

            // check if all scans completed



//            sendResult("Mag Scan updated"
//                            +"("+result.X+","+result.Y+","+result.Z+")"
//                            //+"\n["+timeDelay+"]"
//                    //+"\n["+LastTimeStamp+"]"
//            );
//
//
//            magSensorResultQueue.add(result);





        }// end of onSensorChanged

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };// end of magnetic sensor event listener;
    //==============================================================================================



    //==============================================================================================
    private void ScanMag(){

        //sensorMag

        // register the listener above, NOTE microsecond(i.e. not milli[1k], but 1 million-th of a second)
        Log.d("MAGscan", "Initiated");
        sensorManager.registerListener(magSensorListener,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);

    }// end of startSensor
    //==============================================================================================


    //==============================================================================================
    private void checkAllScansCompleted(){

        Log.d("Trace", "TrackerScannerSingle.checkAllScansCompleted()");

        if(
                combinedScanResult.wifiScanResult != null &&
                combinedScanResult.magSensorResult != null &&
                combinedScanResult.location != null
        ){
            Log.d("Trace", "TrackerScannerSingle.checkAllScansCompleted()-ALL SCANS COMPLETED!");

            combinedScanResultQueue.add(combinedScanResult);


            scanButton.setEnabled(true);

            postButton.setText("Post Data - ("+String.valueOf(combinedScanResultQueue.size())+")");






        }




    }// end of checkAllScansCompleted
    //==============================================================================================




    //##############################################################################################
    // post result functions



    //==============================================================================================
    private ServerMessage encodeCombinedResult(CombinedScanResult combinedScanResult){

        ServerMessage serverMessage = new ServerMessage();

        String[] server_values = getResources().getStringArray(R.array.server_values);
        //SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(appContext);
        String address = SP.getString("ServerAddress", server_values[1]);

        String dataBase = SP.getString("database", "alpha");

        String deviceID = SP.getString("DeviceID", "");

        boolean useSSL = SP.getBoolean("SSL_switch", true);

        String protocol = "http";
        if (useSSL) {
            protocol += "s";
        }

        String port = Constants.port;

        String endpoint = "/";

        String urlString = protocol + "://" + address + port + endpoint;

        //------------------------------------------------------------------

        double latitude = 0.0;
        double longitude = 0.0;
        double altitude = 0.0;
        double accuracy = 0.0;
        // String provider = "";
        if (combinedScanResult.location != null) {
            latitude = combinedScanResult.location.getLatitude();
            longitude = combinedScanResult.location.getLongitude();
            altitude = combinedScanResult.location.getAltitude();
            accuracy = combinedScanResult.location.getAccuracy();

            // provider = combinedScanResult.location.getProvider();
        }


        List<String> macAddressList = new ArrayList<>();
        List<String> signalStrengths = new ArrayList<>();

        for (WifiResult wifiResult : combinedScanResult.wifiScanResult.wifiResult) {

            macAddressList.add(wifiResult.macAddress);
            signalStrengths.add(Integer.toString(wifiResult.signalStrength));
        }

        String gpsTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss",
                Locale.getDefault()).format(new Date(combinedScanResult.location.getTime()));


        //------------------------------------------------------------------
        // build message...

        Map<String, String> parameters = new HashMap<>();


        parameters.put("MAGIC_NUM", Constants.verificationCode);

        parameters.put("UUID", deviceID);

        parameters.put("DATABASE", dataBase);

        parameters.put("TIME", combinedScanResult.dateTime);


        parameters.put("GPSTIME", gpsTime);

        parameters.put("X", Double.toString(latitude));
        parameters.put("Y", Double.toString(longitude));
        parameters.put("ALTITUDE", Double.toString(altitude));
        parameters.put("ACC", Double.toString(accuracy));

        String macAddressJson = new Gson().toJson(macAddressList);


        //parameters.put("MacAddresses",macAddressList.toString());
        parameters.put("MacAddressesJson", macAddressJson);

        parameters.put("signalStrengthsJson", new Gson().toJson(signalStrengths));


        String message = new JSONObject(parameters).toString();

        //------------------------------------------------------------------
        serverMessage.urlString = urlString;
        serverMessage.message = message;
        serverMessage.useSSL = useSSL;
        serverMessage.address = address;


        return serverMessage;
    }// end of encodeCombinedResult
    //==============================================================================================


//    //==============================================================================================
//    private void postCombinedResult() {
//
//
//
//        // empty the queue
//        while (this.combinedScanResultQueue.size() > 0) {
//
//            ServerMessage serverMessage = encodeCombinedResult(combinedScanResultQueue.poll());
//
//            PostToServer thisPost = new PostToServer(this,
//                    getResources().openRawResource(R.raw.nginxselfsigned),
//                    serverMessage
//            );
//
//            thisPost.execute();
//
//
//            this.sendResult("Sending to server: GPS location and WiFi scan.");
//
//
//        }// end of looping queue
//
//    }// end of postCombinedResult
//    //==============================================================================================
//
//    //==============================================================================================
//    private void postResends(){
//
//        // empty the queue
//        while (this.resendQueue.size() > 0) {
//
//            ServerMessage serverMessage = resendQueue.poll();
//
//            PostToServer thisPost = new PostToServer(this,
//                    getResources().openRawResource(R.raw.nginxselfsigned),
//                    serverMessage
//            );
//            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
//            //thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);
//            //thisPost.wifiScanResult = wifiScanResultResendQueue.poll();
//
//            thisPost.execute();
//
//            this.sendResult("Sending to server: Resend!");
//
//
//        }// end of looping queue
//    }
//    //==============================================================================================

//    //==============================================================================================
//    private void postALL(){
//
//        postResends();
//
//        postCombinedResult();
//        postWifiResult();
//        postMagResult();
//    }
//    //==============================================================================================


    //##############################################################################################
    // service functions





}// end of class
