package com.riba2reality.wifimapper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import com.riba2reality.exeterlocatecore.DataStores.Constants;
import com.riba2reality.exeterlocatecore.TrackerScanner;
import com.riba2reality.wifimapper.ui.main.HomescreenFragment;
import com.riba2reality.wifimapper.ui.main.SectionsPagerAdapter;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;

    //BroadcastReceiver receiver;

    HomescreenFragment homescreenFragment;

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
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

        Log.d("Trace", "MainActivity.onCreate");

        setContentView(R.layout.activity_main);
        final SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        homescreenFragment = (HomescreenFragment) sectionsPagerAdapter.getItem(0);

        //------------



        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(TrackerScanner.TRACKERSCANNER_RESULT)
        );

        //FloatingActionButton fab = findViewById(R.id.fab);

        //------------

//        findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                if (ContextCompat.checkSelfPermission(
//                        getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(
//                            MainActivity.this,
//                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                            REQUEST_CODE_LOCATION_PERMISSIONS
//                    );
//                } else {
//                    startLocationService();
//
//                }
//
//
//            }
//
//        });
//
//        findViewById(R.id.stop_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                stopLocationService();
//            }
//        });




    }// end of onCreate method


    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(TrackerScanner.TRACKERSCANNER_MESSAGE);

            // do something here.

            //System.out.println("Message: "+message);

            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            //String message = "Time:" + currentTime + "\nLat:" + latitude + "\nLong:" + longitude;

            String outputMessage = "\n### " + currentTime + " ###" + "\n" + message;

            TextView logTextView = findViewById(R.id.log);

            final ScrollView scroll = findViewById(R.id.logScroll);


            homescreenFragment.addMessage(outputMessage);




//                if (logTextView != null) {
//
//                    Log.d("Trace", "MainActivity.receiver, logTextView != null");
//
//                    // append to the log text
//                    logTextView.append(
//                            "\n### " + currentTime + " ###"
//                                    + "\n" + message
//                    );
//
//                    // count the number of lines to remove, i.e. the number of lines > the maximum
//                    int linesToRemove = logTextView.getLineCount() - getBaseContext().getResources().getInteger(R.integer.max_log_lines);
//
//                    // if there some to remove
//                    if (linesToRemove > 0) {
//                        // get the text from the logger and declare some variables we'll need
//                        Editable txt = logTextView.getEditableText();
//                        int lineStart, lineEnd, i;
//
//                        for (i = 0; i < linesToRemove; i++) {
//                            // get the start and end locations of the first line of the text
//                            lineStart = logTextView.getLayout().getLineStart(0);
//                            lineEnd = logTextView.getLayout().getLineEnd(0);
//
//                            // remove it
//                            txt.delete(lineStart, lineEnd);
//                        }
//                    }
//
//                    scroll.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            scroll.fullScroll(ScrollView.FOCUS_DOWN);
//                        }
//                    });
//
//                }else{
//                    Log.d("Trace", "MainActivity.receiver, logTextView == null");
//                }


        }
    };





}// end of class