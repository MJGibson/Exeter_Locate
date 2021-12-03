package com.riba2reality.exeterlocate;


import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;
import com.riba2reality.exeterlocate.messages.BluetoothMessageActivity;
import com.riba2reality.exeterlocate.messages.GpsMessageActivity;
import com.riba2reality.exeterlocate.messages.InternetMessageActivity;
import com.riba2reality.exeterlocate.messages.WifiMessageActivity;
import com.riba2reality.exeterlocatecore.DataStores.Constants;
import com.riba2reality.exeterlocatecore.TrackerScanner;

import java.util.Calendar;
import java.util.UUID;

/**
 * Exeter Locate App - Is a citizen science driven project, which allows uses to donate their
 * anonymized Location, Wi-Fi, Bluetooth, accelerometer and magnetometer data. By many citizens
 * contributing small amounts of data in the limited area of the geoFence (University of Exeter -
 * Streatham campus), better locations service could be developed.
 *
 * MainActivity Class of type Android Activity, acts as the main frontend for Exeter Locate App.
 * When active this class will display the status of the backend service.
 *
 * Display Icon colour code (Red for deactivated, Green for active).
 *
 * Also a button is provided to start and stop the backend service, depending on current status.
 *
 * Initial calls to start the service request the necessary location permissions from the user, and
 * request to start up the bluetooth if it is not currently active.
 *
 * Upon first start up a random UUID will be generated and stored for future use in the backend.
 *
 * @author <a href="mailto:M.J.Gibson@Exeter.ac.uk">Michael J Gibson</a>
 * @version 1.0
 * @since   2021-08-19
 *
 */
public class MainActivity extends AppCompatActivity {

    // permission variables
    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;
    public static final int REQUEST_ENABLE_BT = 11;
    public static final int UPDATE_REQUEST_CODE = 111;

    // bluetooth
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;

    // UI variables
    private Button startStopButton;
    private boolean running = false;
    private boolean active = false;
    private ConstraintLayout mainLayout;
    //private TextView versionTextView;
    private ConstraintLayout infoButton;
    private ImageView imageViewBackground;

    private View displayIconView;
    private ImageView circleCore;
    private ImageView circleAnimation1;
    private ImageView circleAnimation2;
    private ImageView circleIcon;
    private TextView status_textView_top;
    private TextView status_textView_bottom;
    private Handler iconAniHandler;


    private final int pulseDuration = 2000;
    private final int pulseInterval = 3500;// note must be bigger than pulseDuration
    private final int checkButtonsinterval = 200;



    // UUID
    private String _deviceID;

    // assume false until we have data to say otherwise
    private boolean _insideGeoFence = false;
    private boolean _geoFenceChecked = false;

    Activity _MainActivity;
    AppUpdateManager appUpdateManager;

    //
    private boolean _termsAccepted = false;
//    com.riba2reality.exeterlocate.databinding.ActivityMainBinding _binding;

    private MainActivity selfRef = this;




    //##############################################################################################

