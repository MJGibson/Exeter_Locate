package com.riba2reality.wifimapper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

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

public class TrackerScanner extends Service {


    private final static String verificationCode = "aaz0p3DuHxgxqNOk40XA4csgjeEgJzC7AUEb40gTZXgtAM5TtpleDwdGkbXQICmKwCxuO2WXawQQiobWd3nggGH9plwgJHyERBF9";


    private boolean scanning;
    private WifiManager wifiManager;
    public ArrayList<String> arrayList = new ArrayList<>();
    private List<ScanResult> results;
    private Location lastLocation;

    private PostToServer post;

    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            if(locationResult != null && locationResult.getLastLocation() != null){
                double latitude = locationResult.getLastLocation().getLatitude();
                double longitude = locationResult.getLastLocation().getLongitude();

                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                String message = "Time:" + currentTime + "\nLat:" + latitude + "\nLong:" + longitude;

                lastLocation = new Location(locationResult.getLastLocation());

                //Log.d("LOCATION_UPDATE", message);

                System.out.println(message);

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

            Log.d("WIFI_UPDATE", String.valueOf(arrayList.size()));
            if(scanning)
                scanWifi();
            postResult();

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

        //------

        double latitude;
        double longitude;
        if(lastLocation!=null) {
            latitude = lastLocation.getLatitude();
            longitude = lastLocation.getLongitude();
        }else{
            return;
        }

        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        //------



        //ArrayList<String> wifiList = this.arrayList;
        for (ScanResult scanResult : results) {

            macAddressList.add(scanResult.BSSID);
        }




        //------





//                parameters.put("TIME","12:01");
//                parameters.put("X","42");
//                parameters.put("Y","7");

        parameters.put("MAGIC_NUM",verificationCode);

        parameters.put("TIME",currentTime);
        parameters.put("X",Double.toString(latitude));
        parameters.put("Y",Double.toString(longitude));

        String macAddressJson = new Gson().toJson(macAddressList );


        //parameters.put("MacAddresses",macAddressList.toString());
        parameters.put("MacAddressesJson",macAddressJson);


        String message = new JSONObject(parameters).toString();

        //------

        //String address = "127.0.0.1";
        //String address = "10.0.2.2"; // local for computer the emulator
        //String address = "192.168.0.10";
        //String port = "8000";
        //String port = "27017";

        //
        String address = "82.46.100.70";

        //String address = "httpbin.org/get";


        //String uri = "http://"+address+":"+port;

        //String uri = "http://"+address;
        String uri = "https://"+address;

        //String uri = "http://example.com";
        //String uri = "https://postman-echo.com/get";

        //String message = "hello_message";

        //PostToServer
        post = new PostToServer();


        post.is = getResources().openRawResource(R.raw.cert);




        post.execute(uri,message);
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

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());


        //----------

        wifiManager = (WifiManager)
                getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }


        //----------

        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());


        scanning =true;
        scanWifi();



    }//end of startLocationService

    private void stopLocationService(){

        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(locationCallback);


        try {
            if(post!= null)
                post.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        unregisterReceiver(wifiReceiver);

        stopForeground(true);
        stopSelf();

        scanning =false;

    }// end of stopLocationService

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(intent != null){
            String action = intent.getAction();
            if(action != null){
                if(action.equals(Constants.ACTION_START_LOCATION_SERVICE)){
                    startLocationService();
                }else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)){
                    stopLocationService();
                }

            }// end of if action not null
        }// end of if intent not null
        return super.onStartCommand(intent,flags,startId);
    }// end of onStartCommand






}// end of class
