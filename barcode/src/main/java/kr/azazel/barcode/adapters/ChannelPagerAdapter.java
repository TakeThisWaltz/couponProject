package kr.azazel.barcode.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.azazel.framework.util.LOG;
import com.azazel.framework.view.SmartFragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import kr.azazel.barcode.AzAppConstants;
import kr.azazel.barcode.R;
import kr.azazel.barcode.fragments.BarcodeFragment;
import kr.azazel.barcode.fragments.MySettingsFragment;
import kr.azazel.barcode.vo.MyBarcode;

public class ChannelPagerAdapter extends SmartFragmentStatePagerAdapter implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ChatPagerAdapter";

    private Activity mActivity;

    private List<String> mTitleList;
    private List<Fragment> mFragmentList;

    public ChannelPagerAdapter(AppCompatActivity activity) {
        super(activity.getSupportFragmentManager());
        mTitleList = new ArrayList<String>();
        mFragmentList = new ArrayList<>();
        this.mActivity = activity;
        //activity.getLoaderManager().initLoader(this.hashCode(), null, this);

//        addTab("test0", 0);
        for (MyBarcode.Category cate : MyBarcode.Category.values()) {
            addCategory(cate);
        }

//        addTab("test", R.layout.splash);
        addSettingsTab();
    }

    @Override
    public Fragment getItem(int position) {
        if (position < getCount()) {
            return mFragmentList.get(position);
        } else
            return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (mTitleList.size() > position) {
            return mTitleList.get(position);
        } else {
            return null;
        }
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                mActivity
                , AzAppConstants.URI.CATEGORY_LIST
                , null, null, null, null);
    }

    private void addCategory(MyBarcode.Category category) {
        LOG.i(TAG, "addCategory category : " + category);
        BarcodeFragment fragment = new BarcodeFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("category", category);
        fragment.setArguments(bundle);

        mTitleList.add(category.displayString());
        mFragmentList.add(fragment);
    }

    private void addTab(String title, int layout) {
        LOG.i(TAG, "addTab title : " + title);
        MySettingsFragment fragment = new MySettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("title", title);
        bundle.putSerializable("layout", layout);
        fragment.setArguments(bundle);

        mTitleList.add(title);
        mFragmentList.add(fragment);
    }

    private void addSettingsTab() {
        MySettingsFragment fragment = new MySettingsFragment();
        mTitleList.add(this.mActivity.getString(R.string.menu_type_setting));
        mFragmentList.add(fragment);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        LOG.i(TAG, "onLoadFinished : " + data.getCount());
        mTitleList.clear();
        mFragmentList.clear();
        while (data.moveToNext()) {
            addCategory(MyBarcode.Category.fromValue(data.getInt(1)));
        }
        this.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}