    //==============================================================================================
    /**
     * onCreate Method
     * Sets up the front end, using the activity_main layout, and initialises UI class variables.
     * Checks if a UUID is stored in the shared preferences, and if not creates and stores one,
     * otherwise loading and using the stored UUID.
     * If bluetooth is available on the device then the class variables are intialised.
     * Displays the version of this app and the back end.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up UI
        setContentView(R.layout.activity_main);
//        _binding= com.riba2reality.exeterlocate.databinding.ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(_binding.getRoot());



        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //startStopButton = findViewById(R.id.startStopButton);
        //startStopButton.setOnClickListener(startStopButtonPressed);
        mainLayout = findViewById(R.id.main_layout);
        infoButton = findViewById(R.id.information);
        infoButton.setOnClickListener(infoButtonPressed);
//        imageViewBackground = findViewById(R.id.imageViewBackground);
//        imageViewBackground.setOnClickListener(startStopButtonPressed);

        this.iconAniHandler = new Handler();
        this.displayIconView = findViewById(R.id.displayIcon);
        this.circleCore = findViewById(R.id.circleBase);
        this.circleIcon = findViewById(R.id.circleIcon);
        this.circleIcon.setOnClickListener(startStopButtonPressed);
        this.circleAnimation1 = findViewById(R.id.circleAnimation1);
        this.circleAnimation2 = findViewById(R.id.circleAnimation2);

        this.status_textView_top =  findViewById(R.id.textView_status_top);
        this.status_textView_bottom =  findViewById(R.id.textView_status_bottom);

        // set up title bar
        ActionBar actionBar = getSupportActionBar();
        //actionBar.setDisplayShowHomeEnabled(true);

        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.mipmap.exeter_locate_icon);
        actionBar.setDisplayUseLogoEnabled(true);

        // get rid of action bar...
        actionBar.hide();



        // check if we already have a UUID, if not make a new one and store it
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        _deviceID = SP.getString("DeviceID", "");
        if(_deviceID.isEmpty()){
            _deviceID = UUID.randomUUID().toString();
            SPeditor.putString("DeviceID", _deviceID);
            SPeditor.apply();
        }

        Log.d("mgdev", "MainActivity.onCreate._deviceID="+_deviceID);

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

        LocalBroadcastManager.getInstance(this).registerReceiver((receiverGeoFenceUpdates),
                new IntentFilter(TrackerScanner.TRACKERSCANNER_RESULT)
        );

        _MainActivity = this;

        LocalBroadcastManager.getInstance(this).registerReceiver(receiverTermsAcceptance,
                new IntentFilter(getResources().getString(R.string.terms_acceptance)));


    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    /**
     * onStart Method
     * Calls Overriden method, and starts the monitoring thread, and sets the buttons the correct
     * status.
     */
    @Override
    protected void onStart() {
        active = true;
        super.onStart();

        startAlarms(this);

        checkTermsAcceptance();

        requestGeoFenceUpdate();

        checkForUpdates();

        runButtonsThread();
        runPulseThread();



        checkButtons();
        checkBluetoothEnabled();
        checkWifiEnabled();
        //checkForInternetConnection();
        checkGpsEnabled();

        // add broadcast receivers for ble, wifi turned off
        this.registerReceiver(receiverBle, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        this.registerReceiver(receiverWifi,
                new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
        this.registerReceiver(receiverGPS, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));



        Log.d("mgdev", "MainActivity.onStart");
    }
    //==============================================================================================



    //==============================================================================================
    /**
     * onStop Method
     * Calls overriden method, and stops the monitoring thread by its while running bool to false.
     */
    @Override
    protected void onStop() {
        active = false;
        super.onStop();
        Log.d("mgdev", "MainActivity.onStop");
        // add broadcast receivers for ble, wifi turned off
        this.unregisterReceiver(receiverBle);
        this.unregisterReceiver(receiverWifi);
        this.unregisterReceiver(receiverGPS);

        //this.unregisterReceiver(receiverTermsAcceptance);
    }
    //==============================================================================================


    //##############################################################################################
    //###########################       Receiver Functions     #####################################
    //##############################################################################################


    //==============================================================================================
    BroadcastReceiver receiverTermsAcceptance = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "receiverTermsAcceptance.onReceive");

            _termsAccepted = intent.getBooleanExtra(
                    getResources().getString(R.string.terms_acceptance),
                    false);

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(selfRef);
            final SharedPreferences.Editor SPeditor = SP.edit();


            SPeditor.putBoolean("termsAcceptance", _termsAccepted);
            SPeditor.apply();

