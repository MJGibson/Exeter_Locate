package com.riba2reality.wifimapper;

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
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import com.google.gson.Gson;
import com.riba2reality.wifimapper.DataStores.CombinedScanResult;
import com.riba2reality.wifimapper.DataStores.Constants;
import com.riba2reality.wifimapper.DataStores.LocationResult;
import com.riba2reality.wifimapper.DataStores.SensorResult;
import com.riba2reality.wifimapper.DataStores.ServerMessage;
import com.riba2reality.wifimapper.DataStores.WifiResult;
import com.riba2reality.wifimapper.DataStores.WifiScanResult;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackerScanner extends Service implements LocationListener {


    //----------------------------------------------------------------------------------------------
    // result/dispatch queues

    private final Queue<CombinedScanResult> combinedScanResultQueue = new ConcurrentLinkedQueue<>();
    //public final Queue<CombinedScanResult> combinedScanResultResendQueue = new ConcurrentLinkedQueue<>();

    private final Queue<LocationResult> locationResultQueue = new ConcurrentLinkedQueue<>();

    private final Queue<WifiScanResult> wifiScanResultQueue = new ConcurrentLinkedQueue<>();
    //public final Queue<WifiScanResult> wifiScanResultResendQueue = new ConcurrentLinkedQueue<>();



    private final Queue<SensorResult> magSensorResultQueue = new ConcurrentLinkedQueue<>();
    //public final Queue<SensorResult> magSensorResultResendQueue = new ConcurrentLinkedQueue<>();

    private final Queue<SensorResult> accelSensorResultQueue = new ConcurrentLinkedQueue<>();


    public final Queue<ServerMessage> resendQueue = new ConcurrentLinkedQueue<>();

    // single combined scan
    private CombinedScanResult combinedScanResult = null;


    //----------------------------------------------------------------------------------------------
    // class constants

    static final public String TRACKERSCANNER_RESULT = "com.riba2reality.wifimapper.TrackerScanner.REQUEST_PROCESSED";


    static final public String TRACKERSCANNER_MANUAL_SCAN_RESULT = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_RESULT";
    static final public String TRACKERSCANNER_MANUAL_SCAN_TIMER_UPDATE = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_TIMER_UPDATE";

    // tags
    static final public String TRACKERSCANNER_MESSAGE = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_MSG";

    static final public String TRACKERSCANNER_COMBINED_QUEUE_COUNT = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_COMBINED_QUEUE_COUNT";
    static final public String TRACKERSCANNER_RESEND_QUEUE_COUNT = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_RESEND_QUEUE_COUNT";

    static final public String TRACKERSCANNER_LOCATION_QUEUE_COUNT = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_LOCATION_QUEUE_COUNT";
    static final public String TRACKERSCANNER_WIFI_QUEUE_COUNT = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_WIFI_QUEUE_COUNT";
    static final public String TRACKERSCANNER_MAG_QUEUE_COUNT = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_MAG_QUEUE_COUNT";
    static final public String TRACKERSCANNER_ACCEL_QUEUE_COUNT = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_ACCEL_QUEUE_COUNT";

    static final public String TRACKERSCANNER_MANUAL_SCAN_REMAINING = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_REMAINING";

    private int manualScanCount_loction = 0;
    private int manualScanCount_wifi = 0;
    private int manualScanCount_mag = 0;
    private int manualScanCount_accel = 0;



    final public String MANUAL_SCAN_MESSAGE = "Manual Scan: ";

    //----------------------------------------------------------------------------------------------
    // class variables

    private SensorManager sensorManager;
    //private Sensor sensorMag;

    private WifiManager wifiManager;
    public ArrayList<String> arrayList = null;

    ////private PostToServer post;

    LocalBroadcastManager broadcaster;


    PowerManager pm;
    PowerManager.WakeLock wl;

    LocationManager locationManager;

    // private final int intervalSeconds = 1;

    // private final boolean locationScanned = false;
    private boolean running = false;
    //private boolean wifi_scan_in_queue = false;

    //private SensorEvent lastEvent = null;
    private long LastTimeStamp = Long.MAX_VALUE;



    final Handler handler = new Handler(Looper.getMainLooper());

    //private boolean continueScanning = false;
    private boolean stopScanning = false;

    private long stopManualScanTime;

    private String manualScanMessage = "None";

    //----------------------------------------------------------------------------------------------

    private boolean magAvailable = false;
    private boolean accelAvailable = false;



    //----------------------------------------------------------------------------------------------

    //##############################################################################################
    // class functions

    //==============================================================================================
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("Trace", "TrackerScanner.onCreate()");

        broadcaster = LocalBroadcastManager.getInstance(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //----------------------------------

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // check has sensors
        magAvailable = SP.getBoolean("magAvailable", false);

        accelAvailable = SP.getBoolean("accelAvailable", false);


    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(wl!=null) {
            if (wl.isHeld())
                wl.release();
        }

    }// end of onDestroy
    //==============================================================================================


    //==============================================================================================
    public void sendResult(String message) {
        Intent intent = new Intent(TRACKERSCANNER_RESULT);
        if (message != null)
            intent.putExtra(TRACKERSCANNER_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }
    //==============================================================================================

    //==============================================================================================
    public void sendManualScanResult() {
        Intent intent = new Intent(TRACKERSCANNER_MANUAL_SCAN_RESULT);

        //intent.putExtra(TRACKERSCANNER_COMBINED_QUEUE_COUNT, this.combinedScanResultQueue.size());
        //intent.putExtra(TRACKERSCANNER_RESEND_QUEUE_COUNT, this.resendQueue.size());

        broadcaster.sendBroadcast(intent);
    }
    //==============================================================================================

    //==============================================================================================
    public void sendManualScanUpdate() {
        Intent intent = new Intent(this.TRACKERSCANNER_MANUAL_SCAN_TIMER_UPDATE);

        //intent.putExtra(TRACKERSCANNER_COMBINED_QUEUE_COUNT, this.combinedScanResultQueue.size());
        //intent.putExtra(TRACKERSCANNER_RESEND_QUEUE_COUNT, this.resendQueue.size());

        long durationRemaining = stopManualScanTime - SystemClock.elapsedRealtime();
        intent.putExtra(TRACKERSCANNER_MANUAL_SCAN_REMAINING, durationRemaining);

        intent.putExtra(TRACKERSCANNER_LOCATION_QUEUE_COUNT, this.manualScanCount_loction);
        intent.putExtra(TRACKERSCANNER_WIFI_QUEUE_COUNT, this.manualScanCount_wifi);
        intent.putExtra(TRACKERSCANNER_MAG_QUEUE_COUNT, this.manualScanCount_mag);
        intent.putExtra(TRACKERSCANNER_ACCEL_QUEUE_COUNT, this.manualScanCount_accel);



        broadcaster.sendBroadcast(intent);
    }
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
    // runnable periodic updates

    //==============================================================================================
    private final Runnable updateManualScanCounts = new Runnable() {
        @Override
        public void run() {
            long durationRemaining = stopManualScanTime - SystemClock.elapsedRealtime();

            if(durationRemaining > 0){
                //if(!running){


                sendManualScanUpdate();


                handler.postDelayed(updateManualScanCounts,  1000 );
            } else {
                return;
            }



        }
    };
    //==============================================================================================

    //==============================================================================================
    private final Runnable finaliseManualScan = new Runnable() {
        @Override
        public void run() {
            if (running) {

                manualScanMessage = "None";

                sendManualScanResult();

                if(stopScanning){
                    stopScanning();
                }

            } else {
                return;
            }

        }
    };
    //==============================================================================================


    //==============================================================================================
    private final Runnable periodicUpdate = new Runnable() {
        @Override
        public void run() {
            if (running) {

                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int postInterval = getResources().getInteger(R.integer.defaultVal_post);
                int interval = SP.getInt("interval_posts", postInterval);

                handler.postDelayed(periodicUpdate, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
            } else {
                return;
            }

            //postCombinedResult();
            //postWifiResult();
            //postMagResult();
            postALL();


        }
    };
    //==============================================================================================


//    //==============================================================================================
//    private final Runnable periodicUpdate_scan = new Runnable() {
//        @Override
//        public void run() {
//            if (running) {
//
//                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                int postInterval = getResources().getInteger(R.integer.defaultVal_post);
//                int interval = SP.getInt("interval_scan", postInterval);
//
//                handler.postDelayed(periodicUpdate_scan, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
//            } else {
//                return;
//            }
//
//            scanAll("None");
//
//
//        }
//    };
    //==============================================================================================


    //==============================================================================================
    private final Runnable periodicUpdate_wifi = new Runnable() {
        @Override
        public void run() {



            if (running) {
                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int wifiInterval = getResources().getInteger(R.integer.defaultVal_wifi);
                int interval = SP.getInt("interval_wifi", wifiInterval);

                handler.postDelayed(periodicUpdate_wifi, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
                //wifi_scan_in_queue = true;
            } else {
                return;
            }

            scanWifi();

            //sendResult("Wifi Scan initiated.");

            // after we've ran the event, remove the in-queue flag
            //wifi_scan_in_queue = false;
        }
    };
    //==============================================================================================


    //==============================================================================================
    private final Runnable periodicUpdate_mag = new Runnable() {
        @Override
        public void run() {

            if (running) {

                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int magInterval = getResources().getInteger(R.integer.defaultVal_mag);
                int interval = SP.getInt("interval_mag", magInterval);

                handler.postDelayed(periodicUpdate_mag, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
            } else {
                return;
            }

            ScanMag();



        }
    };
    //==============================================================================================

    //==============================================================================================
    private final Runnable periodicUpdate_accel = new Runnable() {
        @Override
        public void run() {

            if (running) {

                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int accelInterval = getResources().getInteger(R.integer.defaultVal_accel);
                int interval = SP.getInt("interval_accel", accelInterval);

                handler.postDelayed(periodicUpdate_accel, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
            } else {
                return;
            }

            ScanAccel();



        }
    };
    //==============================================================================================


    //##############################################################################################
    // location functions

    //==============================================================================================
    private void requestlocation(boolean singleUpdate) {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        String provider = locationManager.getBestProvider(criteria, true);

        if(singleUpdate){
            locationManager.requestSingleUpdate(provider, this, Looper.getMainLooper());
        }else {

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            int gpsInterval = getResources().getInteger(R.integer.defaultVal_gps);
            // interval needs to be in milliseconds -- scale accordingly
            long interval = SP.getInt("interval_gps", gpsInterval) * 1000;

            // TODO: Check if permission available
            locationManager.requestLocationUpdates(
                    provider,
                    interval,
                    0,
                    this
            );
        }




    }//end of requestlocation
    //==============================================================================================


    //==============================================================================================
    @Override
    public void onLocationChanged(@NonNull Location location) {

        if (location == null)
            return;

        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String message = "Time:" + currentTime + "\nLat:" + location.getLatitude() + "\nLong:" + location.getLongitude();
        //System.out.println(message);

        // Location lastLocation = new Location(location);

        Log.d("Trace", "TrackerScanner.onLocationChanged()");

        //-------------------------------------------------------------

        LocationResult result = new LocationResult();
        result.dateTime = currentTime;
        result.location = location;

        result.message = this.MANUAL_SCAN_MESSAGE;


        //-------------------------------------------------------------

        locationResultQueue.add(result);

        //-------------------------------------------------------------
//        if(running) {
//
//            CombinedScanResult combinedScanResult = new CombinedScanResult();
//            combinedScanResult.location = new Location(location);
//
//            combinedScanResultQueue.add(combinedScanResult);
//        }
        //-------------------------------------------------------------



        //-------------------------------------------------------------
        // EXTRA WIFI SCAN
//        if(running) {
//            scanWifi();
//            ScanMag();
//        }
        //-------------------------------------------------------------

        manualScanCount_loction += 1;


        long durationRemaining = this.stopManualScanTime - SystemClock.elapsedRealtime();

        if(durationRemaining > 0){
            //if(!running){

            //this.combinedScanResult.location = new Location(location);
            //checkAllScansCompleted();

            this.sendResult(MANUAL_SCAN_MESSAGE +
                    "GPS: Location updated." + "Location: " + this.manualScanMessage
            );




        }// end of within manual scan period
        else{
            this.sendResult("GPS: Location updated.");
        }



    }// end of onLocationChanged
    //==============================================================================================





    //##############################################################################################
    // wifi functions

    //==============================================================================================
    final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> results = wifiManager.getScanResults();
            unregisterReceiver(wifiReceiver);

            String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());
            WifiScanResult result = new WifiScanResult();
            result.dateTime = currentTime;

            result.message = MANUAL_SCAN_MESSAGE;


            arrayList = new ArrayList<>();
            for (ScanResult scanResult : results) {


                arrayList.add(scanResult.SSID + " -" + scanResult.BSSID);


                WifiResult wifiResult = new WifiResult();
                wifiResult.macAddress = scanResult.BSSID;
                wifiResult.signalStrength = scanResult.level;


                result.wifiResult.add(wifiResult);


            }



            Log.d("WIFI_UPDATE: ", String.valueOf(arrayList.size()));


            //-------------------------------------------------------------

            wifiScanResultQueue.add(result);

//            if(running) {
//
//
//
//
//                CombinedScanResult combinedScanResult = combinedScanResultQueue.peek();
//
//                if (combinedScanResult != null && combinedScanResultQueue.peek().dateTime == null) {
//                    //?combinedScanResult.dateTime = currentTime;
//                    //combinedScanResult.wifiResult = result.wifiResult;
//
//                    combinedScanResult.wifiScanResult = result;
//                    combinedScanResult.wifiScanResult.dateTime = currentTime;
//                }
//            }
            //-------------------------------------------------------------

            manualScanCount_wifi += 1;

            long durationRemaining = stopManualScanTime - SystemClock.elapsedRealtime();

            //if(!running){
            if(durationRemaining > 0){

                //combinedScanResult.wifiScanResult = result;
                //checkAllScansCompleted();

                sendResult(MANUAL_SCAN_MESSAGE +
                        "WiFi: Scan complete."+ "Location: " + manualScanMessage
                );

            }else{
                sendResult("WiFi: Scan complete.");
            }




        }// end of on recieve
    };
    //==============================================================================================

    //==============================================================================================
    public void scanWifi() {
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }
    //==============================================================================================




    //##############################################################################################
    // magnetic sensor function

    //==============================================================================================
    private SensorEventListener magSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            // only perform a single scan
            sensorManager.unregisterListener(magSensorListener);

            Log.d("Trace", "TrackerScanner.magSensorListener.event()");

            String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());
            SensorResult result = new SensorResult();
            result.dateTime = currentTime;

            result.X = event.values[0];
            result.Y = event.values[1];
            result.Z = event.values[2];

            result.message = manualScanMessage;


            long timeDelay = Long.MAX_VALUE;
            if(LastTimeStamp != Long.MAX_VALUE){

                timeDelay = event.timestamp - LastTimeStamp;
                //timeDelay = LastTimeStamp;
            }




            //-------------------------------------------------------------

            magSensorResultQueue.add(result);

//            if(running) {
//
//
//
//                CombinedScanResult combinedScanResult = combinedScanResultQueue.peek();
//
//                if (combinedScanResult != null && combinedScanResultQueue.peek().magSensorResult == null) {
//                    //?combinedScanResult.dateTime = currentTime;
//                    //combinedScanResult.wifiResult = result.wifiResult;
//
//                    combinedScanResult.magSensorResult = result;
//                    //combinedScanResult.wifiScanResult.dateTime = currentTime;
//                }
//
//
//            }

            LastTimeStamp = event.timestamp;

            //-------------------------------------------------------------

            manualScanCount_mag += 1;

            long durationRemaining = stopManualScanTime - SystemClock.elapsedRealtime();

            //if(!running){
            if(durationRemaining > 0){

                //combinedScanResult.magSensorResult = result;
                //checkAllScansCompleted();

                sendResult(MANUAL_SCAN_MESSAGE+"Mag Scan updated."
                                + "Location: " + manualScanMessage
                                //+" ("+result.X+","+result.Y+","+result.Z+")"
                        //+"\n["+timeDelay+"]"
                        //+"\n["+LastTimeStamp+"]"
                );

            }else{

                sendResult("Mag Scan updated."
                                //+" ("+result.X+","+result.Y+","+result.Z+")"
                        //+"\n["+timeDelay+"]"
                        //+"\n["+LastTimeStamp+"]"
                );

            }

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


    //##############################################################################################
    // accelerometer sensor function


    //==============================================================================================
    private SensorEventListener accelSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            // only perform a single scan
            sensorManager.unregisterListener(accelSensorListener);

            Log.d("Trace", "TrackerScanner.accelSensorListener.event()");

            String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());
            SensorResult result = new SensorResult();
            result.dateTime = currentTime;

            result.X = event.values[0];
            result.Y = event.values[1];
            result.Z = event.values[2];

            result.message = manualScanMessage;


            long timeDelay = Long.MAX_VALUE;
            if(LastTimeStamp != Long.MAX_VALUE){

                timeDelay = event.timestamp - LastTimeStamp;
                //timeDelay = LastTimeStamp;
            }




            //-------------------------------------------------------------

            accelSensorResultQueue.add(result);

//            if(running) {
//
//
//
//                CombinedScanResult combinedScanResult = combinedScanResultQueue.peek();
//
//                if (combinedScanResult != null && combinedScanResultQueue.peek().magSensorResult == null) {
//                    //?combinedScanResult.dateTime = currentTime;
//                    //combinedScanResult.wifiResult = result.wifiResult;
//
//                    combinedScanResult.magSensorResult = result;
//                    //combinedScanResult.wifiScanResult.dateTime = currentTime;
//                }
//
//
//            }

            LastTimeStamp = event.timestamp;

            //-------------------------------------------------------------

            manualScanCount_accel += 1;

            long durationRemaining = stopManualScanTime - SystemClock.elapsedRealtime();

            //if(!running){
            if(durationRemaining > 0){
                //combinedScanResult.accelSensorResult = result;
                //checkAllScansCompleted();

                sendResult(MANUAL_SCAN_MESSAGE+"Accel Scan updated."
                                + "Location: " + manualScanMessage
                                //+" ("+result.X+","+result.Y+","+result.Z+")"
                        //+"\n["+timeDelay+"]"
                        //+"\n["+LastTimeStamp+"]"
                );

            }else{

                sendResult("Accel Scan updated."
                                //+" ("+result.X+","+result.Y+","+result.Z+")"
                        //+"\n["+timeDelay+"]"
                        //+"\n["+LastTimeStamp+"]"
                );
            }

        }// end of onSensorChanged

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };// end of magnetic sensor event listener;
    //==============================================================================================



    //==============================================================================================
    private void ScanAccel(){

        //sensorMag

        // register the listener above, NOTE microsecond(i.e. not milli[1k], but 1 million-th of a second)
        Log.d("ACCELscan", "Initiated");
        sensorManager.registerListener(accelSensorListener,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);

    }// end of startSensor
    //==============================================================================================

    //##############################################################################################
    // Scan sync for single scan

