package com.riba2reality.exeterlocate;

import android.app.Activity;
import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.web.webdriver.Locator;
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
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static com.google.common.truth.Truth.assertThat;
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



//        ActivityScenario<InfoActivity> scenario =
//                ActivityScenario.launch(InfoActivity.class);


    }// end of init
    //==============================================================================================


    //==============================================================================================
    @Test
    public void check_versions(){


        ActivityScenario<InfoActivity> scenario =
                ActivityScenario.launch(InfoActivity.class);




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

        ActivityScenario<InfoActivity> scenario =
                ActivityScenario.launch(InfoActivity.class);

        onWebView()
                // Similar to check(matches(...))
                .check(webMatches(getCurrentUrl(),
                        containsString("ExeterLocateInformationpage.html")));





        onWebView()
                .withElement(findElement(Locator.XPATH, "//a[contains(text(),'What data is collected')]"))
                //.withElement(findElement(Locator.XPATH, "a[href='.#h.269txh13f89t']"))
                //.withElement(findElement(Locator.ID, ".#h.269txh13f89t"))

                .perform(webClick());

    }// end of checkTitle
    //==============================================================================================


    //==============================================================================================
    @Test
    public void check_backButton(){


        ActivityScenario<InfoActivity> scenario =
                ActivityScenario.launch(InfoActivity.class);


        onWebView()
                .withElement(findElement(Locator.XPATH, "//a[contains(text(),'What data is collected')]"))
                //.withElement(findElement(Locator.XPATH, "a[href='.#h.269txh13f89t']"))
                //.withElement(findElement(Locator.ID, ".#h.269txh13f89t"))

                .perform(webClick());


        //Espresso.pressBack();
        scenario.onActivity( activity -> {
            // use 'activity'.
            activity.onBackPressed();
        });

        // having as we've tried to click, the first back press shouldn't close it...

//        onWebView()
//                // Similar to check(matches(...))
//                .check(webMatches(getCurrentUrl(),
//                        containsString("ExeterLocateInformationpage.html")));
//
//        // second one should close it
//        // causes androidx.test.espresso.NoActivityResumedException: Pressed back and killed the app
//        //Espresso.pressBack();
//        scenario.onActivity( activity -> {
//            // use 'activity'.
//            activity.onBackPressed();
//        });



        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED);



    }// end of checkTitle
    //==============================================================================================


}// end of InfoActivityTests
