package com.riba2reality.wifimapper.ui.main;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.riba2reality.wifimapper.Constants;
import com.riba2reality.wifimapper.R;
import com.riba2reality.wifimapper.TrackerScanner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {


    private static final String ARG_SECTION_NUMBER = "section_number";

    public static SettingsFragment newInstance(int index) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);


        // only enable the server IP input if custom is selected, otherwise disable and enter relavtive IP
        final ListPreference serverAddressListPref = (ListPreference)findPreference("ServerList");
        final EditTextPreference serverAddressPref = (EditTextPreference)findPreference("ServerAddress");
        serverAddressListPref.setOnPreferenceChangeListener(new
               Preference.OnPreferenceChangeListener() {
                   public boolean onPreferenceChange(Preference preference, Object newValue) {
                       final String val = newValue.toString();
                       int index = serverAddressListPref.findIndexOfValue(val);
                       if(index==0) {
                           //if(val.equals(""))

                           // stop is as new IP can be put through checks
                           stopLocationService();


                           serverAddressPref.setEnabled(true);
                           serverAddressPref.setText("");

                       }
                       else {
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
        // make the text edit only accept valid IP addresses
        serverAddressPref.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_PHONE);
                //editText.setImeOptions(EditorInfo.IME_ACTION_DONE);

                editText.setFilters(new InputFilter[] { new InputFilter(){
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, android.text.Spanned dest, int dstart, int dend) {
                        if (end > start) {
                            String destTxt = dest.toString();
                            String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                            if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                                return "";
                            }
                            else {
                                String[] splits = resultingTxt.split("\\.");
                                for (int i = 0; i < splits.length; i++) {
                                    if (Integer.valueOf(splits[i]) > 255) {
                                        return "";
                                    }
                                }
                            }
                        }
                        return null;
                    }
                }
                });

                editText.addTextChangedListener(new TextWatcher(){
                    boolean deleting = false;
                    int lastCount = 0;

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!deleting) {
                            String working = s.toString();
                            String[] split = working.split("\\.");
                            String string = split[split.length - 1];
                            if (string.length() == 3 || string.equalsIgnoreCase("0")
                                    || (string.length() == 2 && Character.getNumericValue(string.charAt(0)) > 1)) {
                                s.append('.');
                                return;
                            }
                        }
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (lastCount < count) {
                            deleting = false;
                        }
                        else {
                            deleting = true;
                        }
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // Nothing happens here
                    }
                });





            }// end of onBindEditText
        });

        //----------------------------------------------------------------

        // make interval atleast have a number input dialog
        /*
        EditTextPreference pref_interval = getPreferenceManager().findPreference("interval");
        pref_interval.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_PHONE);
            }
        });

        // set default value from constants
        pref_interval.setText(Constants.interval);
         */

        SeekBarPreference pref_interval = getPreferenceManager().findPreference("interval");

        pref_interval.setValue(5);

        //----------------------------------------------------------------

        // set default
        EditTextPreference databasePref = getPreferenceManager().findPreference("database");
        databasePref.setText(Constants.database);

        // set lister for end results
        databasePref.setOnPreferenceChangeListener(this);


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


    }// end of onCreatePreferences
    //############################################################################################

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (! (preference instanceof EditTextPreference)) {
            return false;
        }

        EditTextPreference editTextPreference = (EditTextPreference)preference;
        String key = editTextPreference.getKey();
        String text = editTextPreference.getText();// current
        String newText = (String)value;

        System.out.println("Text :"+newText);

        if (key == null || key.isEmpty() || newText == null || newText.isEmpty()) {
            return false;
        }

        switch (key) {
            case "database": {

                // get bad names from reasoruce list
                String[] badNames = getResources().getStringArray(R.array.database_bad_names);
                List<String> badNamesList = Arrays.asList(badNames);

                if(badNamesList.contains(newText)){
                    editTextPreference.setText("");
                    Toast.makeText(getActivity(),"Dissallowed Database name", Toast.LENGTH_SHORT).show();

                }else{
                    return true;
                }


            }

//            case USERNAME: {
//                return text.length() > 0;
//            }

        }

        return false;
    }












    //############################################################################################
    private boolean isLocationServiceRunning(){

        ActivityManager activityManager =
                (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
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



//    private void startLocationService(){
//        if(!isLocationServiceRunning()){
//            Intent intent = new Intent(getActivity().
//                    getApplicationContext(), TrackerScanner.class);
//            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
//            getActivity().startService(intent);
//            Toast.makeText(getActivity(),"Location service started", Toast.LENGTH_SHORT).show();
//        }
//    }// end of startLocationService


    private void stopLocationService(){
        if(isLocationServiceRunning()){
            Intent intent = new Intent(getActivity().getApplicationContext(), TrackerScanner.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            //stopService(intent);//?!
            getActivity().startService(intent);
            Toast.makeText(getActivity(),"Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }// end of startLocationService

}// end of class