package com.riba2reality.exeterlocate;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.riba2reality.exeterlocate.messages.BluetoothMessageActivity;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class BluetoothMessageActivityTests extends TestCase {

//    @Rule
//    public ActivityScenarioRule<BluetoothMessageActivity> activityRule
//            = new ActivityScenarioRule<>(BluetoothMessageActivity.class);

    Context context;


    //==============================================================================================
    private void turnWifiOff(){

        //WifiMessageActivity._test = true;
        //Disable bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }


    }// end of turnWifiOff
    //==============================================================================================



    //==============================================================================================
    @Before
    public void init() {

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // turn off Wi-Fi, as otherwise it'll de-activate this activity.
        turnWifiOff();

        ActivityScenario<BluetoothMessageActivity> scenario =
                ActivityScenario.launch(BluetoothMessageActivity.class);


    }// end of init
    //==============================================================================================


    //==============================================================================================
    @Test
    public void checkTitle(){


//        ActivityScenario<InternetMessageActivity> scenario =
//                ActivityScenario.launch(InternetMessageActivity.class);




        onView(withId(R.id.textView_title))
                .check(matches(withText(
                        context.getResources().getString(R.string.MustHaveBluetooth))));


    }// end of checkTitle
    //==============================================================================================

    //==============================================================================================
    @Test
    public void checkMessage(){


//        ActivityScenario<InternetMessageActivity> scenario =
//                ActivityScenario.launch(InternetMessageActivity.class);




        onView(withId(R.id.textView_Message))
                .check(matches(withText(
                        context.getResources().getString(R.string.MustHaveBluetooth_Message))));


    }// end of checkTitle
    //==============================================================================================



}// end of BluetoothMessageActivityTests
