package com.riba2reality.exeterlocateapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
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



    //==============================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
    }
    //==============================================================================================




}// end of InfoActivity