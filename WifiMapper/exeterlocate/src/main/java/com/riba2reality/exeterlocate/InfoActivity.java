package com.riba2reality.exeterlocate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.riba2reality.exeterlocatecore.TrackerScanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

/**
 * Exeter Locate App - Is a citizen science driven project, which allows uses to donate their
 * anonymized Location, Wi-Fi, Bluetooth, accelerometer and magnetometer data. By many citizens
 * contributing small amounts of data in the limited area of the geoFence (University of Exeter -
 * Streatham campus), better locations service could be developed.
 *
 * InfoActivity Class of type Android Activity, displays information and T&C's about the app and
 * project
 *
 * @author <a href="mailto:M.J.Gibson@Exeter.ac.uk">Michael J Gibson</a>
 * @version 1.0
 * @since   2021-08-20
 *
 */
public class InfoActivity extends AppCompatActivity {


    private TextView versionTextView;
    private TextView idTextView;
    private WebView infoWebView;

    //==============================================================================================
    /**
     * Sets up front end
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        versionTextView = findViewById(R.id.textViewVersion);
        //idTextView = findViewById(R.id.textViewID);
        infoWebView = findViewById(R.id.infoWebView);


        // check if we already have a UUID, if not make a new one and store it
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        String _deviceID = SP.getString("DeviceID", "");
        if(_deviceID.isEmpty()){
            _deviceID = UUID.randomUUID().toString();
            SPeditor.putString("DeviceID", _deviceID);
            SPeditor.apply();
        }

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

        // set versions
        //versionTextView.setEnabled(false);
        versionTextView.setText(
                "Version: " + BuildConfig.VERSION_NAME
                + "("+Integer.toString(BuildConfig.VERSION_CODE)+")    "
                //+ "Core Version: " + versionName
                + "Core Version: " + TrackerScanner.libraryVersion
                //+ "\n" + "Device ID: " + _deviceID
                );

//        // set ID disply
//        idTextView.setText(
//                "Device ID: " + _deviceID
//        );

        // set up webview - information, T&C's

        infoWebView.setWebViewClient(new WebViewClient());
        // load from asset file, but could check an actualy website and default to this
        infoWebView.loadUrl("file:///android_asset/ExeterLocateInformationpage.html");

//        String infoPageHtml = readTextFromAsset("ExeterLocateInformationpage.html");
//
//
//        infoWebView.loadDataWithBaseURL(null,
//                "<style>img{display: inline;height: auto;max-width: 100%;}</style>"
//                        + infoPageHtml,
//                "text/html",
//                "UTF-8",
//                null);



    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    private String readTextFromAsset(String fileName){

        String returnValue = "";

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open(fileName)));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                //process line
                returnValue+=mLine;

            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }

        return returnValue;

    }
    //==============================================================================================

    //==============================================================================================
    /**
     * Alters the normal back pressed, so if the webView can go back then that goes back, if not
     * then it exits as normal
     */
    @Override
    public void onBackPressed() {
        if(infoWebView.canGoBack()){
            infoWebView.goBack();
        }else {
            super.onBackPressed();
        }
    }
    //==============================================================================================




}// end of InfoActivity