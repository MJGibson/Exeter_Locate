package com.riba2reality.exeterlocatehome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.TaskStackBuilder;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {


    private static final String CHANNEL_ID = "this.is.my.channelId";//you can add any id you want


    //==============================================================================================
    @Override
    public void onReceive(Context context, Intent intent) {


        Log.d("mgdev", "AlarmReceiver.onReceive");


        Calendar cal = Calendar.getInstance();//getting calender instance
        cal.setTimeInMillis(System.currentTimeMillis());//setting the time from device

        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        boolean weekend = false;
        if(dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SATURDAY){
            weekend = true;
        }


        if(!MainActivity.isLocationServiceRunning(context) && !weekend) {

            //on tap this activity will open
            Intent notificationIntent = new Intent(context, MainActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(notificationIntent);

            PendingIntent pendingIntent = stackBuilder.getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT);//getting the pendingIntent

            Notification.Builder builder = new Notification.Builder(context);//building the notification

            Notification notification = builder.setContentTitle("Exeter Locate")
                    .setContentText("Please remember to turn on Exeter Locate")
                    //.setTicker("New Message Alert!")
                    .setSmallIcon(R.mipmap.exeter_locate_icon)
                    .setContentIntent(pendingIntent).build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID);
            }

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//below creating notification channel, because of androids latest update, O is Oreo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "NotificationExeterLocate",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                notificationManager.createNotificationChannel(channel);
            }

            notificationManager.notify(0, notification);
        }// end of if not isLocationServiceRunning


    }// end of onReceive
    //==============================================================================================





}//end of class
