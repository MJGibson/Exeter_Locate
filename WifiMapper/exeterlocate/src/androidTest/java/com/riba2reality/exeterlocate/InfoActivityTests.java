package com.riba2reality.exeterlocate;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.riba2reality.exeterlocatecore.TrackerScanner;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.model.Atoms.getCurrentUrl;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static org.hamcrest.Matchers.containsString;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class InfoActivityTests extends TestCase {

//    @Rule
//    public ActivityScenarioRule<BluetoothMessageActivity> activityRule
//            = new ActivityScenarioRule<>(BluetoothMessageActivity.class);

    Context context;






    //==============================================================================================
    @Before
    public void init() {

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();



        ActivityScenario<InfoActivity> scenario =
                ActivityScenario.launch(InfoActivity.class);


    }// end of init
    //==============================================================================================


    //==============================================================================================
    @Test
    public void check_versions(){


//        ActivityScenario<InternetMessageActivity> scenario =
//                ActivityScenario.launch(InternetMessageActivity.class);




        onView(withId(R.id.textViewVersion))
                .check(matches(withText("Version: " + BuildConfig.VERSION_NAME
                                + "("+Integer.toString(BuildConfig.VERSION_CODE)+")    "
                                //+ "Core Version: " + versionName
                                + "Core Version: " + TrackerScanner.libraryVersion
                )));


    }// end of checkTitle
    //==============================================================================================

    //==============================================================================================
    @Test
    public void check_webView(){

        onWebView()
                // Similar to check(matches(...))
                .check(webMatches(getCurrentUrl(),
                        containsString("ExeterLocateInformationpage.html")));


    }// end of checkTitle
    //==============================================================================================



}// end of InfoActivityTests
