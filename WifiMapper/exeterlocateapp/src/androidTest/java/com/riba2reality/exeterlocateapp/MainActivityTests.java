package com.riba2reality.exeterlocateapp;

import android.app.Instrumentation;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
//@RunWith(MockitoJUnitRunner.class)
@LargeTest
public class MainActivityTests {


    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule(MainActivity.class);

//    @Rule
//    public GrantPermissionRule mGrantPermissionRule =
//            GrantPermissionRule.grant(
//                    "android.permission.ACCESS_FINE_LOCATION");

    //==============================================================================================
    private boolean grantPermission() {
        Instrumentation instrumentation = getInstrumentation();
        if (Build.VERSION.SDK_INT >= 23) {
            UiObject allowPermission = UiDevice.getInstance(instrumentation).findObject((
                    new UiSelector()).text(Build.VERSION.SDK_INT == 23 ? "Allow"
                    :(Build.VERSION.SDK_INT <= 28 ? "ALLOW"
                    : (Build.VERSION.SDK_INT == 29 ? "Allow only while using the app"
                    : "While using the app"))));
            if (allowPermission.exists()) {
                try{
                    allowPermission.click();
                }
                catch (UiObjectNotFoundException ex){
                    return false;
                }
            }else{
                return false;
            }
        }
        return true;

    }// end of grantPermission
    //==============================================================================================

    //==============================================================================================
    private boolean denyPermission() {
        Instrumentation instrumentation = getInstrumentation();
        if (Build.VERSION.SDK_INT >= 23) {
            UiObject denyPermission = UiDevice.getInstance(instrumentation).findObject((
                    new UiSelector()).text(Build.VERSION.SDK_INT < 28 ? "DENY"
                    : "Deny"));
            if (denyPermission.exists()) {
                try {
                    denyPermission.click();
                }
                catch (UiObjectNotFoundException ex){
                    return false;
                }
            }else{
                return false;
            }
        }else{ // no permission to deny?
            return false;
        }
        return true;

    }// end of grantPermission
    //==============================================================================================






    //==============================================================================================
    @Test
    public void should_displayNoPermission_when_permissionAreDenied() {

        //ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);


        // click start-stop button
        onView(withId(R.id.startStopButton)).perform(click());

        assertTrue("Failed to deny permissions",denyPermission());

//        onView(withText(R.string.PermissionDeniedToastText))
//                .inRoot(withDecorView(not(mActivityRule.getActivity().getWindow().getDecorView())))
//                .check(matches(isDisplayed()));


        //onView(withId(R.id.text)).check(matches(withText("Hello World!")));
    }// end of shouldUpdateTextAfterButtonClick
    //==============================================================================================

//    @Before
//    public void setUp() throws Exception {
//        super.setUp();
//        Intent mLaunchIntent = new Intent(getInstrumentation()
//                .getTargetContext(), MainActivity.class);
//        startActivity(getInstrumentation().getTargetContext(),mLaunchIntent, FLAG_ACTIVITY_NEW_TASK);
//    }


//    @After
//    public void tearDown() {
//        Instrumentation var10000 = InstrumentationRegistry.getInstrumentation();
//        Intrinsics.checkNotNullExpressionValue(var10000, "InstrumentationRegistry.getInstrumentation()");
//        UiAutomation var1 = var10000.getUiAutomation();
//        StringBuilder var10001 = (new StringBuilder()).append("pm revoke ");
//        Instrumentation var10002 = InstrumentationRegistry.getInstrumentation();
//        Intrinsics.checkNotNullExpressionValue(var10002, "InstrumentationRegistry.getInstrumentation()");
//        Context var2 = var10002.getTargetContext();
//        Intrinsics.checkNotNullExpressionValue(var2, "InstrumentationRegistry.…mentation().targetContext");
//        var1.executeShellCommand(var10001.append(var2.getPackageName()).append(" android.permission.ACCESS_COARSE_LOCATION").toString());
//        var10000 = InstrumentationRegistry.getInstrumentation();
//        Intrinsics.checkNotNullExpressionValue(var10000, "InstrumentationRegistry.getInstrumentation()");
//        var1 = var10000.getUiAutomation();
//        var10001 = (new StringBuilder()).append("pm revoke ");
//        var10002 = InstrumentationRegistry.getInstrumentation();
//        Intrinsics.checkNotNullExpressionValue(var10002, "InstrumentationRegistry.getInstrumentation()");
//        var2 = var10002.getTargetContext();
//        Intrinsics.checkNotNullExpressionValue(var2, "InstrumentationRegistry.…mentation().targetContext");
//        var1.executeShellCommand(var10001.append(var2.getPackageName()).append(" android.permission.ACCESS_FINE_LOCATION").toString());
//    }



}// end of MainActivityTests
