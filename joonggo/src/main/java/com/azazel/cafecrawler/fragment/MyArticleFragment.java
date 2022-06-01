package com.azazel.cafecrawler.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.azazel.cafecrawler.CrawlConstants;
import com.azazel.cafecrawler.CrawlUtil;
import com.azazel.cafecrawler.DialogUtil;
import com.azazel.cafecrawler.MetaManager;
import com.azazel.cafecrawler.R;
import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.framework.AzApplication;
import com.azazel.framework.AzSimpleWorker;
import com.azazel.framework.util.LOG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by JJ on 2015-03-18.
 */
public class MyArticleFragment extends Fragment implements ICrawlFragment, AdapterView.OnItemClickListener {
    private static final String TAG = "MyArticleFragment";

    private View mView;

    private ListView mListView;

    private List<CrawlDataHelper.Article> mArticleList;

    private ArticleAdapter mAdapter;

    @Override
    public String getTitle() {
        return "내 글";
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG.f(TAG, "onCreateView : ");

        mView = inflater.inflate(R.layout.frag_mine, container, false);

        mListView = (ListView) mView.findViewById(R.id.list_mine);
        mListView.setOnItemClickListener(this);

        mArticleList = new ArrayList<CrawlDataHelper.Article>();
        mAdapter = new ArticleAdapter(mView.getContext(), R.layout.search_list_item, R.id.tv_list_title, mArticleList, R.id.tv_list_title, R.id.tv_list_writer, R.id.img_list_thmb);
        mListView.setAdapter(mAdapter);


        loadMyArticle();

        return mView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final CrawlDataHelper.Article article = (CrawlDataHelper.Article) parent.getAdapter().getItem(position);
        DialogUtil.showArticleDetailDialog(this.getActivity(), article);
    }

    private void loadMyArticle() {
        LOG.i(TAG, "loadMyArticle ");

        AzApplication.executeJobOnBackground(new AzSimpleWorker(CrawlConstants.DEFAULT_NETWORK_TIMEOUT) {

            @Override
            public void doInBackgroundAndResult() {
                List<CrawlDataHelper.Article> articles = CrawlDataHelper.getInstance().getScrapList(CrawlConstants.ArticleType.MY_ARTICLE, false);
                Collections.sort(articles);

                this.setResult(true, articles);
            }

            @Override
            public void postOperationWithResult(boolean result, Object value) {
                if (result && mArticleList != null) {
                    List<CrawlDataHelper.Article> articles = (List<CrawlDataHelper.Article>) value;

                    mArticleList.clear();
                    mArticleList.addAll(articles);
                    mAdapter.notifyDataSetChanged();

                    mView.findViewById(R.id.layout_no_data).setVisibility(mArticleList.size() > 0 ? View.GONE : View.VISIBLE);
                }
            }

        });
    }

    @Override
    public void refreshView() {
        loadMyArticle();
    }


}
