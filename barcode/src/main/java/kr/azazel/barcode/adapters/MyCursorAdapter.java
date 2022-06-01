package kr.azazel.barcode.adapters;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;

import com.azazel.framework.util.LOG;

/**
 * Created by JJ_Air on 2015-06-12.
 */
public class MyCursorAdapter implements LoaderManager.LoaderCallbacks<Cursor>{

    public static final String TAG = "MyCursorAdapter";

    private Activity mActivity;
    private Uri mUri;
    private CursorAdapter mAdapter;
    private ICursorAdapter mListener;

    public MyCursorAdapter(Activity activity, int id, Uri uri, ICursorAdapter listener){
        this.mActivity = activity;
        this.mUri = uri;
        this.mListener = listener;

        this.mAdapter = new CursorAdapter(activity, null, false) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return mListener.newView(context, cursor, parent);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                mListener.bindView(view, context, cursor);
            }
        };

        activity.getLoaderManager().initLoader(id, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                mActivity
                , mUri
                , null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        LOG.i(TAG, "onLoadFinished - cnt : " + data.getCount());
        mListener.onDataLoadFinished(data.getCount());
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public BaseAdapter getAdapter(){
        return this.mAdapter;
    }
}
