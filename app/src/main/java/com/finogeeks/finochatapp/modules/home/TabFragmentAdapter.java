package com.finogeeks.finochatapp.modules.home;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.view.ViewGroup;


public class TabFragmentAdapter extends FragmentPagerAdapter {

    private Fragment[] fragments;

    public TabFragmentAdapter(FragmentManager fm, Fragment[] fragments) {
        super(fm);
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments[position % fragments.length];
    }

    @Override
    public int getCount() {
        return fragments.length;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
    }
}