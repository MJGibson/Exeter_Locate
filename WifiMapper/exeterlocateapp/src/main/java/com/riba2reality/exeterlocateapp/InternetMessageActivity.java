package com.riba2reality.exeterlocateapp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Exeter Locate App - Is a citizen science driven project, which allows uses to donate their
 * anonymized Location, Wi-Fi, Bluetooth, accelerometer and magnetometer data. By many citizens
 * contributing small amounts of data in the limited area of the geoFence (University of Exeter -
 * Streatham campus), better locations service could be developed.
 *
 * InternetMessageActivity Class of type Android Activity, informs the user that an internet
 * connection is required, and shows a button which will take the user to the Wi-Fi settings.
 * Automatically closing if/when Internet is re-activated
 *
 * @author <a href="mailto:M.J.Gibson@Exeter.ac.uk">Michael J Gibson</a>
 * @version 1.0
 * @since   2021-09-12
 *
 */
public class InternetMessageActivity extends AppCompatActivity {


    private ImageView messageIcon;
    private TextView title;
    private TextView message;
    private Button ok_button;

    private boolean active;

    //----------------------------------------------------------------------------------------------

    //==============================================================================================
    /**
     *  Checks if Wi-Fi has been re-activated, and finishes this activity if so
     */
    @Override
    protected void onStart() {
        super.onStart();
        active = true;
        runThread();

        Log.d("mgdev", "InternetMessageActivity.onStart");


        checkForInternetConnection();


    }// end of onStart
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
        Log.d("mgdev", "InternetMessageActivity.onStop");
    }
    //==============================================================================================

    //==============================================================================================
    private void checkForInternetConnection(){

        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
            finish();
        }
        else {
            connected = false;

        }

    }// end of checkForInternetConnection
    //==============================================================================================

    //==============================================================================================
    /**
     * Creates and sets up the message activity UI and sets up activity
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up UI
        setContentView(R.layout.message_layout);


        // setup pointers to UI elements
        messageIcon = findViewById(R.id.imageView_message_icon);
        title = findViewById(R.id.textView_title);
        message = findViewById(R.id.textView_Message);
        ok_button = findViewById(R.id.message_button);

        // set up title bar
        ActionBar actionBar = getSupportActionBar();
        //actionBar.setDisplayShowHomeEnabled(true);

        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.mipmap.uoe_logo);
        actionBar.setDisplayUseLogoEnabled(true);

        // get rid of action bar...
        actionBar.hide();

        // set up text

        title.setText("For this app to work, you must have an Internet connection");
        message.setText("This App uses the internet to communicate results to server" +
                ". If you have no internet connection, this app will not work.\n\n" +
                "Please go to setting and turn on Wi-Fi or mobile data.");
        messageIcon.setImageResource(R.drawable.internet_disconnected_foreground);
        ok_button.setText("Allow Wi-Fi");
        ok_button.setOnClickListener(allowWifiButtonPressed);

        // add broadcast receivers for ble turned on
        //this.registerReceiver(receiver, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));



    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    /**
     * Click Listener for the 'Allow Wi-Fi' button, which opens the users Wi-Fi settings
     */
    View.OnClickListener allowWifiButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // open bluetooth settings
            Intent intentOpenWifiSettings = new Intent();
            intentOpenWifiSettings.setAction(
                    android.provider.Settings.ACTION_WIFI_SETTINGS);
            startActivity(intentOpenWifiSettings);


        }// end of onClick
    };// end of allowWifiButtonPressed click listener
    //==============================================================================================

//    //==============================================================================================
//    /**
//     * Broadcast receiver for if Wi-Fi settings are changed; if they are turned on it will close
//     * this activity.
//     */
//    BroadcastReceiver receiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            Log.d("mgdev", "WifiMessageActivity.onReceive");
//
//            checkWifiEnabled();
//
////            NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
////            NetworkInfo otherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);
////
////            if(currentNetworkInfo.isConnected()){
////                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
////            }else{
////                Toast.makeText(getApplicationContext(), "Not Connected", Toast.LENGTH_LONG).show();
////            }
//
//        }// end of onReceive
//    };// end of BroadcastReceiver receiver
//    //==============================================================================================


    //==============================================================================================
    @Override
    public void onBackPressed()
    {
        // Don't do anything, until they turn bluetooth
        //super.onBackPressed();

        //close the whole app??
        // System.exit(0);

    }
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



                                // would be better to have broadcast receive for this too, but
                                // otherwise this will check every time there is a pulse
                                checkForInternetConnection();


                            }// end of run function
                        });
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    //==============================================================================================





}//end of InternetMessageActivity