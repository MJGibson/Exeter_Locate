package com.riba2reality.exeterlocateapp.messages;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.riba2reality.exeterlocateapp.R;

/**
 * Exeter Locate App - Is a citizen science driven project, which allows uses to donate their
 * anonymized Location, Wi-Fi, Bluetooth, accelerometer and magnetometer data. By many citizens
 * contributing small amounts of data in the limited area of the geoFence (University of Exeter -
 * Streatham campus), better locations service could be developed.
 *
 * BluetoothMessageActivity Class of type Android Activity, informs the user that the bluetooth is
 * required, and shows a button which will take the user to the bluetooth settings.
 * Automatically closing if/when bluetooth is re-activated
 *
 * @author <a href="mailto:M.J.Gibson@Exeter.ac.uk">Michael J Gibson</a>
 * @version 1.0
 * @since   2021-09-12
 *
 */
public class BluetoothMessageActivity extends AppCompatActivity {


    private ImageView messageIcon;
    private TextView title;
    private TextView message;
    private Button ok_button;

    //----------------------------------------------------------------------------------------------

    //==============================================================================================
    /**
     *  Checks if bluetooth has been re-activated, and finishes this activity if so
     */
    @Override
    protected void onStart() {
        super.onStart();

        Log.d("mgdev", "BluetoothMessageActivity.onStart");

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled :)
        } else {
            // Bluetooth is enabled, so finish

            finish();

        }



    }// end of onStart
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
//
//        Intent intent = getIntent();
//
//        if(intent != null) {
//            String titleText = intent.getStringExtra("title");
//            String messageText = intent.getStringExtra("message");
//            int iconResource = intent.getIntExtra("icon",-1);
//
//            if(titleText != null)
//                title.setText(titleText);
//            if(messageText != null)
//                message.setText(messageText);
//            if(iconResource != -1) {
//
//                messageIcon.setImageResource(iconResource);
//            }
//
//
//        }


        title.setText("For this app to work, you must have Bluetooth on");
        message.setText("This App uses Bluetooth to locate nearby Bluetooth devices" +
                ". If you have Bluetooth turned off, this app will not work.\n\n This App uses " +
                "'Bluetooth low energy' - a battery saving technology.\n\n" +
                "Please go to setting and turn on Bluetooth.");
        messageIcon.setImageResource(R.drawable.bluetoot_disconnected_foreground);
        ok_button.setText("Allow Bluetooth");
        ok_button.setOnClickListener(allowBluetoothButtonPressed);

        // add broadcast receivers for ble turned on
        this.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));



    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    /**
     * Click Listener for the 'Allow bluetooth' button, which opens the users bluetooth settings
     */
    View.OnClickListener allowBluetoothButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // open bluetooth settings
            Intent intentOpenBluetoothSettings = new Intent();
            intentOpenBluetoothSettings.setAction(
                    android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intentOpenBluetoothSettings);


        }// end of onClick
    };// end of allowBluetoothButtonPressed click listerner
    //==============================================================================================

    //==============================================================================================
    /**
     * Broadcast receiver for if bluetooth settings are changed; if they are turned on it will close
     * this activity.
     */
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("mgdev", "BluetoothMessageActivity.onReceive");

            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_ON) {
                    // Bluetooth was re-connected
                    Log.d("mgdev", "BluetoothMessageActivity.onReceive.BluetoothAdapter.STATE_ON");

                    //close this activity
                    finish();


                }
            }

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





}//end of BluetoothMessageActivity