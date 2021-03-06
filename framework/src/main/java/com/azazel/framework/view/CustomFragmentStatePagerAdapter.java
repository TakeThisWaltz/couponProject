package com.azazel.framework.view;

import java.util.ArrayList;


import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;

import com.azazel.framework.util.LOG;

public abstract class CustomFragmentStatePagerAdapter extends FragmentPagerAdapter {
	private static final String TAG = "FragmentStatePagerAdapter";
	private static final boolean DEBUG = false;

	private final FragmentManager mFragmentManager;
	private FragmentTransaction mCurTransaction = null;

	public ArrayList<Fragment.SavedState> mSavedState = new ArrayList<Fragment.SavedState>();
	public ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
	private Fragment mCurrentPrimaryItem = null;

	public CustomFragmentStatePagerAdapter(FragmentManager fm) {
		super(fm);
		mFragmentManager = fm;
	}

	/**
	 * Return the Fragment associated with a specified position.
	 */
	public abstract Fragment getItem(int position);

	@Override
	public void startUpdate(ViewGroup container) {}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		LOG.i(TAG, "instantiateItem - fragments : " + mFragments.size() + ", states : " + mSavedState.size());
		// If we already have this item instantiated, there is nothing
		// to do. This can happen when we are restoring the entire pager
		// from its saved state, where the fragment manager has already
		// taken care of restoring the fragments we previously had instantiated.

		// DONE Remove of the add process of the old stuff
		/* if (mFragments.size() > position) { Fragment f = mFragments.get(position); if (f != null) { return f; } } */

		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}

		Fragment fragment = getItem(position);
		LOG.i(TAG, "Adding item #" + position + ": f=" + fragment);
		if (mSavedState.size() > position) {
			Fragment.SavedState fss = mSavedState.get(position);
			if (fss != null) {
				try // DONE: Try Catch
				{
					fragment.setInitialSavedState(fss);
				} catch (Exception ex) {
					// Schon aktiv (kA was das hei??t xD)
				}
			}
		}
		while (mFragments.size() <= position) {
			mFragments.add(null);
		}
		fragment.setMenuVisibility(false);
		mFragments.set(position, fragment);
		mCurTransaction.add(container.getId(), fragment);

		return fragment;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		LOG.i(TAG, "destroyItem - fragments : " + mFragments.size() + ", states : " + mSavedState.size());
		Fragment fragment = (Fragment) object;

		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}
		mCurTransaction.remove(fragment);

		/*if (mCurTransaction == null) { mCurTransaction = mFragmentManager.beginTransaction(); } if (DEBUG) Log.v(TAG, "Removing item #" + position + ": f=" + object + " v=" + ((Fragment)
		 * object).getView()); while (mSavedState.size() <= position) { mSavedState.add(null); } mSavedState.set(position, mFragmentManager.saveFragmentInstanceState(fragment));
		 * mFragments.set(position, null); mCurTransaction.remove(fragment); */
	}

	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object) {
		LOG.i(TAG, "setPrimaryItem - fragments : " + mFragments.size() + ", states : " + mSavedState.size());
		Fragment fragment = (Fragment) object;
		if (fragment != mCurrentPrimaryItem) {
			if (mCurrentPrimaryItem != null) {
				mCurrentPrimaryItem.setMenuVisibility(false);
			}
			if (fragment != null) {
				fragment.setMenuVisibility(true);
			}
			mCurrentPrimaryItem = fragment;
		}
	}

	@Override
	public void finishUpdate(ViewGroup container) {
		LOG.i(TAG, "finishUpdate - fragments : " + mFragments.size() + ", states : " + mSavedState.size());
		if (mCurTransaction != null) {
			mCurTransaction.commitAllowingStateLoss();
			mCurTransaction = null;
			mFragmentManager.executePendingTransactions();
		}
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return ((Fragment) object).getView() == view;
	}

//	@Override
//	public Parcelable saveState() {
//		LOG.i(TAG, "saveState - fragments : " + mFragments.size() + ", states : " + mSavedState.size());
//		Bundle state = null;
//		if (mSavedState.size() > 0) {
//			state = new Bundle();
//			Fragment.SavedState[] fss = new Fragment.SavedState[mSavedState.size()];
//			mSavedState.toArray(fss);
//			state.putParcelableArray("states", fss);
//		}
//		for (int i = 0; i < mFragments.size(); i++) {
//			Fragment f = mFragments.get(i);
//			if (f != null) {
//				if (state == null) {
//					state = new Bundle();
//				}
//				String key = "f" + i;
//				mFragmentManager.putFragment(state, key, f);
//			}
//		}
//		return state;
//	}
//
//	@Override
//	public void restoreState(Parcelable state, ClassLoader loader) {
//		LOG.i(TAG, "restoreState - fragments : " + mFragments.size() + ", states : " + mSavedState.size());
//		if (state != null) {
//			Bundle bundle = (Bundle) state;
//			bundle.setClassLoader(loader);
//			Parcelable[] fss = bundle.getParcelableArray("states");
//			mSavedState.clear();
//			mFragments.clear();
//			if (fss != null) {
//				for (int i = 0; i < fss.length; i++) {
//					mSavedState.add((Fragment.SavedState) fss[i]);
//				}
//			}
//			Iterable<String> keys = bundle.keySet();
//			for (String key : keys) {
//				if (key.startsWith("f")) {
//					int index = Integer.parseInt(key.substring(1));
//					Fragment f = mFragmentManager.getFragment(bundle, key);
//					if (f != null) {
//						while (mFragments.size() <= index) {
//							mFragments.add(null);
//						}
//						f.setMenuVisibility(false);
//						mFragments.set(index, f);
//					} else {
//						LOG.d(TAG, "Bad fragment at key " + key);
//					}
//				}
//			}
//		}
//	}
}