package com.azazel.cafecrawler.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.azazel.cafecrawler.AlarmManager;
import com.azazel.cafecrawler.CrawlConstants;
import com.azazel.cafecrawler.CrawlManager;
import com.azazel.cafecrawler.DialogUtil;
import com.azazel.cafecrawler.MainActivity;
import com.azazel.cafecrawler.MetaManager;
import com.azazel.cafecrawler.R;
import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.cafecrawler.data.CrawlDataHelper.Category;
import com.azazel.cafecrawler.data.CrawlDataHelper.Search;
import com.azazel.framework.AzApplication;
import com.azazel.framework.AzDefaultWorker;
import com.azazel.framework.AzRuntimeException;
import com.azazel.framework.AzSimpleWorker;
import com.azazel.framework.task.SingleTask;
import com.azazel.framework.task.TaskAsyncHelper;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;
import com.baoyz.widget.PullRefreshLayout;
import com.etsy.android.grid.StaggeredGridView;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by JI on 2015-03-17.
 */
public class HomeFragment extends Fragment implements ICrawlFragment, AdapterView.OnItemSelectedListener, View.OnClickListener, PullRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener {
    private static final String TAG = "HomeFragment";

    private MetaManager mMeta;
    private CrawlDataHelper mDataHelper;
    private AlarmManager mAlarmMgr;
    private CrawlManager mCrawlMgr;
    private MainActivity mMainActivity;

    private View mView;

    private Spinner mCategory;
    private PullRefreshLayout mGridFrame;
    private PullRefreshLayout mListFrame;
    //   private WebView mWebView;
    private ListView mListView;
    private StaggeredGridView mGridView;


    private EditText mEtKeyword;
    private int mSelectedPos;

    private List<Category> mCategoryList;
    private List<CrawlDataHelper.Article> mData;
    private ArticleAdapter mAdapter;

