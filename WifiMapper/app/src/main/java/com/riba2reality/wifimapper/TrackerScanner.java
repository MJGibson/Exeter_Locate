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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

public class TrackerScanner extends Service implements LocationListener {


    //----------------------------------------------------------------------------------------------
    // result/dispatch queues

    private final Queue<WifiScanResult> wifiScanResultQueue = new ConcurrentLinkedQueue<>();
    public final Queue<WifiScanResult> wifiScanResultResendQueue = new ConcurrentLinkedQueue<>();

    private final Queue<CombinedScanResult> combinedScanResultQueue = new ConcurrentLinkedQueue<>();
    public final Queue<CombinedScanResult> combinedScanResultResendQueue = new ConcurrentLinkedQueue<>();

    private final Queue<MagSensorResult> magSensorResultQueue = new ConcurrentLinkedQueue<>();
    public final Queue<MagSensorResult> magSensorResultResendQueue = new ConcurrentLinkedQueue<>();


    //----------------------------------------------------------------------------------------------

    // class variables

    private SensorManager sensorManager;
    private Sensor sensorMag;

    private WifiManager wifiManager;
    public ArrayList<String> arrayList = null;

    private PostToServer post;

    LocalBroadcastManager broadcaster;

    static final public String TRACKERSCANNER_RESULT = "com.riba2reality.wifimapper.TrackerScanner.REQUEST_PROCESSED";

    static final public String TRACKERSCANNER_MESSAGE = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_MSG";

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




    //##############################################################################################
    // class functions

    //==============================================================================================
    @Override
    public void onCreate() {
        super.onCreate();

        broadcaster = LocalBroadcastManager.getInstance(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

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
    public void sendResult(String message) {
        Intent intent = new Intent(TRACKERSCANNER_RESULT);
        if (message != null)
            intent.putExtra(TRACKERSCANNER_MESSAGE, message);
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

            postCombinedResult();

            postWifiResult();

            postMagResult();


        }
    };
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




    //##############################################################################################
    // location functions

    //==============================================================================================
    private void requestlocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        String provider = locationManager.getBestProvider(criteria, true);

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


        //locationManager.requestSingleUpdate(provider, this, Looper.getMainLooper());


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

        // Location lastLocation = new Location(location);

        CombinedScanResult combinedScanResult = new CombinedScanResult();
        combinedScanResult.location = new Location(location);

        combinedScanResultQueue.add(combinedScanResult);

        this.sendResult("GPS: Location updated.");

        //
        scanWifi();

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




            arrayList = new ArrayList<>();
            for (ScanResult scanResult : results) {


                arrayList.add(scanResult.SSID + " -" + scanResult.BSSID);


                WifiResult wifiResult = new WifiResult();
                wifiResult.macAddress = scanResult.BSSID;
                wifiResult.signalStrength = scanResult.level;


                result.wifiResult.add(wifiResult);


            }

            sendResult("WiFi: Scan complete.");

            Log.d("WIFI_UPDATE: ", String.valueOf(arrayList.size()));

            wifiScanResultQueue.add(result);

            CombinedScanResult combinedScanResult = combinedScanResultQueue.peek();

            if (combinedScanResult != null && combinedScanResultQueue.peek().dateTime == null) {
                combinedScanResult.dateTime = currentTime;
                combinedScanResult.wifiResult = result.wifiResult;
            }



        }
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


            sendResult("Mag Scan updated"
                            +"("+result.X+","+result.Y+","+result.Z+")"
                            +"\n["+timeDelay+"]"
                    //+"\n["+LastTimeStamp+"]"
            );


            magSensorResultQueue.add(result);


            LastTimeStamp = event.timestamp;

            // only perform a single scan
            sensorManager.unregisterListener(magSensorListener);

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
    // post result functions

    //==============================================================================================
    private void postWifiResult() {

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


        // empty the resend queue first
        while (this.wifiScanResultResendQueue.size() > 0) {

            PostWifiResultToServer thisPost = new PostWifiResultToServer(this);

            thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);

            thisPost.wifiScanResult = wifiScanResultResendQueue.poll();

            thisPost.execute(
                    address,
                    protocol,
                    String.valueOf(useSSL),
                    deviceID,
                    dataBase
            );

            this.sendResult("Sending to server: WiFi scan result (resend queue).");


        }// end of looping queue

