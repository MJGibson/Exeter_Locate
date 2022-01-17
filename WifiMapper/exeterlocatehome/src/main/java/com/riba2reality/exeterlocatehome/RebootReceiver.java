package com.riba2reality.exeterlocatehome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {


            Log.d("mgdev", "RebootReceiver.onReceive");

            MainActivity.startAlarms(context);

            //---------------------------

            Toast.makeText(context, "REBOOT received....", Toast.LENGTH_SHORT).show();

            //String packageName = "com.riba2reality.exeterlocate.MainActivity";
            String packageName = MainActivity.class.getName();

            // check if we already have a UUID, if not make a new one and store it
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);
            final SharedPreferences.Editor SPeditor = SP.edit();
            String deviceID = SP.getString("DeviceID", "");
            if(deviceID.isEmpty()){
                deviceID = UUID.randomUUID().toString();
                SPeditor.putString("DeviceID", deviceID);
                SPeditor.apply();
            }

            MainActivity.luanchService(packageName, context, deviceID);


        }// end of if BOOT_COMPLETED

    }// end of onReceive

}// end of class