    private long mLastestArticleId;
    @Override
    public String getTitle() {
        return "홈";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG.f(TAG, "onCreateView : ");

        mMeta = MetaManager.getInstance();
        mDataHelper = CrawlDataHelper.getInstance();
        mAlarmMgr = AlarmManager.getInstance();
        mCrawlMgr = CrawlManager.getInstance();
        mMainActivity = (MainActivity) this.getActivity();


        mView = inflater.inflate(R.layout.frag_home, container, false);
//        mWebView = (WebView) mView.findViewById(R.id.web_detail);
        mGridFrame = (PullRefreshLayout) mView.findViewById(R.id.layout_grid_search);
        mGridFrame.setOnRefreshListener(this);
        mListFrame = (PullRefreshLayout) mView.findViewById(R.id.layout_list_search);
        mListFrame.setOnRefreshListener(this);

        mGridView = (StaggeredGridView) mView.findViewById(R.id.grid_search);
        mListView = (ListView) mView.findViewById(R.id.list_search);
        mGridView.setOnItemClickListener(this);
        mListView.setOnItemClickListener(this);


        mCategory = (Spinner) mView.findViewById(R.id.category_sel);

        mEtKeyword = (EditText) mView.findViewById(R.id.et_keyword);
        mEtKeyword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH){
                    String keyword = ((EditText) mView.findViewById(R.id.et_keyword)).getText().toString();
                    if(!AzUtil.isNullOrEmptyWithTrim(keyword))
                        searchPerform(keyword);
                }
                return false;
            }
        });

        mCategoryList = mMeta.getCategoryList();

        List<Category>  topList = mDataHelper.getCategoryTop(5);


        mCategoryList.addAll(1, topList);

        mView.findViewById(R.id.btn_search).setOnClickListener(this);
        mView.findViewById(R.id.btn_add).setOnClickListener(this);

        ArrayAdapter<Category> adapter = new ArrayAdapter<Category>(mView.getContext()
                , android.R.layout.simple_spinner_item, mCategoryList);
        mCategory.setAdapter(adapter);
        mCategory.setOnItemSelectedListener(HomeFragment.this);

        mEtKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mView.findViewById(R.id.btn_add).setVisibility(View.GONE);
                mView.findViewById(R.id.btn_search).setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mData = new ArrayList<CrawlDataHelper.Article>();
        mAdapter = new ArticleAdapter(mView.getContext(), R.layout.search_grid_item, R.id.tv_grid_title, mData, R.id.tv_grid_title, R.id.tv_grid_writer, R.id.img_grid_thmb);
        mGridView.setAdapter(mAdapter);

        loadRecentArticle();

        return mView;
    }

    private void loadRecentArticle() {
        showView(mGridView);
        AzApplication.executeJobOnBackground(new AzDefaultWorker(CrawlConstants.DEFAULT_NETWORK_TIMEOUT) {
            @Override
            public void preOperation() {
                mGridFrame.setRefreshing(true);
            }

            @Override
            public void doInBackgroundAndResult() {
                List<Search> keywords = mDataHelper.getObserveList(true);
                if (keywords.size() == 0)
                    publishProgress(0, 0, mCrawlMgr.recentList());
                else {
                    final List<CrawlDataHelper.Article> tmpResult = new ArrayList<CrawlDataHelper.Article>();
                    TaskAsyncHelper asyncHelper = new TaskAsyncHelper(CrawlConstants.THREAD_ASYNC_LIMIT, true);
                    for (final Search keyword : keywords) {
                        asyncHelper.addTask(new SingleTask() {
                            @Override
                            public Object doJob(Object[] args) {
                                List<CrawlDataHelper.Article> recent = mCrawlMgr.search(keyword, 1, 0);
                                if (mGridFrame.getVisibility() == View.VISIBLE) {
                                    publishProgress(0, 0, recent);
                                }
                                return null;
                            }
                        });
                    }

                    asyncHelper.runTask(new TaskAsyncHelper.TaskCompleteListener() {
                        @Override
                        public void onFinish(boolean isSuccess, Object[] resultArr, AzRuntimeException e) {
                            LOG.i(TAG, "onFinish asyncHelper : " + resultArr.length);
                        }
                    });
                }

                this.setResult(true, null);
            }

            @Override
            public void onProgress(long proc, long total, Object obj) {
                List<CrawlDataHelper.Article> result = (List<CrawlDataHelper.Article>) obj;
                LOG.i(TAG, "onProgress : " + result.size());
                for (CrawlDataHelper.Article article : result) {
                    int idx = mData.indexOf(article);
                    if (idx < 0) {
                        mData.add(article);
                        LOG.i(TAG, "new added : " + article);
                    } else if (mData.get(idx).compareTo(article) > 0) {
                        mData.remove(idx);
                        mData.add(article);
                        LOG.i(TAG, "new replaced : " + article);
                    }
                }

                Collections.sort(mData);
                mAdapter.notifyDataSetChanged();
            }


            @Override
            public void postOperationWithResult(boolean result, Object value) {
                mGridFrame.setRefreshing(false);
            }

        });
    }

    private void search(final String categoryId, final String keyword) {
        if (AzUtil.isNullOrEmptyWithTrim(keyword)) return;

        if (mGridFrame.getVisibility() == View.VISIBLE) {
            showView(mListView);
            mAdapter = null;
            mGridView.setAdapter(null);
        }

        mListFrame.setRefreshing(true);
        AzApplication.executeJobOnBackground(new AzSimpleWorker(CrawlConstants.DEFAULT_NETWORK_TIMEOUT) {

            @Override
            public void doInBackgroundAndResult() {
                List<CrawlDataHelper.Article> result = mCrawlMgr.search(categoryId, keyword, 1);

                this.setResult(true, result);
            }

            @Override
            public void postOperationWithResult(boolean result, Object value) {
                List<CrawlDataHelper.Article> articles = (List<CrawlDataHelper.Article>) value;

                if (mAdapter == null) {
                    mData = articles;
                    mAdapter = new ArticleAdapter(mView.getContext(), R.layout.search_list_item, R.id.tv_list_title, mData, R.id.tv_list_title, R.id.tv_list_writer, R.id.img_list_thmb);
                    mListView.setAdapter(mAdapter);
                } else {
                    if (mData.size() > 0) {
                        for (CrawlDataHelper.Article article : articles) {
                            int idx = mData.indexOf(article);
                            if (idx < 0)
                                mData.add(article);
                            else if (mData.get(idx).compareTo(article) > 0) {
                                mData.remove(idx);
                                mData.add(article);
                            }
                        }
                    } else mData.addAll(articles);
                }

                Collections.sort(mData);

                if(mData.size() > 0) mLastestArticleId = mData.get(0).articleId;

                mAdapter.notifyDataSetChanged();

                mListFrame.setRefreshing(false);
            }

        });
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
        mView.findViewById(R.id.btn_add).setVisibility(View.GONE);
        mView.findViewById(R.id.btn_search).setVisibility(View.VISIBLE);
        mSelectedPos = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        mSelectedPos = -1;
    }

    @Override
    public void onClick(View v) {
        String keyword = ((EditText) mView.findViewById(R.id.et_keyword)).getText().toString();
        if(AzUtil.isNullOrEmptyWithTrim(keyword)) return;
        switch (v.getId()) {
            case R.id.btn_search: {

               //mCrawlMgr.testReUp();
               // CrawlUtil.showWebViewPopup(HomeFragment.this.getActivity(), "http://cafe.naver.com/ArticleWrite.nhn?articleid=307140421&clubid=10050146&m=modify");
                searchPerform(keyword);

                break;
            }
            case R.id.btn_add: {
                if (mSelectedPos > -1) {
                    Search his = mAlarmMgr.addSearchKeyword(mCategoryList.get(mSelectedPos).categoryId, mCategoryList.get(mSelectedPos).title, keyword, mLastestArticleId);
                    if (his.id > -1) {
                        Toast.makeText(this.getActivity(), R.string.toast_new_keyword, Toast.LENGTH_SHORT).show();
                        mMainActivity.onAlarmAdded();
                    }
                }
                break;
            }
        }
    }

    private void searchPerform(String keyword) {
        String category = ((CrawlDataHelper.Category) mCategory.getSelectedItem()).categoryId;

        Bundle params = new Bundle();
        params.putString("keyword", keyword);
        params.putString("category", category);
        FirebaseAnalytics.getInstance(getActivity()).logEvent("search", params);

        mData.clear();

        mDataHelper.increaseCategoryHit(category);

        search(category, keyword);

        mView.findViewById(R.id.btn_add).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.btn_search).setVisibility(View.GONE);

        AzUtil.hideKeyboard(mEtKeyword);
    }

    @Override
    public void onRefresh() {
        if (mGridFrame.getVisibility() == View.VISIBLE)
            loadRecentArticle();
        else if (mListFrame.getVisibility() == View.VISIBLE) {
            String category = ((CrawlDataHelper.Category) mCategory.getSelectedItem()).categoryId;
            String keyword = ((EditText) mView.findViewById(R.id.et_keyword)).getText().toString();
            search(category, keyword);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //  mWebView.loadUrl("about:blank");
        final CrawlDataHelper.Article article = (CrawlDataHelper.Article) parent.getAdapter().getItem(position);
        DialogUtil.showArticleDetailDialog(this.getActivity(), article);

        //mWebView.loadUrl(String.format(CrawlConstants.Urls.DETAIL_VIEW, article.articleId));
        // showView(mWebView);
    }

    private void showView(View view) {
        LOG.f(TAG, "showView - GridView : " + (mGridView == view) + ", ListView : " + (mListView == view));
        //  mWebView.setVisibility((mWebView == view ? View.VISIBLE : View.GONE));
        mGridFrame.setVisibility((mGridView == view ? View.VISIBLE : View.GONE));
        mListFrame.setVisibility((mListView == view ? View.VISIBLE : View.GONE));
    }

    @Override
    public void refreshView() {

    }

}