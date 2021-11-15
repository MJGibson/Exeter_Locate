package com.riba2reality.wifimapper.ui.main;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.riba2reality.wifimapper.BuildConfig;
import com.riba2reality.exeterlocatecore.DataStores.Constants;
import com.riba2reality.wifimapper.R;
import com.riba2reality.exeterlocatecore.TrackerScanner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {


    private static final String ARG_SECTION_NUMBER = "section_number";

    //==============================================================================================
    public static SettingsFragment newInstance(int index) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }
    //==============================================================================================


    //==============================================================================================
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        Log.d("Trace", "SettingsFragment.onCreatePreferences()");


        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final SharedPreferences.Editor SPeditor = SP.edit();
        //----------------------------------------------------------------

        EditTextPreference deviceIDPref = findPreference("DeviceID");
        deviceIDPref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String nV = (String) newValue;
                        boolean empty = nV.equals("");

                        // if the user entered an empty string, replace it with a randomly generated one
                        if (empty) {
                            nV = UUID.randomUUID().toString();
                            ((EditTextPreference) preference).setText(nV);
                        }

                        SPeditor.putString("DeviceID", nV);
                        SPeditor.apply();
                        // true if the string was valid (so newValue is marked correct), else false
                        // meaning that we've intercepted and replaced the user's input
                        return !empty;
                    }
                }
        );
        String s = SP.getString("DeviceID", UUID.randomUUID().toString());
        deviceIDPref.setText(s);


        //----------------------------------------------------------------

        // only enable the server IP input if custom is selected, otherwise disable and enter relative IP
        final ListPreference serverAddressListPref = findPreference("ServerList");
        final EditTextPreference serverAddressPref = findPreference("ServerAddress");
        serverAddressListPref.setOnPreferenceChangeListener(new
                Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String val = newValue.toString();
                        int index = serverAddressListPref.findIndexOfValue(val);
                        if (index == 0) {
                            //if(val.equals(""))

                            // stop is as new IP can be put through checks
                            stopLocationService();


                            serverAddressPref.setEnabled(true);
                            serverAddressPref.setText("");

                        } else {
                            serverAddressPref.setEnabled(false);
                            serverAddressPref.setText(val);
                        }
                        return true;
                    }
                });

        // set default value
        int ip_index = 1;
        serverAddressPref.setEnabled(false);
        serverAddressPref.setText(
                getResources().getStringArray(R.array.server_values)[ip_index]
        );
        serverAddressListPref.setValueIndex(1);
        //----------------------------------------------------------------

        final ListPreference postTypeListPref = findPreference("post_type");

        postTypeListPref.setValueIndex(0); // default to POST




        //----------------------------------------------------------------
        // make the text edit only accept valid IP addresses
        serverAddressPref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_PHONE);
                //editText.setImeOptions(EditorInfo.IME_ACTION_DONE);

                editText.setFilters(new InputFilter[]{new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, android.text.Spanned dest, int dstart, int dend) {
                        if (end > start) {
                            String destTxt = dest.toString();
                            String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                            if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                                return "";
                            } else {
                                String[] splits = resultingTxt.split("\\.");
                                for (String split : splits) {
                                    if (Integer.parseInt(split) > 255) {
                                        return "";
                                    }
                                }
                            }
                        }
                        return null;
                    }
                }
                });

                editText.addTextChangedListener(new TextWatcher() {
                    boolean deleting = false;
                    final int lastCount = 0;

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!deleting) {
                            String working = s.toString();
                            String[] split = working.split("\\.");
                            String string = split[split.length - 1];
                            if (string.length() == 3 || string.equalsIgnoreCase("0")
                                    || (string.length() == 2 && Character.getNumericValue(string.charAt(0)) > 1)) {
                                s.append('.');
                            }
                        }
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        deleting = lastCount >= count;
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // Nothing happens here
                    }
                });


            }// end of onBindEditText
        });

        //----------------------------------------------------------------
        // posting

        SeekBarPreference pref_interval_post = getPreferenceManager().findPreference("interval_posts");

        //System.out.println("Post interval: "+Integer.toString(postInterval));

        int postInterval = SP.getInt("defaultVal_post", -1);
        if (postInterval == -1) {
            postInterval = getContext().getResources().getInteger(R.integer.defaultVal_post);
        }
        pref_interval_post.setValue(postInterval);

        // set lister for end results
        pref_interval_post.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SPeditor.putInt("defaultVal_post", (int) newValue);
                        SPeditor.apply();
                        stopLocationService();
                        return true;
                    }
                }

        );

        //----------------------------------------------------------------
        // manual scan

        SeekBarPreference pref_manual_scan_duration = getPreferenceManager().findPreference("duration_manual_scan");

        //System.out.println("Post interval: "+Integer.toString(postInterval));

        int manual_scan_duration = SP.getInt("defaultVal_manual_scan", -1);
        if (manual_scan_duration == -1) {
            manual_scan_duration = getContext().getResources().getInteger(R.integer.defaultVal_manual_scan);
        }
        pref_manual_scan_duration.setValue(manual_scan_duration);

        // set lister for end results
        pref_manual_scan_duration.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SPeditor.putInt("defaultVal_manual_scan", (int) newValue);
                        SPeditor.apply();
                        stopLocationService();
                        return true;
                    }
                }

        );

        //----------------------------------------------------------------
        // combined scanning (deprecated - on continious scanning)

        SeekBarPreference pref_interval_scan = getPreferenceManager().findPreference("interval_scan");

        //System.out.println("Post interval: "+Integer.toString(postInterval));

        int scanInterval = SP.getInt("defaultVal_scan", -1);
        if (scanInterval == -1) {
            scanInterval = getContext().getResources().getInteger(R.integer.defaultVal_gps);
        }
        pref_interval_scan.setValue(scanInterval);

        // set lister for end results
        pref_interval_scan.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SPeditor.putInt("defaultVal_scan", (int) newValue);
                        SPeditor.apply();
                        stopLocationService();
                        return true;
                    }
                }
        );

        //----------------------------------------------------------------

        SeekBarPreference pref_interval_gps = getPreferenceManager().findPreference("interval_gps");

        //System.out.println("Post interval: "+Integer.toString(postInterval));

        int gpsInterval = SP.getInt("defaultVal_gps", -1);
        if (gpsInterval == -1) {
            gpsInterval = getContext().getResources().getInteger(R.integer.defaultVal_gps);
        }
        pref_interval_gps.setValue(gpsInterval);

        // set lister for end results
        pref_interval_gps.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SPeditor.putInt("defaultVal_gps", (int) newValue);
                        SPeditor.apply();
                        stopLocationService();
                        return true;
                    }
                }
        );

        //----------------------------------------------------------------
        // wifi

        SeekBarPreference pref_interval_wifi = getPreferenceManager().findPreference("interval_wifi");

        //System.out.println("Post interval: "+Integer.toString(postInterval));

        int wifiInterval = SP.getInt("defaultVal_wifi", -1);
        if (wifiInterval == -1) {
            wifiInterval = getContext().getResources().getInteger(R.integer.defaultVal_wifi);
        }
        pref_interval_wifi.setValue(wifiInterval);

        // set lister for end results
        pref_interval_wifi.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SPeditor.putInt("defaultVal_wifi", (int) newValue);
                        SPeditor.apply();
                        stopLocationService();
                        return true;
                    }
                }

        );

        //----------------------------------------------------------------
        // BLE

        SeekBarPreference pref_interval_BLE = getPreferenceManager().findPreference("interval_ble");


        int bleInterval = SP.getInt("defaultVal_ble", -1);
        if (bleInterval == -1) {
            bleInterval = getContext().getResources().getInteger(R.integer.defaultVal_ble);
        }
        pref_interval_BLE.setValue(bleInterval);

        // set lister for end results
        pref_interval_BLE.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SPeditor.putInt("defaultVal_ble", (int) newValue);
                        SPeditor.apply();
                        stopLocationService();
                        return true;
                    }
                }

        );




        //----------------------------------------------------------------
        // magnetometer

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        SeekBarPreference pref_interval_mag = getPreferenceManager().findPreference("interval_mag");

        //System.out.println("Post interval: "+Integer.toString(postInterval));

        int magInterval = SP.getInt("defaultVal_mag", -1);
        if (magInterval == -1) {
            magInterval = getContext().getResources().getInteger(R.integer.defaultVal_mag);
        }
        pref_interval_mag.setValue(magInterval);

        // check if sensor is present


        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)

        {
            // Success! There's a magnetometer.
            prefs.edit().putBoolean("magAvailable", true).apply();
        } else {
            // Failure! No magnetometer.
            pref_interval_mag.setEnabled(false);
            prefs.edit().putBoolean("magAvailable", false).apply();

        }


        // set lister for end results
        pref_interval_mag.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SPeditor.putInt("defaultVal_mag", (int) newValue);
                        SPeditor.apply();
                        stopLocationService();
                        return true;
                    }
                }

        );


        //----------------------------------------------------------------


        SeekBarPreference pref_interval_accel = getPreferenceManager().findPreference("interval_accel");

        //System.out.println("Post interval: "+Integer.toString(postInterval));

        int accelInterval = SP.getInt("defaultVal_accel", -1);
        if (accelInterval == -1) {
            accelInterval = getContext().getResources().getInteger(R.integer.defaultVal_accel);
        }
        pref_interval_accel.setValue(accelInterval);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
        {
            // Success! There's a magnetometer.
            prefs.edit().putBoolean("accelAvailable", true).apply();
        } else {
            // Failure! No magnetometer.
            pref_interval_accel.setEnabled(false);
            prefs.edit().putBoolean("accelAvailable", false).apply();

        }

        // set lister for end results
        pref_interval_accel.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SPeditor.putInt("defaultVal_accel", (int) newValue);
                        SPeditor.apply();
                        stopLocationService();
                        return true;
                    }
                }

        );


        //----------------------------------------------------------------

        // bluetooth











        //----------------------------------------------------------------




        // set default
        EditTextPreference databasePref = getPreferenceManager().findPreference("database");

        String databaseString = SP.getString("database", "");
        if (databaseString.equals("")) {
            databaseString = Constants.database;

        }
        databasePref.setText(databaseString);
        // set lister for end results
        databasePref.setOnPreferenceChangeListener(this);

        databasePref.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SPeditor.putString("database", (String)newValue);
                        SPeditor.apply();
                        return true;
                    };
                }
        );


