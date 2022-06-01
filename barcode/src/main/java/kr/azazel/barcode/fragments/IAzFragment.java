package kr.azazel.barcode.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by JJ_Air on 2015-06-18.
 */
public interface IAzFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container);
    public void refreshView();
}
