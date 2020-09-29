package com.riba2reality.wifimapper.ui.main;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.riba2reality.wifimapper.Constants;
import com.riba2reality.wifimapper.MainActivity;
import com.riba2reality.wifimapper.R;
import com.riba2reality.wifimapper.TrackerScanner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;

/**
 * A placeholder fragment containing a simple view.
 */
public class FirstTabFragment extends Fragment implements OnMapReadyCallback, LocationListener {

    private static final String ARG_SECTION_NUMBER = "section_number";

    //private PageViewModel pageViewModel;

    static final LatLng HAMBURG = new LatLng(53.558, 9.927);

    float updateRange = 1.0f;
    long updateTimeMilliSecs = 1000;

    boolean startTracking = false;

    public double lat;
    public double lon;
    public String time;


    final static int PERMISSION_ALL = 1;
    final static String[] PERMISSIONS = {
            //Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private GoogleMap map;
    MarkerOptions mo;
    Marker marker;
    LocationManager locationManager;

    public static FirstTabFragment newInstance(int index) {
        FirstTabFragment fragment = new FirstTabFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
//        int index = 1;
//        if (getArguments() != null) {
//            index = getArguments().getInt(ARG_SECTION_NUMBER);
//        }
//        pageViewModel.setIndex(index);

//        // when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
//                .findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);


        mo = new MarkerOptions().position(new LatLng(0, 0)).title("My current location");

        /*

        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);




        Criteria criteria = new Criteria();
        criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(true);
        criteria.setSpeedRequired(true);
        String provider = locationManager.getBestProvider(criteria, true);




        if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        //this.onLocationChanged(locationManager.getLastKnownLocation(provider));

        if (Build.VERSION.SDK_INT >= 23 && !isPermissionGranted()) {
            requestPermissions(PERMISSIONS, PERMISSION_ALL);
        }
        //else requestlocation();
        //if (!isLocationEnabled())
        //    showAlert(1);

*/


    }// end of onCreate method

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_ALL && grantResults.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(startTracking) {
                    startLocationService();
                    startTracking = false;
                }
            }else{
                Toast.makeText(this.getActivity(), "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }

    }// end of onRequestPermissionsResult

    private boolean isLocationServiceRunning(){

        ActivityManager activityManager =
                (ActivityManager) getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if(activityManager != null){

            for(ActivityManager.RunningServiceInfo service :
                    activityManager.getRunningServices(Integer.MAX_VALUE)){

                if(TrackerScanner.class.getName().equals(service.service.getClassName())){
                    if(service.foreground){
                        return true;
                    }
                }

            }// end of looping
            return false;
        }// end of if activityManger not null
        return false;
    }// end of isLocationServiceRunning


    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            getActivity().getApplicationContext().startService(intent);
            Toast.makeText(this.getActivity(),"Location service started", Toast.LENGTH_SHORT).show();
        }
    }// end of startLocationService


    private void stopLocationService(){
        if(isLocationServiceRunning()){
            Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            //stopService(intent);//?!
            getActivity().getApplicationContext().startService(intent);
            Toast.makeText(this.getActivity(),"Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }// end of startLocationService

    /////private Intent _trackScannerIntent = null;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);

        //-----------
        /////_trackScannerIntent = root.getApplicationContext()



        //-----------

/*
        root.findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public  void onClick(View v){

                if(ContextCompat.checkSelfPermission(
                        getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
                )!= PackageManager.PERMISSION_GRANTED){

                    startTracking = true;

                    ActivityCompat.requestPermissions(
                            getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSION_ALL
                    );
                }else{
                    startLocationService();

                }


            }

        });

        root.findViewById(R.id.stop_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                stopLocationService();
            }
        });



 */
        //final TextView textView = root.findViewById(R.id.section_label);
//        pageViewModel.getText().observe(this, new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });
        return root;

    }// en dof onCreateView


    @Override
    public void onMapReady(GoogleMap googleMap) {
        //LatLng sydney = new LatLng(-33.852, 151.211);
        //googleMap.addMarker(new MarkerOptions()
        //        .position(sydney)
        //        .title("Marker in Sydney"));

        map = googleMap;
        marker = map.addMarker(mo);

    }// end of onMapReady function

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);

        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);




    }


    @Override
    public void onLocationChanged(@NonNull Location location) {

        if(location==null)
            return;
        LatLng myCords = new LatLng(location.getLatitude(), location.getLongitude());


        if (map!= null && marker != null) {
            marker.setPosition(myCords);
            map.moveCamera(CameraUpdateFactory.newLatLng(myCords));

            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String message = "Time:" + currentTime + "\nLat:" + location.getLatitude() + "\nLong:" + location.getLongitude();

            Toast myToast = Toast.makeText(getActivity().getBaseContext(), message, Toast.LENGTH_SHORT);
            myToast.show();

            this.lat = location.getLatitude();
            this.lon = location.getLongitude();
            this.time = currentTime;

        }



    }// end of onLocationChanged

    private void requestlocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        String provider = locationManager.getBestProvider(criteria, true);

        locationManager.requestLocationUpdates(provider,
                updateTimeMilliSecs,
                updateRange,
                this);



    }//end of requestlocation
/*
    private boolean isLocationEnabled(){
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ;
        //|| locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }//end of isLocationEnabled

 */

    private boolean isPermissionGranted(){
        if(//checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            //     PackageManager.PERMISSION_GRANTED ||
                getActivity().
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
        ){
            Log.v("myLog","Permission is granted");
            return true;
        }else{
            Log.v("myLog","Permission NOT granted");
            return false;
        }
    }// end of isPermissionGranted

    private void showAlert(final int status){
        String message, title, btnText;
        if(status == 1){
            message = "You Location Settings is set to 'OFF'.\nPlease Enable Location to " +
                    "use this app";
            title = "Enable Location";
            btnText = "Location Settings";
        }else{
            message = "Please allow this app to access location!";
            title = "permission access";
            btnText = "Grant";
        }
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this.getActivity());
        dialog.setCancelable(false);
        dialog.setTitle(title)
                .setMessage(message)
                .setPositiveButton(btnText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        if (status == 1) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(myIntent);
                        } else
                            requestPermissions(PERMISSIONS, PERMISSION_ALL);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        getActivity().
                        finish();
                    }
                });
        dialog.show();

    }// end of showAlert

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }



}// end of class