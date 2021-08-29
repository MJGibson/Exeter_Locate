package com.riba2reality.exeterlocateapp;


import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.riba2reality.exeterlocatecore.TrackerScanner;

import java.util.UUID;

/**
 * Exeter Locate App - Is a citizen science driven project, which allows uses to donate their
 * anonymized Location, Wi-Fi, Bluetooth, accelerometer and magnetometer data. By many citizens
 * contributing small amounts of data in the limited area of the geoFence (University of Exeter -
 * Streatham campus), better locations service could be developed.
 *
 * MainActivity Class of type Android Activity, acts as the main frontend for Exeter Locate App.
 * When active this class will display the status of the backend service by displaying a background
 * colour (Red for deactivated, Green for active). NOTE: this is currently achieved by starting and
 * stopping a monitoring thread that updates the status every 300 milliseconds.
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

    // bluetooth
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;

    // UI variables
    private Button startStopButton;
    private boolean running = false;
    private boolean active = false;
    private ConstraintLayout mainLayout;
    //private TextView versionTextView;
    private FloatingActionButton infoButton;
    private ImageView imageViewBackground;

    private View displayIconView;
    private ImageView circleCore;
    private ImageView circleAnimation1;
    private ImageView circleAnimation2;
    private Handler iconAniHandler;




    // UUID
    private String _deviceID;


    //##############################################################################################

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
        runThread();
        checkButtons();
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
    }
    //==============================================================================================

    //==============================================================================================
    /**
     * checkButtons method
     * Uses isLocationServiceRunning to check if the service is running, and sets up the start/stop
     * button appropriatley, along with the class boolean 'running'
     */
    private void checkButtons(){

        if( isLocationServiceRunning() ){
            startStopButton.setText(R.string.start_button_stop_text);
            running = true;
        }else{
            startStopButton.setText(R.string.start_button_initial_text);
            running = false;
        }


    }//end of checkButtons
    //==============================================================================================

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
        startStopButton = findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(startStopButtonPressed);
        mainLayout = findViewById(R.id.main_layout);
        infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(infoButtonPressed);
        imageViewBackground = findViewById(R.id.imageViewBackground);

        this.iconAniHandler = new Handler();
        this.displayIconView = findViewById(R.id.displayIcon);
        this.circleCore = findViewById(R.id.circleBase);
        this.circleAnimation1 = findViewById(R.id.circleAnimation1);
        this.circleAnimation2 = findViewById(R.id.circleAnimation2);






        ActionBar actionBar = getSupportActionBar();
        //actionBar.setDisplayShowHomeEnabled(true);

        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.mipmap.uoe_logo);
        actionBar.setDisplayUseLogoEnabled(true);



        // set versions
//        versionTextView = findViewById(R.id.version_textView);
//
//        //versionTextView.setEnabled(false);
//        versionTextView.setText(
//                "Version: " + BuildConfig.VERSION_NAME + "\n"
//                //+ "Core Version: " + versionName
//                + "Core Version: " + TrackerScanner.libraryVersion
//                );

        // check if we already have a UUID, if not make a new one and store it
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        String _deviceID = SP.getString("DeviceID", "");
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
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        // add displayIconView
        //setContentView(new DisplayIconView(imageView));


        startDisplayIconAnimation();

    }// end of onCreate
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

    //==============================================================================================
    /**
     * startInfoActivty method
     * Starts the InfoActivity
     */
    public void startInfoActivty(){
        Intent intent = new Intent(this, InfoActivity.class);
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
    private boolean isLocationServiceRunning() {

        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
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

        if (!isLocationServiceRunning()) {

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

        startStopButton.setText(R.string.start_button_stop_text);
        running = true;

        Log.d("mgdev", "MainActivity.startService");

        Intent intent = new Intent(this.getApplicationContext(), TrackerScanner.class);
        intent.setAction(
                getResources().getString(R.string.action_start_location_service)
        );
        intent.putExtra("MODE", true); // engage citizen mode

        String address = "3.9.100.243";

        String dataBase = "dev";

        String deviceID = _deviceID;

        boolean useSSL = true;

        intent.putExtra("ServerAddress", address);
        intent.putExtra("database", dataBase);
        intent.putExtra("DeviceID", deviceID);
        intent.putExtra("SSL_switch", useSSL);


        startService(intent);
        Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show();

        //runThread();

    }// end of startService
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


        if (isLocationServiceRunning()) {

            startStopButton.setText(R.string.start_button_initial_text);
            running = false;

            Intent intent = new Intent(getApplicationContext(), TrackerScanner.class);
            intent.setAction(
                    getResources().getString(R.string.action_stop_location_service)
            );
            startService(intent);
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }// end of startLocationService
    //==============================================================================================


    //==============================================================================================

    /**
     * runThread method
     *
     * Creates a new monitor thread which will run while the activity is shown, and changes the
     * background colour every 300 milliseconds to either green or red if the TrackerScanner is
     * running or not respectively.
     *
     */
    private void runThread() {

        new Thread() {
            public void run() {
                while (active) {
                    try {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                //btn.setText("#" + i);

                                if(isLocationServiceRunning()){
//                                    mainLayout.setBackgroundColor(getResources().getColor(
//                                            R.color.green)
//                                    );
                                }else{
//                                    mainLayout.setBackgroundColor(getResources().getColor(
//                                            R.color.red)
//                                    );
                                }




                            }// end of run function
                        });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    //==============================================================================================

//    //==============================================================================================
//    public class DisplayIconView extends View
//    {
//        Paint paint = null;
//
//        public DisplayIconView(Context context)
//        {
//            super(context);
//            paint = new Paint();
//        }
//
//        public DisplayIconView(Context context, AttributeSet attrs)
//        {
//            super(context,attrs);
//            paint = new Paint();
//        }
//
//        @Override
//        protected void onDraw(Canvas canvas)
//        {
//            super.onDraw(canvas);
//            int x = getWidth();
//            int y = getHeight();
//            int radius;
//            radius = x/4;
//            paint.setStyle(Paint.Style.FILL);
//            paint.setColor(Color.RED);
//            //canvas.drawPaint(paint);
//            // Use Color.parseColor to define HTML colors
//            //paint.setColor(Color.parseColor("#CD5C5C"));
//            canvas.drawCircle(x / 2, y / 4, radius, paint);
//        }
//    }
    //==============================================================================================

    //==============================================================================================
    private void startDisplayIconAnimation(){
        this.diplayIconAnimation.run();
        this.displayIconView.setVisibility(View.VISIBLE);
    }// end of startDisplayIconAnimation
    //==============================================================================================


    //==============================================================================================
    private void stopDisplayIconAnimation(){
        iconAniHandler.removeCallbacks(diplayIconAnimation);
        this.displayIconView.setVisibility(View.GONE);
    }// end of startDisplayIconAnimation
    //==============================================================================================


    //==============================================================================================
    private Runnable diplayIconAnimation = new Runnable() {
        @Override
        public void run() {


            circleAnimation1.animate().scaleX(4f).scaleY(4f).alpha(0f).setDuration(1000)
                    .withEndAction(
                            new Runnable() {
                                @Override
                                public void run() {

                                    circleAnimation1.setScaleX(1f);
                                    circleAnimation1.setScaleY(1f);
                                    circleAnimation1.setAlpha(1f);

                                }// end of run
                            }// end of Runable
                    );

            circleAnimation2.animate().scaleX(4f).scaleY(4f).alpha(0f).setDuration(700)
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

            iconAniHandler.postDelayed(diplayIconAnimation, 1500);


        }// end of run
    }; // end of diplayIconAnimation
    //==============================================================================================


}// end of MainActivity