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
import android.widget.CursorAdapter;

import com.azazel.framework.AzApplication;
import com.azazel.framework.util.LOG;

import kr.azazel.barcode.local.AzAppDataHelper;

/**
 * Created by JJ_Air on 2015-06-12.
 */
public class MyCursorAdapter implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "MyCursorAdapter";

    private Activity mActivity;
    private Uri mUri;
    private CursorAdapter mAdapter;
    private ICursorAdapter mListener;
    private int category;
    private AzAppDataHelper dataHelper;

    public MyCursorAdapter(Activity activity, int category, Uri uri, ICursorAdapter listener) {
        this.dataHelper = AzAppDataHelper.getInstance();
        this.mActivity = activity;
        this.mUri = uri;
        this.mListener = listener;
        this.category = category;

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

        mAdapter.setFilterQueryProvider(q -> {
            LOG.d(TAG, "query : " + q);
            String[] args = q.toString().split("\\|");
            String keyword = args[0];
            String sort = args.length > 1 ? args[1] : null;
            Cursor cs = dataHelper.queryBarcodesByCategory(category + "", keyword, sort);
            AzApplication.executeUIJob(() -> {
                mAdapter.swapCursor(cs);
                mAdapter.notifyDataSetChanged();
            });
            return null;
        });

        activity.getLoaderManager().initLoader(category, null, this);
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

    public CursorAdapter getAdapter() {
        return this.mAdapter;
    }

    public void setFilter(String query) {
        mAdapter.getFilter().filter(query);
    }
}
