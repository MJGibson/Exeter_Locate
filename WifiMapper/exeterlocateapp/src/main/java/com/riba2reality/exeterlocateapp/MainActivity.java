package com.riba2reality.exeterlocateapp;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;
    public static final int REQUEST_ENABLE_BT = 11;

    private Button startStopButton;
    private boolean running = false;

    private ConstraintLayout mainLayout;


    //##############################################################################################

    //==============================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStopButton = findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(startStopButtonPressed);

        mainLayout = findViewById(R.id.main_layout);


    }// end of onCreate
    //==============================================================================================


    //==============================================================================================
    View.OnClickListener startStopButtonPressed = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            if(!running){
                startStopButton.setText(R.string.start_button_stop_text);
                mainLayout.setBackgroundColor(getResources().getColor(R.color.green));
                running = true;
            }else{
                mainLayout.setBackgroundColor(getResources().getColor(R.color.red));
                startStopButton.setText(R.string.start_button_initial_text);
                running = false;
                return; // close any services up here...
            }



            if (ContextCompat.checkSelfPermission(
                    getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_LOCATION_PERMISSIONS
                );
            } else {
                //startLocationService();

            }


        }
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
                //startLocationService();
            } else {
                Toast.makeText(MainActivity.this,
                        "Permission denied!",
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
        if (!isLocationServiceRunning()) {


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
                    }else{
                        startService();
                    }

                }
            }

        }
    }// end of startLocationService
    //==============================================================================================

    //==============================================================================================
    private void startService(){

        Intent intent = new Intent(this.getApplicationContext(), TrackerScanner.class);
        intent.setAction(
                getResources().getString(R.string.action_start_location_service)
        );
        startService(intent);
        Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show();

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


}// end of MainActivity