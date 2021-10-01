package com.riba2reality.wifimapper.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.riba2reality.wifimapper.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class SecondTabFragment extends Fragment {

    private WifiManager wifiManager;
    // private final int size = 0;
    public final ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final String ARG_SECTION_NUMBER = "section_number";

//    private PageViewModel pageViewModel;

    public static SecondTabFragment newInstance(int index) {
        SecondTabFragment fragment = new SecondTabFragment();
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


    }// end of onCreate method

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_wifiscanner, container, false);

//        final TextView textView = root.findViewById(R.id.section_label);
//        pageViewModel.getText().observe(this, new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });


        Button buttonScan = root.findViewById(R.id.scanBtn);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanWifi();
            }
        });

        ListView listView = root.findViewById(R.id.wifiList);
        wifiManager = (WifiManager) getActivity().
                getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this.getActivity(), "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        adapter = new ArrayAdapter<>(this.getActivity(), android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        scanWifi();


        return root;
    }// end of onCreateView

    public void scanWifi() {
        arrayList.clear();
        getActivity().
                registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this.getActivity(), "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
    }

    final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> results = wifiManager.getScanResults();
            getActivity().unregisterReceiver(this);

            for (ScanResult scanResult : results) {
                //arrayList.add(scanResult.SSID + " - " + scanResult.capabilities);
                arrayList.add(scanResult.SSID + "(" + scanResult.level + ") -" + scanResult.BSSID);
                //arrayList.add(scanResult.SSID + " - \""+scanResult. +"\""
                adapter.notifyDataSetChanged();
            }
        }
    };


}// end of class