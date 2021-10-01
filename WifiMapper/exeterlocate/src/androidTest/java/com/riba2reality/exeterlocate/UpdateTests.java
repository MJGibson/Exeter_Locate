package com.riba2reality.exeterlocate;

import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.test.core.app.ActivityScenario;

import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager;
import com.google.android.play.core.install.model.AppUpdateType;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class UpdateTests  extends TestCase {


    FakeAppUpdateManager fakeAppUpdateManager;
    public static final int UPDATE_REQUEST_CODE = 111;

    //==============================================================================================
    @Before
    public void setup() {

        //fakeAppUpdateManager = new FakeAppUpdateManager();
        WifiManager wifi = (WifiManager) getInstrumentation().getTargetContext().getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled(true);
    }// end of setup
    //==============================================================================================


    //==============================================================================================
    @Test
    public void testImmediateUpdate_Completes(){


        fakeAppUpdateManager = new FakeAppUpdateManager(getInstrumentation().getTargetContext().getApplicationContext());

        // Setup flexible update.
        fakeAppUpdateManager.setUpdateAvailable(AppUpdateType.IMMEDIATE);

        fakeAppUpdateManager.isImmediateFlowVisible();

        ActivityScenario.launch(MainActivity.class);

//        fakeAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(MainActivity.updateListener());



        // Validate that flexible update is prompted to the user.
//        assertTrue(fakeAppUpdateManager.isConfirmationDialogVisible());












    }// end of testImmediateUpdate_Completes
    //==============================================================================================



}// end of UpdateTests
