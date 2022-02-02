package com.riba2reality.exeterlocate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class ResetAlarmReceiver extends BroadcastReceiver {




    //==============================================================================================
    @Override
    public void onReceive(Context context, Intent intent) {


        Log.d("mgdev", "ResetAlarmReceiver.onReceive");


        Calendar cal = Calendar.getInstance();//getting calender instance
        cal.setTimeInMillis(System.currentTimeMillis());//setting the time from device

        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        boolean weekend = false;
        if(dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY){
            weekend = true;
        }


        if(
                //!MainActivity.isLocationServiceRunning(context) &&
                        !weekend) {
            // reset the service


            MainActivity.stopLocationService(context);

            MainActivity.startService(context);


        }// end of if


    }// end of onReceive
    //==============================================================================================





}//end of class
