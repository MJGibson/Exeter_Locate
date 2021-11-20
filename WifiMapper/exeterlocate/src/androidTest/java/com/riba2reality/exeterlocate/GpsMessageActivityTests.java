package com.riba2reality.exeterlocate;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.riba2reality.exeterlocate.messages.GpsMessageActivity;

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
public class GpsMessageActivityTests extends TestCase {

//    @Rule
//    public ActivityScenarioRule<GpsMessageActivity> activityRule
//            = new ActivityScenarioRule<>(GpsMessageActivity.class);

    Context context;


    private void turnGPSOff(){
//        LocationManager LocationManager = (LocationManager) InstrumentationRegistry.getInstrumentation().getContext().getSystemService(LOCATION_SERVICE);
//
//        try {
//            Log.d("mgtest" ,"Removing Test providers");
//
//            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//            locationManager.addTestProvider("Test", false, false,
//                    false, false, false, false,
//                    false, Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
//            //locationManager.setTestProviderEnabled("Test", true);
//
//            // Set up your test
//
//            Location location = new Location("Test");
//            location.setLatitude(10.0);
//            location.setLongitude(20.0);
//            locationManager.setTestProviderLocation("Test", location);
//
//            locationManager.setTestProviderEnabled("Test", false);
//
////            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
////
////            locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER,false);
//
//        } catch (IllegalArgumentException error) {
//            Log.d("mgtest","Got exception in removing test  provider");
//        }
//
//
//
//
//        // ensure this test provider is used by the class, must be static as we don't have access
//        // to the instance within instrument tests
//        GpsMessageActivity.provider = "Test";

        GpsMessageActivity._test = true;

    }// end of turnGPSOff
    //==============================================================================================



    //==============================================================================================
    @Before
    public void init() {

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // turn off GPS, as otherwise it'll de-activate this activity.
//        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
//        intent.putExtra("disable", false);
//        context.sendBroadcast(intent);
        turnGPSOff();


    }// end of init
    //==============================================================================================


    //==============================================================================================
    @Test
    public void checkTitle(){


        ActivityScenario<GpsMessageActivity> scenario = ActivityScenario.launch(GpsMessageActivity.class);




        onView(withId(R.id.textView_title))
                .check(matches(withText(
                        context.getResources().getString(R.string.MustHaveGPS))));


    }// end of checkTitle
    //==============================================================================================

    //==============================================================================================
    @Test
    public void checkMessage(){


        ActivityScenario<GpsMessageActivity> scenario = ActivityScenario.launch(GpsMessageActivity.class);




        onView(withId(R.id.textView_Message))
                .check(matches(withText(
                        context.getResources().getString(R.string.MustHaveGPS_message))));


    }// end of checkTitle
    //==============================================================================================



}// end of GpsMessageActivityTests
