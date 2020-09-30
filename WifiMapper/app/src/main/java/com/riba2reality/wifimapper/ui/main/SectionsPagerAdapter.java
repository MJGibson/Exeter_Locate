package com.riba2reality.wifimapper.ui.main;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.riba2reality.wifimapper.R;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.HomeScreen,R.string.tab_text_1, R.string.tab_text_2, R.string.Settings};
    private final Context mContext;

    private Fragment mapTab;
    private Fragment wifiTab;
    private Fragment settings;
    private Fragment homeScreen;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;

        int i = 0;

        homeScreen = HomescreenFragment.newInstance(++i);

        mapTab = FirstTabFragment.newInstance(++i);
        wifiTab = SecondTabFragment.newInstance(++i);

        settings = SettingsFragment.newInstance(++i);




    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        //return PlaceholderFragment.newInstance(position + 1);

        switch(position) {
            case 0:
                //return FirstTabFragment.newInstance(position + 1);
                return homeScreen;
            case 1:
                //return FirstTabFragment.newInstance(position + 1);
                return mapTab;

            case 2:
                //return SecondTabFragment.newInstance(position + 1);
                return wifiTab;

            case 3:
                //return SecondTabFragment.newInstance(position + 1);
                return settings;


            // Other fragments
        }

        // default value
        return null;

    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return 4;
    }
}