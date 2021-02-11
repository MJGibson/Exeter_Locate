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

import com.riba2reality.wifimapper.ui.main.MagSensorResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

public class TrackerScanner extends Service implements LocationListener {


    // private final static String verificationCode = "aaz0p3DuHxgxqNOk40XA4csgjeEgJzC7AUEb40gTZXgtAM5TtpleDwdGkbXQICmKwCxuO2WXawQQiobWd3nggGH9plwgJHyERBF9";

    // make a user definable variable later
    //private final static String dataBase = "testTest";

    //---------------------------------------------------------------------------------------------
    // result/dispatch queues

    private final Queue<WifiScanResult> wifiScanResultQueue = new ConcurrentLinkedQueue<>();
    public final Queue<WifiScanResult> wifiScanResultResendQueue = new ConcurrentLinkedQueue<>();

    private final Queue<CombinedScanResult> combinedScanResultQueue = new ConcurrentLinkedQueue<>();
    public final Queue<CombinedScanResult> combinedScanResultResendQueue = new ConcurrentLinkedQueue<>();

    private final Queue<MagSensorResult> magSensorResultQueue = new ConcurrentLinkedQueue<>();


    //---------------------------------------------------------------------------------------------

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

    final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable periodicUpdate = new Runnable() {
        @Override
        public void run() {
            if (running) {

                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int postInterval = getResources().getInteger(R.integer.defaultVal_post);
                int interval = SP.getInt("interval_posts", postInterval);

                handler.postDelayed(periodicUpdate, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
            } else
                return;
            //

            postCombinedResult();
            postWifiResult();

/*
            if(scanning== false ) {

                scanning= true;

                locationScanned = false;
                requestlocation();

                wifiScanned     = false;
                scanWifi();


            }
*/

        }
    };


    private final Runnable periodicUpdate_wifi = new Runnable() {
        @Override
        public void run() {
            scanWifi();

            // after we've ran the event, remove the in-queue flag
            wifi_scan_in_queue = false;
        }
    };


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

    public void sendResult(String message) {
        Intent intent = new Intent(TRACKERSCANNER_RESULT);
        if (message != null)
            intent.putExtra(TRACKERSCANNER_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }


//    public TrackerScanner()
//    {
//        System.out.println("tracker created..");
//
//
//        wifiScanResultQueue = new ConcurrentLinkedQueue<WifiScanResult>();
//        combinedScanResultQueue = new ConcurrentLinkedQueue<CombinedScanResult>();
//    }


    @Override
    public void onCreate() {
        super.onCreate();

//        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wifiScanner:TrackerScanner");
//        wl.acquire();


        broadcaster = LocalBroadcastManager.getInstance(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wl.isHeld())
            wl.release();

    }

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

        // TODO check this doesn't inflate the number of scan loops
        scanWifi();

    }// end of onLocationChanged

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

// --Commented out by Inspection START (19/12/2020 12:57):
//    private final LocationCallback locationCallback = new LocationCallback() {
//        @Override
//        public void onLocationResult(LocationResult locationResult) {
//            super.onLocationResult(locationResult);
//
//            if (locationResult != null && locationResult.getLastLocation() != null) {
//                // double latitude = locationResult.getLastLocation().getLatitude();
//                // double longitude = locationResult.getLastLocation().getLongitude();
//
//                //String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
//                //String message = "Time:" + currentTime + "\nLat:" + latitude + "\nLong:" + longitude;
//                //System.out.println(message);
//
//                lastLocation = new Location(locationResult.getLastLocation());
//
//                //Log.d("LOCATION_UPDATE", message);
//
//                //System.out.println(message);
//
//                //scanWifi();
//
//                //Log.d("WIFI_UPDATE", String.valueOf(arrayList.size()));
//
//
//            }// end of if location not null
//
//        }// end of onLocationresult
//    };
// --Commented out by Inspection STOP (19/12/2020 12:57)

    final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> results = wifiManager.getScanResults();
            unregisterReceiver(wifiReceiver);

            String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());
            WifiScanResult result = new WifiScanResult();
            result.dateTime = currentTime;
            //result.wifiResult

            // boolean updatingWifiResults = true;
            arrayList = new ArrayList<>();
            for (ScanResult scanResult : results) {
                //arrayList.add(scanResult.SSID + " - " + scanResult.capabilities);
                arrayList.add(scanResult.SSID + " -" + scanResult.BSSID);
                //arrayList.add(scanResult.SSID + " - \""+scanResult. +"\""
                ///adapter.notifyDataSetChanged();

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

            if (running && !wifi_scan_in_queue) {
                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int wifiInterval = getResources().getInteger(R.integer.defaultVal_wifi);
                int interval = SP.getInt("interval_wifi", wifiInterval);

                handler.postDelayed(periodicUpdate_wifi, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
                wifi_scan_in_queue = true;
            }

        }
    };

