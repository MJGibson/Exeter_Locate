package com.riba2reality.wifimapper;

import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.Gson;
import com.riba2reality.wifimapper.ui.main.FirstTabFragment;
import com.riba2reality.wifimapper.ui.main.SecondTabFragment;
import com.riba2reality.wifimapper.ui.main.SectionsPagerAdapter;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Map<String, String> parameters = new HashMap<>();

                List<String> macAddressList = new ArrayList<>();

                //------

                FirstTabFragment mapTab = (FirstTabFragment)sectionsPagerAdapter.getItem(0);

                LatLng myCords = new LatLng(mapTab.lat, mapTab.lon);

                String time = mapTab.time;

                //------

                SecondTabFragment wifiTab = (SecondTabFragment) sectionsPagerAdapter.getItem(1);

                ArrayList<String> wifiList = wifiTab.arrayList;


                for(int i = 0; i <  wifiList.size(); ++i){

                    String[] MacAndName = wifiList.get(i).split("-");

                    String macAddress = MacAndName[MacAndName.length-1];

                    macAddressList.add(macAddress);

                }



                //------



                /*
                macAddressList.add("000.111.222.444");
                macAddressList.add("777");
                macAddressList.add("ABC");

                 */

//                parameters.put("TIME","12:01");
//                parameters.put("X","42");
//                parameters.put("Y","7");

                parameters.put("TIME",time);
                parameters.put("X",Double.toString(mapTab.lat));
                parameters.put("Y",Double.toString(mapTab.lon));

                String macAddressJson = new Gson().toJson(macAddressList );


                //parameters.put("MacAddresses",macAddressList.toString());
                parameters.put("MacAddressesJson",macAddressJson);


                String message = new JSONObject(parameters).toString();

                //------

                //String address = "127.0.0.1";
                //String address = "10.0.2.2"; // local for computer the emulator
                String address = "192.168.0.10";
                //String port = "8000";
                //String port = "27017";


                //String address = "httpbin.org/get";


                //String uri = "http://"+address+":"+port;

                //String uri = "http://"+address;
                String uri = "https://"+address;

                //String uri = "http://example.com";
                //String uri = "https://postman-echo.com/get";

                //String message = "hello_message";

                PostToServer post = new PostToServer();


                post.is = getResources().openRawResource(R.raw.cert);




                post.execute(uri,message);








                Snackbar.make(view, "Posted", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });




//        // Get the SupportMapFragment and request notification
//        // when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.mapView);
//        mapFragment.getMapAsync(this);




    }// end of onCreate method




//    @Override
//    public void onMapReady(GoogleMap googleMap) {
//        LatLng sydney = new LatLng(-33.852, 151.211);
//        googleMap.addMarker(new MarkerOptions()
//                .position(sydney)
//                .title("Marker in Sydney"));
//    }





}// end of class