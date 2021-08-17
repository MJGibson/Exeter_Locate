package com.riba2reality.exeterlocateapp;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.riba2reality.exeterlocatecore.TrackerScanner;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;
    public static final int REQUEST_ENABLE_BT = 11;

    private Button startStopButton;
    private boolean running = false;

    private boolean active = false;

    private ConstraintLayout mainLayout;

    private String _deviceID;


    //##############################################################################################

    //==============================================================================================
    @Override
    protected void onStart() {
        active = true;
        super.onStart();
        runThread();
        checkButtons();
        Log.d("mgdev", "MainActivity.onStart");
    }
    //==============================================================================================

    //==============================================================================================
    @Override
    protected void onStop() {
        active = false;
        super.onStop();
        Log.d("mgdev", "MainActivity.onStop");
    }
    //==============================================================================================

    //==============================================================================================
    private void checkButtons(){

        if( isLocationServiceRunning() ){
            startStopButton.setText(R.string.start_button_stop_text);
            //mainLayout.setBackgroundColor(getResources().getColor(R.color.green));
            running = true;
        }else{
            startStopButton.setText(R.string.start_button_initial_text);
            running = false;
        }


    }//end of checkButtons
    //==============================================================================================

    //==============================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStopButton = findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(startStopButtonPressed);

        mainLayout = findViewById(R.id.main_layout);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor SPeditor = SP.edit();
        String _deviceID = SP.getString("DeviceID", "");
        if(_deviceID.isEmpty()){
            _deviceID = UUID.randomUUID().toString();
            SPeditor.putString("DeviceID", _deviceID);
            SPeditor.apply();
        }

        Log.d("mgdev", "MainActivity.onCreate._deviceID="+_deviceID);

        // check if bluetooth is available and fetch it
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }else {

            // ble stuff
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }



    }// end of onCreate
    //==============================================================================================


    //==============================================================================================
    View.OnClickListener startStopButtonPressed = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            Log.d("mgdev", "MainActivity.onClick");

            if (!running) {
                startStopButton.setText(R.string.start_button_stop_text);
                //mainLayout.setBackgroundColor(getResources().getColor(R.color.green));
                running = true;

                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION_PERMISSIONS
                    );
                } else {

                    Log.d("mgdev", "MainActivity.onClick.permission.accepted");

                    startLocationService();

                }//end of if permission already accepted

            } else {
                //mainLayout.setBackgroundColor(getResources().getColor(R.color.red));
                startStopButton.setText(R.string.start_button_initial_text);
                running = false;

                stopLocationService();

                return; // close any services up here...
            }




        }// end of onClick
    };
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Toast.makeText(MainActivity.this,
                        getResources().getString(R.string.PermissionDeniedToastText),
                        Toast.LENGTH_SHORT).show();
            }
        }

    }// end of onRequestPermissionsResult
    //==============================================================================================

    //==============================================================================================
    private boolean isLocationServiceRunning() {

        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
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
    private void startLocationService() {

        Log.d("mgdev", "MainActivity.startLocationService");

        if (!isLocationServiceRunning()) {

            //Log.d("mgdev", "MainActivity.startLocationService.!isLocationServiceRunning()");

            if(bluetoothAdapter == null){
                // then no bluetooth capabilties
            }else{
                if(bluetoothLeScanner == null){
                    // probably no BLE
                }else{
                    // request bluetooth activation
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        return;
                    }
//                    else{
//                        startService();
//                    }

                }
            }

            startService();

        }
    }// end of startLocationService
    //==============================================================================================

    //==============================================================================================
    private void startService(){

        Log.d("mgdev", "MainActivity.startService");

        Intent intent = new Intent(this.getApplicationContext(), TrackerScanner.class);
        intent.setAction(
                getResources().getString(R.string.action_start_location_service)
        );
        intent.putExtra("MODE", true); // engage citizen mode

        String address = "3.9.100.243";

        String dataBase = "dev";

        String deviceID = _deviceID;

        boolean useSSL = true;

        intent.putExtra("ServerAddress", address);
        intent.putExtra("database", dataBase);
        intent.putExtra("DeviceID", deviceID);
        intent.putExtra("SSL_switch", useSSL);


        startService(intent);
        Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show();

        //runThread();

    }// end of startService
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode,  data);

        if( resultCode == RESULT_OK){
            startService();
            Log.d("mgdev", "HomescreenFragment.onActivityResult.RESULT_OK");
        }else{
            Log.d("mgdev", "HomescreenFragment.onActivityResult. ELSE");
        }

    }
    //==============================================================================================


    //==============================================================================================
    private void stopLocationService() {


        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), TrackerScanner.class);
            intent.setAction(
                    getResources().getString(R.string.action_stop_location_service)
            );
            startService(intent);
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }// end of startLocationService
    //==============================================================================================


    //==============================================================================================
    private void runThread() {

        new Thread() {
            public void run() {
                while (active) {
                    try {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                //btn.setText("#" + i);

                                if(isLocationServiceRunning()){
                                    mainLayout.setBackgroundColor(getResources().getColor(R.color.green));
                                }else{
                                    mainLayout.setBackgroundColor(getResources().getColor(R.color.red));
                                }




                            }// end of run function
                        });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    //==============================================================================================


}// end of MainActivity