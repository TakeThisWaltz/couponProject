package kr.azazel.barcode.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.azazel.framework.util.LOG;

import kr.azazel.barcode.MetaManager;
import kr.azazel.barcode.R;

/**
 * Created by JJ_Air on 2015-06-18.
 */
public class SettingFragment extends Fragment {
    private String TAG = "SettingFragment";

    private View mView;
    private MetaManager mMeta;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMeta = MetaManager.getInstance();
        Bundle args = getArguments();


        LOG.f(TAG, "onCreate : category : ");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.i(TAG, "onDestroy : ");
        TAG = "ChatFragment";
        //mChatRoom = null;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LOG.i(TAG, "onSaveInstanceState : ");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.frag_setting, container, false);


        return mView;
    }


}
