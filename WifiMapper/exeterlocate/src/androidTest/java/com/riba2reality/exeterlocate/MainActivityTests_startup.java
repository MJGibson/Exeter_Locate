package com.riba2reality.exeterlocate;

import android.app.Activity;
import android.webkit.WebView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
//@RunWith(MockitoJUnitRunner.class)
@LargeTest
public class MainActivityTests_startup   {


//    @Rule
//    public ActivityScenarioRule<MainActivity> mActivityRule =
//            new ActivityScenarioRule(MainActivity.class);


//    @Rule
//    public ActivityTestRule<MainActivity> activityRule =
//            new ActivityTestRule<>(MainActivity.class);



//    @Rule
//    public GrantPermissionRule mGrantPermissionRule =
//            GrantPermissionRule.grant(
//                    "android.permission.ACCESS_FINE_LOCATION");

    //==============================================================================================
    @Before
    public void setUp() {

    }
    //==============================================================================================


    //##############################################################################################
    //###################################      TESTS       #########################################
    //##############################################################################################

    //==============================================================================================
    @Test
    public void test_disagree_button_exits() {

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);


        //startActivity(getInstrumentation().getTargetContext(),new Intent(), null);


        onView(withText("Disagree"))
//                .inRoot(isDialog()) // <---
                .check(matches(isDisplayed()));
                //.perform(click());

        onView(withText("Disagree")).perform(click());

//        // Initialize UiDevice instance
//        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
//
//        // Search for correct button in the dialog.
//        UiObject button = uiDevice.findObject(new UiSelector().text("Disagree"));
//
//        try {
//            if (button.exists() && button.isEnabled()) {
//                button.click();
//            }else{
//                fail("Button doesn't exist");
//            }
//
//        } catch (UiObjectNotFoundException e) {
//            e.printStackTrace();
//            fail();
//        }


        //System.out.println(scenario.getResult().getResultCode());


        // note time out after 45 seconds
        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED);


    }// end of test_disagree_button_exits
    //==============================================================================================


    //==============================================================================================
    @Test
    public void test_agree_button_exits() {

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);



        for(int i = 0; i < 5; ++i)
            onView(isAssignableFrom(WebView.class)).perform(swipeUp());


        onView(withText("Accept"))
//                .inRoot(isDialog()) // <---
                .check(matches(isDisplayed()));

//        onView(withText("Accept")).perform(click());

    }// end of test_agree_button_exits
    //==============================================================================================




//    public Matcher<View> getConstraints() {
//        return allOf(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
//                isDescendantOfA(anyOf(
//                    isAssignableFrom(ScrollView.class),
//                    isAssignableFrom(HorizontalScrollView.class),
//                    isAssignableFrom(NestedScrollView.class)
//
//                ))
//        );
//    }



}// end of MainActivityTests
