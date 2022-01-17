package com.riba2reality.exeterlocatehome;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ScrollView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Exeter Locate App - Is a citizen science driven project, which allows uses to donate their
 * anonymized Location, Wi-Fi, Bluetooth, accelerometer and magnetometer data. By many citizens
 * contributing small amounts of data in the limited area of the geoFence (University of Exeter -
 * Streatham campus), better locations service could be developed.
 *
 * TermsActivity Class of type Android Activity, shows the user the terms and conditions.
 * With a disagree button which will close the app, and once the terms are fully read, by scrolling
 * to the bottom, then an Accept button appears which allows the user to move into the App.
 *
 * @author <a href="mailto:M.J.Gibson@Exeter.ac.uk">Michael J Gibson</a>
 * @version 1.0
 * @since   2021-09-12
 *
 */
public class TermsActivity extends AppCompatActivity {


//    private ImageView messageIcon;
//    private TextView title;
//    private TextView message;
    private Button accept_button;
    private Button disagree_button;
    private WebView infoWebView;

    private ScrollView scrollView;



    private LocalBroadcastManager broadcaster;

    //----------------------------------------------------------------------------------------------

    //==============================================================================================
    /**
     *
     */
    @Override
    protected void onStart() {
        super.onStart();

        Log.d("mgdev", "TermsActivity.onStart");


//        this.registerReceiver(receiverTermsAcceptance,
//                new IntentFilter(getResources().getString(R.string.terms_acceptance)));


    }// end of onStart
    //==============================================================================================

    //==============================================================================================
    /**
     * onStop Method
     * Calls overriden method, and stops receivers
     */
    @Override
    protected void onStop() {
        super.onStop();

        Log.d("mgdev", "TermsActivity.onStop");

        // add broadcast receivers
//        this.unregisterReceiver(receiverTermsAcceptance);

    }
    //==============================================================================================


    //==============================================================================================
    /**
     * Creates and sets up the terms activity UI and sets up activity
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up UI
        setContentView(R.layout.terms_layout);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        broadcaster = LocalBroadcastManager.getInstance(this);


        // setup pointers to UI elements
//        messageIcon = findViewById(R.id.imageView_message_icon);
//        title = findViewById(R.id.textView_title);
//        message = findViewById(R.id.textView_Message);
        accept_button = findViewById(R.id.accept_button);
        disagree_button = findViewById(R.id.disagree_button);
        infoWebView = findViewById(R.id.infoWebView);

        scrollView =  findViewById(R.id.scroll);

        //------------------------------------------------------------

        accept_button.setOnClickListener(acceptButtonPressed);

        disagree_button.setOnClickListener(disagreeButtonPressed);


        //------------------------------------------------------------

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

        //------------------------------------------------------------



        // set up text

//        title.setText("For this app to work, you must have GPS on");
//        message.setText("This App uses GPS to locate this device when scanning other devices." +
//                "This information helps researchers build a map of other scans." +
//                "If you have GPS turned off, this app will not work.\n\n" +
//                "Please go to setting and turn on GPS.");
//        messageIcon.setImageResource(R.drawable.gps_disconnected_foreground);
//        ok_button.setText("Allow GPS");
//        ok_button.setOnClickListener(allowGPSButtonPressed);
//
//        // add broadcast receivers for ble turned on
//
//        this.registerReceiver(receiverGPS, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        //------------------------------------------------------------

        infoWebView.setWebViewClient(new WebViewClient());
        // load from asset file, but could check an actualy website and default to this
        infoWebView.loadUrl("file:///android_asset/ExeterLocateConcentPage.html");

        //------------------------------------------------------------

        accept_button.setVisibility(View.INVISIBLE);

        scrollView.getViewTreeObserver().addOnScrollChangedListener(
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        if (scrollView.getChildAt(0).getBottom()
                                <= (scrollView.getHeight() + scrollView.getScrollY())) {
                            //scroll view is at bottom
                            Log.v("mgdev", "--------BOTTOM--------");
                            accept_button.setVisibility(View.VISIBLE);
                        } else {
                            //scroll view is not at bottom
                            //Log.v("mgdev", "--------BOTTOM--------");
                        }
                    }
                }
        );


        //------------------------------------------------------------


    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    /**
     * Click Listener for the 'Accept' button, which closes the terms activity
     */
    View.OnClickListener acceptButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //
            sendResult();
            finish();


        }// end of onClick
    };// end of allowWifiButtonPressed click listener
    //==============================================================================================

    //==============================================================================================
    /**
     * Click Listener for the 'Disagree' button, which closes the application
     */
    View.OnClickListener disagreeButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // close application?
            finishAffinity();


        }// end of onClick
    };// end of allowWifiButtonPressed click listener
    //==============================================================================================


    //==============================================================================================
    public void sendResult() {
        Intent intent = new Intent(getResources().getString(R.string.terms_acceptance));

        intent.putExtra(getResources().getString(R.string.terms_acceptance), true);

        broadcaster.sendBroadcast(intent);
    }
    //==============================================================================================

//    //==============================================================================================
//    BroadcastReceiver receiverTermsAcceptance = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            Log.d("mgdev", "receiverTermsAcceptance.onReceive");
//
//            boolean termsAccepted = intent.getBooleanExtra(
//                    getResources().getString(R.string.terms_acceptance),
//                    false);
//
//            if(termsAccepted){
//                finish();
//            }
//
//
//        }// end of onReceive
//    };// end of receiverTermsAcceptance
//    //==============================================================================================




    //==============================================================================================
    @Override
    public void onBackPressed()
    {
        // Don't do anything, until they turn bluetooth
        //super.onBackPressed();

        //close the whole app??
        finishAffinity();
        // System.exit(0);

    }
    //==============================================================================================





}//end of GpsMessageActivity