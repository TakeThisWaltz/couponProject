package kr.azazel.barcode.fragments;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.azazel.framework.AzApplication;
import com.azazel.framework.util.LOG;

import kr.azazel.barcode.R;
import kr.azazel.barcode.vo.MyBarcode;

/**
 * Created by JJ_Air on 2015-06-18.
 */
public class BarcodeListFragment implements IAzFragment {
    private final String TAG;
    private MyBarcode.Category category;

    private View mView;

    public BarcodeListFragment(MyBarcode.Category category) {
        TAG = "BarcodeListFragment_" + category.displayString();
        this.category = category;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container) {
        LOG.i(TAG, "onCreateView : " + category);
        mView = inflater.inflate(R.layout.layout_list, container, false);

//        final SwipeMenuListView listView = (SwipeMenuListView)mView.findViewById(R.id.v_list);
//
//        SwipeMenuCreator creator = new SwipeMenuCreator() {
//
//            @Override
//            public void create(SwipeMenu menu) {
//                // create "open" item
//                SwipeMenuItem openItem = new SwipeMenuItem(
//                        mView.getContext().getApplicationContext());
//                // set item background
//                openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
//                        0xCE)));
//                // set item width
//                openItem.setWidth(dp2px(90));
//                // set item title
//                openItem.setTitle("Open");
//                // set item title fontsize
//                openItem.setTitleSize(18);
//                // set item title font color
//                openItem.setTitleColor(Color.WHITE);
//                // add to menu
//                menu.addMenuItem(openItem);
//
//                // create "delete" item
//                SwipeMenuItem deleteItem = new SwipeMenuItem(
//                        mView.getContext().getApplicationContext());
//                // set item background
//                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
//                        0x3F, 0x25)));
//                // set item width
//                deleteItem.setWidth(dp2px(90));
//                // set a icon
//                deleteItem.setIcon(R.mipmap.ic_delete);
//                // add to menu
//                menu.addMenuItem(deleteItem);
//            }
//        };
//
//// set creator
//        listView.setMenuCreator(creator);
//
//        listView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                LOG.d(TAG, "onLongClick : " + v);
//                listView.smoothOpenMenu(listView.indexOfChild(v));
//                return true;
//            }
//        });

        return mView;
    }

    @Override
    public void refreshView() {

    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                AzApplication.APP_CONTEXT.getResources().getDisplayMetrics());
    }
}
