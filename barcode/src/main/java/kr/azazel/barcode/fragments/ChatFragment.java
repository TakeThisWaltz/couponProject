//package kr.azazel.coupon.fragments;
//
//import android.content.Context;
//import android.database.Cursor;
//import android.net.Uri;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ListView;
//import android.widget.TextView;
//
//import com.azazel.framework.AzApplication;
//import com.azazel.framework.util.LOG;
//
//import kr.azazel.coupon.MetaManager;
//import kr.azazel.coupon.AzConstants;
//import kr.azazel.coupon.adapters.ICursorAdapter;
//import kr.azazel.coupon.adapters.MyCursorAdapter;
//
///**
// * Created by JJ_Air on 2015-06-18.
// */
//public class ChatFragment implements IAzFragment{
//    private final String TAG;
//    private final Wall mWall;
//    private View mView;
//
//    private MyCursorAdapter mAdapter;
//
//    private MetaManager mMeta;
//
//    public ChatFragment(Wall wall){
//        TAG = "FeedFragment_" + wall ;
//        mWall = wall;
//        mMeta = MetaManager.getInstance();
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container) {
//        LOG.i(TAG, "onCreateView : " + mWall + ", enable : " + mMeta.isServiceAvailable());
//        mView = inflater.inflate(R.layout.frag_chat, container, false);
//
//        TextView tvName = (TextView)mView.findViewById(R.id.tv_chat_frag_name);
//        tvName.setText("Chat : " + mWall.name);
//
//        if(mMeta.isServiceAvailable()){
//            mView.findViewById(R.id.layout_chat_input_view).setClickable(true);
//            mView.findViewById(R.id.et_chat_write).setEnabled(true);
//        }
//
//
//        mAdapter = new MyCursorAdapter(AzApplication.ACTIVATED_ACTIVITY, R.layout.frag_chat, Uri.withAppendedPath(AzConstants.URI.WALL_CHAT, mWall.id+""), new ICursorAdapter() {
//            @Override
//            public View newView(Context context, Cursor cursor, ViewGroup parent) {
//                return null;
//            }
//
//            @Override
//            public void bindView(View view, Context context, Cursor cursor) {
//
//            }
//        });
//
//        ((ListView)mView.findViewById(R.id.list_chat_view)).setAdapter(mAdapter.getAdapter());
//
//        return mView;
//    }
//
//    @Override
//    public void refreshView() {
//
//    }
//
//    private void enableViews(){
//
//    }
//}