            // assuming the terms are accepted, then start the location service already...
            if (_termsAccepted){
                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION_PERMISSIONS
                    );
                } else {

                    Log.d("mgdev", "MainActivity.onClick.permission.accepted");

                    startLocationService();

                }//end of if permission already accepted
            }


        }// end of onReceive
    };// end of receiverTermsAcceptance
    //==============================================================================================


    //==============================================================================================
    BroadcastReceiver receiverGeoFenceUpdates = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "receiverGeoFenceUpdates.onReceive");

            _insideGeoFence = intent.getBooleanExtra(TrackerScanner.TRACKERSCANNER_GEOFENCE_UPDATE,
                    false);
            _geoFenceChecked =  intent.getBooleanExtra(
                    TrackerScanner.TRACKERSCANNER_GEOFENCE_UPDATE_FIRST,
                    false);


        }// end of onReceive
    };
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiverBle = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "receiverBle.onReceive");

            checkBluetoothEnabled();

        }// end of onReceive
    };
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiverWifi = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "MainActivity.receiverWifi.onReceive");

//            //  check if wifi is off, and activate the wifi message....
//            WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//            if (!wifi.isWifiEnabled()){
//                startMessageActivityWifiOff();
//            }
            checkWifiEnabled();

        }// end of onReceive
    };
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiverGPS = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "MainActivity.receiverGPS.onReceive");


            checkGpsEnabled();

        }// end of onReceive
    };
    //==============================================================================================

    //##############################################################################################
    //###########################       Check Functions     ########################################
    //##############################################################################################

    //==============================================================================================
    private void checkTermsAcceptance(){


        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        boolean _termsAccepted = SP.getBoolean("termsAcceptance", false);

        if(!_termsAccepted){

            startTermsAcceptance();

        }

    }// end of checkTermsAcceptance
    //==============================================================================================

    //==============================================================================================
    private void checkForUpdates(){

        appUpdateManager = AppUpdateManagerFactory.create(this);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();


        Log.d("mgdev", "MainActivity.checkForUpdates");

        // Checks that the platform will allow the specified type of update.
        //appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {

        appUpdateInfoTask.addOnSuccessListener(updateListener);



    }// end of checkForUpdates
    //==============================================================================================

    //==============================================================================================
    OnSuccessListener<AppUpdateInfo> updateListener = new OnSuccessListener<AppUpdateInfo>() {
        @Override
        public void onSuccess(AppUpdateInfo appUpdateInfo) {

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    // This example applies an immediate update. To apply a flexible update
                    // instead, pass in AppUpdateType.FLEXIBLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {



                Log.d("mgdev", "MainActivity.updateListener.available");

                try {
                    appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                            AppUpdateType.IMMEDIATE,
                            // The current activity making the update request.
                            _MainActivity,
                            // Include a request code to later monitor this update request.
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }


            }// end of if update available and immediate

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    // This example applies an immediate update. To apply a flexible update
                    // instead, pass in AppUpdateType.FLEXIBLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {



                Log.d("mgdev", "MainActivity.updateListener.available");

                try {
                    appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                            AppUpdateType.FLEXIBLE,
                            // The current activity making the update request.
                            _MainActivity,
                            // Include a request code to later monitor this update request.
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }


            }// end of if update available and immediate

        }// end of onSuccess
    };// end of updateListener
    //==============================================================================================


    //==============================================================================================
    private void requestGeoFenceUpdate(){

        // if location service is not running, we assume don't know and are therefore outside
        // geoFence
        if( isLocationServiceRunning(this) ){

            Intent intent = new Intent(this.getApplicationContext(), TrackerScanner.class);
            intent.setAction(
                    Constants.ACTION_REQUEST_GEOFENCE_UPDATE
            );

            startService(intent);

            Log.d("test1", "MainActivty.requestGeoFenceUpdate()");


        }// end of location service is running
        else{
            _insideGeoFence = false;
        }

    }// end of requestGeoFenceUpdate
    //==============================================================================================

    //==============================================================================================
    private void checkGpsEnabled(){

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        boolean _termsAccepted = SP.getBoolean("termsAcceptance", false);

        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) && _termsAccepted && !GpsMessageActivity._test) {

            Log.d("mgdev", "MainActivity.checkGpsEnabled. GPS disabled");

//            // note this isn't perfect
//            if(TrackerScanner.runningOnEmulator()){
//
//            }else {
                startMessageActivityGPSOff();
//            }

        }// end of if gps not enabled

    }// end of checkGpsEnabled
    //==============================================================================================

    //==============================================================================================
    private void checkForInternetConnection(){

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        boolean _termsAccepted = SP.getBoolean("termsAcceptance", false);

        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
        }
        else {
            connected = false;
            if(_termsAccepted)
                startMessageActivityInternetOff();
        }

    }// end of checkForInternetConnection
    //==============================================================================================

    //==============================================================================================
    private void checkBluetoothEnabled(){

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        boolean _termsAccepted = SP.getBoolean("termsAcceptance", false);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth ???
        } else {
            if (!mBluetoothAdapter.isEnabled() && _termsAccepted && !GpsMessageActivity._test) {
                // Bluetooth is not enable :)
                startMessageActivityBluetoothOff();
            }
        }

    }// end of checkBluetoothEnabled
    //==============================================================================================

    //==============================================================================================
    private void checkWifiEnabled(){

        Log.d("mgdev", "MainActivity.checkWifiEnabled");

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        boolean _termsAccepted = SP.getBoolean("termsAcceptance", false);



        WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled() && _termsAccepted && !GpsMessageActivity._test){
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1 && TrackerScanner.runningOnEmulator()){

            }else {
                startMessageActivityWifiOff();
            }
        }

    }// end of checkWifiEnabled
    //==============================================================================================

    //==============================================================================================
    /**
     * checkButtons method
     * Uses isLocationServiceRunning to check if the service is running, and sets up the start/stop
     * button appropriatley, along with the class boolean 'running'.
     * Also sets the DisplayIcon to the correct settings
     */
    private void checkButtons(){

        if( isLocationServiceRunning(this) ){
            //startStopButton.setText(R.string.start_button_stop_text);
            running = true;

            circleCore.setColorFilter(
                    getResources().getColor(R.color.green),
                    PorterDuff.Mode.SRC_ATOP);
            circleAnimation1.setColorFilter(
                    getResources().getColor(R.color.green),
                    PorterDuff.Mode.SRC_ATOP);
            circleAnimation2.setColorFilter(
                    getResources().getColor(R.color.green),
                    PorterDuff.Mode.SRC_ATOP);
            circleIcon.setImageResource(R.mipmap.tick_round);

            if(_insideGeoFence) {
                status_textView_top.setText(
                        R.string.AppActive);
            }else{
                if(!_geoFenceChecked){
                    status_textView_top.setText(
                            R.string.AppActive_NotCheckedGeoFence);
                }else {
                    status_textView_top.setText(
                            R.string.AppActive_OutsideGeoFence);
                }
            }

            status_textView_bottom.setText(R.string.To_pause_record);

            //startDisplayIconAnimation();

            //
            if(!_geoFenceChecked){

                // request an update from back end..



            }//end of if geo fence not checked

        }else{
            //startStopButton.setText(R.string.start_button_initial_text);
            running = false;

            circleCore.setColorFilter(
                    getResources().getColor(R.color.red),
                    PorterDuff.Mode.SRC_ATOP);
            circleAnimation1.setColorFilter(
                    getResources().getColor(R.color.red),
                    PorterDuff.Mode.SRC_ATOP);
            circleAnimation2.setColorFilter(
                    getResources().getColor(R.color.red),
                    PorterDuff.Mode.SRC_ATOP);
            circleIcon.setImageResource(R.mipmap.cross_round);
            //circleIcon.image

            status_textView_top.setText(R.string.AppNotactive);
            status_textView_bottom.setText(R.string.To_continue_record);

        }// end of if/else isLocationServiceRunning

    }//end of checkButtons
    //==============================================================================================



    //==============================================================================================
    /**
     * infoButtonPressed OnClickListener
     *
     * Attached to the info button, by onCreate; This function
     *
     */
    View.OnClickListener infoButtonPressed = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            Log.d("mgdev", "MainActivity.infoButtonPressed");

            // start the Info activity
            startInfoActivty();

        }// end of onClick
    };// end of infoButtonPressed
    //==============================================================================================

    //##############################################################################################
    //#############################     Start others things        #################################
    //##############################################################################################


    //==============================================================================================
    public static void startAlarms(Context context){

        Log.d("mgdev", "MainActivity.startAlarms");

        //we are using alarm manager for the notification
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        //this intent will be called when taping the notification
        Intent notificationIntent = new Intent(context, AlarmReceiver.class);
        //this pendingIntent will be called by the broadcast receiver
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 100,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar cal = Calendar.getInstance();//getting calender instance

        cal.setTimeInMillis(System.currentTimeMillis());//setting the time from device
        cal.set(Calendar.HOUR_OF_DAY, 8); // cal.set NOT cal.add
        cal.set(Calendar.MINUTE,30);
        cal.set(Calendar.SECOND, 0);

        // add a day to make it tomorrow
        cal.add(Calendar.DAY_OF_MONTH, 1);

        //Log.d("mgdev", "Alarms: "+cal.toString());

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                //10000,
                AlarmManager.INTERVAL_DAY,
                broadcast);//alarm manager will repeat the notification each day at the set time


    }// end of startAlarms
    //==============================================================================================


    //==============================================================================================
    public void startTermsAcceptance(){

        Intent intent = new Intent(this, TermsActivity.class);

        startActivity(intent);


//        //---------------------------------------
//
//
//
//        AlertDialog.Builder alert = new AlertDialog.Builder(this,
//                R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Background );
//
//        WebView webView = new WebView(this);
//        //webView.setId(R.id.);
//
//        webView.loadUrl("file:///android_asset/ExeterLocateConcentPage.html");
//        webView.setWebViewClient(new WebViewClient(){
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url){
//                view.loadUrl(url);
//                return true;
//            }
//        });
//        alert.setView(webView);
//
//        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
//        final SharedPreferences.Editor SPeditor = SP.edit();
//
//        alert.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                _termsAccepted = true;
//                SPeditor.putBoolean("termsAcceptance", _termsAccepted);
//                SPeditor.apply();
//                dialog.dismiss();
//            }
//        });
//
//        alert.setNegativeButton("Disagree", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                finish();
//
//                //System.exit(0);
//            }
//        });
//
//        alert.setCancelable(false);
//
//        AlertDialog dialog = alert.create();
//
//        //dialog.getWindow().setBackgroundDrawable(getDrawable(R.color.white));
//
//        dialog.show();
//
//        // Access the button and set it to invisible
//        final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
//        button.setVisibility(View.INVISIBLE);
//
//        webView.getViewTreeObserver().addOnScrollChangedListener(
//                new ViewTreeObserver.OnScrollChangedListener() {
//                    @Override
//                    public void onScrollChanged() {
//
//                        //Log.v("mgdev", "+++ scrollchanged "+webView.getScrollY());
//
//                        int maxScrollExtent =
//                                (int) ((webView.getContentHeight() *
//                                        webView.getResources().getDisplayMetrics().density)
//                                        - webView.getHeight())-1;
//
//                        int diff = ((int)(maxScrollExtent * 0.9)) - webView.getScrollY();
//
//                        //Log.v("mgdev", "-----------------------::"+diff);
//                        //Log.v("mgdev", "maxScrollExtent"+maxScrollExtent);
//                        //Log.v("mgdev", "webView.getScrollY()"+webView.getScrollY());
//
//                        // if diff is zero, then the bottom has been reached
//                        if (diff <= 0 && maxScrollExtent != 0) {
//
//                                //Log.v("mgdev", "Accept?!?!");
//
//                                button.setVisibility(View.VISIBLE);
//
//                            }// end of if diff = 0
//                    }// end of onScrollChanged
//                }// end of ViewTreeObserver.OnScrollChangedListener
//        );// end of addOnScrollChangedListener
//
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            webView.getViewTreeObserver().addOnWindowFocusChangeListener(
//                    new ViewTreeObserver.OnWindowFocusChangeListener() {
//                        @Override
//                        public void onWindowFocusChanged(boolean hasFocus) {
//
//                            Log.v("mgdev", "dialog.onWindowFocusChanged");
//
//                            if(hasFocus && _termsAccepted){
//                                dialog.hide();
//                            }
//
//
//                        }// end of onWindowFocusChanged
//                    }//end of OnWindowFocusChangeListener
//            );// end of addOnWindowFocusChangeListener
//        }




    }// end of startTermsAcceptance
    //==============================================================================================

    //==============================================================================================
    /**
     * startInfoActivty method
     * Starts the InfoActivity
     */
    public void startInfoActivty(){


        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        boolean InfoActivityActive = SP.getBoolean("InfoActivityActive", false);
        if(!InfoActivityActive){
            InfoActivityActive = true;

            Intent intent = new Intent(this, InfoActivity.class);
            startActivity(intent);

            SPeditor.putBoolean("InfoActivityActive", InfoActivityActive);
            SPeditor.apply();
        }

    }//end of
    //==============================================================================================

    //==============================================================================================
    /**
     * startMessageActivityGPSOff method
     * Starts a MessageActivity about GPS being turned off
     */
    public void startMessageActivityGPSOff(){
        Intent intent = new Intent(this, GpsMessageActivity.class);

        startActivity(intent);
    }//end of
    //==============================================================================================
    //==============================================================================================
    /**
     * startMessageActivityInternetOff method
     * Starts a MessageActivity about internet being turned off
     */
    public void startMessageActivityInternetOff(){
        Intent intent = new Intent(this, InternetMessageActivity.class);

        startActivity(intent);
    }//end of
    //==============================================================================================

    //==============================================================================================
    /**
     * startMessageActivityBluetoothOff method
     * Starts a MessageActivity about bluetooth being turned off
     */
    public void startMessageActivityBluetoothOff(){
        Intent intent = new Intent(this, BluetoothMessageActivity.class);



//        intent.putExtra("title","For this app to work, you must have Bluetooth on");
//        intent.putExtra("message","This App uses Bluetooth to locate nearby Bluetooth devices" +
//                ". If you have Bluetooth turned off, this app will not work.\n\n This App uses " +
//                "'Bluetooth low energy' - a battery saving technology.\n\n" +
//                "Please go to setting and turn on Bluetooth.");
//        intent.putExtra("icon",R.drawable.bluetoot_disconnected_foreground);


        startActivity(intent);
    }//end of
    //==============================================================================================

    //==============================================================================================
    /**
     * startMessageActivityWifiOff method
     * Starts a MessageActivity about Wifi being turned off
     */
    public void startMessageActivityWifiOff(){
        Intent intent = new Intent(this, WifiMessageActivity.class);
        startActivity(intent);
    }//end of
    //==============================================================================================

    //==============================================================================================
    /**
     * startStopButtonPressed OnClickListener
     *
     * Attached to the start/stop button, by onCreate; This function does the work of checking the
     * class variables 'running', and either stops or starts the service via the stopLocationService()
     * and startLocationService() functions. Before firing the startLocationService() function however,
     * it checks it has location permission and if not requests them, leaving the launching of the
     * service the function that deals with said permission request result
     *
     */
    View.OnClickListener startStopButtonPressed = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            Log.d("mgdev", "MainActivity.onClick");

            if (!running) {


                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION_PERMISSIONS
                    );
                } else {

                    Log.d("mgdev", "MainActivity.onClick.permission.accepted");

                    startLocationService();

                }//end of if permission already accepted

            } else {

                stopLocationService();

                return; // close any services up here...
            }




        }// end of onClick
    };
    //==============================================================================================

    //==============================================================================================

    /**
     * onRequestPermissionsResult method
     * Deals with permission request result from the users, if the request is granted then it calls
     * the startLocationService() function, otherwise displaying a toast message.
     *
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Toast.makeText(MainActivity.this,
                        getResources().getString(R.string.PermissionDeniedToastText),
                        Toast.LENGTH_SHORT).show();
            }
        }

    }// end of onRequestPermissionsResult
    //==============================================================================================

    //==============================================================================================

    /**
     * isLocationServiceRunning method
     * Checks if the TrackerScanner Service is running.
     *
     * @return True if the TrackerScanner Service is running, othewise false
     */
    public static boolean isLocationServiceRunning(Context context) {

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {

            for (ActivityManager.RunningServiceInfo service :
                    activityManager.getRunningServices(Integer.MAX_VALUE)) {

                if (TrackerScanner.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }

            }// end of looping
            return false;
        }// end of if activityManger not null
        return false;
    }// end of isLocationServiceRunning
    //==============================================================================================


    //==============================================================================================

    /**
     * startLocationService Method
     * Checks if the Service is running via the isLocationServiceRunning() method, and then checks
     * if this device has bluetooth and initialises the relavant class variables. If bluetooth is
     * available and not activated, then a request of the user to activate it is made, and the
     * function that deals with the bluetooth activation request result will be left to call the
     * startService() function. If no bluetooth is available on this device, then this function
     * calls startService().
     *
     */
    private void startLocationService() {

        Log.d("mgdev", "MainActivity.startLocationService");

        if (!isLocationServiceRunning(this)) {

            //Log.d("mgdev", "MainActivity.startLocationService.!isLocationServiceRunning()");

            if(bluetoothAdapter == null){
                // then no bluetooth capabilties
            }else{
                if(bluetoothLeScanner == null){
                    // probably no BLE
                }else{
                    // request bluetooth activation
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        return;
                    }
//                    else{
//                        startService();
//                    }

                }
            }

            startService();

        }
    }// end of startLocationService
    //==============================================================================================

    //==============================================================================================
    /**
     * startService method
     * Sets start/stop button text to "stop", and class variable 'running' to true.
     * Then launches the TrackerScanner service in citizen science mode
     *
     */
    private void startService(){

//        startStopButton.setText(R.string.start_button_stop_text);
//        running = true;

        String packageName =  this.getClass().getName();

        Log.d("mgdev", "MainActivity.startService: "+packageName );
        //Log.d("mgdev", "MainActivity.startService");


        luanchService(packageName, this, _deviceID);



        checkButtons();

        //runThread();

    }// end of startService
    //==============================================================================================

    //==============================================================================================
    public static void luanchService(String packageName, Context context, String deviceID){


        Log.d("mgdev", "MainActivity.luanchService: "+packageName );

        Intent intent = new Intent(context, TrackerScanner.class);
        intent.setAction(
                context.getResources().getString(R.string.action_start_location_service)
        );
        intent.putExtra("MODE", true); // engage citizen mode

        String address = "riba2reality.com";

        String dataBase = "devTest";

        String postType = "POST";


        boolean useSSL = true;

        intent.putExtra("ServerAddress", address);
        intent.putExtra("database", dataBase);
        intent.putExtra("DeviceID", deviceID);
        intent.putExtra("SSL_switch", useSSL);

        intent.putExtra("PACKAGE", packageName);

        intent.putExtra("post_type", postType);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        Toast.makeText(context, "Location service started", Toast.LENGTH_SHORT).show();

    }// end of luanchService
    //==============================================================================================

    //==============================================================================================

    /**
     * onActivityResult method
     * Deals with the result of requesting the user activate bluetooth, if accepted it calls the
     * startService method, otherwise displaying a toast message.
     *
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode,  data);

        if( resultCode == RESULT_OK){
            startService();
            Log.d("mgdev", "HomescreenFragment.onActivityResult.RESULT_OK");
        }else{
            Log.d("mgdev", "HomescreenFragment.onActivityResult. ELSE");
            Toast.makeText(MainActivity.this,
                    getResources().getString(R.string.BluetoothRequired),
                    Toast.LENGTH_SHORT).show();
        }

    }
    //==============================================================================================


    //==============================================================================================

    /**
     * stopLocationService method
     *
     * Checks if the TrackerScanner service is running and stops it, also setting class variable
     * 'running' to false, and the start/stop button back to "start"
     *
     */
    private void stopLocationService() {


        if (isLocationServiceRunning(this)) {

//            startStopButton.setText(R.string.start_button_initial_text);
//            running = false;

            Intent intent = new Intent(getApplicationContext(), TrackerScanner.class);
            intent.setAction(
                    getResources().getString(R.string.action_stop_location_service)
            );
            startService(intent);
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();

            checkButtons();

        }// end of if isLocationServiceRunning
    }// end of startLocationService
    //==============================================================================================


    //==============================================================================================

    /**
     * runButtonsThread method
     *
     * Creates a new monitor thread which will run while the activity is shown, and changes the
     * background colour every 300 milliseconds to either green or red if the TrackerScanner is
     * running or not respectively.
     *
     */
    private void runButtonsThread() {

        new Thread() {
            public void run() {
                while (active) {
                    try {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                checkButtons();

                                // would be better to have broadcast receive for this too, but
                                // otherwise this will check every time there is a pulse
                                //checkForInternetConnection();


                            }// end of run function
                        });
                        Thread.sleep(checkButtonsinterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    //==============================================================================================


    //==============================================================================================
    /**
     * runPulseThread method
     *
     * Creates a new monitor thread which will run while the activity is shown, activates the
     * pulsing animation if the backend service is running
     *
     */
    private void runPulseThread() {

        new Thread() {
            public void run() {
                while (active) {
                    try {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                if(isLocationServiceRunning(selfRef)){
                                    startDisplayIconAnimation();
                                }


                            }// end of run function
                        });
                        Thread.sleep(pulseInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }// end of runPulseThread
    //==============================================================================================


    //==============================================================================================
    /**
     * startDisplayIconAnimation
     *
     * starts a display animation thread.
//     * Makes the view visible.
     *
     */
//    private void startDisplayIconAnimation(){
//        this.diplayIconAnimation.run();
////        this.displayIconView.setVisibility(View.VISIBLE);
//    }// end of startDisplayIconAnimation
    //==============================================================================================


    //==============================================================================================
    /**
     * stopDisplayIconAnimation
     *
     * stops the icon animation if in profress, and makes it invisible.
     *
     */
//    private void stopDisplayIconAnimation(){
//        iconAniHandler.removeCallbacks(diplayIconAnimation);
//        this.displayIconView.setVisibility(View.GONE);
//    }// end of startDisplayIconAnimation
    //==============================================================================================


    //==============================================================================================
    /**
     * diplayIconAnimation Runnable
     *
     * Create a location pulse, by enlarging one circle then the next, and fading them as they go.
     * Finally setting the circles back to their original state.
     *
     */
    private void startDisplayIconAnimation() {

        new Thread() {
            @Override
            public void run() {

                float scaleFactor = 8f;

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        circleAnimation1.animate()
                                .scaleX(scaleFactor)
                                .scaleY(scaleFactor)
                                .alpha(0f)
                                .setDuration(pulseDuration)
                                .withEndAction(
                                        new Runnable() {
                                            @Override
                                            public void run() {

                                                circleAnimation1.setScaleX(1f);
                                                circleAnimation1.setScaleY(1f);
                                                circleAnimation1.setAlpha(1f);


                                            }// end of run
                                        }// end of Runable
                                )
                        ;


                    }// end of run function
                });



                try {

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        circleAnimation2.animate()
                                .scaleX(scaleFactor)
                                .scaleY(scaleFactor)
                                .alpha(0f)
                                .setDuration(pulseDuration)
                                .withEndAction(
                                        new Runnable() {
                                            @Override
                                            public void run() {

                                                circleAnimation2.setScaleX(1f);
                                                circleAnimation2.setScaleY(1f);
                                                circleAnimation2.setAlpha(1f);

                                            }// end of run
                                        }// end of Runable
                                );


                    }// end of run function
                });



                //iconAniHandler.postDelayed(diplayIconAnimation, 1500);


            }// end of run
        }.start(); // end of Runnable
    }// end of startDiplayIconAnimation
    //==============================================================================================


}// end of MainActivity