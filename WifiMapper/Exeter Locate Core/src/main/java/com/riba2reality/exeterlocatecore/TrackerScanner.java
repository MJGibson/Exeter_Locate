package com.riba2reality.exeterlocatecore;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.maps.android.PolyUtil;
import com.riba2reality.exeterlocatecore.DataStores.BluetoothLEResult;
import com.riba2reality.exeterlocatecore.DataStores.BluetoothLEScanResult;
import com.riba2reality.exeterlocatecore.DataStores.CombinedScanResult;
import com.riba2reality.exeterlocatecore.DataStores.Constants;
import com.riba2reality.exeterlocatecore.DataStores.LimitedCapacityConcurrentLinkedQueue;
import com.riba2reality.exeterlocatecore.DataStores.LocationResult;
import com.riba2reality.exeterlocatecore.DataStores.SensorResult;
import com.riba2reality.exeterlocatecore.DataStores.ServerMessage;
import com.riba2reality.exeterlocatecore.DataStores.WifiResult;
import com.riba2reality.exeterlocatecore.DataStores.WifiScanResult;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exeter Locate App - Is a citizen science driven project, which allows uses to donate their
 * anonymized Location, Wi-Fi, Bluetooth, accelerometer and magnetometer data. By many citizens
 * contributing small amounts of data in the limited area of the geoFence (University of Exeter -
 * Streatham campus), better locations service could be developed.
 *
 * TrackerScanner Service acts as the main backend for the Exeter Locate App. When running it has two
 * modes, either citizen science mode or development mode. In either mode it performs regular scans
 * of the GPS, Wi-Fi, Bluetooth low-energy, accelerometer and magnetometer, which are placed in a
 * queues for each. At regular intervals the these queues are emptied, converted into json encoded
 * messages to the server, and a new thread is launched to asynchronously post each message to the
 * server.
 * Each message that fails to post to the server is placed back into a resend queue, which is
 * attempted again at each posting interval.
 *
 *
 *
 * @author <a href="mailto:M.J.Gibson@Exeter.ac.uk">Michael J Gibson</a>
 * @version 1.5.0
 * @since   2021-08-19
 *
 */
public class TrackerScanner extends Service implements LocationListener {

    //----------------------------------------------------------------------------------------------

    // as we can no longer access BuildConfig.VERSION_NUM for libraries
    public static final String libraryVersion = "1.7.1";

    //public static final int REQUEST_ENABLE_BT = 11;

    private final int queueByteLimit = 1024 * 1024 * 10; // 10 MB

    private int gPS_lambda = 900;   // gps and combined
    private int gPS_duration = 60;
    private int gPS_scan_interval = 5;

    private int cell_location_interval = 30;

    private int wifi_lambda = 30;
    private int post_lambda = 60;
    private int ble_lambda = 60;
    private int ble_duration = 10;
    private int mag_lambda = 60;
//    private int accel_lambda = 60;

    private int _number_of_GPS_Scan_attempts = 5;
    private int _current_GPS_Scan_attempts = 0;

    public static boolean _test = false;

    //----------------------------------------------------------------------------------------------
    // result/dispatch queues

//    private final Queue<CombinedScanResult> combinedScanResultQueue = new ConcurrentLinkedQueue<>();
//    private final Queue<LocationResult> locationResultQueue = new ConcurrentLinkedQueue<>();
//    private final Queue<WifiScanResult> wifiScanResultQueue = new ConcurrentLinkedQueue<>();
//    private final Queue<SensorResult> magSensorResultQueue = new ConcurrentLinkedQueue<>();
//    private final Queue<SensorResult> accelSensorResultQueue = new ConcurrentLinkedQueue<>();
//    public final Queue<BluetoothLEScanResult> bluetoothLEScanResultQueue =
//            new ConcurrentLinkedQueue<>();
//    public final Queue<ServerMessage> resendQueue = new ConcurrentLinkedQueue<>();


    private final LimitedCapacityConcurrentLinkedQueue messageQueue =
            new LimitedCapacityConcurrentLinkedQueue(queueByteLimit);

    public final LimitedCapacityConcurrentLinkedQueue messageResendQueue =
            new LimitedCapacityConcurrentLinkedQueue(queueByteLimit);

    //----------------------------------------------------------------------------------------------
    // class constants

    static final public String TRACKERSCANNER_RESULT = "com.riba2reality.exeterlocatecore.TrackerScanner.REQUEST_PROCESSED";


    static final public String TRACKERSCANNER_MANUAL_SCAN_RESULT = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_RESULT";
    static final public String TRACKERSCANNER_MANUAL_SCAN_TIMER_UPDATE = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_TIMER_UPDATE";

    // tags
    static final public String TRACKERSCANNER_MESSAGE = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_MSG";
    static final public String TRACKERSCANNER_GEOFENCE_UPDATE = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_GEOFENCE_UPDATE";
    static final public String TRACKERSCANNER_GEOFENCE_UPDATE_FIRST = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_GEOFENCE_UPDATE_FIRST";

    static final public String TRACKERSCANNER_COMBINED_QUEUE_COUNT = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_COMBINED_QUEUE_COUNT";
    static final public String TRACKERSCANNER_RESEND_QUEUE_COUNT = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_RESEND_QUEUE_COUNT";

    static final public String TRACKERSCANNER_LOCATION_QUEUE_COUNT = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_LOCATION_QUEUE_COUNT";
    static final public String TRACKERSCANNER_WIFI_QUEUE_COUNT = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_WIFI_QUEUE_COUNT";
    static final public String TRACKERSCANNER_MAG_QUEUE_COUNT = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_MAG_QUEUE_COUNT";
//    static final public String TRACKERSCANNER_ACCEL_QUEUE_COUNT = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_ACCEL_QUEUE_COUNT";
    static final public String TRACKERSCANNER_BLE_QUEUE_COUNT = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_BLE_QUEUE_COUNT";

    static final public String TRACKERSCANNER_MANUAL_SCAN_REMAINING = "com.riba2reality.exeterlocatecore.TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_REMAINING";

    //final private long _bluetooth_scan_period = 1000;


    final public String MANUAL_SCAN_MESSAGE = "Manual Scan: ";

    final String channelId = "location_notification_channel";

    //----------------------------------------------------------------------------------------------
    // class variables


    private LocationResult lastLocation = new LocationResult();
    private WifiScanResult lastWifiScan;
    private SensorResult lastMagScan;
//    private SensorResult lastAccelScan;




    BluetoothLEScanResult currentResult;

    private boolean _bluetooth_scanning = false;
    //private boolean _bluetooth_scan_queued = false;

    private boolean _locationScanning = false;

    private int manualScanCount_loction = 0;
    private int manualScanCount_wifi = 0;
    private int manualScanCount_mag = 0;
//    private int manualScanCount_accel = 0;
    private int manualScanCount_ble = 0;

    NotificationManager _notificationManager;
    NotificationCompat.Builder _builder;

    private SensorManager sensorManager;
    //private Sensor sensorMag;

    private WifiManager wifiManager;
    public ArrayList<String> arrayList = null;

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;




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
    private long stopGPSScanTime;

    private String manualScanMessage = "None";

    //----------------------------------------------------------------------------------------------

    private boolean magAvailable = false;
//    private boolean accelAvailable = false;


    //----------------------------------------------------------------------------------------------




    // citizen science mode (true), or development mode (false)
    private boolean _mode = false;

    private String _serverAddress;
    private String _database;
    private String _deviceID;
    private boolean _useSSL;
    private String _callingPackage;

    private String _postType;

    private boolean insideGeoFence = false;
    private boolean geoFenceChecked = false;

    private InputStream _userPFX;

    private boolean bluetoothIsOn = false;
    private boolean wifiIsOn = false;
    private boolean gpsIsOn = false;

    private boolean has_periodicUpdate_gps = false;

    private boolean _optOut = false;
    private boolean _optOutReceived = false;

    //----------------------------------------------------------------------------------------------

    //##############################################################################################
    // class functions

    //==============================================================================================
    public void setgPS_lambda(int lambda){
        this.gPS_lambda = lambda;
    }
    //==============================================================================================

    //==============================================================================================
    public void setwifi_lambda(int lambda){
        this.wifi_lambda = lambda;
    }
    //==============================================================================================

    //==============================================================================================
    public void setpost_lambda(int lambda){
        this.post_lambda = lambda;
    }
    //==============================================================================================

    //==============================================================================================
    public void setble_lambda(int lambda){this.ble_lambda = lambda;}
    //==============================================================================================

