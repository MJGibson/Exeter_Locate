package com.riba2reality.exeterlocate;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.riba2reality.exeterlocate.messages.InternetMessageActivity;

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
public class InternetMessageActivityTests extends TestCase {

//    @Rule
//    public ActivityScenarioRule<InternetMessageActivity> activityRule
//            = new ActivityScenarioRule<>(InternetMessageActivity.class);

    Context context;


    //==============================================================================================
    private void turnInternetOff(){
        //GpsMessageActivity._test = true;

        InternetMessageActivity._test = true;


    }// end of turnBluetoothOff
    //==============================================================================================



    //==============================================================================================
    @Before
    public void init() {

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // turn off Internet, as otherwise it'll de-activate this activity.

        turnInternetOff();

        ActivityScenario<InternetMessageActivity> scenario =
                ActivityScenario.launch(InternetMessageActivity.class);


    }// end of init
    //==============================================================================================


    //==============================================================================================
    @Test
    public void checkTitle(){


//        ActivityScenario<InternetMessageActivity> scenario =
//                ActivityScenario.launch(InternetMessageActivity.class);




        onView(withId(R.id.textView_title))
                .check(matches(withText(
                        context.getResources().getString(R.string.MustHaveInternet))));


    }// end of checkTitle
    //==============================================================================================

    //==============================================================================================
    @Test
    public void checkMessage(){


//        ActivityScenario<InternetMessageActivity> scenario =
//                ActivityScenario.launch(InternetMessageActivity.class);




        onView(withId(R.id.textView_Message))
                .check(matches(withText(
                        context.getResources().getString(R.string.MustHaveInternet_Message))));


    }// end of checkTitle
    //==============================================================================================



}// end of InternetMessageActivityTests
