package com.riba2reality.wifimapper.ui.main;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.riba2reality.wifimapper.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * A placeholder fragment containing a simple view.
 */
public class BluetoothTabFragment extends Fragment {

    //----------------------------------------------------------------------------------------------

    //private WifiManager wifiManager;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;


    public static final int REQUEST_ENABLE_BT = 11;


    public final ArrayList<String> arrayList = new ArrayList<>();
    public final HashMap<String,Integer> rssiMap = new HashMap<>();
    private ArrayAdapter<String> adapter;



    private static final String ARG_SECTION_NUMBER = "section_number";

//    private PageViewModel pageViewModel;

    //----------------------------------------------------------------------------------------------

    //##############################################################################################
    // class functions
    //##############################################################################################

    //==============================================================================================
    public static BluetoothTabFragment newInstance(int index) {
        BluetoothTabFragment fragment = new BluetoothTabFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }// end of BluetoothTabFragment (static constructor)
    //==============================================================================================

    //==============================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }// end of onCreate method
    //==============================================================================================

    //==============================================================================================
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {

        View root = inflater.inflate(R.layout.fragment_wifiscanner, container, false);



        Button buttonScan = root.findViewById(R.id.scanBtn);
        buttonScan.setText("Scan BLE");

        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanBLE();
            }
        });

        ListView listView = root.findViewById(R.id.wifiList);

        //----------------------------
        //bluetooth setup

        // check if bluetooth is available and fetch it
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        // ble stuff
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        //wifiManager = (WifiManager) getActivity().
        //        getApplicationContext().getSystemService(Context.WIFI_SERVICE);

//        if (!wifiManager.isWifiEnabled()) {
//            Toast.makeText(this.getActivity(), "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
//            wifiManager.setWifiEnabled(true);
//        }

        adapter = new ArrayAdapter<>(this.getActivity(), android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);


        //scanWifi();


        return root;
    }// end of onCreateView
    //==============================================================================================


    //==============================================================================================
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode,  data);

        if( resultCode == RESULT_OK){
            startBLEScan();
            Log.d("mgdev", "onActivityResult.RESULT_OK");
        }else{
            Log.d("mgdev", "onActivityResult. ELSE");
        }

    }
    //==============================================================================================


    //==============================================================================================
    private void scanBLE(){

        Log.d("mgdev", "scanBLE");

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
                    startBLEScan();
                }

            }
        }

    }// end of scanBLE
    //==============================================================================================


    //==============================================================================================
    private void startBLEScan(){

        Log.d("mgdev", "startBLEScan");
        bluetoothLeScanner.startScan(leScanCallback);

    }// end of startBLEScan
    //==============================================================================================

    //==============================================================================================
    private ScanCallback leScanCallback = new ScanCallback() {


        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            Log.d("mgdev", "BluetoothTabFragment.onBatchScanResults");

            for (ScanResult result : results) {

                String message;
                String device = result.getDevice().getAddress();

                String name = result.getDevice().getName();

                message = "" + device + "---"+ name;

                if(!rssiMap.keySet().contains(message)){
                    rssiMap.put(message,result.getRssi());

                    message += "; "+ result.getRssi();

                    arrayList.add(message);


                }
            }
            adapter.notifyDataSetChanged();
        }


        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            //bluetoothLeScanner.stopScan(leScanCallback);


            Log.d("mgdev", "BluetoothTabFragment.onScanResult");



            String message;
            String device = result.getDevice().getAddress();

            String name = result.getDevice().getName();

            message = "" + device + "---"+ name;

            //if(!arrayList.contains(message))
            //    arrayList.add(message);

            if(!rssiMap.keySet().contains(message)){
                rssiMap.put(message,result.getRssi());

                message += "; "+ result.getRssi();

                arrayList.add(message);


            }


            adapter.notifyDataSetChanged();
        }

    };
    //==============================================================================================


}// end of class