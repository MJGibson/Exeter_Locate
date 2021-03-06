package com.riba2reality.wifimapper.ui.main;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.riba2reality.exeterlocatecore.DataStores.Constants;
import com.riba2reality.exeterlocatecore.TrackerScanner;
import com.riba2reality.wifimapper.R;

import java.util.Calendar;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomescreenFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomescreenFragment extends Fragment {


    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String ARG_SECTION_NUMBER = "section_number";

    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;


    public static final int REQUEST_ENABLE_BT = 11;

    private ScrollView scroll;
    private TextView logTextView;

    private final Queue<String> messagesQueue = new ConcurrentLinkedQueue<>();

    private Date lastScrollTime;

    private Button startButton;
    private Button stopButton;




    //==============================================================================================
    public HomescreenFragment() {
        // Required empty public constructor
    }
    //==============================================================================================


    //==============================================================================================
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomescreenFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomescreenFragment newInstance(String param1, String param2) {
        HomescreenFragment fragment = new HomescreenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    //==============================================================================================

    //==============================================================================================
    public static HomescreenFragment newInstance(int index) {
        HomescreenFragment fragment = new HomescreenFragment();
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
        if (getArguments() != null) {
            // TODO: Rename and change types of parameters
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    //==============================================================================================

    //==============================================================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d("Trace", "HomescreenFragment.onCreateView");


        lastScrollTime = Calendar.getInstance().getTime();


        CharSequence txt = null;

        if(logTextView!=null){

            Log.d("Trace", "HomescreenFragment.onCreateView(logTextView!=null)");

            txt = logTextView.getText();

        }


        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_homescreen, container, false);

        logTextView = rootView.findViewById(R.id.log);

        scroll = rootView.findViewById(R.id.logScroll);

        if(txt!=null){
            logTextView.setText(txt);
        }

        if(messagesQueue.size() > 0){
            while (this.messagesQueue.size() > 0) {
                String message = messagesQueue.poll();
                addMessage(message);
            }

        }
        //----------------------------------------------------------------------

        startButton = rootView.findViewById(R.id.start_button);
        startButton.setOnClickListener(startButtonPressed);

        stopButton = rootView.findViewById(R.id.stop_button);
        stopButton.setOnClickListener(stopButtonPressed);

        if (!isLocationServiceRunning()) {
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
        }else{
            stopButton.setEnabled(true);
            startButton.setEnabled(false);
        }

        // check if bluetooth is available and fetch it
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }else {

            // ble stuff
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        return rootView;
    }
    //==============================================================================================


    //==============================================================================================
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // enable the text box content to be save when rotating screen
        // we can then extract the value and write it to the text box
        // instead of having an empty text box after rotation
        TextView log = getActivity().findViewById(R.id.log);
        if(log!=null) {
            outState.putCharSequence("textbox_contents", (log).getText());
        }
    }// end of onSaveInstanceState
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // if we've saved the state before (e.g. we rotated the screen),
        // then load up the text previously in the text box
        if (savedInstanceState != null) {
            TextView logTextView = view.findViewById(R.id.log);
            if(logTextView != null )
                logTextView.setText(savedInstanceState.getCharSequence("textbox_contents"));
        }
    }// end of onViewCreated
    //==============================================================================================

    //==============================================================================================
    View.OnClickListener startButtonPressed = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            if (ContextCompat.checkSelfPermission(
                    getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_LOCATION_PERMISSIONS
                );
            } else {
                startLocationService();

            }


        }
    };
    //==============================================================================================

    //==============================================================================================
    View.OnClickListener stopButtonPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            stopLocationService();
        }
    };
    //==============================================================================================


    //==============================================================================================
    private ViewTreeObserver.OnScrollChangedListener scrollChangedListener = new ViewTreeObserver.OnScrollChangedListener(){

        @Override
        public void onScrollChanged() {
            //Date currentTime = Calendar.getInstance().getTime();
            lastScrollTime = Calendar.getInstance().getTime();

        }
    };
    //==============================================================================================

    //==============================================================================================
    public void addMessage(String message){

        if (logTextView != null && getActivity() != null) {

            Log.d("Trace", "HomeScreenFragment.addMessage, logTextView != null");

            // append to the log text
            logTextView.append( message );

            // count the number of lines to remove, i.e. the number of lines > the maximum


            int linesToRemove = logTextView.getLineCount() - getActivity().getBaseContext().getResources().getInteger(R.integer.max_log_lines); // TODO check we can get activity...

            // if there some to remove
            if (linesToRemove > 0) {
                // get the text from the logger and declare some variables we'll need
                Editable txt = logTextView.getEditableText();
                int lineStart, lineEnd, i;

                for (i = 0; i < linesToRemove; i++) {
                    // get the start and end locations of the first line of the text
                    lineStart = logTextView.getLayout().getLineStart(0);
                    lineEnd = logTextView.getLayout().getLineEnd(0);

                    // remove it
                    txt.delete(lineStart, lineEnd);
                }
            }


            // check if enough time has passed, otherwise the user is using the scroll
            Date nextScrollTime = lastScrollTime;
            nextScrollTime.setSeconds(lastScrollTime.getSeconds() + 30);
            Date currentTime = Calendar.getInstance().getTime();

            if(currentTime.compareTo(nextScrollTime) > 0) {


                scroll.post(new Runnable() {
                    @Override
                    public void run() {
                        scroll.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }

        }else{
            messagesQueue.add(message);
        }




    }// end of addMessage
    //==============================================================================================


    //==============================================================================================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Toast.makeText(getActivity(), "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }

    }// end of onRequestPermissionsResult
    //==============================================================================================

    //==============================================================================================
    private boolean isLocationServiceRunning() {

        ActivityManager activityManager =
                (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
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

            String[] server_values = getResources().getStringArray(R.array.server_values);

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String serverAddress = SP.getString("ServerAddress", server_values[1]);

            //System.out.println("ServerAddress: "+serverAddress);

            if (serverAddress.isEmpty() || serverAddress.equals(server_values[0])) {
                Toast.makeText(getActivity(), "Please set Server Address", Toast.LENGTH_SHORT).show();
                return;
            }

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

        Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);
        intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
        intent.putExtra("MODE", false); // disengage citizen mode, thus dev mode

        String[] server_values = getResources().getStringArray(com.riba2reality.exeterlocatecore.R.array.server_values);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
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


        getActivity().startService(intent);
        Toast.makeText(getActivity(), "Location service started", Toast.LENGTH_SHORT).show();

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
            Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            //stopService(intent);//?!
            getActivity().startService(intent);
            Toast.makeText(getActivity(), "Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }// end of startLocationService
    //==============================================================================================


}// end of class