//    //==============================================================================================
//    private void checkAllScansCompleted(){
//
//        Log.d("Trace", "TrackerScanner.checkAllScansCompleted()");
//
//        if(
//
//                combinedScanResult.wifiScanResult != null
//                        &&
//
//                (combinedScanResult.magSensorResult != null == magAvailable)
//                        &&
//
//                (combinedScanResult.location != null)
//                        &&
//
//                (combinedScanResult.accelSensorResult != null== accelAvailable)
//        ){
//            Log.d("Trace", "TrackerScanner.checkAllScansCompleted() - ALL SCANS COMPLETED!");
//
//
//            if(!magAvailable){
//                combinedScanResult.magSensorResult = new SensorResult();
//            }
//
//            if(!accelAvailable){
//                combinedScanResult.accelSensorResult = new SensorResult();
//            }
//
//
//            float gravity[] = {combinedScanResult.accelSensorResult.X,
//                                combinedScanResult.accelSensorResult.Y,
//                                combinedScanResult.accelSensorResult.Z};
//
//            float geomagnetic[] = {combinedScanResult.magSensorResult.X,
//                    combinedScanResult.magSensorResult.Y,
//                    combinedScanResult.magSensorResult.Z};
//
//            float identifyMatrix_R[] = new float[9];
//            float rotationMatrix_I[] = new float[9];
//
//            if(magAvailable && accelAvailable) {
//
//                boolean success = sensorManager.getRotationMatrix(identifyMatrix_R, rotationMatrix_I,
//                        gravity, geomagnetic
//                );
//
//                Log.d("getRotationMatrix", "identifyMatrix_R: " + Arrays.toString(identifyMatrix_R));
//                Log.d("getRotationMatrix", "rotationMatrix_I: " + Arrays.toString(rotationMatrix_I));
//
//                combinedScanResult.matrix_R = identifyMatrix_R;
//                combinedScanResult.matrix_I = rotationMatrix_I;
//
//            }
//
//            //--------------
//
//            combinedScanResultQueue.add(combinedScanResult);
//
//            //scanButton.setEnabled(true);
//            //postButton.setText("Post Data - ("+String.valueOf(combinedScanResultQueue.size())+")");
//
//
//
//
//
//
//            this.sendSingleScanResult();
//
//            if(continueScanning){
//                startScanning();
//            }
//
//
//            postALL();
//
//
//
//
//        }// end of if scans completed
//
//    }// end of checkAllScansCompleted
//    //==============================================================================================



    //##############################################################################################
    // post result functions

    //==============================================================================================
    private ServerMessage encodeLocationResult(LocationResult locationResult){

        ServerMessage serverMessage = new ServerMessage();

        String[] server_values = getResources().getStringArray(R.array.server_values);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String address = SP.getString("ServerAddress", server_values[1]);

        String dataBase = SP.getString("database", "alpha");

        String deviceID = SP.getString("DeviceID", "");

        boolean useSSL = SP.getBoolean("SSL_switch", true);

        String protocol = "http";
        if (useSSL) {
            protocol += "s";
        }

        String port = Constants.port;

        String endpoint = "/gps/";

        String urlString = protocol + "://" + address + port + endpoint;

        //------------------------------------------------------------------

        double latitude = 0.0;
        double longitude = 0.0;
        double altitude = 0.0;
        double accuracy = 0.0;
        String gpsTime = "";
        // String provider = "";
        if (locationResult.location != null) {
            latitude = locationResult.location.getLatitude();
            longitude = locationResult.location.getLongitude();
            altitude = locationResult.location.getAltitude();
            accuracy = locationResult.location.getAccuracy();

            gpsTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss",
                    Locale.getDefault()).format(new Date(locationResult.location.getTime()));

        }





        //------------------------------------------------------------------
        // build message...

        Map<String, String> parameters = new HashMap<>();


        parameters.put("MAGIC_NUM", Constants.verificationCode);

        parameters.put("UUID", deviceID);

        parameters.put("DATABASE", dataBase);

        parameters.put("MESSAGE", this.manualScanMessage);

        //------------
        // GPS
        parameters.put("GPS_TIME", gpsTime);
        parameters.put("X", Double.toString(latitude));
        parameters.put("Y", Double.toString(longitude));
        parameters.put("ALTITUDE", Double.toString(altitude));
        parameters.put("ACC", Double.toString(accuracy));

        String message = new JSONObject(parameters).toString();

        //------------------------------------------------------------------
        serverMessage.urlString = urlString;
        serverMessage.message = message;
        serverMessage.useSSL = useSSL;
        serverMessage.address = address;


        return serverMessage;
    }// end of encodeLocationResult
    //==============================================================================================

    //==============================================================================================
    private void postLocationResult() {

        // empty the queue
        while (this.locationResultQueue.size() > 0) {

            ServerMessage serverMessage = encodeLocationResult(locationResultQueue.poll());

            PostToServer thisPost = new PostToServer(this,
                    getResources().openRawResource(R.raw.nginxselfsigned),
                    serverMessage
            );
            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
            //thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);
            //thisPost.wifiScanResult = wifiScanResultResendQueue.poll();

            thisPost.execute();

            this.sendResult("Sending to server: GPS scan result.");


        }// end of looping queue


    }// end of postWifiResult
    //==============================================================================================


    //==============================================================================================
    private ServerMessage encodeWifiResult(WifiScanResult wifiScanResult){

        ServerMessage serverMessage = new ServerMessage();

        String[] server_values = getResources().getStringArray(R.array.server_values);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String address = SP.getString("ServerAddress", server_values[1]);

        String dataBase = SP.getString("database", "alpha");

        String deviceID = SP.getString("DeviceID", "");

        boolean useSSL = SP.getBoolean("SSL_switch", true);

        String protocol = "http";
        if (useSSL) {
            protocol += "s";
        }

        String port = Constants.port;

        String endpoint = "/wifi/";

        String urlString = protocol + "://" + address + port + endpoint;

        //------------------------------------------------------------------

        List<String> macAddressList = new ArrayList<>();
        List<String> signalStrengths = new ArrayList<>();

        for (WifiResult wifiResult : wifiScanResult.wifiResult) {

            macAddressList.add(wifiResult.macAddress);
            signalStrengths.add(Integer.toString(wifiResult.signalStrength));
        }

        Map<String, String> parameters = new HashMap<>();


        parameters.put("MAGIC_NUM", Constants.verificationCode);

        parameters.put("UUID", deviceID);

        parameters.put("DATABASE", dataBase);

        parameters.put("MESSAGE", this.manualScanMessage);

        parameters.put("WIFI_TIME", wifiScanResult.dateTime);

        String macAddressJson = new Gson().toJson(macAddressList);

        parameters.put("MacAddressesJson", macAddressJson);

        parameters.put("signalStrengthsJson", new Gson().toJson(signalStrengths));


        String message = new JSONObject(parameters).toString();

        //------------------------------------------------------------------
        serverMessage.urlString = urlString;
        serverMessage.message = message;
        serverMessage.useSSL = useSSL;
        serverMessage.address = address;


        return serverMessage;
    }// end of encodeWifiResult
    //==============================================================================================

    //==============================================================================================
    private void postWifiResult() {

        // empty the queue
        while (this.wifiScanResultQueue.size() > 0) {

            ServerMessage serverMessage = encodeWifiResult(wifiScanResultQueue.poll());

            PostToServer thisPost = new PostToServer(this,
                    getResources().openRawResource(R.raw.nginxselfsigned),
                    serverMessage
                    );
            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
            //thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);
            //thisPost.wifiScanResult = wifiScanResultResendQueue.poll();

            thisPost.execute();

            this.sendResult("Sending to server: WiFi scan result.");


        }// end of looping queue


    }// end of postWifiResult
    //==============================================================================================

    //==============================================================================================
    private ServerMessage encodeMagResult(SensorResult magSensorResult){

        ServerMessage serverMessage = new ServerMessage();

        String[] server_values = getResources().getStringArray(R.array.server_values);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String address = SP.getString("ServerAddress", server_values[1]);

        String dataBase = SP.getString("database", "alpha");

        String deviceID = SP.getString("DeviceID", "");

        boolean useSSL = SP.getBoolean("SSL_switch", true);

        String protocol = "http";
        if (useSSL) {
            protocol += "s";
        }

        String port = Constants.port;

        String endpoint = "/mag/";

        String urlString = protocol + "://" + address + port + endpoint;

        //------------------------------------------------------------------

        Map<String, String> parameters = new HashMap<>();


        parameters.put("MAGIC_NUM", Constants.verificationCode);

        parameters.put("UUID", deviceID);

        parameters.put("DATABASE", dataBase);

        parameters.put("MESSAGE", this.manualScanMessage);

        parameters.put("MAG_TIME", magSensorResult.dateTime);
        //-----

        parameters.put("MAG_X",Double.toString(magSensorResult.X));
        parameters.put("MAG_Y",Double.toString(magSensorResult.Y));
        parameters.put("MAG_Z",Double.toString(magSensorResult.Z));


        //-----

        String message = new JSONObject(parameters).toString();

        //------------------------------------------------------------------
        serverMessage.urlString = urlString;
        serverMessage.message = message;
        serverMessage.useSSL = useSSL;
        serverMessage.address = address;


        return serverMessage;
    }// end of encodeWifiResult
    //==============================================================================================

    //==============================================================================================
    private void postMagResult() {


        // empty the queue
        while (this.magSensorResultQueue.size() > 0) {

            ServerMessage serverMessage = encodeMagResult(magSensorResultQueue.poll());

            PostToServer thisPost = new PostToServer(this,
                    getResources().openRawResource(R.raw.nginxselfsigned),
                    serverMessage
            );
            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
            //thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);
            //thisPost.wifiScanResult = wifiScanResultResendQueue.poll();

            thisPost.execute();

            this.sendResult("Sending to server: Magnetic sensor result.");


        }// end of looping queue


    }// end of postWifiResult
    //==============================================================================================


    //==============================================================================================
    private ServerMessage encodeAccelResult(SensorResult sensorResult){

        ServerMessage serverMessage = new ServerMessage();

        String[] server_values = getResources().getStringArray(R.array.server_values);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String address = SP.getString("ServerAddress", server_values[1]);

        String dataBase = SP.getString("database", "alpha");

        String deviceID = SP.getString("DeviceID", "");

        boolean useSSL = SP.getBoolean("SSL_switch", true);

        String protocol = "http";
        if (useSSL) {
            protocol += "s";
        }

        String port = Constants.port;

        String endpoint = "/accel/";

        String urlString = protocol + "://" + address + port + endpoint;

        //------------------------------------------------------------------

        Map<String, String> parameters = new HashMap<>();


        parameters.put("MAGIC_NUM", Constants.verificationCode);

        parameters.put("UUID", deviceID);

        parameters.put("DATABASE", dataBase);

        parameters.put("MESSAGE", this.manualScanMessage);

        parameters.put("ACCEL_TIME", sensorResult.dateTime);
        //-----

        parameters.put("ACCEL_X",Double.toString(sensorResult.X));
        parameters.put("ACCEL_Y",Double.toString(sensorResult.Y));
        parameters.put("ACCEL_Z",Double.toString(sensorResult.Z));


        //-----

        String message = new JSONObject(parameters).toString();

        //------------------------------------------------------------------
        serverMessage.urlString = urlString;
        serverMessage.message = message;
        serverMessage.useSSL = useSSL;
        serverMessage.address = address;


        return serverMessage;
    }// end of encodeAccelResult
    //==============================================================================================

    //==============================================================================================
    private void postAccelResult() {


        // empty the queue
        while (this.accelSensorResultQueue.size() > 0) {

            ServerMessage serverMessage = encodeAccelResult(accelSensorResultQueue.poll());

            PostToServer thisPost = new PostToServer(this,
                    getResources().openRawResource(R.raw.nginxselfsigned),
                    serverMessage
            );
            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
            //thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);
            //thisPost.wifiScanResult = wifiScanResultResendQueue.poll();

            thisPost.execute();

            this.sendResult("Sending to server: Accelerometer sensor result.");


        }// end of looping queue


    }// end of postAccelResult
    //==============================================================================================



    //==============================================================================================
    private ServerMessage encodeCombinedResult(CombinedScanResult combinedScanResult){

        ServerMessage serverMessage = new ServerMessage();

        String[] server_values = getResources().getStringArray(R.array.server_values);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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

        if(combinedScanResult.wifiScanResult!= null) {
            if(combinedScanResult.wifiScanResult.wifiResult!= null) {
                for (WifiResult wifiResult : combinedScanResult.wifiScanResult.wifiResult) {

                    macAddressList.add(wifiResult.macAddress);
                    signalStrengths.add(Integer.toString(wifiResult.signalStrength));
                }
            }
        }

        String gpsTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss",
                Locale.getDefault()).format(new Date(combinedScanResult.location.getTime()));


        //------------------------------------------------------------------
        // build message...

        Map<String, String> parameters = new HashMap<>();


        parameters.put("MAGIC_NUM", Constants.verificationCode);

        parameters.put("UUID", deviceID);

        parameters.put("DATABASE", dataBase);

        parameters.put("MESSAGE", combinedScanResult.message);


        //------------




        //------------
        // GPS
        parameters.put("GPS_TIME", gpsTime);
        parameters.put("X", Double.toString(latitude));
        parameters.put("Y", Double.toString(longitude));
        parameters.put("ALTITUDE", Double.toString(altitude));
        parameters.put("ACC", Double.toString(accuracy));

        //------------
        // magnetic
        //if(combinedScanResult.magSensorResult!=null) {
            parameters.put("MAG_TIME", combinedScanResult.magSensorResult.dateTime);
            parameters.put("MAG_X", String.valueOf(combinedScanResult.magSensorResult.X));
            parameters.put("MAG_Y", String.valueOf(combinedScanResult.magSensorResult.Y));
            parameters.put("MAG_Z", String.valueOf(combinedScanResult.magSensorResult.Z));
        //}

        //------------
        // accelerometer

        parameters.put("ACCEL_TIME", combinedScanResult.accelSensorResult.dateTime);
        parameters.put("ACCEL_X", String.valueOf(combinedScanResult.accelSensorResult.X));
        parameters.put("ACCEL_Y", String.valueOf(combinedScanResult.accelSensorResult.Y));
        parameters.put("ACCEL_Z", String.valueOf(combinedScanResult.accelSensorResult.Z));



        //------------
        // wifi

        parameters.put("WIFI_TIME", combinedScanResult.wifiScanResult.dateTime);
        String macAddressJson = new Gson().toJson(macAddressList);


        //parameters.put("MacAddresses",macAddressList.toString());
        parameters.put("MacAddressesJson", macAddressJson);

        parameters.put("signalStrengthsJson", new Gson().toJson(signalStrengths));

        parameters.put("matrix_R", new Gson().toJson(combinedScanResult.matrix_R));
        parameters.put("matrix_I", new Gson().toJson(combinedScanResult.matrix_I));


        String message = new JSONObject(parameters).toString();

        //------------------------------------------------------------------
        serverMessage.urlString = urlString;
        serverMessage.message = message;
        serverMessage.useSSL = useSSL;
        serverMessage.address = address;


        return serverMessage;
    }// end of encodeCombinedResult
    //==============================================================================================


    //==============================================================================================
    private void postCombinedResult() {



        // empty the queue
        while (this.combinedScanResultQueue.size() > 0) {

            ServerMessage serverMessage = encodeCombinedResult(combinedScanResultQueue.poll());

            PostToServer thisPost = new PostToServer(this,
                    getResources().openRawResource(R.raw.nginxselfsigned),
                    serverMessage
            );

            thisPost.execute();


            this.sendResult("Sending to server: GPS location and WiFi scan.");


        }// end of looping queue

    }// end of postCombinedResult
    //==============================================================================================

    //==============================================================================================
    private void postResends(){

        // empty the queue
        while (this.resendQueue.size() > 0) {

            ServerMessage serverMessage = resendQueue.poll();

            PostToServer thisPost = new PostToServer(this,
                    getResources().openRawResource(R.raw.nginxselfsigned),
                    serverMessage
            );
            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
            //thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);
            //thisPost.wifiScanResult = wifiScanResultResendQueue.poll();

            thisPost.execute();

            this.sendResult("Sending to server: Resend!");


        }// end of looping queue
    }
    //==============================================================================================

    //==============================================================================================
    private void postALL(){



        postResends();

        postCombinedResult();
        postWifiResult();

        if(magAvailable)
            postMagResult();
        if(accelAvailable)
            postAccelResult();
        postLocationResult();



        //this.sendSingleScanResult();

    }
    //==============================================================================================


    //##############################################################################################
    // service functions

    //==============================================================================================
    private void startLocationService() {

        String channelId = "location_notification_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                channelId
        );

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Location Service");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setContentIntent(pendingIntent);
        builder.setContentText("Running");
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (notificationManager != null &&
                    notificationManager.getNotificationChannel(channelId) == null
            ) {
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelId,
                        "Location Service",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationChannel.setDescription("This channel is used by location service");
                notificationManager.createNotificationChannel(notificationChannel);

            }// end of if notificationManager not null
        }// end of if API 26 or greater


        // request on going locations
        //requestlocation(false);

        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());

    }//end of startLocationService
    //==============================================================================================

    //==============================================================================================
    private void stopLocationService() {


        locationManager.removeUpdates(this);

//        try {
//            if (post != null)
//                post.get();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // if service stopped when scanned wifi, the receiver already unregistered
        try {
            unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        }

        stopForeground(true);
        stopSelf();

    }// end of stopLocationService
    //==============================================================================================


    //==============================================================================================
    private void stopService() {

        handler.removeCallbacks(periodicUpdate);

        handler.removeCallbacks(periodicUpdate_wifi);

        if(magAvailable)
            handler.removeCallbacks(periodicUpdate_mag);

        if(accelAvailable)
            handler.removeCallbacks(periodicUpdate_accel);

        //handler.removeCallbacks(periodicUpdate_scan);

        running = false;

        System.out.println("Stop initiated...");

        //
        if(running) {
            wl.release();
        }

    }// end of stop service
    //==============================================================================================

    //==============================================================================================
    public void scanAll(String message){





        Log.d("Trace", "TrackerScanner.scanAll()");

        combinedScanResult = new CombinedScanResult();
        combinedScanResult.message = message;

        // request single updates
        requestlocation(true);

        scanWifi();

        if(magAvailable)
            ScanMag();

        if(accelAvailable)
            ScanAccel();


    }// end of scan all
    //==============================================================================================


    //==============================================================================================
    private void startScanning(){


        //combinedScanResult = new CombinedScanResult();

        //continueScanning = false;

        startLocationService();



        //                    wifiManager = (WifiManager)
        //                            getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wifiScanner:TrackerScanner");
        wl.acquire();

        running = true;

        // fire off a new wifi and gps scan (these also start the timers post hoc)
        handler.post(periodicUpdate);
        handler.post(periodicUpdate_wifi);

        if(magAvailable)
            handler.post(periodicUpdate_mag);

        if(accelAvailable)
            handler.post(periodicUpdate_accel);

        requestlocation(false);

        //handler.post(periodicUpdate_scan);



    }
    //==============================================================================================


    //==============================================================================================
    private void stopScanning(){
        // empty the queues before the lose them
        //postCombinedResult();
        //postWifiResult();
        //postMagResult();
        postALL();


        stopLocationService();
        stopService();
    }
    //==============================================================================================



    //==============================================================================================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {


                switch (action)
                //if (action.equals(Constants.ACTION_START_LOCATION_SERVICE))
                {
                    case Constants.ACTION_START_LOCATION_SERVICE:

                        startScanning();

                        this.sendResult("Started scanning.");

                        stopScanning = false;

                    return START_STICKY;

                    //} else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    case Constants.ACTION_STOP_LOCATION_SERVICE:

                        this.sendResult("Stopping scanning... sending all remaining scans.");

                        stopScanning();

                    break;

                    case Constants.ACTION_SINGLE_SCAN:


                        if(!running){
                            //continueScanning=true;
                            //stopService();

                            startScanning();

                            this.sendResult("Started scanning.");

                            stopScanning = true;

                        }else{
                            stopScanning = false;
                        }


                        manualScanCount_loction = 0;
                        manualScanCount_wifi = 0;
                        manualScanCount_mag = 0;
                        manualScanCount_accel = 0;

                        String message = intent.getStringExtra("message");

                        int manualScanDuration = intent.getIntExtra("duration",-1);

                        this.manualScanMessage = message;

                        this.stopManualScanTime = SystemClock.elapsedRealtime() + (manualScanDuration * 1000);

                        //this.scanAll(message);

                        handler.postDelayed(finaliseManualScan, manualScanDuration * 1000 );

                        handler.postDelayed(updateManualScanCounts, 1000 );


                    break;
                    case Constants.ACTION_POST_ALL:

                        Log.d("Trace", "TrackerScanner.onStartCommand().ACTION_POST_ALL");

                        postALL();

                        //this.sendSingleScanResult();


                    break;

                    case Constants.ACTION_REQUEST_UPDATE:

                        //this.sendSingleScanResult();

                    break;
                }// end of switch case for action type



            }// end of if action not null
        }// end of if intent not null
        return super.onStartCommand(intent, flags, startId);
    }// end of onStartCommand
    //==============================================================================================


}// end of class
