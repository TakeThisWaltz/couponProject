package com.azazel.cafecrawler;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.TabLayout;
import androidx.viewpager.widget.ViewPager;
import android.view.KeyEvent;
import android.widget.Toast;

import com.azazel.cafecrawler.CrawlConstants.Event;
import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.cafecrawler.fragment.AlarmFragment;
import com.azazel.cafecrawler.fragment.HomeFragment;
import com.azazel.cafecrawler.fragment.ICrawlFragment;
import com.azazel.cafecrawler.fragment.MyArticleFragment;
import com.azazel.cafecrawler.fragment.ScrapFragment;
import com.azazel.cafecrawler.fragment.SettingFragment;
import com.azazel.framework.AzAppCompatActivity;
import com.azazel.framework.AzApplication;
import com.azazel.framework.util.LOG;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;


public class MainActivity extends AzAppCompatActivity {
    public static final String TAG = "MainActivity";

    private ICrawlFragment[] mFragmentList = new ICrawlFragment[]{new HomeFragment(), new AlarmFragment(), new ScrapFragment(), new MyArticleFragment(), new SettingFragment()};
    private static final int IDX_HOME = 0;
    private static final int IDX_ALARM = 1;
    private static final int IDX_SCRAP = 2;
    private static final int IDX_MINE = 3;
    private static final int IDX_SETTING = 4;


    private MetaManager mMeta;
    private CrawlManager mCrawlMgr;
    private ViewPager mViewPager;
    private CrawlDataHelper mDataHelper;
    private AdView mAdView;

    private FirebaseAnalytics mFirebaseAnalytics;


    @Override
    public boolean pauseListenWhenActivityOnPause() {
        return false;
    }

    @Override
    public String getName() {
        return TAG;
    }


    @Override
    public void onEventReceived(Message msg) {
        LOG.f(TAG, "onEventReceived : " + msg.what + ", obj : " + msg.obj);
        switch (msg.what) {
            case Event.EXIT: {
                LOG.i(TAG, "EXIT!!");
                finish();
                break;
            }
            case Event.INIT_START: {
                LOG.i(TAG, "INIT_START!!");
                initActivity();
                break;
            }
            case Event.SCRAP_CHANGED: {
                LOG.i(TAG, "SCRAP_CHANGED!!");
                onScrapChanged();
                break;
            }
            case Event.MINE_LOADED: {
                LOG.i(TAG, "MINE_LOADED!!");
                onMineLoaded();
                break;
            }
            case Event.TRY_EXIT: {
                LOG.i(TAG, "TRY_EXIT!!");
                exitFlag = false;
                break;
            }


        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        LOG.i(TAG, "onNewIntent : " + intent);
        //LOG.i(TAG, "onNewIntent - search : " + intent.getExtras().getLong("search_id", -1) + ", article : " + intent.getExtras().getLong("article_id", -1) + ", data : " + intent.getData());
        this.setIntent(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);


//        Bundle bundle = new Bundle();
//        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
//        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
//        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
//        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
//        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
//        analytics.setLocalDispatchPeriod(1800);
//
//        Tracker tracker = analytics.newTracker("UA-58216535-3"); // Replace with actual tracker/property Id
//        tracker.enableExceptionReporting(true);
//        tracker.enableAdvertisingIdCollection(true);
//        tracker.enableAutoActivityTracking(true);


        mMeta = MetaManager.getInstance();
        mCrawlMgr = CrawlManager.getInstance();
        mDataHelper = CrawlDataHelper.getInstance();
        CrawlUtil.initImageLoader(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mViewPager == null)
            startActivity(new Intent(this, SplashActivity.class));
        else {
            Intent intent = getIntent();
            LOG.i(TAG, "intent extras : " + intent.getExtras() + ", data : " + intent.getData());
            long searchId = intent.getLongExtra("search_id", -1);
            if (searchId > -1) {
                LOG.i(TAG, "search_id : " + searchId);
                CrawlDataHelper.Search search = mDataHelper.getSearch(searchId);
                DialogUtil.showSearchListDialog(this, search);
                intent.putExtra("search_id", (long) -1);
            }

            long articleId = intent.getLongExtra("article_id", -1);
            if (articleId > -1) {
                CrawlDataHelper.Article article = mDataHelper.getArticle(articleId);
                LOG.i(TAG, "article_id : " + articleId + ", article : " + article);
                if (article != null)
                    DialogUtil.showArticleDetailDialog(this, article);
                intent.putExtra("article_id", (long) -1);
            }

        }

        if (mAdView != null)
            mAdView.resume();

    }

    @Override
    public void onPause() {
        if (mAdView != null)
            mAdView.pause();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdView != null)
            mAdView.destroy();
    }


    private synchronized void initActivity() {
        LOG.f(TAG, "initActivity : " + mViewPager);
        if (mViewPager == null) {
            setContentView(R.layout.activity_main);

            mViewPager = (ViewPager) findViewById(R.id.layoutViewpager);
            mViewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), mFragmentList));
            mViewPager.setOffscreenPageLimit(mFragmentList.length);
            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(mViewPager);

//            for(int i=0;i<mFragmentList.length;i++){
//                tabLayout.getTabAt(i).setText(mFragmentList[i].getTitle());
//            }


            mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
    }

    public void onAlarmAdded() {
        mFragmentList[IDX_ALARM].refreshView();
    }

    public void onScrapChanged() {
        mFragmentList[IDX_SCRAP].refreshView();
    }

    public void onMineLoaded() {
        mFragmentList[IDX_SCRAP].refreshView();
        mFragmentList[IDX_MINE].refreshView();
    }


    private boolean exitFlag = false;

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (exitFlag == false) {
                Toast.makeText(this, R.string.toast_exit,
                        Toast.LENGTH_SHORT).show();
                exitFlag = true;
                AzApplication.sendEmptyMessageDelayed(TAG, Event.TRY_EXIT, 2000);
                return false;
            } else {
                finish();
                return false;
            }
        }

        return super.onKeyUp(keyCode, event);
    }


}
