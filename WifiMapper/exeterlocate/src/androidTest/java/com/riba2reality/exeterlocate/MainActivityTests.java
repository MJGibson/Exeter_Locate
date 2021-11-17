package com.riba2reality.exeterlocate;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.riba2reality.exeterlocatecore.TrackerScanner;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
//@RunWith(MockitoJUnitRunner.class)
@LargeTest
public class MainActivityTests extends TestCase {


//    @Rule
//    public ActivityScenarioRule<MainActivity> mActivityRule =
//            new ActivityScenarioRule(MainActivity.class);





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

    //##############################################################################################
    //###################################      TESTS       #########################################
    //##############################################################################################

    private static ViewAction swipeFromTopToBottom() {
        return new GeneralSwipeAction(Swipe.FAST, GeneralLocation.BOTTOM_CENTER,
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {
                        float[] coordinates =  GeneralLocation.CENTER.calculateCoordinates(view);
                        coordinates[1] = 0;
                        return coordinates;
                    }
                }, Press.FINGER);
    }

    //==============================================================================================
    @FlakyTest
    @Test
    public void test_agree_button_exits() {

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);


        // find web view and swipe down until Accept is presented
        for(int i = 0; i < 50; ++i) {
            //onView(isAssignableFrom(WebView.class)).perform(swipeUp());
            onView(withId(R.id.scroll)).perform(swipeUp());

//            onView(withId(R.id.scroll))
//                .perform(swipeFromTopToBottom());
        }
//        onView(withId(R.id.scroll))
//                .perform(swipeFromTopToBottom());
                //.perform(ViewActions.swipeUp());


//        // Initialize UiDevice instance
//        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
//
//        // Search for correct button in the dialog.
//        UiObject webView = uiDevice.findObject(new UiSelector().className(WebView.class));
//
//        try {
//            if (webView.exists() ) {
//
//                int height = webView.getBounds().height();
//
//                //Log.d("mg_test", "test_agree_button_exits: "+height);
//
//                for(int i = 0; i < 5; ++i)
//                    webView.swipeUp(5);
//            }else{
//                fail("Webview doesn't exist");
//            }
//
//        } catch (UiObjectNotFoundException e) {
//            e.printStackTrace();
//            fail();
//        }


        // find accept button
        onView(allOf(withId(R.id.accept_button),withText("Accept")))
                //.inRoot(isDialog()) // <---
                .check(matches(isDisplayed()));

//        onView(withText("Accept")).perform(click());

//        // Search for correct button in the dialog.
//        UiObject button = uiDevice.findObject(new UiSelector().text("ACCEPT"));
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

    }// end of test_agree_button_exits
    //==============================================================================================

    //==============================================================================================
    @FlakyTest
    @Test
    public void test_disagree_button_exits() {

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);


        //startActivity(getInstrumentation().getTargetContext(),new Intent(), null);


        onView(allOf(withId(R.id.disagree_button),withText("Disagree")))
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





//    //==============================================================================================
////    @Test
////    @FlakyTest
//    public void should_displayNoPermission_when_permissionAreDenied() {
//
//        //ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
//
//
//
//
//        // click start-stop button
//        onView(withId(R.id.circleIcon)).perform(click());
//        try {
//            sleep(400);
//
//            onView(withId(R.id.circleIcon)).check(matches(withText(R.string.start_button_stop_text)));
//
////        assertTrue("Failed to deny permissions",denyPermission());
//
//            assertTrue("Failed to Start Service",isLocationServiceRunning());
//
//        }
//        catch ( InterruptedException ex){
//
//        }
//
//
//        //onView(withId(R.id.startStopButton)).check(matches(withText(R.string.start_button_stop_text)));
//
////        assertTrue("Failed to deny permissions",denyPermission());
//
//        //assertTrue("Failed to Start Service",isLocationServiceRunning());
//
////        onView(withText(R.string.PermissionDeniedToastText))
////                .inRoot(withDecorView(not(mActivityRule.getActivity().getWindow().getDecorView())))
////                .check(matches(isDisplayed()));
//
//
//        //onView(withId(R.id.text)).check(matches(withText("Hello World!")));
//    }// end of shouldUpdateTextAfterButtonClick
//    //==============================================================================================


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

//    @Before
//    public void setUp() throws Exception {
//        super.setUp();
//        Intent mLaunchIntent = new Intent(getInstrumentation()
//                .getTargetContext(), MainActivity.class);
//        mLaunchIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
//        startActivity(getInstrumentation().getTargetContext(),mLaunchIntent,null );
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