    //==============================================================================================
    public void setmag_lambda(int lambda){
        this.mag_lambda = lambda;
    }
    //==============================================================================================

//    //==============================================================================================
//    public void setaccel_lambda(int lambda){
//        this.accel_lambda = lambda;
//    }
//    //==============================================================================================

    //==============================================================================================
    public void setgPS_duration(int duration){
        this.gPS_duration = duration;
    }
    //==============================================================================================

    //==============================================================================================
    public void setgPS_scan_interval(int interval){
        this.gPS_scan_interval = interval;
    }
    //==============================================================================================

    //==============================================================================================
    public void setBle_duration(int duration){
        this.ble_duration = duration;
    }
    //==============================================================================================


    //==============================================================================================
    /**
     * onCreate function initialises the class variables
     *
     */
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("Trace", "TrackerScanner.onCreate()");
        //System.out.println("TrackerScanner.onCreate()");

        broadcaster = LocalBroadcastManager.getInstance(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //----------------------------------

        //SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // check has sensors
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)

        {
            // Success! There's a magnetometer.
            magAvailable = true;
        } else {
            // Failure! No magnetometer.
            magAvailable = false;

        }

//        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
//        {
//            // Success! There's a magnetometer.
//            accelAvailable = true;
//        } else {
//            // Failure! No magnetometer.
//            accelAvailable = false;
//
//        }

        //---------------------------------
        //initiate the bluetooth

