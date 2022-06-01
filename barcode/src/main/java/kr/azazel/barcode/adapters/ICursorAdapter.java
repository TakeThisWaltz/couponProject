package kr.azazel.barcode.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by JJ_Air on 2015-06-12.
 */
public interface ICursorAdapter {
    public View newView(Context context, Cursor cursor, ViewGroup parent);
    public void bindView(View view, Context context, Cursor cursor);
    public void onDataLoadFinished(int dataCount);

    interface IDataLoadLisner{
        public void onDataLoadFinished(int dataCount);
    }
}
