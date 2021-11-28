package com.riba2reality.exeterlocate;

import android.app.AlarmManager;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlarmManager;


@RunWith(RobolectricTestRunner.class)

//@LargeTest
public class AlarmReceiverTests extends TestCase {

//    @Rule
//    public ActivityScenarioRule<BluetoothMessageActivity> activityRule
//            = new ActivityScenarioRule<>(BluetoothMessageActivity.class);

    Context context;

    ShadowAlarmManager shadowAlarmManager;
    AlarmManager alarmManager;


    //==============================================================================================
    @Before
    public void init() {

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();



//        ActivityScenario<BluetoothMessageActivity> scenario =
//                ActivityScenario.launch(BluetoothMessageActivity.class);

        //alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        alarmManager = (AlarmManager) RuntimeEnvironment.getApplication().getSystemService(Context.ALARM_SERVICE);
        //alarmManager = (AlarmManager) ApplicationProvider.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = Shadows.shadowOf(alarmManager);


    }// end of init
    //==============================================================================================


    //==============================================================================================
    @Test
    public void CheckAlarms(){


//        ActivityScenario<InternetMessageActivity> scenario =
//                ActivityScenario.launch(InternetMessageActivity.class);



        Assert.assertNull(shadowAlarmManager.getNextScheduledAlarm());
        //new ResetAlarm(RuntimeEnvironment.getApplication().getApplicationContext());
        MainActivity.startAlarms(RuntimeEnvironment.getApplication().getApplicationContext());
        ShadowAlarmManager.ScheduledAlarm repeatingAlarm = shadowAlarmManager.getNextScheduledAlarm();
        Assert.assertNotNull(repeatingAlarm);




    }// end of checkTitle
    //==============================================================================================



}// end of AlarmReceiverTests