/*
        databasePref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                //editText.setInputType(InputType.TYPE_CLASS_PHONE);



                editText.setFilters(new InputFilter[] { new InputFilter(){
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, android.text.Spanned dest, int dstart, int dend) {
                        if (end > start) {
                            String destTxt = dest.toString();
                            String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);

                            // get bad names from reasoruce list
                            String[] badNames = getResources().getStringArray(R.array.database_bad_names);
                            List<String> badNamesList = Arrays.asList(badNames);

                            if(badNamesList.contains(resultingTxt)){
                                return "";
                            }


                        }
                        return null;
                    }
                }
                });


                editText.addTextChangedListener(new TextWatcher(){



                    @Override
                    public void afterTextChanged(Editable s) {
                        String working = s.toString();

                        // get bad names from reasoruce list
                        String[] badNames = getResources().getStringArray(R.array.database_bad_names);
                        List<String> badNamesList = Arrays.asList(badNames);

                        if(badNamesList.contains(working)){
                            s.clear();
                            Toast.makeText(getActivity(),"Dissallowed Database name", Toast.LENGTH_SHORT).show();

                        }

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // Nothing happens here
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // Nothing happens here
                    }
                });





            }// end of onBindEditText
        });

        */


        //----------------------------------------------------------------

        // display version number of this app
        EditTextPreference versionPref = getPreferenceManager().findPreference("versionNum");


        versionPref.setEnabled(false);
        versionPref.setText(BuildConfig.VERSION_NAME);

        //----------------------------------------------------------------

        // display core version number
        EditTextPreference versionCorePref =
                getPreferenceManager().findPreference("coreVersionNum");


        versionCorePref.setEnabled(false);
        versionCorePref.setText(TrackerScanner.libraryVersion);


    }// end of onCreatePreferences
    //==============================================================================================


    //==============================================================================================
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (!(preference instanceof EditTextPreference)) {
            return false;
        }

        EditTextPreference editTextPreference = (EditTextPreference) preference;
        String key = editTextPreference.getKey();
        // String text = editTextPreference.getText();// current
        String newText = (String) value;

        System.out.println("Text :" + newText);

        if (key == null || key.isEmpty() || newText == null || newText.isEmpty()) {
            return false;
        }

        if ("database".equals(key)) {// get bad names from reasoruce list
            String[] badNames = getResources().getStringArray(R.array.database_bad_names);
            List<String> badNamesList = Arrays.asList(badNames);

            if (badNamesList.contains(newText)) {
                editTextPreference.setText("");
                Toast.makeText(getActivity(), "Dissallowed Database name", Toast.LENGTH_SHORT).show();

            } else {
                return true;
            }

//            case USERNAME: {
//                return text.length() > 0;
//            }
        }

        return false;
    }
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
//    private void startLocationService(){
//        if(!isLocationServiceRunning()){
//            Intent intent = new Intent(getActivity().
//                    getApplicationContext(), TrackerScanner.class);
//            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
//            getActivity().startService(intent);
//            Toast.makeText(getActivity(),"Location service started", Toast.LENGTH_SHORT).show();
//        }
//    }// end of startLocationService
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