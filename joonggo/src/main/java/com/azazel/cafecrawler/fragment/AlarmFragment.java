package com.azazel.cafecrawler.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.azazel.cafecrawler.AlarmManager;
import com.azazel.cafecrawler.CrawlConstants;
import com.azazel.cafecrawler.CrawlManager;
import com.azazel.cafecrawler.CrawlUtil;
import com.azazel.cafecrawler.DialogUtil;
import com.azazel.cafecrawler.MainActivity;
import com.azazel.cafecrawler.MetaManager;
import com.azazel.cafecrawler.R;
import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.framework.AzApplication;
import com.azazel.framework.AzSimpleWorker;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;
import com.azazel.framework.view.ImageViewSwitch;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JJ on 2015-03-18.
 */
public class AlarmFragment extends Fragment implements ICrawlFragment, AdapterView.OnItemClickListener {
    private static final String TAG = "AlarmFragment";

    private MetaManager mMeta;
    private CrawlDataHelper mDataHelper;
    private AlarmManager mAlarmMgr;
    private CrawlManager mCrawlMgr;
    private MainActivity mMainActivity;

    private View mView;

    private ListView mAlarmList;

    private ArrayList<CrawlDataHelper.Search> mSearchList;
    private ArrayAdapter<CrawlDataHelper.Search> mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG.f(TAG, "onCreateView : ");

        mMeta = MetaManager.getInstance();
        mDataHelper = CrawlDataHelper.getInstance();
        mCrawlMgr = CrawlManager.getInstance();
        mAlarmMgr = AlarmManager.getInstance();
        mMainActivity = (MainActivity) this.getActivity();


        mView = inflater.inflate(R.layout.frag_alarm, container, false);
        mAlarmList = (ListView) mView.findViewById(R.id.list_alarm);

        mSearchList = mDataHelper.getObserveList(false);
        if(mSearchList.size() == 0){
            mView.findViewById(R.id.layout_no_alarm).setVisibility(View.VISIBLE);
        }

        mAdapter = new AlarmAdapter(mView.getContext()
                , R.layout.alarm_list_item, R.id.tv_alarm_keyword, mSearchList);
        mAlarmList.setAdapter(mAdapter);
        mAlarmList.setOnItemClickListener(this);

        return mView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LOG.d(TAG, "onItemClick : " + position);
        final CrawlDataHelper.Search search = (CrawlDataHelper.Search) parent.getAdapter().getItem(position);
        DialogUtil.showSearchListDialog(this.getActivity(), search);
    }

    @Override
    public String getTitle() {
        return "알람";
    }

    @Override
    public void refreshView() {
        AzApplication.executeJobOnBackground(new AzSimpleWorker(CrawlConstants.DEFAULT_NETWORK_TIMEOUT) {
            @Override
            public void doInBackgroundAndResult() {
                List<CrawlDataHelper.Search> result = mDataHelper.getObserveList(false);

                mSearchList.clear();
                mSearchList.addAll(result);

                this.setResult(true, result);
            }

            @Override
            public void postOperationWithResult(boolean result, Object value) {
                mView.findViewById(R.id.layout_no_alarm).setVisibility(mSearchList.size() > 0 ? View.GONE : View.VISIBLE);

                mAdapter.notifyDataSetChanged();
            }

        });
    }

    class AlarmAdapter extends ArrayAdapter<CrawlDataHelper.Search> {
        public AlarmAdapter(Context context, int resource, int textResId, List<CrawlDataHelper.Search> data) {
            super(context, resource, textResId, data);
            LOG.f(TAG, "init AlarmAdapter ");
        }

        class ViewHolder {
            TextView category;
            TextView keyword;
            ImageViewSwitch chkAlarm;
            View btnDel;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ViewHolder holder = (ViewHolder) view.getTag();

            if (holder == null) {
                holder = new ViewHolder();
                holder.category = (TextView) view.findViewById(R.id.tv_alarm_category);
                holder.keyword = (TextView) view.findViewById(R.id.tv_alarm_keyword);
                holder.chkAlarm = new ImageViewSwitch((ImageView) view.findViewById(R.id.chk_alarm_item), new ImageViewSwitch.OnImageCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(ImageViewSwitch view, boolean isChecked) {
                        CrawlDataHelper.Search item = (CrawlDataHelper.Search) view.getTag();
                        if (item.alarm != isChecked) {
                            item.alarm = isChecked;
                            mAlarmMgr.setSearchAlarm(item.id, item.alarm);
                            Toast.makeText(AzApplication.APP_CONTEXT, isChecked ? R.string.toast_article_alarm_on : R.string.toast_article_alarm_off, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                holder.btnDel = view.findViewById(R.id.btn_alarm_delete);
                view.setTag(holder);

                holder.btnDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CrawlDataHelper.Search item = (CrawlDataHelper.Search) v.getTag();
                        mAlarmMgr.deleteKeyword(item.id);
                        refreshView();
                    }
                });
            }

            CrawlDataHelper.Search item = getItem(position);
            if (AzUtil.isNullOrEmpty(item.categoryId)) holder.category.setVisibility(View.GONE);
            else {
                holder.category.setVisibility(View.VISIBLE);
                holder.category.setText(item.categoryTitle);
            }
            holder.keyword.setText(item.keyword);
            holder.chkAlarm.setTag(item);
            holder.btnDel.setTag(item);
            holder.chkAlarm.setChecked(item.alarm);


            return view;
        }
    }
}
