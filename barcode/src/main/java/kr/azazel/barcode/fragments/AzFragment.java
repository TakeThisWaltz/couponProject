package kr.azazel.barcode.fragments;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.azazel.framework.AzApplication;
import com.azazel.framework.util.LOG;

import kr.azazel.barcode.AzAppConstants;
import kr.azazel.barcode.MainActivity;
import kr.azazel.barcode.R;
import kr.azazel.barcode.adapters.BarcodeAdapter;
import kr.azazel.barcode.adapters.ICursorAdapter;
import kr.azazel.barcode.vo.MyBarcode;

/**
 * Created by JJ_Air on 2015-06-18.
 */
public class AzFragment extends Fragment {
    private String TAG = "AzFragment";

    private MyBarcode.Category category;
    private View mView;

    private IAzFragment mFragment;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        category = (MyBarcode.Category)args.getSerializable("category");

        if (category == null) {
            LOG.i(TAG, "onCreate : argument is null, savedInstance : " + savedInstanceState.getInt("category"));
            category = (MyBarcode.Category)savedInstanceState.getSerializable("category");
        }
        TAG += "_" + category.displayString();

        LOG.f(TAG, "onCreate : category : " + category);

        mFragment = new BarcodeListFragment(category);
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
        LOG.i(TAG, "onSaveInstanceState : " + category);
        if (category != null) {
            outState.putSerializable("wall", category);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        LOG.f(TAG, "onCreateView : " + category);
        mView = mFragment.onCreateView(inflater, container);
        ListView listView = (ListView)mView.findViewById(R.id.v_list);
        BarcodeAdapter adapter = new BarcodeAdapter(this.getActivity(), this.hashCode(), Uri.withAppendedPath(AzAppConstants.URI.CATEGORY_LIST, category.value() + "/barcodes"), new ICursorAdapter.IDataLoadLisner() {
            @Override
            public void onDataLoadFinished(int dataCount) {
                LOG.d(TAG, "onDataLoadFinished - cnt : " + dataCount);
                mView.findViewById(R.id.tv_no_data).setVisibility(dataCount > 0 ? View.GONE : View.VISIBLE);

            }
        });
        listView.setAdapter(adapter.getAdapter());

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                LOG.d(TAG, "onScrollStateChanged - " + scrollState);
                if(scrollState == SCROLL_STATE_TOUCH_SCROLL)
                    AzApplication.sendEmptyMessage(MainActivity.TAG, AzAppConstants.Event.LISTVIEW_SCROLL_START);
                else if(scrollState == SCROLL_STATE_IDLE)
                    AzApplication.sendEmptyMessage(MainActivity.TAG, AzAppConstants.Event.LISTVIEW_SCROLL_END);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        return mView;
    }


}
