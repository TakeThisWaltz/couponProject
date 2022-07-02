package kr.azazel.barcode.fragments;

import android.net.Uri;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.azazel.framework.AzApplication;
import com.azazel.framework.util.LOG;

import java.util.ArrayList;
import java.util.List;

import kr.azazel.barcode.AzAppConstants;
import kr.azazel.barcode.MainActivity;
import kr.azazel.barcode.MetaManager;
import kr.azazel.barcode.R;
import kr.azazel.barcode.adapters.BarcodeAdapter;
import kr.azazel.barcode.adapters.ICursorAdapter;
import kr.azazel.barcode.vo.BarcodeSort;
import kr.azazel.barcode.vo.MyBarcode;

/**
 * Created by JJ_Air on 2015-06-18.
 */
public class AzFragment extends Fragment {
    private String TAG = "AzFragment";

    private MyBarcode.Category category;
    private View mView;
    private MetaManager mMeta;

    private IAzFragment mFragment;

    private String searchKeyword;
    private BarcodeSort barcodeSort;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMeta = MetaManager.getInstance();
        Bundle args = getArguments();
        category = (MyBarcode.Category) args.getSerializable("category");

        if (category == null) {
            LOG.i(TAG, "onCreate : argument is null, savedInstance : " + savedInstanceState.getInt("category"));
            category = (MyBarcode.Category) savedInstanceState.getSerializable("category");
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
        ListView listView = (ListView) mView.findViewById(R.id.v_list);
        SearchView searchView = mView.findViewById(R.id.search_view);

        BarcodeAdapter adapter = new BarcodeAdapter(this.getActivity(), category.value(), Uri.withAppendedPath(AzAppConstants.URI.CATEGORY_LIST, category.value() + "/barcodes"), new ICursorAdapter.IDataLoadLisner() {
            @Override
            public void onDataLoadFinished(int dataCount) {
                LOG.d(TAG, "onDataLoadFinished - cnt : " + dataCount);
                mView.findViewById(R.id.tv_no_data).setVisibility(dataCount > 0 ? View.GONE : View.VISIBLE);

            }
        });
        listView.setAdapter(adapter.getAdapter());
        searchView.setTag(adapter);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchKeyword = query;
                adapter.getAdapter().getFilter().filter(searchKeyword + "|" + barcodeSort.query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(TextUtils.isEmpty(newText.trim())){
                    searchKeyword = "";
                    adapter.getAdapter().getFilter().filter(searchKeyword + "|" + barcodeSort.query);
                }
                return false;
            }
        });

        Spinner selSort = mView.findViewById(R.id.sel_sort);

        List<BarcodeSort> sortList = new ArrayList<>();
        for (BarcodeSort value : BarcodeSort.values()) {
            if (!(category != MyBarcode.Category.COUPON && value == BarcodeSort.EXPIRE_DATE)) {
                sortList.add(value);
            }
        }
        selSort.setTag(sortList);
        selSort.setAdapter(new ArrayAdapter<>(mView.getContext()
                , android.R.layout.simple_spinner_item, sortList));
        selSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                barcodeSort = ((List<BarcodeSort>) parent.getTag()).get(position);
                mMeta.setBarcodeSortValue(category, barcodeSort);
                adapter.getAdapter().getFilter().filter(searchKeyword + "|" + barcodeSort.query);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        barcodeSort = mMeta.getBarcodeSortValue(category);
        selSort.setSelection(sortList.indexOf(barcodeSort));


        //        searchView.setOnCreateContextMenuListener(v -> {
//            LOG.d(TAG, "search : " + searchView.getQuery());
//            BarcodeAdapter thisAdapter = (BarcodeAdapter)v.getTag();
//            thisAdapter.getAdapter().getFilter().filter(searchView.getQuery());
//        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                LOG.d(TAG, "onScrollStateChanged - " + scrollState);
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL)
                    AzApplication.sendEmptyMessage(MainActivity.TAG, AzAppConstants.Event.LISTVIEW_SCROLL_START);
                else if (scrollState == SCROLL_STATE_IDLE)
                    AzApplication.sendEmptyMessage(MainActivity.TAG, AzAppConstants.Event.LISTVIEW_SCROLL_END);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        return mView;
    }


}
