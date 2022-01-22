package com.riba2reality.wifimapper.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;
import com.riba2reality.exeterlocatecore.DataStores.Constants;
import com.riba2reality.exeterlocatecore.TrackerScanner;
import com.riba2reality.wifimapper.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.Map;

/**
 * Manual Scan Fragment {@link Fragment} subclass.
 * Use the {@link ManualScanFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 *
 *
 */
public class ManualScanFragment extends Fragment {


    private int selectedLocation = -1;

    boolean scanCompleted = true;

    private MapView map;
    private IMapController mapController;


    //==============================================================================================
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        if (map != null)
            map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }// end of onResume
    //==============================================================================================

    //==============================================================================================
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        if (map != null)
            map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }// end of onPause
    //==============================================================================================

    //==============================================================================================
//    @Override
//    public void onStart() {
//        super.onStart();
//
//        Log.d("Trace", "ManualScanFragment.onStart()");
//
//    }// end of onStart
    //==============================================================================================

    //==============================================================================================
//    @Override
//    public void onStop() {
//        super.onStop();
//    }// end of onStop
    //==============================================================================================

    //==============================================================================================
    /**
     * No Arguement contrustor
     */
    public ManualScanFragment() {
        // Required empty public constructor

    }// enf of main constructor
    //==============================================================================================


    //==============================================================================================
    /**
     * Factory method which return a new instance of this Manual Scan Fragment
     * @param index
     * @return
     */
    public static ManualScanFragment newInstance(int index) {
        ManualScanFragment fragment = new ManualScanFragment();
        return fragment;
    }
    //==============================================================================================

    //==============================================================================================
    /**
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }// end of onCreate
    //==============================================================================================

    //==============================================================================================
    /**
     * onCreateView sets up the fragment
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d("Trace", "ManualScanFragment.onCreateView()");


        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without
        // permissions
        //if no tiles are displayed, you can try overriding the cache path using
        // Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name,
        // abusing osm's tile servers will get you banned based on this string

        //inflate and create the map



        // Inflate the layout for this fragment
        View rootView =
                inflater.inflate(R.layout.fragment_manual_scan, container, false);

        Spinner dropdown = rootView.findViewById(R.id.spinner1);

        // Create an ArrayAdapter using the string array and a default spinner
        ArrayAdapter<CharSequence> staticAdapter = ArrayAdapter
                .createFromResource(this.getActivity(), R.array.locations2,
                        android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        staticAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        dropdown.setAdapter(staticAdapter);

        dropdown.setOnItemSelectedListener(dropDownListerner);

        //-------------------------

        Button scanButton = rootView.findViewById(R.id.manualScanButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manualScan();
            }
        });

        scanButton.setEnabled(scanCompleted);

        //-------------------------



        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver((receiver),
                new IntentFilter(TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_RESULT)
        );

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver((updateReceiver),
                new IntentFilter(TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_TIMER_UPDATE)
        );

        //-------------------------

        //set up map

        map = rootView.findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(16);


        //Constants.stethamCampusCenterPoint.latitude
        //GeoPoint startPoint = new GeoPoint(51496994, -134733);

        float lat   = (float)Constants.stethamCampusCenterPoint.latitude;   //in DecimalDegrees
        float lng   = (float)Constants.stethamCampusCenterPoint.longitude;   //in DecimalDegrees
        GeoPoint startPoint = new GeoPoint((int)(lat * 1E6), (int)(lng * 1E6));

        mapController.setCenter(startPoint);


        setup_map_points();

        return rootView;
    }// end of on create view
    //==============================================================================================



    //==============================================================================================
    private void setup_map_points(){


        Map<String, LatLng> tempMap = Constants.Fiducal_locations;

        ArrayList<OverlayItem> overlayItemArrayList = new ArrayList<>();

        for (String key : tempMap.keySet()) {
            //

            Log.d("Trace", "ManualScanFragment.setup_map_points() "+key);

            LatLng point = tempMap.get(key);

            //Log.d("Trace", "ManualScanFragment.setup_map_points() "+point.latitude+", "+point.longitude);

            float lat   = (float)point.latitude;   //in DecimalDegrees
            float lng   = (float)point.longitude;

            GeoPoint geoPoint = new GeoPoint((int)(lat * 1E6), (int)(lng * 1E6));

            OverlayItem overlayItem = new OverlayItem("Location:"+key, "California", geoPoint);
            Drawable markerDrawable =
                    getContext().getDrawable(R.drawable.ic_menu_mylocation);


            overlayItem.setMarker(markerDrawable);


            overlayItemArrayList.add(overlayItem);



        }// end of looping fiducial locations


        ItemizedOverlay<OverlayItem> locationOverlay =
                new ItemizedIconOverlay<OverlayItem>(
                        overlayItemArrayList,
                        new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>()
                        {
            @Override
            public boolean onItemSingleTapUp(int i, OverlayItem overlayItem) {

                Drawable markerDrawable =
                        getContext().getDrawable(R.drawable.ic_menu_mylocation);

                Drawable selectedMarkerDrawable =
                        getContext().getDrawable(R.drawable.green_location_foreground);
                selectedMarkerDrawable.setBounds(0, 0, 10, 10);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    selectedMarkerDrawable.setColorFilter(
//                            getContext().getColor(R.color.green), PorterDuff.Mode.MULTIPLY);
//                }

                //for(OverlayItem item: overlayItemArrayList){
//                OverlayItem item;
//                for(int index = 0; index < overlayItemArrayList.size(); ++index){
//                    item = overlayItemArrayList.get(index);
//                    if(index == i) {
//                        item.setMarker(selectedMarkerDrawable);
//                    }else {
//                        item.setMarker(markerDrawable);
//                    }
//                }

                for(OverlayItem item: overlayItemArrayList){
                    item.setMarker(markerDrawable);
                }

                Toast.makeText(getActivity(), "Item's Title : "+overlayItem.getTitle() +"\nItem's Desc : "+overlayItem.getSnippet(), Toast.LENGTH_SHORT).show();

                overlayItem.setMarker(selectedMarkerDrawable);

                map.invalidate();

                return true; // Handled this event.
            }

            @Override
            public boolean onItemLongPress(int i, OverlayItem overlayItem) {
                return false;
            }
        }, getActivity().getApplicationContext());

        map.getOverlays().add(locationOverlay);
        //mMapView.getOverlays().add(locationOverlay);




    }// end of setup_map_points
    //==============================================================================================


    //==============================================================================================
    /**
     * Request and updated from the Backend
     */
    private void requestUpdate() {
        Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);

        intent.setAction(Constants.ACTION_REQUEST_UPDATE);

        getActivity().startService(intent);
    }
    //==============================================================================================



    //==============================================================================================
    /**
     * Drop down listener which changes the images depending on the drop downs selection
     */
    private AdapterView.OnItemSelectedListener dropDownListerner = new AdapterView.OnItemSelectedListener() {


        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            selectedLocation = position;

            //ImageView mImageView= (ImageView) getActivity().findViewById(R.id.imageView);


            String imageName = getCharForNumber(position);

            if(imageName==null){
                return;//bail
            }

            Log.d("locImage: ", imageName);

            int image_id = getResources().getIdentifier("fig_" + imageName, "drawable", getActivity().getPackageName());




            //mImageView.setImageResource(image_id);

            //-------------------------------------------------

            Button scanButton = getActivity().findViewById(R.id.manualScanButton);
            scanButton.setText(getString(R.string.perform_scan) + ": "+imageName);

            //-------------------------------------------------

            ImageView mImageViewPic= (ImageView) getActivity().findViewById(R.id.imageViewPic);

            //String pictureName = imageName + "_pic";
            String pictureName = imageName ;

            int picture_id = getResources().getIdentifier(pictureName, "drawable", getActivity().getPackageName());

            mImageViewPic.setImageResource(picture_id);




            //-------------------------------------------------

        }// end of onItenSelected

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

    };// end of dropDownListerner
    //==============================================================================================

    //==============================================================================================
    /**
     * Returns the lower case letter of alphabet at the given index(@param i)
     * @param i Index of the alphabet
     * @return return the letter of the alphabet at the given index
     */
    private String getCharForNumber(int i) {
        String returnVal = i >= 0 && i < 27 ? String.valueOf((char)(i + 97)) : null;
//        if(returnVal.compareTo("n")==0){
//            returnVal="o"; // there is no "n"
//        }
        return returnVal;
    }
    //==============================================================================================


    //==============================================================================================
    /**
     * Initiates a manual scan with the currently selected location
     */
    private void manualScan(){

        Log.d("Trace", "ManualScan()");

        scanCompleted = false;

        getActivity().findViewById(R.id.manualScanButton).setEnabled(false);
        //getActivity().findViewById(R.id.manualPostButton).setEnabled(false);

        String imageName = getCharForNumber(selectedLocation);


        //----------------------------------------------------------

        String[] server_values = getResources().getStringArray(R.array.server_values);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String serverAddress = SP.getString("ServerAddress", server_values[1]);

        //System.out.println("ServerAddress: "+serverAddress);

        if (serverAddress.isEmpty() || serverAddress.equals(server_values[0])) {
            Toast.makeText(getActivity(), "Please set Server Address", Toast.LENGTH_SHORT).show();
            return;
        }
        //----------------------------------------------------------

        Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);

        intent.setAction(Constants.ACTION_SINGLE_SCAN);

        intent.putExtra("message", imageName);

        //----------------------------------------------------------


        int manualScanDuration = getResources().getInteger(R.integer.defaultVal_manual_scan);
        manualScanDuration = SP.getInt("duration_manual_scan", manualScanDuration);

        intent.putExtra("duration", manualScanDuration);


        //----------------------------------------------------------


        intent.putExtra("MODE", false); // disengage citizen mode, thus dev mode

        //String[] server_values = getResources().getStringArray(com.riba2reality.exeterlocatecore.R.array.server_values);
        //SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String address = SP.getString("ServerAddress", server_values[1]);

        String dataBase = SP.getString("database", "alpha");

        String deviceID = SP.getString("DeviceID", "");

        boolean useSSL = SP.getBoolean("SSL_switch", true);

        String postType = SP.getString("post_type", "POST");

        // get the packagename of the main activity, note this is a fragment
        String packageName =  getActivity().getClass().getName();

        intent.putExtra("ServerAddress", address);
        intent.putExtra("database", dataBase);
        intent.putExtra("DeviceID", deviceID);
        intent.putExtra("SSL_switch", useSSL);

        intent.putExtra("PACKAGE", packageName);


        intent.putExtra("post_type", postType);



        //----------------------------------------------------------


        getActivity().startService(intent);



    }// end of manual scan
    //==============================================================================================

    //==============================================================================================
    /**
     *  Post the manual scans to the server
     */
    private void postManualScans(){

        Log.d("Trace", "postManualScans()");



        Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);

        intent.setAction(Constants.ACTION_POST_ALL);

        //intent.putExtra("message", imageName);

        getActivity().startService(intent);



    }// end of postManualScans
    //==============================================================================================

    //==============================================================================================
    /**
     * BroadcastReceiver for when manual scan is completed
     */
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("Trace", "ManualScanFragment.receiver.onReceive");

            scanCompleted = true;


            Button scanButton = getActivity().findViewById(R.id.manualScanButton);

            if(scanButton!=null){
                scanButton.setEnabled(true);
            }



        }// end of onRecieve
    };// end of new BroadcastReceiver
    //==============================================================================================

    //==============================================================================================
    /**
     * BroadcastReceiver for when the backend sends updates about the manual scan
     */
    BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("Trace", "ManualScanFragment.updateReceiver.onReceive");

            //

            String buttonStandardText = getResources().getString(R.string.perform_scan);

            double remainingDuration = ((double)intent.getLongExtra(TrackerScanner.TRACKERSCANNER_MANUAL_SCAN_REMAINING,-1))/1000.0;

            int locationQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_LOCATION_QUEUE_COUNT,-1);
            int wifiQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_WIFI_QUEUE_COUNT,-1);
            int magQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_MAG_QUEUE_COUNT,-1);
//            int accelQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_ACCEL_QUEUE_COUNT,-1);
            int bleQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_BLE_QUEUE_COUNT,-1);

            String buttonMessage = buttonStandardText
                    + " L["+locationQueueSize+ "]"
                    + " W["+wifiQueueSize+ "]"
                    + " M["+magQueueSize+ "]"
//                    + " A["+accelQueueSize+ "]"
                    + " b["+bleQueueSize+ "]"
                    + " Time remaining["+remainingDuration+ "]";



            Button scanButton = getActivity().findViewById(R.id.manualScanButton);

            if(scanButton!=null){
                //scanButton.setEnabled(true);

                scanButton.setText(buttonMessage);

            }




        }// end of onRecieve
    };// end of new BroadcastReceiver
    //==============================================================================================


}// end of class