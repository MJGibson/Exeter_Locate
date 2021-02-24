package com.riba2reality.wifimapper;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.riba2reality.wifimapper.DataStores.Constants;
import com.riba2reality.wifimapper.ui.main.SectionsPagerAdapter;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;

    BroadcastReceiver receiver;

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(TrackerScanner.TRACKERSCANNER_RESULT)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    // enable the text box content to be save when rotating screen
    // we can then extract the value and write it to the text box
    // instead of having an empty text box after rotation
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        TextView log = findViewById(R.id.log);
        if(log!=null) {
            outState.putCharSequence("textbox_contents", (log).getText());
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // if we've saved the state before (e.g. we rotated the screen),
        // then load up the text previously in the text box
        if (savedInstanceState != null) {
            TextView logTextView = findViewById(R.id.log);
            logTextView.setText(savedInstanceState.getCharSequence("textbox_contents"));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);


        //------------

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra(TrackerScanner.TRACKERSCANNER_MESSAGE);

                // do something here.

                //System.out.println("Message: "+message);

                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                //String message = "Time:" + currentTime + "\nLat:" + latitude + "\nLong:" + longitude;

                TextView logTextView = findViewById(R.id.log);

                final ScrollView scroll = findViewById(R.id.logScroll);


                if (logTextView != null) {
                    // append to the log text
                    logTextView.append(
                            "\n### " + currentTime + " ###"
                                    + "\n" + message
                    );

                    // count the number of lines to remove, i.e. the number of lines > the maximum
                    int linesToRemove = logTextView.getLineCount() - getBaseContext().getResources().getInteger(R.integer.max_log_lines);

                    // if there some to remove
                    if (linesToRemove > 0) {
                        // get the text from the logger and declare some variables we'll need
                        Editable txt = logTextView.getEditableText();
                        int lineStart, lineEnd, i;

                        for (i = 0; i < linesToRemove; i++) {
                            // get the start and end locations of the first line of the text
                            lineStart = logTextView.getLayout().getLineStart(0);
                            lineEnd = logTextView.getLayout().getLineEnd(0);

                            // remove it
                            txt.delete(lineStart, lineEnd);
                        }
                    }

                    scroll.post(new Runnable() {
                        @Override
                        public void run() {
                            scroll.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });

                }

            }
        };

        //FloatingActionButton fab = findViewById(R.id.fab);

        //------------

        findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION_PERMISSIONS
                    );
                } else {
                    startLocationService();

                }


            }

        });

        findViewById(R.id.stop_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationService();
            }
        });




    }// end of onCreate method





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }

    }// end of onRequestPermissionsResult

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


    private void startLocationService() {
        if (!isLocationServiceRunning()) {

            String[] server_values = getResources().getStringArray(R.array.server_values);

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String serverAddress = SP.getString("ServerAddress", server_values[1]);

            //System.out.println("ServerAddress: "+serverAddress);

            if (serverAddress.isEmpty() || serverAddress.equals(server_values[0])) {
                Toast.makeText(this, "Please set Server Address", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(getApplicationContext(), TrackerScanner.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show();
        }
    }// end of startLocationService


    private void stopLocationService() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), TrackerScanner.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            //stopService(intent);//?!
            startService(intent);
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }// end of startLocationService


}// end of class