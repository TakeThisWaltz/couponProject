package com.azazel.cafecrawler;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.azazel.cafecrawler.fragment.ICrawlFragment;
import com.azazel.framework.view.SmartFragmentStatePagerAdapter;

public class ViewPagerAdapter extends SmartFragmentStatePagerAdapter {

    private int mPageCount;
    private ICrawlFragment[] fragments ;

    public ViewPagerAdapter(FragmentManager fm, ICrawlFragment[] fragments) {
        super(fm);
        this.fragments=fragments;
        mPageCount = fragments.length;
    }

    @Override
    public Fragment getItem(int position) {
        return (Fragment) fragments[position];
    }

    @Override
    public int getCount() {
        return mPageCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return fragments[position].getTitle();
    }

}