        // check if bluetooth is available and fetch it
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }else {

            // ble stuff
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }

        // set up user pfx


        //---------------------------------

        _notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        _builder = new NotificationCompat.Builder(
                getApplicationContext(), channelId
        );

        checkBluetoothEnabled();
        checkWifiEnabled();
        checkGpsEnabled();


        this.registerReceiver(receiverBle, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        this.registerReceiver(receiverWifi,
                new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
        this.registerReceiver(receiverGPS, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        this.registerReceiver(receiverTurnOnBle,new IntentFilter("TurnOnBluetooth"));
        this.registerReceiver(receiverTurnOnGPS,new IntentFilter("TurnOnGPS"));
        this.registerReceiver(receiverTurnOnWifi,new IntentFilter("TurnOnWifi"));


    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wl != null) {
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

        if(_mode){

            Log.d("test1", "Tracker.sendResult.ACTION_REQUEST_GEOFENCE_UPDATE()");

            intent.putExtra(TRACKERSCANNER_GEOFENCE_UPDATE, this.insideGeoFence);
            intent.putExtra(TRACKERSCANNER_GEOFENCE_UPDATE_FIRST, this.geoFenceChecked);
        }

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
//        intent.putExtra(TRACKERSCANNER_ACCEL_QUEUE_COUNT, this.manualScanCount_accel);
        intent.putExtra(TRACKERSCANNER_BLE_QUEUE_COUNT, this.manualScanCount_ble);


        broadcaster.sendBroadcast(intent);
    }
    //==============================================================================================

    //==============================================================================================
    @Override
    @Deprecated
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
    //##############################################################################################
    // runnable periodic updates
    //##############################################################################################
    //##############################################################################################



    //==============================================================================================
    public static int getPoisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do {
            k++;
            p *= Math.random();
        } while (p > L);

        return k - 1;
    }// end of getPoisson
    //==============================================================================================

    //==============================================================================================
    private final Runnable finaliseLocationScans = new Runnable() {
        @Override
        public void run() {

            Log.d("mgdev", "finaliseLocationScans.run()");

            stopLocationServices();

            if(!geoFenceChecked && running){

                if(_current_GPS_Scan_attempts >= _number_of_GPS_Scan_attempts){
                    // reset the counter and don't try again
                    _current_GPS_Scan_attempts = 0;
                }else {
                    // try again, while within number of attempts
                    requestlocation();
                }
            }

        }// end of run
    };// end of finaliseLocationScans
    //==============================================================================================


    //==============================================================================================
    private final Runnable updateManualScanCounts = new Runnable() {
        @Override
        public void run() {
            long durationRemaining = stopManualScanTime - SystemClock.elapsedRealtime();

            if (durationRemaining > 0) {
                //if(!running){


                sendManualScanUpdate();


                handler.postDelayed(updateManualScanCounts, 1000);
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

                if (stopScanning) {
                    stopScanning(true);
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

                int postInterval = getResources().getInteger(R.integer.defaultVal_post);
                int interval = postInterval;
                if(_mode){
                    interval = TrackerScanner.getPoisson(post_lambda);

                }else{
                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    interval = SP.getInt("interval_posts", postInterval);
                }


                handler.postDelayed(periodicUpdate, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
            } else {
                return;
            }

            postALL();

        }
    };
    //==============================================================================================


    //==============================================================================================
    private final Runnable periodicUpdate_scan = new Runnable() {
        @Override
        public void run() {
            if (running) {
                int postInterval = getResources().getInteger(R.integer.defaultVal_post);
                int interval = postInterval;
                if(_mode){
                    interval = TrackerScanner.getPoisson(gPS_lambda);
                }else {
                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    interval = SP.getInt("interval_scan", postInterval);
                }
                handler.postDelayed(periodicUpdate_scan, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
            } else {
                return;
            }

            combinedScan();


        }
    };
    //==============================================================================================

    //==============================================================================================
    private final Runnable periodicUpdate_gps = new Runnable() {
        @Override
        public void run() {



            if (running) {

                has_periodicUpdate_gps = true;

                int gpsInterval = getResources().getInteger(R.integer.defaultVal_gps);
                int interval = gpsInterval;
                if(_mode){
                    interval = TrackerScanner.getPoisson(gPS_lambda);
                }else {
                    SharedPreferences SP =
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    interval = SP.getInt("interval_gps", gpsInterval);

                }
                handler.postDelayed(periodicUpdate_gps, interval * 1000
                        - SystemClock.elapsedRealtime() % 1000);
            } else {
                return;
            }

            requestlocation();

        }// end of run
    };
    //==============================================================================================

    //==============================================================================================
    private final Runnable periodicUpdate_wifi = new Runnable() {
        @Override
        public void run() {
            if (running) {
                int wifiInterval = getResources().getInteger(R.integer.defaultVal_wifi);
                int interval = wifiInterval;
                if(_mode){
                    interval = TrackerScanner.getPoisson(wifi_lambda);
                }else {
                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    interval = SP.getInt("interval_wifi", wifiInterval);

                }
                handler.postDelayed(periodicUpdate_wifi, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
            } else {
                return;
            }

            scanWifi();

        }// end of run
    };
    //==============================================================================================

    //==============================================================================================
    private final Runnable periodicUpdate_ble_start = new Runnable() {
        @Override
        public void run() {
            if (running) {
                int bleInterval = getResources().getInteger(R.integer.defaultVal_ble);
                int interval = bleInterval;
                if(_mode){
                    interval = TrackerScanner.getPoisson(ble_lambda);
                }else {
                    SharedPreferences SP =
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    interval = SP.getInt("interval_ble", bleInterval); // todo
                }
                handler.postDelayed(periodicUpdate_ble_start,
                        interval * 1000 - SystemClock.elapsedRealtime() % 1000);

            } else {
                return;
            }

            //Log.d("mgdev", "periodicUpdate_ble_start[" + currentResult.bluetoothLEResults.size() + "]");

            startBLEScan();

        }// end of run function
    };// end of runable periodicUpdate_ble_start
    //==============================================================================================


    //==============================================================================================
    private final Runnable periodicUpdate_ble = new Runnable() {
        @Override
        public void run() {
            if (running) {


            } else {
                return;
            }

            //Log.d("mgdev", "periodicUpdate_ble[" + currentResult.bluetoothLEResults.size() + "]");

            completeBLEScan();

        }// end of run function
    };// end of runable periodicUpdate_ble
    //==============================================================================================


    //==============================================================================================
    private final Runnable periodicUpdate_mag = new Runnable() {
        @Override
        public void run() {
            if (running) {
                int magInterval = getResources().getInteger(R.integer.defaultVal_mag);
                int interval = magInterval;
                if(_mode){
                    interval = TrackerScanner.getPoisson(mag_lambda);
                }else {
                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    interval = SP.getInt("interval_mag", magInterval);
                }
                handler.postDelayed(periodicUpdate_mag, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
            } else {
                return;
            }

            ScanMag();


        }// end of run
    };// end of runnable periodicUpdate_mag
    //==============================================================================================

//    //==============================================================================================
//    private final Runnable periodicUpdate_accel = new Runnable() {
//        @Override
//        public void run() {
//            if (running) {
//                int accelInterval = getResources().getInteger(R.integer.defaultVal_accel);
//                int interval = accelInterval;
//                if(_mode){
//                    interval = TrackerScanner.getPoisson(accel_lambda);
//                }else {
//                    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                    interval = SP.getInt("interval_accel", accelInterval);
//                }
//                handler.postDelayed(periodicUpdate_accel, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
//            } else {
//                return;
//            }
//
//            ScanAccel();
//
//
//        }
//    };
//    //==============================================================================================

    //##############################################################################################
    // recievers

    //==============================================================================================
    BroadcastReceiver receiverBle = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "TrackerScanner.receiverBle.onReceive");

            checkBluetoothEnabled();
            updateNotification();

        }// end of onReceive
    };
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiverWifi = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "TrackerScanner.receiverWifi.onReceive");

            checkWifiEnabled();
            updateNotification();

        }// end of onReceive
    };
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiverGPS = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "TrackerScanner.receiverGPS.onReceive");


            checkGpsEnabled();
            updateNotification();

        }// end of onReceive
    };
    //==============================================================================================

    //##############################################################################################
    // check functions

    //==============================================================================================
    private void checkBluetoothEnabled(){

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth ???
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // Bluetooth is not enable :)
                bluetoothIsOn = false;

            }else{
//                if(bluetoothIsOn != true) {
                    bluetoothIsOn = true;

//                }
            }
        }

    }// end of checkBluetoothEnabled
    //==============================================================================================


    //==============================================================================================
    private void checkWifiEnabled(){

        Log.d("mgdev", "MainActivity.checkWifiEnabled");

        WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()){
            wifiIsOn = false;

        }else{
            wifiIsOn = true;

        }

    }// end of checkWifiEnabled
    //==============================================================================================

    //==============================================================================================
    private void checkGpsEnabled(){

        Log.d("mgdev", "MainActivity.checkGpsEnabled. GPS disabled");

        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            gpsIsOn=false;

        }// end of if gps not enabled
        else{
            gpsIsOn=true;

        }

    }// end of checkGpsEnabled
    //==============================================================================================

    //##############################################################################################
    // combined location, wifi, accel and mag functions

    //==============================================================================================
    public void combinedScan(){

        Log.d("Trace", "TrackerScanner.combinedScan()");

        CombinedScanResult thisCombinedScanResult = new CombinedScanResult();
        thisCombinedScanResult.message = manualScanMessage;

//        if (lastLocation == null){
//            return; // bail
//        }

        thisCombinedScanResult.location = lastLocation.location;
        thisCombinedScanResult.dateTime = lastLocation.dateTime;

        if(lastWifiScan == null){
            return; // bail
        }

        thisCombinedScanResult.wifiScanResult = lastWifiScan;


        if(lastMagScan == null){
            return; // bail
        }

        if(!magAvailable){
            thisCombinedScanResult.magSensorResult = new SensorResult();
        }
        else{
            thisCombinedScanResult.magSensorResult = lastMagScan;
        }

//        if(lastAccelScan == null){
//            return; // bail
//        }
//
//        if(!accelAvailable){
//            thisCombinedScanResult.accelSensorResult = new SensorResult();
//        }
//        else{
//            thisCombinedScanResult.accelSensorResult = lastAccelScan;
//        }
//
//        if(magAvailable && accelAvailable) {
//
//            float gravity[] = {thisCombinedScanResult.accelSensorResult.X,
//                    thisCombinedScanResult.accelSensorResult.Y,
//                    thisCombinedScanResult.accelSensorResult.Z};
//
//            float geomagnetic[] = {thisCombinedScanResult.magSensorResult.X,
//                    thisCombinedScanResult.magSensorResult.Y,
//                    thisCombinedScanResult.magSensorResult.Z};
//
//            float identifyMatrix_R[] = new float[9];
//            float rotationMatrix_I[] = new float[9];
//
//
//
//            boolean success = sensorManager.getRotationMatrix(identifyMatrix_R, rotationMatrix_I,
//                    gravity, geomagnetic
//            );
//
//            Log.d("getRotationMatrix", "identifyMatrix_R: " + Arrays.toString(identifyMatrix_R));
//            Log.d("getRotationMatrix", "rotationMatrix_I: " + Arrays.toString(rotationMatrix_I));
//
//            thisCombinedScanResult.matrix_R = identifyMatrix_R;
//            thisCombinedScanResult.matrix_I = rotationMatrix_I;
//
//        }

        //--------------

        ServerMessage serverMessage = encodeCombinedResult(thisCombinedScanResult);


        this.messageQueue.add(serverMessage);

//        this.combinedScanResultQueue.add(thisCombinedScanResult);


        long durationRemaining = stopManualScanTime - SystemClock.elapsedRealtime();

        //manualScanCount_ += 1;

        //if(!running){
        if (durationRemaining > 0) {

            sendResult(MANUAL_SCAN_MESSAGE +
                    "Combined: Scan complete." + "Location: " + manualScanMessage
            );

        } else {
            sendResult("Combined: Scan complete.");
        }




    }// end of scan all
    //==============================================================================================

    //##############################################################################################
    // location functions

    //==============================================================================================
    public static boolean runningOnEmulator(){

        // Android SDK emulator
        boolean result = (Build.FINGERPRINT.startsWith("google/sdk_gphone_")
                //&& Build.FINGERPRINT.endsWith(":user/release-keys")
                && Build.MANUFACTURER.equalsIgnoreCase("Google")
                && Build.PRODUCT.startsWith("sdk_gphone_")
                & Build.BRAND.equalsIgnoreCase("google")
                && Build.MODEL.startsWith("sdk_gphone_"))
                //
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                //bluestacks
                || "QC_Reference_Phone" == Build.BOARD
                && !"Xiaomi".equalsIgnoreCase(Build.MANUFACTURER) //bluestacks
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST=="Build2" //MSI App Player
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.PRODUCT == "google_sdk";
        return result;

    }// end of runningOnEmulator
    //==============================================================================================


    //==============================================================================================
    /**
     * Start scanning Cell based location
     *
     */
    private void requestCelllocation() {


        boolean runningOnEmulator = runningOnEmulator();


        Log.d("mgdev", "TrackerScanner.requestCelllocation()"+runningOnEmulator);

//            Criteria criteria = new Criteria();
//            criteria.setAccuracy(Criteria.ACCURACY_FINE);
//            criteria.setPowerRequirement(Criteria.POWER_HIGH);
//            String provider = locationManager.getBestProvider(criteria, true);
        String provider = LocationManager.NETWORK_PROVIDER;

        if(runningOnEmulator || _test
           ){
            provider = LocationManager.GPS_PROVIDER;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }


        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int gpsInterval = getResources().getInteger(R.integer.defaultVal_gps);
        // interval needs to be in milliseconds -- scale accordingly
        long interval = gpsInterval;

        if(_mode){
            interval = cell_location_interval;
        }else {
            interval = SP.getInt("interval_gps", gpsInterval);
        }

        // dont forget to make milliseconds
        interval *= 1000;


        // only used in citizen science mode
//        this.stopGPSScanTime = SystemClock.elapsedRealtime() + (gPS_duration * 1000);


        // TODO: Check if permission available
        locationManager.requestLocationUpdates(
                provider,
                interval,
                0,
                cellLocationListener
        );





    }//end of requestCelllocation
    //==============================================================================================


    //==============================================================================================
    private LocationListener cellLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {

            checkGeoFence(location);



        }// end of onLocationChanged

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras){

        }

        @Override
        public void onProviderEnabled(String provider){}

        @Override
        public void onProviderDisabled(String provider){}

    };// end of cellLocationListener
    //==============================================================================================




    //==============================================================================================
    /**
     * Start GPS scanning, with an interval parameter (in seconds), taken from SharedPreferences.
     * Also notes the times these GPS
     * scans are started and calculates the times they should stop if in citizen science mode.
     */
    private void requestlocation() {

        if(!_locationScanning) {

            Log.d("mgdev", "TrackerScanner.requestlocation()");

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            String provider = locationManager.getBestProvider(criteria, true);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }


            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            int gpsInterval = getResources().getInteger(R.integer.defaultVal_gps);
            // interval needs to be in milliseconds -- scale accordingly
            long interval = gpsInterval;

            if(_mode){
                interval = gPS_scan_interval;
            }else {
                interval = SP.getInt("interval_gps", gpsInterval);
            }

            // dont forget to make milliseconds
            interval *= 1000;


            // only used in citizen science mode
            //this.stopGPSScanTime = SystemClock.elapsedRealtime() + (gPS_duration * 1000);
            //this.stopGPSScanTime = (gPS_duration * 1000) - SystemClock.elapsedRealtime();
            this.stopGPSScanTime = gPS_duration * 1000 - SystemClock.elapsedRealtime() % 1000;

            _current_GPS_Scan_attempts += 1;


            handler.postDelayed(finaliseLocationScans, this.stopGPSScanTime);



            // TODO: Check if permission available
            locationManager.requestLocationUpdates(
                    provider,
                    interval,
                    0,
                    this
            );


            _locationScanning = true;
        }//end of if LocationScanning not true


    }//end of requestlocation
    //==============================================================================================


    //==============================================================================================
    @Override
    public void onLocationChanged(@NonNull Location location) {

        if (location == null)
            return;

        Log.d("mgdev", "TrackerScanner.onLocationChanged()");
        //-------------------------------------------------------------

        long durationRemaining;

        if(_mode){

            checkGeoFence(location);



//            durationRemaining = stopGPSScanTime - SystemClock.elapsedRealtime();
//
//            Log.d("mgdev", "TrackerScanner.onLocationChanged(),durationRemaining: "+durationRemaining);
//
//            if (durationRemaining <= 0) {
//
//                stopLocationServices();
//
//
//            } // end of if over gps scan time
//            else {
                // only add the location result, if inside geo fence
                if(insideGeoFence) {
                    addLocationResult(location);
                }
//            }



        }// end of if citizen mode
        else{

            // always add the location result
            addLocationResult(location);

        }

        //-------------------------------------------------------------

        manualScanCount_loction += 1;


        durationRemaining = this.stopManualScanTime - SystemClock.elapsedRealtime();

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

    //==============================================================================================
    private void addLocationResult(@NonNull Location location){

        String currentTime =
                new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String message = "Time:" + currentTime +
                "\nLat:" + location.getLatitude() +
                "\nLong:" + location.getLongitude();


        LocationResult result = new LocationResult();

        result.dateTimeOld = currentTime;

        result.dateTime = location.getTime();

        result.location = location;

        result.message = this.MANUAL_SCAN_MESSAGE;

        lastLocation = result;

//        locationResultQueue.add(result);

        ServerMessage serverMessage = encodeLocationResult(result);
        messageQueue.add(serverMessage);

    }// end of addLocationResult
    //==============================================================================================


    //==============================================================================================
    private void checkGeoFence(@NonNull Location location){

        // check if inide polygon
        LatLng locationCords = new LatLng(location.getLatitude(), location.getLongitude());


        if( location.getProvider().equals(LocationManager.NETWORK_PROVIDER)){



            Location strethamCenterPoint = new Location("strethamCenterPoint");
            strethamCenterPoint.setLatitude(Constants.stethamCampusCenterPoint.latitude);
            strethamCenterPoint.setLongitude(Constants.stethamCampusCenterPoint.longitude);

            float distance = location.distanceTo(strethamCenterPoint);


            Log.d("mgdev", "TrackerScanner.checkGeoFence.distance= "+distance+
                    "     Provider: "+location.getProvider());

            if(distance > 1500){

                // turn off gps updates
                if(has_periodicUpdate_gps){
                    handler.removeCallbacks(periodicUpdate_gps);
                }


            }else{

                if(!has_periodicUpdate_gps){
                    handler.post(periodicUpdate_gps);
                }


            }

            if(_locationScanning){
                return; // if we're scanning gps, use that to check geo fence.
            }


        }// end of if network/cell based location


        boolean inside =
                PolyUtil.containsLocation(
                        locationCords,
                        Constants.stethamCampusPolygon,
                        true);

        // check if we're entering of leaving the geo fence,
        // and thus need to start or stop other scans

        // changing..
        if(insideGeoFence != inside){
            if(inside){
                // entering
                scanOthers();
            }else{
                // leaving
                stopOthers();

            }// end of leaving
        }// end of entering or leaving

        boolean update = false;
        if(insideGeoFence != inside || !geoFenceChecked)
            update = true;


        // finally update class attribute
        insideGeoFence = inside;

        geoFenceChecked = true;

        if(update)
            updateNotification();


    }// end of checkGeoFence
    //==============================================================================================





    //##############################################################################################
    // wifi functions


    //==============================================================================================
    /**
     * Hashes a string
     *
     * @param input input string
     * @return the hashed string
     */
    public static String encryptThisString(String input)
    {
        try {
            // getInstance() method is called with algorithm SHA-224
            MessageDigest md = MessageDigest.getInstance("SHA-224");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            // return the HashText
            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    //==============================================================================================

    //==============================================================================================
    final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "TrackerScanner.BroadcastReceiver.onReceive()");

            List<ScanResult> results = wifiManager.getScanResults();
            unregisterReceiver(wifiReceiver);

            WifiScanResult result = new WifiScanResult();

//            String currentTime = new SimpleDateFormat
//                    ("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());
//
//            result.dateTime = currentTime;

            result.dateTime = Calendar.getInstance().getTimeInMillis();


            result.message = MANUAL_SCAN_MESSAGE;


            //arrayList = new ArrayList<>();
            for (ScanResult scanResult : results) {


                //arrayList.add(scanResult.SSID + " -" + scanResult.BSSID);


                WifiResult wifiResult = new WifiResult();

                wifiResult.macAddress = encryptThisString(scanResult.BSSID);
                wifiResult.signalStrength = scanResult.level;


                result.wifiResult.add(wifiResult);


            }



            ///Log.d("WIFI_UPDATE: ", String.valueOf(arrayList.size()));


            //-------------------------------------------------------------

            lastWifiScan = result;

//            wifiScanResultQueue.add(result);

            ServerMessage serverMessage = encodeWifiResult(result);
            messageQueue.add(serverMessage);


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
        Log.d("mgdev", "TrackerScanner.scanWifi()");
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

//            String currentTime =
//                    new SimpleDateFormat(
//                            "yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());

            SensorResult result = new SensorResult();

            result.dateTime = Calendar.getInstance().getTimeInMillis();

//            result.dateTime_old = currentTime;

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

            lastMagScan = result;

//            magSensorResultQueue.add(result);
            ServerMessage serverMessage = encodeMagResult(result);
            messageQueue.add(serverMessage);


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

        // register the listener above, NOTE microsecond(i.e. not milli[1k], but 1 million-th of a
        // second)
        Log.d("MAGscan", "Initiated");
        sensorManager.registerListener(
                magSensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);

    }// end of startSensor
    //==============================================================================================


//    //##############################################################################################
//    // accelerometer sensor function
//
//
//    //==============================================================================================
//    private SensorEventListener accelSensorListener = new SensorEventListener() {
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//
//            // only perform a single scan
//            sensorManager.unregisterListener(accelSensorListener);
//
//            Log.d("Trace", "TrackerScanner.accelSensorListener.event()");
//
////            String currentTime =
////                    new SimpleDateFormat(
////                            "yyyy-MM-dd:HH:mm:ss",
////                            Locale.getDefault()).format(new Date());
//
//            SensorResult result = new SensorResult();
//
//            result.dateTime = Calendar.getInstance().getTimeInMillis();
//
////            result.dateTime_old = currentTime;
//
//            result.X = event.values[0];
//            result.Y = event.values[1];
//            result.Z = event.values[2];
//
//            result.message = manualScanMessage;
//
//
//            long timeDelay = Long.MAX_VALUE;
//            if(LastTimeStamp != Long.MAX_VALUE){
//
//                timeDelay = event.timestamp - LastTimeStamp;
//                //timeDelay = LastTimeStamp;
//            }
//
//
//
//
//            //-------------------------------------------------------------
//
//            lastAccelScan = result;
//
////            accelSensorResultQueue.add(result);
//            ServerMessage serverMessage = encodeAccelResult(result);
//            messageQueue.add(serverMessage);
//
//
//            LastTimeStamp = event.timestamp;
//
//            //-------------------------------------------------------------
//
//            manualScanCount_accel += 1;
//
//            long durationRemaining = stopManualScanTime - SystemClock.elapsedRealtime();
//
//            //if(!running){
//            if(durationRemaining > 0){
//                //combinedScanResult.accelSensorResult = result;
//                //checkAllScansCompleted();
//
//                sendResult(MANUAL_SCAN_MESSAGE+"Accel Scan updated."
//                                + "Location: " + manualScanMessage
//                                //+" ("+result.X+","+result.Y+","+result.Z+")"
//                        //+"\n["+timeDelay+"]"
//                        //+"\n["+LastTimeStamp+"]"
//                );
//
//            }else{
//
//                sendResult("Accel Scan updated."
//                                //+" ("+result.X+","+result.Y+","+result.Z+")"
//                        //+"\n["+timeDelay+"]"
//                        //+"\n["+LastTimeStamp+"]"
//                );
//            }
//
//        }// end of onSensorChanged
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//
//        }
//    };// end of magnetic sensor event listener;
//    //==============================================================================================
//
//
//
//    //==============================================================================================
//    private void ScanAccel(){
//
//        //sensorMag
//
//        // register the listener above, NOTE microsecond(i.e. not milli[1k], but 1 million-th of a second)
//        Log.d("ACCELscan", "Initiated");
//        sensorManager.registerListener(accelSensorListener,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
//
//    }// end of startSensor
//    //==============================================================================================


    //##############################################################################################
    // bluetooth low engergy sensor function

    //==============================================================================================
    private void completeBLEScan(){

//        bluetoothLEScanResultQueue.add(currentResult);
        ServerMessage serverMessage = encodeBLEResult(currentResult);
        messageQueue.add(serverMessage);

//        String currentTime =
//                new SimpleDateFormat
//                        ("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());


        BluetoothLEScanResult result = new BluetoothLEScanResult();

        result.dateTime = Calendar.getInstance().getTimeInMillis();

//        result.dateTime_old = currentTime;

        result.message = MANUAL_SCAN_MESSAGE;

        currentResult = result;

        long durationRemaining = stopManualScanTime - SystemClock.elapsedRealtime();

        manualScanCount_ble += 1;

        //if(!running){
        if (durationRemaining > 0) {

            //combinedScanResult.wifiScanResult = result;
            //checkAllScansCompleted();

            sendResult(MANUAL_SCAN_MESSAGE +
                    "Ble: Scan complete." + "Location: " + manualScanMessage
            );

        } else {
            sendResult("Ble: Scan complete.");
        }

//        if(_mode){

            stopBLEScanning(false);

//        }


    }// end of completeBLEScan
    //==============================================================================================


    //==============================================================================================
    private void startBLEScan(){

        Log.d("mgdev", "startBLEScan");

        if( bluetoothAdapter.isEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

//            if(bluetoothIsOn!= true){
//                bluetoothIsOn = true;
//                updateNotification();
//            }

            if (!_bluetooth_scanning) {

//                String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss",
//                        Locale.getDefault()).format(new Date());
                BluetoothLEScanResult result = new BluetoothLEScanResult();
//                result.dateTime_old = currentTime;
                result.dateTime = Calendar.getInstance().getTimeInMillis();

                result.message = MANUAL_SCAN_MESSAGE;


                // Stops scanning after a predefined scan period.
                //handler.postDelayed(periodicUpdate_finaliseBleScan, _bluetooth_scan_period);

                currentResult = result;

                _bluetooth_scanning = true;
                bluetoothLeScanner.startScan(leScanCallback);

                // post the handler to stop this these scans, with the required delay
                int bleInterval = getResources().getInteger(R.integer.defaultVal_ble_duration);
                int interval = bleInterval;

                SharedPreferences SP =
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (_mode) {
                    interval = ble_duration;
                } else {
                    interval = SP.getInt("duration_ble", bleInterval);
                }

                // post the handler that will complete the scan with duration
                handler.postDelayed(periodicUpdate_ble,
                        interval * 1000 - SystemClock.elapsedRealtime() % 1000);

            }//end of if not scanning bluetooth

        }
//        else{
//
//            if(bluetoothIsOn!= false){
//                bluetoothIsOn = false;
//                updateNotification();
//            }
//
//
//        }


    }// end of startBLEScan
    //==============================================================================================

    //==============================================================================================
    private ScanCallback leScanCallback;
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            leScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                    super.onScanResult(callbackType, result);

                    Log.d("mgdev", "BluetoothTabFragment.onScanResult");

                    BluetoothLEResult bleResult = new BluetoothLEResult();
                    bleResult.macAddress = encryptThisString(result.getDevice().getAddress());
                    //bleResult.signalStrength = result.getRssi();

//            String message;
//            String device = result.getDevice().getAddress();
//            String name = result.getDevice().getName();
//            message = "" + device + "---"+ name;

                    //result.wifiResult.add(wifiResult);


                    if (!currentResult.bluetoothLEResults.keySet().contains(bleResult)) {
                        //arrayList.add(message);
                        currentResult.bluetoothLEResults.put(bleResult, result.getRssi());

                    }

                }// end of onScanResult
            };
        }
    }// end of ScanCallback
    //==============================================================================================



    //##############################################################################################
    // post result functions

    //==============================================================================================
    private void sendOptOutOption(){



        //----------------------------------------

        //String address = "riba2reality.com";
        String address = _serverAddress;


        String protocol = "http";
        if (_useSSL) {
            protocol += "s";
        }

        String port = Constants.port;

        //String message = "";
        String endpoint = "/user/";

        String urlString = protocol + "://" + address + port + endpoint;

        //
        PostToServer thisPost = new PostToServer(this,
                getResources().openRawResource(com.riba2reality.exeterlocatecore.R.raw.nginxselfsigned),
                getResources().openRawResource(com.riba2reality.exeterlocatecore.R.raw.user),
                encodeResult(_optOut),
                _useSSL,
                address,
                urlString,
                null

        );

        PostToServer.TYPE postType = PostToServer.TYPE.POST;
        thisPost.setPostType(postType);

        thisPost.execute();




    }// end of sendOptOutOption
    //==============================================================================================

    //==============================================================================================
    private ServerMessage encodeResult(boolean result){

        String message = "";

        ServerMessage serverMessage = new ServerMessage();

        //------------------------------------------------------------------
        // build message...

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("UUID", _deviceID);

        parameters.put("DATABASE", _database);

        //parameters.put("MESSAGE", this.manualScanMessage);

        parameters.put("OPT_OUT", String.valueOf(result));

        //------------

        try {
            message = TrackerScanner.getPostDataString(parameters);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //------------------------------------------------------------------
//        serverMessage.urlString = urlString;
        serverMessage.message = message;
        serverMessage.messageType = ServerMessage.MessageType.User;
//        serverMessage.useSSL = _useSSL;
//        serverMessage.address = _serverAddress;


        return serverMessage;
    }// end of encodeResult
    //==============================================================================================

    //==============================================================================================
    private ServerMessage encodeLocationResult(LocationResult locationResult){

        String message = "";

        ServerMessage serverMessage = new ServerMessage();

        //------------------------------------------------------------------

        double latitude = 0.0;
        double longitude = 0.0;
        double altitude = 0.0;
        double accuracy = 0.0;
//        String gpsTime = "";
//        String provider = "";
        long time = -1;

        if (locationResult.location != null) {
            latitude = locationResult.location.getLatitude();
            longitude = locationResult.location.getLongitude();
            altitude = locationResult.location.getAltitude();
            accuracy = locationResult.location.getAccuracy();

//            gpsTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss",
//                    Locale.getDefault()).format(new Date(locationResult.location.getTime()));

//            provider = locationResult.location.getProvider();

            time = locationResult.location.getTime();

        }
        //------------------------------------------------------------------
        // build message...

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("UUID", _deviceID);

        parameters.put("DATABASE", _database);

        parameters.put("MESSAGE", this.manualScanMessage);

        //------------
        // GPS

        parameters.put("GPS_TIME", Long.toString(time));

        //parameters.put("GPS_TIME_OLD", gpsTime);



        parameters.put("X", Double.toString(latitude));
        parameters.put("Y", Double.toString(longitude));
        parameters.put("ALTITUDE", Double.toString(altitude));
        parameters.put("ACC", Double.toString(accuracy));

//        parameters.put("PROVIDER", provider);

        try {
            message = getPostDataString(parameters);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //------------------------------------------------------------------
//        serverMessage.urlString = urlString;
        serverMessage.message = message;
        serverMessage.messageType = ServerMessage.MessageType.LOCATION;
//        serverMessage.useSSL = _useSSL;
//        serverMessage.address = _serverAddress;


        return serverMessage;
    }// end of encodeLocationResult
    //==============================================================================================

//    //==============================================================================================
//    private void postLocationResult() {
//
//        // empty the queue
//        while (this.locationResultQueue.size() > 0) {
//
//            ServerMessage serverMessage = encodeLocationResult(locationResultQueue.poll());
//
//            PostToServer thisPost = new PostToServer(this,
//                    getResources().openRawResource(R.raw.fullchain),
//                    getResources().openRawResource(R.raw.user),
//                    serverMessage
//            );
//            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
//            //thisPost.is = getResources().openRawResource(R.raw.fullchain);
//            //thisPost.wifiScanResult = wifiScanResultResendQueue.poll();
//
//            thisPost.execute();
//
//            this.sendResult("Sending to server: GPS scan result.");
//
//
//        }// end of looping queue
//
//
//    }// end of postWifiResult
//    //==============================================================================================


    //==============================================================================================
    private ServerMessage encodeWifiResult(WifiScanResult wifiScanResult){

        ServerMessage serverMessage = new ServerMessage();


        //------------------------------------------------------------------

        List<String> macAddressList = new ArrayList<>();
        List<String> signalStrengths = new ArrayList<>();

        for (WifiResult wifiResult : wifiScanResult.wifiResult) {

            macAddressList.add(wifiResult.macAddress);
            signalStrengths.add(Integer.toString(wifiResult.signalStrength));
        }

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("UUID", _deviceID);

        parameters.put("DATABASE", _database);

        parameters.put("MESSAGE", this.manualScanMessage);

//        parameters.put("WIFI_TIME", wifiScanResult.dateTime);

        parameters.put("WIFI_TIME",
                Long.toString(wifiScanResult.dateTime));

        String macAddressJson = new Gson().toJson(macAddressList);

        parameters.put("MacAddressesJson", macAddressJson);

        parameters.put("signalStrengthsJson", new Gson().toJson(signalStrengths));

        String message = "";

        try {
            message = getPostDataString(parameters);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //------------------------------------------------------------------
        serverMessage.message = message;
        serverMessage.messageType = ServerMessage.MessageType.WIFI;


        return serverMessage;
    }// end of encodeWifiResult
    //==============================================================================================

//    //==============================================================================================
//    private void postWifiResult() {
//
//        // empty the queue
//        while (this.wifiScanResultQueue.size() > 0) {
//
//            ServerMessage serverMessage = encodeWifiResult(wifiScanResultQueue.poll());
//
//            PostToServer thisPost = new PostToServer(this,
//                    getResources().openRawResource(R.raw.fullchain),
//                    getResources().openRawResource(R.raw.user),
//                    serverMessage
//                    );
//            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
//            //thisPost.is = getResources().openRawResource(R.raw.fullchain);
//            //thisPost.wifiScanResult = wifiScanResultResendQueue.poll();
//
//            thisPost.execute();
//
//            this.sendResult("Sending to server: WiFi scan result.");
//
//
//        }// end of looping queue
//
//
//    }// end of postWifiResult
//    //==============================================================================================

    //==============================================================================================
    private ServerMessage encodeBLEResult(BluetoothLEScanResult bleScanResult){

        ServerMessage serverMessage = new ServerMessage();

        //------------------------------------------------------------------

        List<String> macAddressList = new ArrayList<>();
        List<String> signalStrengths = new ArrayList<>();

        for (BluetoothLEResult bleResult : bleScanResult.bluetoothLEResults.keySet()) {

            macAddressList.add(bleResult.macAddress);
            signalStrengths.add(Integer.toString(bleScanResult.bluetoothLEResults.get(bleResult)));
        }

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("UUID", _deviceID);

        parameters.put("DATABASE", _database);

        parameters.put("MESSAGE", this.manualScanMessage);

        parameters.put("BLE_TIME", Long.toString(bleScanResult.dateTime));

//        parameters.put("BLE_TIME_OLD", bleScanResult.dateTime_old);

        String macAddressJson = new Gson().toJson(macAddressList);

        parameters.put("MacAddressesJson", macAddressJson);

        parameters.put("signalStrengthsJson", new Gson().toJson(signalStrengths));

        String message = "";

        try {
            message = getPostDataString(parameters);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //------------------------------------------------------------------
        serverMessage.message = message;
        serverMessage.messageType = ServerMessage.MessageType.BLUETOOTH;


        return serverMessage;
    }// end of encodeBLEResult
    //==============================================================================================

//    //==============================================================================================
//    private void postBLEResult() {
//
//        // empty the queue
//        while (this.bluetoothLEScanResultQueue.size() > 0) {
//
//            ServerMessage serverMessage = encodeBLEResult(bluetoothLEScanResultQueue.poll());
//
//            PostToServer thisPost = new PostToServer(this,
//                    getResources().openRawResource(R.raw.fullchain),
//                    getResources().openRawResource(R.raw.user),
//                    serverMessage
//            );
//            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
//            //thisPost.is = getResources().openRawResource(R.raw.fullchain);
//            //thisPost.wifiScanResult = wifiScanResultResendQueue.poll();
//
//            thisPost.execute();
//
//            this.sendResult("Sending to server: BLE scan result.");
//
//
//        }// end of looping queue
//
//
//    }// end of postBLEResult
//    //==============================================================================================



    //==============================================================================================
    private ServerMessage encodeMagResult(SensorResult magSensorResult){

        ServerMessage serverMessage = new ServerMessage();

        //------------------------------------------------------------------

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("UUID", _deviceID);

        parameters.put("DATABASE", _database);

        parameters.put("MESSAGE", this.manualScanMessage);

        parameters.put("MAG_TIME", Long.toString(magSensorResult.dateTime));

//        parameters.put("MAG_TIME_OLD", magSensorResult.dateTime_old);
        //-----

        parameters.put("MAG_X",Double.toString(magSensorResult.X));
        parameters.put("MAG_Y",Double.toString(magSensorResult.Y));
        parameters.put("MAG_Z",Double.toString(magSensorResult.Z));
        //-----

        String message = "";

        try {
            message = getPostDataString(parameters);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //------------------------------------------------------------------

        serverMessage.message = message;
        serverMessage.messageType = ServerMessage.MessageType.MAG;


        return serverMessage;
    }// end of encodeMagResult
    //==============================================================================================


//    //==============================================================================================
//    private ServerMessage encodeAccelResult(SensorResult sensorResult){
//
//        ServerMessage serverMessage = new ServerMessage();
//
//        //------------------------------------------------------------------
//
//        HashMap<String, String> parameters = new HashMap<>();
//
//        parameters.put("UUID", _deviceID);
//
//        parameters.put("DATABASE", _database);
//
//        parameters.put("MESSAGE", this.manualScanMessage);
//
//        parameters.put("ACCEL_TIME", Long.toString(sensorResult.dateTime));
//
////        parameters.put("ACCEL_TIME_OLD", sensorResult.dateTime_old);
//        //-----
//
//        parameters.put("ACCEL_X",Double.toString(sensorResult.X));
//        parameters.put("ACCEL_Y",Double.toString(sensorResult.Y));
//        parameters.put("ACCEL_Z",Double.toString(sensorResult.Z));
//        //-----
//
//        String message = "";
//
//        try {
//            message = getPostDataString(parameters);
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//
//        //------------------------------------------------------------------
//
//        serverMessage.message = message;
//        serverMessage.messageType = ServerMessage.MessageType.ACCEL;
//
//
//
//        return serverMessage;
//    }// end of encodeAccelResult
//    //==============================================================================================




    //==============================================================================================
    private ServerMessage encodeCombinedResult(CombinedScanResult combinedScanResult){

        ServerMessage serverMessage = new ServerMessage();

        //------------------------------------------------------------------

        double latitude = 0.0;
        double longitude = 0.0;
        double altitude = 0.0;
        double accuracy = 0.0;
//        String gpsTime = "";
        long gpsTime = -1;

        if (combinedScanResult.location != null) {
            latitude = combinedScanResult.location.getLatitude();
            longitude = combinedScanResult.location.getLongitude();
            altitude = combinedScanResult.location.getAltitude();
            accuracy = combinedScanResult.location.getAccuracy();

//            gpsTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss",
//                    Locale.getDefault()).format(new Date(combinedScanResult.location.getTime()));

            gpsTime = Calendar.getInstance().getTimeInMillis();

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

        //------------------------------------------------------------------
        // build message...

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put("UUID", _deviceID);

        parameters.put("DATABASE", _database);

        parameters.put("MESSAGE", combinedScanResult.message);

        //------------
        //------------
        // GPS
        parameters.put("GPS_TIME", Long.toString(gpsTime));

        parameters.put("X", Double.toString(latitude));
        parameters.put("Y", Double.toString(longitude));
        parameters.put("ALTITUDE", Double.toString(altitude));
        parameters.put("ACC", Double.toString(accuracy));

        //------------
        // magnetic
        //if(combinedScanResult.magSensorResult!=null) {
            parameters.put("MAG_TIME", Long.toString(combinedScanResult.magSensorResult.dateTime));
            parameters.put("MAG_X", String.valueOf(combinedScanResult.magSensorResult.X));
            parameters.put("MAG_Y", String.valueOf(combinedScanResult.magSensorResult.Y));
            parameters.put("MAG_Z", String.valueOf(combinedScanResult.magSensorResult.Z));
        //}

        //------------
//        // accelerometer
//
//        parameters.put("ACCEL_TIME", Long.toString(combinedScanResult.accelSensorResult.dateTime));
//        parameters.put("ACCEL_X", String.valueOf(combinedScanResult.accelSensorResult.X));
//        parameters.put("ACCEL_Y", String.valueOf(combinedScanResult.accelSensorResult.Y));
//        parameters.put("ACCEL_Z", String.valueOf(combinedScanResult.accelSensorResult.Z));

        //------------
        // wifi

        parameters.put("WIFI_TIME",
                Long.toString(combinedScanResult.wifiScanResult.dateTime));
        String macAddressJson = new Gson().toJson(macAddressList);

        parameters.put("MacAddressesJson", macAddressJson);

        parameters.put("signalStrengthsJson", new Gson().toJson(signalStrengths));

//        parameters.put("matrix_R", new Gson().toJson(combinedScanResult.matrix_R));
//        parameters.put("matrix_I", new Gson().toJson(combinedScanResult.matrix_I));

        String message = "";

        try {
            message = getPostDataString(parameters);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //------------------------------------------------------------------

        serverMessage.message = message;
        serverMessage.messageType = ServerMessage.MessageType.COMBINED;


        return serverMessage;
    }// end of encodeCombinedResult
    //==============================================================================================

    //==============================================================================================
    public static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            if(entry.getValue()==null){
                result.append(URLEncoder.encode("", "UTF-8"));
            }else {
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
        }

        return result.toString();
    }
    //==============================================================================================

    //==============================================================================================
    private void postResults(LimitedCapacityConcurrentLinkedQueue queueRef) {

        String protocol = "http";
        if (_useSSL) {
            protocol += "s";
        }

        String port = Constants.port;

        String message = "";



        // empty the queue
        while (queueRef.size() > 0) {

            PostToServer.TYPE postType = PostToServer.TYPE.POST;

            ServerMessage serverMessage = queueRef.poll();

            // switch on type
            String endpoint = "/gps/";

            switch (serverMessage.messageType){
                case MAG:
                    endpoint = "/mag/";
                    message = "Magnetic";
                    break;
                case WIFI:
                    endpoint = "/wifi/";
                    message = "Wi-Fi";
                    postType = PostToServer.TYPE.valueOf(_postType);
                    break;
//                case ACCEL:
//                    endpoint = "/accel/";
//                    message = "Accelerometer";
//                    break;
                case COMBINED:
                    endpoint = "/";
                    message = "Combined";
                    break;
                case LOCATION:
                    endpoint = "/gps/";
                    message = "Location";
                    break;
                case BLUETOOTH:
                    endpoint = "/ble/";
                    message = "Bluetooth";
                    break;
                case User:
                    endpoint = "/user/";
                    message = "User";
                    break;

            }

            String urlString = protocol + "://" + _serverAddress + port + endpoint;

            PostToServer thisPost = new PostToServer(this,
                    getResources().openRawResource(R.raw.nginxselfsigned),
                    getResources().openRawResource(R.raw.user),
                    serverMessage,
                    _useSSL,
                    _serverAddress,
                    urlString,
                    null

            );

            thisPost.setPostType(postType);

            thisPost.execute();


            this.sendResult("Sending to server: "+message+" scan.");


        }// end of looping queue

    }// end of postCombinedResult
    //==============================================================================================

//    //==============================================================================================
//    private void postResends(){
//
//        String protocol = "http";
//        if (_useSSL) {
//            protocol += "s";
//        }
//
//        String port = Constants.port;
//
//        // switch on type
//        String endpoint = "/gps/";
//
//        String urlString = protocol + "://" + _serverAddress + port + endpoint;
//
//        // empty the queue
//        while (this.resendQueue.size() > 0) {
//
//            ServerMessage serverMessage = resendQueue.poll();
//
//            PostToServer thisPost = new PostToServer(this,
//                    getResources().openRawResource(R.raw.fullchain),
//                    getResources().openRawResource(R.raw.user),
//                    serverMessage
//            );
//            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
//            //thisPost.is = getResources().openRawResource(R.raw.fullchain);
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

    //==============================================================================================
    private void postALL(){


        Log.d("mgdev", "TrackerScanner.postALL()");

//        postResends();
//        postCombinedResult();
//        postWifiResult();
//        postBLEResult();
//        if(magAvailable)
//            postMagResult();
//        if(accelAvailable)
//            postAccelResult();
//        postLocationResult();

        postResults(this.messageQueue);

        postResults(this.messageResendQueue);



        //this.sendSingleScanResult();

    }
    //==============================================================================================


    //##############################################################################################
    // service functions

    //==============================================================================================
    private void stopNotificationService(boolean stopSelf) {

        stopForeground(true);
        if(stopSelf) {

            stopSelf();
        }
        try{
            this.unregisterReceiver(receiverBle);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        try{
            this.unregisterReceiver(receiverWifi);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        try{
        this.unregisterReceiver(receiverGPS);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        try{
        this.unregisterReceiver(receiverTurnOnBle);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        try{
        this.unregisterReceiver(receiverTurnOnGPS);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        try{
        this.unregisterReceiver(receiverTurnOnWifi);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }



    }// end of stopNotificationService
    //==============================================================================================


    //==============================================================================================
    private void stopService() {

        stopLocationServices();

        locationManager.removeUpdates(cellLocationListener);

        handler.removeCallbacks(periodicUpdate);

        handler.removeCallbacks(finaliseLocationScans);

        running = false;

        // always stop others here?
        stopOthers();

        System.out.println("Stop initiated...");

        //
        if(running) {
            wl.release();
        }

    }// end of stop service
    //==============================================================================================


    //==============================================================================================
    private void stopLocationServices(){

        handler.removeCallbacks(periodicUpdate_gps);

        has_periodicUpdate_gps = false;

        locationManager.removeUpdates(this);
        _locationScanning = false;



    }// end of stopLocationServices
    //==============================================================================================


    //==============================================================================================
    private void stopOthers(){



        handler.removeCallbacks(periodicUpdate_wifi);
        // if service stopped when scanned wifi, the receiver already unregistered
        try {
            unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        }

        if(magAvailable)
            handler.removeCallbacks(periodicUpdate_mag);

//        if(accelAvailable)
//            handler.removeCallbacks(periodicUpdate_accel);

        handler.removeCallbacks(periodicUpdate_scan);

        stopBLEScanning(true);



    }// end of stopOthers
    //==============================================================================================

    //==============================================================================================
    private void stopBLEScanning(boolean stopStartingBLEScans){

        Log.d("mgdev", "TrackerScanner.stopBLEScanning()");

        handler.removeCallbacks(periodicUpdate_ble);
        if(stopStartingBLEScans){
            handler.removeCallbacks(periodicUpdate_ble_start);
        }

        _bluetooth_scanning = false;

        if(bluetoothLeScanner != null && bluetoothAdapter.isEnabled()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        )
            bluetoothLeScanner.stopScan(leScanCallback);

    }// end of stopBLEScanning
    //==============================================================================================


    //==============================================================================================
    private void updateNotification() {

        Log.d("mgdev", "TrackerScanner.updateNotification");

        String message = "Scanning...";


        if(!geoFenceChecked)
            message += "Location not determined...";
        else{
            if(insideGeoFence){
                message = "On-campus Scanning...";
            }else{
                message = "Off-campus, Not recording data...";
            }
        }


        if(!bluetoothIsOn)
            message += "\nBluetooth is turned off";

        if(!wifiIsOn)
            message += "\nWi-Fi is turned off";

        if(!gpsIsOn)
            message += "\nGPS is turned off";




        if(!
                (bluetoothIsOn && wifiIsOn && gpsIsOn)
        ) {
            _builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            _builder.clearActions();


            if(!bluetoothIsOn) {
                Intent enableBtIntent = new Intent("TurnOnBluetooth");
                //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //snoozeIntent.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
                PendingIntent enableBtPendingIntent =
                        PendingIntent.getBroadcast(this, 0, enableBtIntent, 0);
                _builder.addAction(R.drawable.bluetoot_disconnected_foreground, "Turn on Bluetooth"
                        , enableBtPendingIntent);
            }

            if(!gpsIsOn){

                Intent enableGPSIntent = new Intent("TurnOnGPS");
                PendingIntent enableGPSPendingIntent =
                        PendingIntent.getBroadcast(this, 0, enableGPSIntent, 0);
                _builder.addAction(R.drawable.bluetoot_disconnected_foreground, "Turn on GPS"
                        , enableGPSPendingIntent);

            }// end of if gps is not on

            if(!wifiIsOn){

                Intent enableWifiIntent = new Intent("TurnOnWifi");
                PendingIntent enableWifiPendingIntent =
                        PendingIntent.getBroadcast(this, 0, enableWifiIntent, 0);
                _builder.addAction(R.drawable.bluetoot_disconnected_foreground, "Turn on Wi-Fi"
                        , enableWifiPendingIntent);


            }// end of if wifi is not on

        }// end of any of bluetooth,wifi, gps are off
        else{
            _builder.setStyle(null);
            _builder.clearActions();

        }

        _builder.setContentText(message);
        // these should already be set, but seems some phones need re-directing, particularlly
        // huawei
        _builder.setSmallIcon(R.mipmap.exeter_locate_icon);
        _builder.setContentTitle("Exeter Locate");
        _builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        _builder.setContentText(message);
        _builder.setAutoCancel(true);
        //_builder.setOnlyAlertOnce(true);
        _builder.setPriority(NotificationCompat.PRIORITY_MAX);


        _notificationManager.notify(Constants.LOCATION_SERVICE_ID, _builder.build());


    }//end of updateNotification
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiverTurnOnBle = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "TrackerScanner.receiverTurnOnBle");

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth ???
            } else {
                if (!mBluetoothAdapter.isEnabled()) {
                    // Bluetooth is not enable :)
                    mBluetoothAdapter.enable();

                }
            }

        }// end of onReceive
    };
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiverTurnOnGPS = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "TrackerScanner.receiverTurnOnGPS");

            Intent intentOpenGPSSettings = new Intent();
            intentOpenGPSSettings.setAction(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intentOpenGPSSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentOpenGPSSettings);

        }// end of onReceive
    };
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiverTurnOnWifi = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "TrackerScanner.receiverTurnOnWifi");

            WifiManager wManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (Build.VERSION.SDK_INT<Build.VERSION_CODES.Q)
                wManager.setWifiEnabled(true); // deprecated by google...
            else
            {
                //Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);

                Intent intentOpenWifiSettings = new Intent();
                intentOpenWifiSettings.setAction(
                        android.provider.Settings.ACTION_WIFI_SETTINGS);
                intentOpenWifiSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentOpenWifiSettings);
            }

        }// end of onReceive
    };
    //==============================================================================================


    //==============================================================================================
    private void startNotificationService() {

//        String channelId = "location_notification_channel";
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

//        NotificationCompat.Builder builder = new NotificationCompat.Builder(
//                getApplicationContext(),
//                channelId
//        );

        _builder.setSmallIcon(R.mipmap.exeter_locate_icon);
        _builder.setContentTitle("Exeter Locate");
        _builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        _builder.setContentIntent(pendingIntent);
        _builder.setContentText("Scanning, Location not determined...");
        _builder.setAutoCancel(true);
        //_builder.setOnlyAlertOnce(true);
        _builder.setPriority(NotificationCompat.PRIORITY_MAX);
        _builder.setSilent(true);

        //builder.setOngoing(false);

        Class<?> clazz = null;

        try {
            clazz = Class.forName(_callingPackage);

            Intent notificationIntent = new Intent(getApplicationContext(), clazz);

            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntentNotifPressed = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            _builder.setContentIntent(pendingIntentNotifPressed);

            Log.d("mgdev", "TrackerScanner.startNotificationService()._callingPackage: "+_callingPackage);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (_notificationManager != null &&
                    _notificationManager.getNotificationChannel(channelId) == null
            ) {
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelId,
                        "Exeter Locate",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationChannel.setDescription("This channel is used by Exeter Locate");
                notificationChannel.setSound(null,null);

                _notificationManager.createNotificationChannel(notificationChannel);
            }// end of if notificationManager not null
        }// end of if API 26 or greater


        Notification notification = _builder.build();

        //notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;



        startForeground(Constants.LOCATION_SERVICE_ID, notification);

    }//end of startNotificationService
    //==============================================================================================


    //==============================================================================================
    private void startScanning(){

        geoFenceChecked = false;

        running = true;
        startNotificationService();

        // get wake lock to keep service alive
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wifiScanner:TrackerScanner");
        wl.acquire();

        _current_GPS_Scan_attempts = 0;

        // activate the location service
        if(_mode){
            handler.post(periodicUpdate_gps);

            requestCelllocation();

        }else {
            requestlocation();



        }

        // always activate the posting
        handler.post(periodicUpdate);


        // check if citizen mode, then only lunch other scans if inside geoFence
        if(_mode){
            if(insideGeoFence){
                scanOthers();
            }
        }else{
            // dev mode: always launch other scans
            scanOthers();
        }

        //
        if(!_optOutReceived)
            sendOptOutOption();


    }// end of startScanning
    //==============================================================================================





    //==============================================================================================
    private void scanOthers(){

        handler.post(periodicUpdate_wifi);

        if(magAvailable)
            handler.post(periodicUpdate_mag);

//        if(accelAvailable)
//            handler.post(periodicUpdate_accel);



        handler.post(periodicUpdate_scan); // combined

//        if (!bluetoothAdapter.isEnabled()) {
//            // not alot we can do at this point, UI has to of already asked for this
//        } else {
//            startBLEScan();
//        }

        handler.post(periodicUpdate_ble_start);

    }//end of scanOthers
    //==============================================================================================




    //==============================================================================================
    private void stopScanning(boolean stopSelf){
        // empty the queues before the lose them
        //postCombinedResult();
        //postWifiResult();
        //postMagResult();
        postALL();


        stopNotificationService(stopSelf);

        stopService();


    }
    //==============================================================================================


    //==============================================================================================
    private void getDataFromIntent(Intent intent){

        _mode = intent.getBooleanExtra("MODE", false);

        _serverAddress = intent.getStringExtra("ServerAddress");
        _database = intent.getStringExtra("database");
        _deviceID = intent.getStringExtra("DeviceID");
        _useSSL = intent.getBooleanExtra("SSL_switch", true);

        _callingPackage = intent.getStringExtra("PACKAGE");

        _postType = intent.getStringExtra("post_type");

        _optOut = intent.getBooleanExtra("OPT-OUT", false);
        _optOutReceived = intent.getBooleanExtra("OPT-OUT-RECEIVED", false);


    }// end of getDataFromIntent
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

                        Log.d("mgdev", "TrackerScanner.onStartCommand().ACTION_START_LOCATION_SERVICE");

                        getDataFromIntent(intent);

                        if(running){
                            stopScanning(false);
                        }

                        startScanning();

                        this.sendResult("Started scanning.");

                        stopScanning = false;


                        return START_STICKY;

                    //} else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    case Constants.ACTION_STOP_LOCATION_SERVICE:

                        this.sendResult("Stopping scanning... sending all remaining scans.");

                        stopScanning(true);


                        break;

                    case Constants.ACTION_SINGLE_SCAN: // used by wifiMapper for manual scans

                        getDataFromIntent(intent);

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
//                        manualScanCount_accel = 0;
                        manualScanCount_ble = 0;

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

                    case Constants.ACTION_REQUEST_GEOFENCE_UPDATE:

                        this.sendResult(Constants.ACTION_REQUEST_GEOFENCE_UPDATE);

                        Log.d("test1", "Tracker.ACTION_REQUEST_GEOFENCE_UPDATE()");


                        break;

                }// end of switch case for action type



            }// end of if action not null
        }// end of if intent not null
        return super.onStartCommand(intent, flags, startId);
    }// end of onStartCommand
    //==============================================================================================


}// end of class
