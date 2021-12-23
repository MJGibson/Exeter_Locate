package com.riba2reality.exeterlocate;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.riba2reality.exeterlocate.messages.WifiMessageActivity;

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
public class WifiMessageActivityTests extends TestCase {

//    @Rule
//    public ActivityScenarioRule<InternetMessageActivity> activityRule
//            = new ActivityScenarioRule<>(InternetMessageActivity.class);

    Context context;


    //==============================================================================================
    private void turnWifiOff(){

        WifiMessageActivity._test = true;
//        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
//        wifiManager.setWifiEnabled(false);


    }// end of turnWifiOff
    //==============================================================================================



    //==============================================================================================
    @Before
    public void init() {

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // turn off Wi-Fi, as otherwise it'll de-activate this activity.
        turnWifiOff();

        ActivityScenario<WifiMessageActivity> scenario =
                ActivityScenario.launch(WifiMessageActivity.class);


    }// end of init
    //==============================================================================================


    //==============================================================================================
    @Test
    public void checkTitle(){


//        ActivityScenario<InternetMessageActivity> scenario =
//                ActivityScenario.launch(InternetMessageActivity.class);




        onView(withId(R.id.textView_title))
                .check(matches(withText(
                        context.getResources().getString(R.string.MustHaveWifi))));


    }// end of checkTitle
    //==============================================================================================

    //==============================================================================================
    @Test
    public void checkMessage(){


//        ActivityScenario<InternetMessageActivity> scenario =
//                ActivityScenario.launch(InternetMessageActivity.class);




        onView(withId(R.id.textView_Message))
                .check(matches(withText(
                        context.getResources().getString(R.string.MustHaveWifi_Message))));


    }// end of checkTitle
    //==============================================================================================



}// end of WifiMessageActivityTests
