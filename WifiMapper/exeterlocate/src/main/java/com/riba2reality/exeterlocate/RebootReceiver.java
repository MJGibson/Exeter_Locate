package com.riba2reality.exeterlocate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {


            Log.d("mgdev", "RebootReceiver.onReceive");

            MainActivity.startAlarms(context);


        }// end of if BOOT_COMPLETED

    }// end of onReceive

}// end of class