        // empty the queue
        while (this.wifiScanResultQueue.size() > 0) {

            PostWifiResultToServer thisPost = new PostWifiResultToServer(this);

            thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);

            thisPost.wifiScanResult = wifiScanResultQueue.poll();


            thisPost.execute(
                    address,
                    protocol,
                    String.valueOf(useSSL),
                    deviceID,
                    dataBase
            );

            this.sendResult("Sending to server: WiFi scan result.");


        }// end of looping queue

    }// end of postWifiResult
    //==============================================================================================

    //==============================================================================================
    private void postMagResult() {

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


        // empty the resend queue first
        while (this.magSensorResultResendQueue.size() > 0) {

            PostMagResultToServer thisPost = new PostMagResultToServer(this);

            thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);

            thisPost.magSensorResult = magSensorResultResendQueue.poll();

            thisPost.execute(
                    address,
                    protocol,
                    String.valueOf(useSSL),
                    deviceID,
                    dataBase
            );

            this.sendResult("Sending to server: Magnetic senor result (resend queue).");


        }// end of looping queue

        // empty the queue
        while (this.magSensorResultQueue.size() > 0) {

            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
            PostMagResultToServer thisPost = new PostMagResultToServer(this);

            thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);

            thisPost.magSensorResult = magSensorResultQueue.poll();


            thisPost.execute(
                    address,
                    protocol,
                    String.valueOf(useSSL),
                    deviceID,
                    dataBase
            );

            this.sendResult("Sending to server: Magenetic Sensor result.");


        }// end of looping queue

    }// end of postWifiResult
    //==============================================================================================


    //==============================================================================================
    private void postCombinedResult() {

        String[] server_values = getResources().getStringArray(R.array.server_values);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String address = SP.getString("ServerAddress", server_values[1]);

        String dataBase = SP.getString("database", "alpha");
        //
        String deviceID = SP.getString("DeviceID", "");

        boolean useSSL = SP.getBoolean("SSL_switch", true);

        String protocol = "http";
        if (useSSL) {
            protocol += "s";
        }

        // empty the resend queue first
        while (this.combinedScanResultResendQueue.size() > 0) {

            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
            PostCombinedResultToServer thisPost = new PostCombinedResultToServer(this);

            thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);

            thisPost.combinedScanResult = combinedScanResultResendQueue.poll();


            thisPost.execute(
                    address,
                    protocol,
                    String.valueOf(useSSL),
                    deviceID,
                    dataBase
            );

            this.sendResult("Sending to server: GPS location and WiFi scan (resend queue).");


        }// end of looping queue

        // empty the queue
        while (this.combinedScanResultQueue.size() > 0) {

            //PostWifiResultToServer thisPost = new PostWifiResultToServer(this);
            PostCombinedResultToServer thisPost = new PostCombinedResultToServer(this);

            thisPost.is = getResources().openRawResource(R.raw.nginxselfsigned);

            thisPost.combinedScanResult = combinedScanResultQueue.poll();


            thisPost.execute(
                    address,
                    protocol,
                    String.valueOf(useSSL),
                    deviceID,
                    dataBase
            );

            this.sendResult("Sending to server: GPS location and WiFi scan.");


        }// end of looping queue

    }// end of postCombinedResult
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


        requestlocation();
        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());

    }//end of startLocationService
    //==============================================================================================

    //==============================================================================================
    private void stopLocationService() {


        locationManager.removeUpdates(this);

        try {
            if (post != null)
                post.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

        handler.removeCallbacks(periodicUpdate_mag);

        running = false;

        System.out.println("Stop initiated...");

        //
        wl.release();

    }// end of stop service
    //==============================================================================================


    //==============================================================================================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Constants.ACTION_START_LOCATION_SERVICE)) {


                    startLocationService();


                    wifiManager = (WifiManager)
                            getApplicationContext().getSystemService(Context.WIFI_SERVICE);

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
                    handler.post(periodicUpdate_mag);

                    this.sendResult("Started scanning.");

                    return START_STICKY;
                } else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)) {

                    this.sendResult("Stopping scanning... sending all remaining scans.");

                    // empty the queues before the lose them
                    postCombinedResult();
                    postWifiResult();
                    postMagResult();


                    stopLocationService();
                    stopService();

                }

            }// end of if action not null
        }// end of if intent not null
        return super.onStartCommand(intent, flags, startId);
    }// end of onStartCommand
    //==============================================================================================


}// end of class
