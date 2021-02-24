package com.riba2reality.wifimapper.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

import com.riba2reality.wifimapper.DataStores.Constants;
import com.riba2reality.wifimapper.R;
import com.riba2reality.wifimapper.TrackerScanner;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ManualScanFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ManualScanFragment extends Fragment {


    private int selectedLocation = -1;

    //private TrackerScannerSingle scans;




    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_SECTION_NUMBER = "section_number";



    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver((receiver),
                new IntentFilter(TrackerScanner.TRACKERSCANNER_SINGLE_SCAN_RESULT)
        );
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(receiver);
        super.onStop();
    }

    //==============================================================================================
    public ManualScanFragment() {
        // Required empty public constructor



    }
    //==============================================================================================


    //==============================================================================================
    public static ManualScanFragment newInstance(int index) {
        ManualScanFragment fragment = new ManualScanFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (getArguments() != null) {
//            // TODO: Rename and change types of parameters
//            String mParam1 = getArguments().getString(ARG_PARAM1);
//            String mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }
    //==============================================================================================

    //==============================================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_manual_scan, container, false);

        Spinner dropdown = rootView.findViewById(R.id.spinner1);

        // Create an ArrayAdapter using the string array and a default spinner
        ArrayAdapter<CharSequence> staticAdapter = ArrayAdapter
                .createFromResource(this.getActivity(), R.array.locations1,
                        android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        staticAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        dropdown.setAdapter(staticAdapter);

        dropdown.setOnItemSelectedListener(dropDownListerner);

        //--------

        rootView.findViewById(R.id.manualScanButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manualScan();
            }
        });

        //scans = new TrackerScannerSingle(getActivity());
        //

        rootView.findViewById(R.id.manualPostButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postManualScans();
            }
        });

        requestUpdate();

        return rootView;
    }// end of on create view
    //==============================================================================================


    //==============================================================================================
    private void requestUpdate() {
        Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);

        intent.setAction(Constants.ACTION_REQUEST_UPDATE);

        //intent.putExtra("message", imageName);

        getActivity().startService(intent);
    }
    //==============================================================================================



    //==============================================================================================
    private AdapterView.OnItemSelectedListener dropDownListerner = new AdapterView.OnItemSelectedListener() {


        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            selectedLocation = position;

            ImageView mImageView= (ImageView) getActivity().findViewById(R.id.imageView);

            String imageName = getCharForNumber(position);

            if(imageName==null){
                return;//bail
            }

//            if(imageName.compareTo("n")==0){
//                imageName="o"; // there is no "n"
//            }

            //imageName += ".png";

            //Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            Log.d("locImage: ", imageName);

            int image_id = getResources().getIdentifier(imageName, "drawable", getActivity().getPackageName());

            mImageView.setImageResource(image_id);




        }// end of onItenSelected

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

    };// end of dropDownListerner
    //==============================================================================================

    //==============================================================================================
    private String getCharForNumber(int i) {
        String returnVal = i >= 0 && i < 27 ? String.valueOf((char)(i + 97)) : null;
        if(returnVal.compareTo("n")==0){
            returnVal="o"; // there is no "n"
        }
        return returnVal;
    }
    //==============================================================================================


    //==============================================================================================
    private void manualScan(){

        Log.d("Trace", "ManualScan()");

        getActivity().findViewById(R.id.manualScanButton).setEnabled(false);
        getActivity().findViewById(R.id.manualPostButton).setEnabled(false);

        String imageName = getCharForNumber(selectedLocation);


        String[] server_values = getResources().getStringArray(R.array.server_values);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String serverAddress = SP.getString("ServerAddress", server_values[1]);

        //System.out.println("ServerAddress: "+serverAddress);

        if (serverAddress.isEmpty() || serverAddress.equals(server_values[0])) {
            Toast.makeText(getActivity(), "Please set Server Address", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);

        intent.setAction(Constants.ACTION_SINGLE_SCAN);

        intent.putExtra("message", imageName);

        getActivity().startService(intent);



        //Toast.makeText(getActivity(), "Started single scan set", Toast.LENGTH_SHORT).show();




//
//        if(scans==null) {
//            scans = new TrackerScannerSingle(getActivity());
//        }
//
//

//        scans.scanAll(imageName);

    }// end of manual scan
    //==============================================================================================

    //==============================================================================================
    private void postManualScans(){

        Log.d("Trace", "postManualScans()");
//
//        if(scans==null) {
//            scans = new TrackerScannerSingle(getActivity());
//        }else{
//            scans.postQueuesToServer();
//        }

        getActivity().findViewById(R.id.manualScanButton).setEnabled(false);
        getActivity().findViewById(R.id.manualPostButton).setEnabled(false);


        String[] server_values = getResources().getStringArray(R.array.server_values);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String serverAddress = SP.getString("ServerAddress", server_values[1]);

        //System.out.println("ServerAddress: "+serverAddress);

        if (serverAddress.isEmpty() || serverAddress.equals(server_values[0])) {
            Toast.makeText(getActivity(), "Please set Server Address", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);

        intent.setAction(Constants.ACTION_POST_ALL);

        //intent.putExtra("message", imageName);

        getActivity().startService(intent);



    }// end of postManualScans
    //==============================================================================================

    //==============================================================================================
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("Trace", "ManualScanFragment.receiver.onReceive");

            //String message = intent.getStringExtra(TrackerScanner.TRACKERSCANNER_MESSAGE);

            //String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());


            //TextView logTextView = findViewById(R.id.log);
            //final ScrollView scroll = findViewById(R.id.logScroll);

            //scanButton.setEnabled(true);
            //postButton.setText("Post Data - ("+String.valueOf(combinedScanResultQueue.size())+")");

            getActivity().findViewById(R.id.manualScanButton).setEnabled(true);
            getActivity().findViewById(R.id.manualPostButton).setEnabled(true);

            int combinedQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_COMBINED_QUEUE_COUNT,-1);

            int resendQueueSize = intent.getIntExtra(TrackerScanner.TRACKERSCANNER_RESEND_QUEUE_COUNT,-1);

            String postButtonTextUpdate = "Post Data - ("+String.valueOf(combinedQueueSize)+"),["
                    +String.valueOf(resendQueueSize)+"]";

            Button postButton = getActivity().findViewById(R.id.manualPostButton);
            postButton.setText(postButtonTextUpdate);



        }// end of onRecieve
    };// end of new BroadcastReceiver
    //==============================================================================================

}// end of class