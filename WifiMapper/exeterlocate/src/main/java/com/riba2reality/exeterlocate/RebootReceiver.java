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

//            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//
//            Intent notificationIntent = new Intent("android.media.action.DISPLAY_NOTIFICATION");
//            notificationIntent.addCategory("android.intent.category.DEFAULT");
//
//            PendingIntent broadcast = PendingIntent.getBroadcast(context, 100, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//            Calendar cal = Calendar.getInstance();
//            cal.set(Calendar.YEAR, 2017);
//            cal.set(Calendar.MONTH, 8-1);
//            cal.set(Calendar.DAY_OF_MONTH, 10);
//            cal.set(Calendar.HOUR_OF_DAY, 10);
//            cal.set(Calendar.MINUTE, 15);
//            cal.set(Calendar.SECOND, 4);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), broadcast);
//            }
        }// end of if BOOT_COMPLETED

    }// end of onReceive

}// end of class
