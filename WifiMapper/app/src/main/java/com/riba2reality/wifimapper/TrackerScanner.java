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

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.riba2reality.wifimapper.ui.main.FirstTabFragment;
import com.riba2reality.wifimapper.ui.main.SecondTabFragment;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class TrackerScanner extends Service implements LocationListener {


    private final static String verificationCode = "aaz0p3DuHxgxqNOk40XA4csgjeEgJzC7AUEb40gTZXgtAM5TtpleDwdGkbXQICmKwCxuO2WXawQQiobWd3nggGH9plwgJHyERBF9";

    // make a user definable variable later
    //private final static String dataBase = "testTest";



    private WifiManager wifiManager;
    public ArrayList<String> arrayList = new ArrayList<>();
    private List<ScanResult> results;
    private Location lastLocation;

    private PostToServer post;

    LocalBroadcastManager broadcaster;

    static final public String TRACKERSCANNER_RESULT = "com.riba2reality.wifimapper.TrackerScanner.REQUEST_PROCESSED";

    static final public String TRACKERSCANNER_MESSAGE = "com.riba2reality.wifimapper.TrackerScanner.TRACKERSCANNER_MSG";

    PowerManager pm;
    PowerManager.WakeLock wl;

    LocationManager locationManager;

    private final int intervalSeconds = 1;

    private boolean locationScanned = false;
    private boolean wifiScanned     = false;
    private boolean scanning        = false;

    private boolean running = false;

    Handler handler = new Handler(Looper.getMainLooper());
    private Runnable periodicUpdate = new Runnable() {
        @Override
        public void run() {
            if(running) {

                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int interval =SP.getInt("interval", 5);

                handler.postDelayed(periodicUpdate, interval * 1000 - SystemClock.elapsedRealtime() % 1000);
            }
            else
                return;
            //

            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String message = "Time:" + currentTime;
            //+ "\nLat:" + location.getLatitude() + "\nLong:" + location.getLongitude();

//        Toast myToast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT);
//        myToast.show();

            System.out.println(message);


            if(scanning== false ) {

                scanning= true;

                locationScanned = false;
                requestlocation();

                wifiScanned     = false;
                scanWifi();


            }

        }
    };

    private void requestlocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        String provider = locationManager.getBestProvider(criteria, true);

//        locationManager.requestLocationUpdates(provider,
//                10,
//                10,
//                this);


        locationManager.requestSingleUpdate(provider, this, Looper.getMainLooper());



    }//end of requestlocation

    public void sendResult(String message) {
        Intent intent = new Intent(TRACKERSCANNER_RESULT);
        if(message != null)
            intent.putExtra(TRACKERSCANNER_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }


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
    public void onDestroy()
    {
        super.onDestroy();
        if(wl.isHeld())
            wl.release();

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

        if(location==null)
            return;
//        LatLng myCords = new LatLng(location.getLatitude(), location.getLongitude());

        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String message = "Time:" + currentTime + "\nLat:" + location.getLatitude() + "\nLong:" + location.getLongitude();

//        Toast myToast = Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT);
//        myToast.show();

        System.out.println(message);

        lastLocation = new Location(location);

        // set scanned bool
        locationScanned = true;

        if(wifiScanned){
            postResult();
        }



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

    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            if(locationResult != null && locationResult.getLastLocation() != null){
                double latitude = locationResult.getLastLocation().getLatitude();
                double longitude = locationResult.getLastLocation().getLongitude();

                //String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                //String message = "Time:" + currentTime + "\nLat:" + latitude + "\nLong:" + longitude;
                //System.out.println(message);

                lastLocation = new Location(locationResult.getLastLocation());

                //Log.d("LOCATION_UPDATE", message);

                //System.out.println(message);

                //scanWifi();

                //Log.d("WIFI_UPDATE", String.valueOf(arrayList.size()));


            }// end of if location not null

        }// end of onLocationresult
    };

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            results = wifiManager.getScanResults();
            unregisterReceiver(wifiReceiver);

            for (ScanResult scanResult : results) {
                //arrayList.add(scanResult.SSID + " - " + scanResult.capabilities);
                arrayList.add(scanResult.SSID + " -" + scanResult.BSSID);
                //arrayList.add(scanResult.SSID + " - \""+scanResult. +"\""
                ///adapter.notifyDataSetChanged();
            }

            //postResult();

            Log.d("WIFI_UPDATE", String.valueOf(arrayList.size()));

            /*
            if(scanning) {



                SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int interval =SP.getInt("interval", 0);
                int intervalmill = interval * 1000;




                scanWifi();

            }

             */

            // set wifi scanned bool
            wifiScanned = true;

            if(locationScanned){
                postResult();
            }



        };
    };

    public void scanWifi() {
        arrayList.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();

/*
        Method startScanActiveMethod = null;
        try {
            Method method = WifiManager.class.getMethod("startScanActive");
            method.setAccessible(true);
            Object r = method.invoke(null);  // null for static hidden method
        } catch (NoSuchMethodException e) {
            e.printStackTrace();

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }*/



        //Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
        //return null;
    }

    private void postResult(){
        Map<String, String> parameters = new HashMap<>();

        List<String> macAddressList = new ArrayList<>();
        List<String> signalStrengths = new ArrayList<>();

        //------

        double latitude;
        double longitude;
        double altitude;
        double accuracy;
        String provider;
        if(lastLocation!=null) {
            latitude = lastLocation.getLatitude();
            longitude = lastLocation.getLongitude();
            altitude = lastLocation.getAltitude();
            accuracy = lastLocation.getAccuracy();

            provider = lastLocation.getProvider();
        }else{
            return;
        }

        String currentTime = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault()).format(new Date());


        String messageOut = "Time:" + currentTime + "\nLat:" + latitude + "\nLong:" + longitude
                + "\naltitude:" + altitude;
        //System.out.println(messageOut);
        System.out.println("post...");

        //System.out.println("Provider: "+provider);

        //------



        //ArrayList<String> wifiList = this.arrayList;
        for (ScanResult scanResult : results) {

            macAddressList.add(scanResult.BSSID);
            signalStrengths.add(Integer.toString(scanResult.level));
        }

        //------

        // results collected, switch scanning back on.
        scanning = false;



        //------

        String[] server_values = getResources().getStringArray(R.array.server_values);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String address =SP.getString("ServerAddress", server_values[1]);

        String dataBase =SP.getString("database", "alpha");

        boolean useSSL = SP.getBoolean("SSL_switch",true);


        //------