    public void scanWifi() {
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

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


// --Commented out by Inspection START (19/12/2020 12:57):
//    private void postResult() {
//        Map<String, String> parameters = new HashMap<>();
//
//        List<String> macAddressList = new ArrayList<>();
//        List<String> signalStrengths = new ArrayList<>();
//
//        //------
//
//        double latitude;
//        double longitude;
//        double altitude;
//        double accuracy;
//        String provider;
//        if (lastLocation != null) {
//            latitude = lastLocation.getLatitude();
//            longitude = lastLocation.getLongitude();
//            altitude = lastLocation.getAltitude();
//            accuracy = lastLocation.getAccuracy();
//
//            // provider = lastLocation.getProvider();
//        } else {
//
//            this.sendResult("Error: GPS location missing.");
//
//            return;
//        }
//
//
//        if (arrayList == null) {
//
//            this.sendResult("Error: WiFi scan results missing.");
//
//            return;
//        }
//
//        //ArrayList<String> wifiList = this.arrayList;
//        for (ScanResult scanResult : results) {
//
//            macAddressList.add(scanResult.BSSID);
//            signalStrengths.add(Integer.toString(scanResult.level));
//        }
//
//        //------
//
//        String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());
//
//
//        String messageOut = "Time:" + currentTime + "\nLat:" + latitude + "\nLong:" + longitude
//                + "\naltitude:" + altitude;
//        //System.out.println(messageOut);
//        System.out.println("post...");
//
//        //System.out.println("Provider: "+provider);
//
//        //------
//
//
//        // results collected, switch scanning back on.
//        boolean scanning = false;
//
//
//        //------
//
//        String[] server_values = getResources().getStringArray(R.array.server_values);
//
//        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        String address = SP.getString("ServerAddress", server_values[1]);
//
//        String dataBase = SP.getString("database", "alpha");
//
//        boolean useSSL = SP.getBoolean("SSL_switch", true);
//
//
//        //------
//
//
////                parameters.put("TIME","12:01");
////                parameters.put("X","42");
////                parameters.put("Y","7");
//
//        parameters.put("MAGIC_NUM", verificationCode);
//
//        parameters.put("DATABASE", dataBase);
//
//        parameters.put("TIME", currentTime);
//        parameters.put("X", Double.toString(latitude));
//        parameters.put("Y", Double.toString(longitude));
//
//        parameters.put("ALTITUDE", Double.toString(altitude));
//        parameters.put("ACC", Double.toString(accuracy));
//
//        String macAddressJson = new Gson().toJson(macAddressList);
//
//
//        //parameters.put("MacAddresses",macAddressList.toString());
//        parameters.put("MacAddressesJson", macAddressJson);
//
//        parameters.put("signalStrengthsJson", new Gson().toJson(signalStrengths));
//
//
//        String message = new JSONObject(parameters).toString();
//
//        //------
//
//        //String address = "127.0.0.1";
//        //String address = "10.0.2.2"; // local for computer the emulator
//        //String address = "192.168.0.10";
//        //String port = "8000";
//        //String port = "27017";
//
//        //
//        //String address = "82.46.100.70";
//        //////String address = Constants.ServerAddress;
//
//
//        //String address = "httpbin.org/get";
//
//
//        //String uri = "http://"+address+":"+port;
//
//        //String uri = "http://"+address;
//
//        String protocol = "http";
//        if (useSSL) {
//            protocol += "s";
//        }
//
//        //String uri = "https://"+address;
//        String uri = protocol + "://" + address;
//
//        //String uri = "http://example.com";
//        //String uri = "https://postman-echo.com/get";
//
//        //String message = "hello_message";
//
//        //PostToServer
//        post = new PostToServer(this);
//
//
//        post.is = getResources().openRawResource(R.raw.nginxselfsigned);
//
//
//        post.execute(uri, message, String.valueOf(useSSL), address);
//
//
//        this.sendResult("Sending[" + protocol + "]: " + message.length() + " bytes");
//
//
//    }// end of postResult
// --Commented out by Inspection STOP (19/12/2020 12:57)


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

        //requestlocation();
        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());

    }//end of startLocationService

    private void stopLocationService() {

//        LocationServices.getFusedLocationProviderClient(this)
//                .removeLocationUpdates(locationCallback);


        // NOTE re-enage later!
        ////locationManager.removeUpdates(this);


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

    private void stopService() {

        handler.removeCallbacks(periodicUpdate);

        handler.removeCallbacks(periodicUpdate_wifi);

        running = false;

        System.out.println("Stop initiated...");

        //
        wl.release();

    }// end of stop service


    private SensorEventListener magSensorListener;

    private void startSensors(){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //sensorMag

        magSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());
                MagSensorResult result = new MagSensorResult();
                result.dateTime = currentTime;

                result.X = event.values[0];
                result.Y = event.values[1];
                result.Z = event.values[2];

                sendResult("Magnetic Scan updated"
                +"x,y,z("+result.X+","+result.Y+","+result.Z+")"
                );


            }// end of onSensorChanged

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };// end of magnetic sensor event listener

        // register the listener above, NOTE microsecond(i.e. not milli[1k], but 1 million-th of a second)
        Log.d("MAGscan", "Initiated");
        sensorManager.registerListener(magSensorListener,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1000000);




    }// end of startSensor


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Constants.ACTION_START_LOCATION_SERVICE)) {


                    startLocationService();
                    startSensors();


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
                    //handler.post(periodicUpdate);
                    //handler.post(periodicUpdate_wifi);
                    //this.sendResult("Started GPS and WiFi scanning.");






                    return START_STICKY;
                } else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)) {
                    //System.out.println("Stop initiated...");
                    this.sendResult("Stopping GPS and WiFi scanning... sending all remaining scans.");

                    // empty the queues before the lose them
                    ///postCombinedResult();
                    ///postWifiResult();

                    stopLocationService();
                    ///stopService();
                }

            }// end of if action not null
        }// end of if intent not null
        return super.onStartCommand(intent, flags, startId);
    }// end of onStartCommand


}// end of class
