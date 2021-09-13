package com.riba2reality.exeterlocateapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
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
 * GpsMessageActivity Class of type Android Activity, informs the user that the GPS is
 * required, and shows a button which will take the user to the GPS settings.
 * Automatically closing if/when GPS is re-activated
 *
 * @author <a href="mailto:M.J.Gibson@Exeter.ac.uk">Michael J Gibson</a>
 * @version 1.0
 * @since   2021-09-12
 *
 */
public class GpsMessageActivity extends AppCompatActivity {


    private ImageView messageIcon;
    private TextView title;
    private TextView message;
    private Button ok_button;

    //----------------------------------------------------------------------------------------------

    //==============================================================================================
    /**
     *  Checks if GPS has been re-activated, and finishes this activity if so
     */
    @Override
    protected void onStart() {
        super.onStart();

        Log.d("mgdev", "WifiMessageActivity.onStart");


        checkGpsEnabled();


    }// end of onStart
    //==============================================================================================



    //==============================================================================================
    private void checkGpsEnabled(){


        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if (manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {

            Log.d("mgdev", "GpsMessageActivity.checkGpsEnabled. GPS enabled");

            finish();

        }// end of if gps enabled

    }// end of checkGpsEnabled
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
        actionBar.setLogo(R.mipmap.exeter_locate_icon);
        actionBar.setDisplayUseLogoEnabled(true);

        // get rid of action bar...
        actionBar.hide();

        // set up text

        title.setText("For this app to work, you must have GPS on");
        message.setText("This App uses GPS to locate this device when scanning other devices." +
                "This information helps researchers build a map of other scans." +
                "If you have GPS turned off, this app will not work.\n\n" +
                "Please go to setting and turn on GPS.");
        messageIcon.setImageResource(R.drawable.gps_disconnected_foreground);
        ok_button.setText("Allow GPS");
        ok_button.setOnClickListener(allowGPSButtonPressed);

        // add broadcast receivers for ble turned on

        this.registerReceiver(receiverGPS, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));



    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    /**
     * Click Listener for the 'Allow GPS' button, which opens the users GPS settings
     */
    View.OnClickListener allowGPSButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // open bluetooth settings
            Intent intentOpenGPSSettings = new Intent();
            intentOpenGPSSettings.setAction(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intentOpenGPSSettings);


        }// end of onClick
    };// end of allowWifiButtonPressed click listener
    //==============================================================================================

    //==============================================================================================
    /**
     * Broadcast receiver for if GPS settings are changed; if they are turned on it will close
     * this activity.
     */
    BroadcastReceiver receiverGPS = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "WifiMessageActivity.onReceive");

            checkGpsEnabled();

        }// end of onReceive
    };// end of BroadcastReceiver receiver
    //==============================================================================================


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





}//end of GpsMessageActivity