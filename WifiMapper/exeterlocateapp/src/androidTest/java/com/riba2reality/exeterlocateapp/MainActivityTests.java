package com.riba2reality.exeterlocateapp;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.riba2reality.exeterlocatecore.TrackerScanner;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static androidx.core.content.ContextCompat.startActivity;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static java.lang.Thread.sleep;

@RunWith(AndroidJUnit4.class)
//@RunWith(MockitoJUnitRunner.class)
@LargeTest
public class MainActivityTests extends TestCase {


//    @Rule
//    public ActivityScenarioRule<MainActivity> mActivityRule = new ActivityScenarioRule(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION");

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
    private boolean isLocationServiceRunning() {

        ActivityManager activityManager =
                (ActivityManager) getInstrumentation().getTargetContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {

            for (ActivityManager.RunningServiceInfo service :
                    activityManager.getRunningServices(Integer.MAX_VALUE)) {

                if (TrackerScanner.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }

            }// end of looping
            return false;
        }// end of if activityManger not null
        return false;
    }// end of isLocationServiceRunning
    //==============================================================================================



    //==============================================================================================
    @Test
    @FlakyTest
    public void should_displayNoPermission_when_permissionAreDenied() {

        //ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);




        // click start-stop button
        onView(withId(R.id.startStopButton)).perform(click());
        try {
            sleep(400);

            onView(withId(R.id.startStopButton)).check(matches(withText(R.string.start_button_stop_text)));

//        assertTrue("Failed to deny permissions",denyPermission());

            assertTrue("Failed to Start Service",isLocationServiceRunning());

        }
        catch ( InterruptedException ex){

        }


        //onView(withId(R.id.startStopButton)).check(matches(withText(R.string.start_button_stop_text)));

//        assertTrue("Failed to deny permissions",denyPermission());

        //assertTrue("Failed to Start Service",isLocationServiceRunning());

//        onView(withText(R.string.PermissionDeniedToastText))
//                .inRoot(withDecorView(not(mActivityRule.getActivity().getWindow().getDecorView())))
//                .check(matches(isDisplayed()));


        //onView(withId(R.id.text)).check(matches(withText("Hello World!")));
    }// end of shouldUpdateTextAfterButtonClick
    //==============================================================================================


//    public void testStartServiceOnInit () {
//        final AtomicBoolean serviceStarted = new AtomicBoolean(false);
//
//
//
//        setActivityContext(new ContextWrapper(getInstrumentation().getTargetContext()) {
//            @Override
//            public ComponentName startService(Intent service) {
//                Log.v("mockcontext", "Start service: " + service.toUri(0));
//                if (service.getComponent().getClassName().equals ("net.meridiandigital.tasks.TasksService"))
//                    serviceStarted.set(true);
//                return service.getComponent();
//            }
//        });
//        startActivity(getInstrumentation().getTargetContext(),new Intent(), null);
//        assertTrue ("Service should have been started", serviceStarted.get());
//    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Intent mLaunchIntent = new Intent(getInstrumentation()
                .getTargetContext(), MainActivity.class);
        mLaunchIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(getInstrumentation().getTargetContext(),mLaunchIntent,null );
    }


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
