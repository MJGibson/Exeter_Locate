package com.riba2reality.wifimapper.ui.main;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.riba2reality.wifimapper.R;

public class SettingsFragment extends PreferenceFragmentCompat {


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


        final ListPreference itemList = (ListPreference)findPreference("ServerList");
        final EditTextPreference itemList2 = (EditTextPreference)findPreference("ServerAddress");
        itemList.setOnPreferenceChangeListener(new
               Preference.OnPreferenceChangeListener() {
                   public boolean onPreferenceChange(Preference preference, Object newValue) {
                       final String val = newValue.toString();
                       int index = itemList.findIndexOfValue(val);
                       if(index==0)
                       //if(val.equals(""))
                           itemList2.setEnabled(true);
                       else
                           itemList2.setEnabled(false);
                       return true;
                   }
               });


        EditTextPreference pref1 = getPreferenceManager().findPreference("ServerAddress");
        pref1.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
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



    }// end of onCreatePreferences



}// end of class