//                parameters.put("TIME","12:01");
//                parameters.put("X","42");
//                parameters.put("Y","7");

        parameters.put("MAGIC_NUM",verificationCode);

        parameters.put("DATABASE",dataBase);

        parameters.put("TIME",currentTime);
        parameters.put("X",Double.toString(latitude));
        parameters.put("Y",Double.toString(longitude));

        parameters.put("ALTITUDE",Double.toString(altitude));
        parameters.put("ACC",Double.toString(accuracy));

        String macAddressJson = new Gson().toJson(macAddressList );


        //parameters.put("MacAddresses",macAddressList.toString());
        parameters.put("MacAddressesJson",macAddressJson);

        parameters.put("signalStrengthsJson",new Gson().toJson(signalStrengths ));


        String message = new JSONObject(parameters).toString();

        //------

        //String address = "127.0.0.1";
        //String address = "10.0.2.2"; // local for computer the emulator
        //String address = "192.168.0.10";
        //String port = "8000";
        //String port = "27017";

        //
        //String address = "82.46.100.70";
        //////String address = Constants.ServerAddress;


        //String address = "httpbin.org/get";


        //String uri = "http://"+address+":"+port;

        //String uri = "http://"+address;

        String protocol = "http";
        if(useSSL){
            protocol+="s";
        }

        //String uri = "https://"+address;
        String uri = protocol+"://"+address;

        //String uri = "http://example.com";
        //String uri = "https://postman-echo.com/get";

        //String message = "hello_message";

        //PostToServer
        post = new PostToServer(this);


        post.is = getResources().openRawResource(R.raw.cert);




        post.execute(uri,message, String.valueOf(useSSL));



        this.sendResult("Sending["+protocol+"]: "+message.length()+" bytes");


    }// end of postResult


    private void startLocationService(){

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

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            if(notificationManager != null &&
                    notificationManager.getNotificationChannel(channelId) == null
            ){
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelId,
                        "Location Service",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationChannel.setDescription("This channel is used by location service");
                notificationManager.createNotificationChannel(notificationChannel);

            }// end of if notificationManager not null
        }// end of if API 26 or greater

//        LocationRequest locationRequest = new LocationRequest();
//        locationRequest.setInterval(4000);
//        locationRequest.setFastestInterval(2000);
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//
//        LocationServices.getFusedLocationProviderClient(this)
//                .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//
//
//        //----------
//
//        wifiManager = (WifiManager)
//                getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//
//        if (!wifiManager.isWifiEnabled()) {
//            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
//            wifiManager.setWifiEnabled(true);
//        }


        //----------

        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());






    }//end of startLocationService

    private void stopLocationService(){

//        LocationServices.getFusedLocationProviderClient(this)
//                .removeLocationUpdates(locationCallback);


        try {
            if(post!= null)
                post.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // if service stopped when scaned wifi, the reciever already unregisted
        try {
            unregisterReceiver(wifiReceiver);
        }catch(IllegalArgumentException e) {

            e.printStackTrace();
        }

        stopForeground(true);
        stopSelf();

//        scanning =false;

    }// end of stopLocationService

    private  void stopService(){

        handler.removeCallbacks(periodicUpdate);


        running = false;

        System.out.println("Stop intiated...");

        //
        wl.release();

    }// end of stop service

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(intent != null){
            String action = intent.getAction();
            if(action != null){
                if(action.equals(Constants.ACTION_START_LOCATION_SERVICE)){
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





                    handler.post(periodicUpdate);
                    return START_STICKY;
                }else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)){
                    //System.out.println("Stop intiated...");

                    stopLocationService();
                    stopService();
                }

            }// end of if action not null
        }// end of if intent not null
        return super.onStartCommand(intent,flags,startId);
    }// end of onStartCommand






}// end of class
