package com.azazel.cafecrawler;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.IBinder;
import android.os.Message;

import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.cafecrawler.data.CrawlDataHelper.Article;
import com.azazel.cafecrawler.data.CrawlDataHelper.Search;
import com.azazel.framework.AzApplication;
import com.azazel.framework.AzRuntimeException;
import com.azazel.framework.AzService;
import com.azazel.framework.AzSimpleWorker;
import com.azazel.framework.task.SingleTask;
import com.azazel.framework.task.TaskAsyncHelper;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.FileTool;
import com.azazel.framework.util.LOG;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrawlService extends AzService {
    public static final String TAG = "CrawlService";

    private MetaManager mMeta;
    private CrawlManager mCrawlMgr;
    private AlarmManager mAlarmMgr;
    private CrawlDataHelper mDataHelper;

    private boolean mIsObserving;
    private boolean mIsReUploading;
    private boolean mIsInitializing;

    private Map<Long, Integer> mNewArticleCntMap;

    @Override
    public void onCreate() {
        super.onCreate();
        mMeta = MetaManager.getInstance();
        mCrawlMgr = CrawlManager.getInstance();
        mDataHelper = CrawlDataHelper.getInstance();
        mAlarmMgr = AlarmManager.getInstance();
        mNewArticleCntMap = new HashMap<Long, Integer>();

        runVisitingBot();
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public synchronized void onEventReceived(Message msg) {
        LOG.f(TAG, "onEventReceived : " + msg.what + ", obj : " + msg.obj);
        switch (msg.what) {
            case CrawlConstants.Event.LOGIN_COMPLETED: {
                LOG.i(TAG, "LOGIN_COMPLETED!!");


                AzApplication.sendEmptyMessage(MainActivity.TAG, CrawlConstants.Event.INIT_START);
                AzApplication.sendEmptyMessage(SplashActivity.TAG, CrawlConstants.Event.EXIT);

                loadMyArticles();

                break;
            }

            case CrawlConstants.Event.REFRESH_MY_ARTICLE: {
                LOG.i(TAG, "REFRESH_MY_ARTICLE!!");
                loadMyArticles();

                break;
            }

            case CrawlConstants.Event.LOGIN_FAILED: {
                LOG.i(TAG, "LOGIN_FAILED!!");

                mIsInitializing = false;
                AzApplication.sendEmptyMessage(SplashActivity.TAG, CrawlConstants.Event.EXIT);
                AzApplication.sendEmptyMessage(MainActivity.TAG, CrawlConstants.Event.EXIT);
                break;
            }

            case CrawlConstants.Event.TIME_OUT: {
                LOG.i(TAG, "TIME_OUT!! : mIsInitializing" + mIsInitializing);

                if(mIsInitializing) {
                    mIsInitializing = false;
                    startApp();
                }

                break;
            }


        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.f(TAG, "onStartCommand : " + (intent != null ? intent.getAction() : "null"));

        if (CrawlConstants.Intents.START_APP.equals(intent.getAction()))
            startApp();
        else if (CrawlConstants.Intents.OBSERVING.equals(intent.getAction()))
            observing();
        else if (CrawlConstants.Intents.RE_UPLOAD.equals(intent.getAction()))
            reUpload();

        return START_NOT_STICKY;
    }

    private void startApp() {
        LOG.f(TAG, "startApp - isInitializing : " + mIsInitializing);
        if (mIsInitializing) return;

        mIsInitializing = true;

        if (!mMeta.isCategoryLoaded()) {
            loadCategory();
        } else {
            mCrawlMgr.checkLogin();
        }
    }


    private void loadMyArticles() {
        LOG.f(TAG, "loadMyArticles - mIsInitializing : " + mIsInitializing);

        AzApplication.executeJobOnBackground(new AzSimpleWorker(CrawlConstants.DEFAULT_NETWORK_TIMEOUT) {
            @Override
            public void doInBackgroundAndResult() {

                TaskAsyncHelper asyncHelper = new TaskAsyncHelper(CrawlConstants.THREAD_ASYNC_LIMIT, true);

                asyncHelper.addTask(new SingleTask() {
                    @Override
                    public Object doJob(Object[] args) {
                        final List<Article> list = mCrawlMgr.getMyArticle(mMeta.getUserId());

                        return list;
                    }
                });
                asyncHelper.addTask(new SingleTask() {
                    @Override
                    public Object doJob(Object[] args) {
//                        final List<Article> list = mCrawlMgr.getMyComment(mMeta.getUserId());
//
//                        return list;
                        return new ArrayList<Article>();
                    }
                });

                asyncHelper.runTask(new TaskAsyncHelper.TaskCompleteListener() {
                    @Override
                    public void onFinish(boolean isSuccess, Object[] resultArr, AzRuntimeException e) {
                        LOG.i(TAG, "loadMyArticles - get list finished : " + resultArr.length);
                        final List<Article> myArticles = (List<Article>) resultArr[0];
                        List<Article> comment = (List<Article>) resultArr[1];

                        if (myArticles != null && comment != null) {
                            for (Article item : comment) {
                                if (!myArticles.contains(item)) myArticles.add(item);
                            }

                            TaskAsyncHelper asyncHelperMy = new TaskAsyncHelper(CrawlConstants.THREAD_ASYNC_LIMIT, true);
                            for (final Article item : myArticles) {

                                asyncHelperMy.addTask(new SingleTask() {
                                    @Override
                                    public Object doJob(Object[] args) {
                                        Article current = mDataHelper.getArticle(item.articleId);

                                        if (current != null) {
                                            Article updated = mCrawlMgr.getArticle(current);
                                            if (current.alarm && current.comment > 0 && updated.comment > current.comment) {
                                                CrawlUtil.showNewCommentNotification(updated, updated.comment - current.comment, mMeta.isVibrationAlarm());
                                                updated.isRead = false;
                                            }
                                            mDataHelper.updateScrap(updated);
                                        } else {
//                                            Article updated = mCrawlMgr.getArticle(item);
                                            mAlarmMgr.addScrap(item);
                                        }

                                        return null;
                                    }
                                });
                            }

                            asyncHelperMy.runTask(new TaskAsyncHelper.TaskCompleteListener() {
                                @Override
                                public void onFinish(boolean isSuccess, Object[] result, AzRuntimeException e) {
                                    LOG.i(TAG, "getArticles - onFinish : " + isSuccess);
                                    if (isSuccess)
                                        mDataHelper.initMyArticles(myArticles);
                                }
                            });

                        }


                        setResult(isSuccess, null);
                    }
                });

            }

            @Override
            public void postOperationWithResult(boolean result, Object value) {
                LOG.i(TAG, "loadMyArticles finished : " + result);
                if (result)
                    AzApplication.sendEmptyMessage(MainActivity.TAG, CrawlConstants.Event.MINE_LOADED);

                if (mDataHelper.isObserving())
                    mAlarmMgr.setObservingAlarm();

                if (mDataHelper.isReUp())
                    mAlarmMgr.setReUpAlarm();


                mIsInitializing = false;
            }

        });


    }

    private boolean isNewAdded(long searchId, int curCnt) {
        if (mNewArticleCntMap.containsKey(searchId)) {
            boolean result = curCnt > mNewArticleCntMap.get(searchId);
            mNewArticleCntMap.put(searchId, curCnt);
            return result;
        } else {
            mNewArticleCntMap.put(searchId, curCnt);
            return curCnt > 0;
        }
    }

    private void observing() {
        LOG.f(TAG, "observing isObservingKeyword : " + mDataHelper.isObservingKeyword() + ", isObservingScrap : " + mDataHelper.isObservingScrap()
                + ", isObservingMine : " + mDataHelper.isObservingMine() + ", onGoing : " + mIsObserving);
        if (!mIsObserving) {
            mIsObserving = true;
            AzApplication.executeJobOnBackground(new AzSimpleWorker(CrawlConstants.OBSERVING_INTERVAL - 10000) {
                @Override
                public void doInBackgroundAndResult() {

                    TaskAsyncHelper asyncHelper = new TaskAsyncHelper(CrawlConstants.THREAD_ASYNC_LIMIT, false);

                    if (mDataHelper.isObservingKeyword()) {
                        final List<Search> list = mDataHelper.getObserveList(true);
                        for (final Search search : list) {

                            asyncHelper.addTask(new SingleTask() {
                                @Override
                                public Object doJob(Object[] args) {
                                    List<Article> result = mCrawlMgr.search(search, 1, search.newArticle);
                                    LOG.i(TAG, "observing - keyword : " + search.keyword + ", result : " + result.size());
                                    if (isNewAdded(search.id, result.size()))
                                        CrawlUtil.showNewNotification(search, result.size(), result.get(result.size() - 1), mMeta.isVibrationAlarm());

                                    return null;
                                }
                            });
                        }
                    }

                    if (mDataHelper.isObservingScrap()) {
                        final List<Article> scrap = mDataHelper.getScrapList(CrawlConstants.ArticleType.SCRAP, true);
                        scrap.addAll(mDataHelper.getScrapList(CrawlConstants.ArticleType.MY_COMMENT, true));
                        for (final Article article : scrap) {
                            if (!article.isDeleted) {
                                asyncHelper.addTask(new SingleTask() {
                                    @Override
                                    public Object doJob(Object[] args) {
                                        Article result = mCrawlMgr.getArticle(article);
                                        LOG.i(TAG, "observing comment : " + result.articleId + ", title : " + result.title + ", result : " + result.comment);
                                        if (result.isDeleted) {
                                            mDataHelper.setScrapDeleted(article.articleId);
                                        } else if (result.comment > article.comment) {
                                            result.isRead = false;
                                            mDataHelper.setIsReadScrap(result);
                                            CrawlUtil.showNewCommentNotification(result, result.comment - article.comment, mMeta.isVibrationAlarm());
                                        }

                                        return null;
                                    }
                                });
                            }
                        }
                    }

                    if (mDataHelper.isObservingMine()) {
                        final List<Article> scrap = mDataHelper.getScrapList(CrawlConstants.ArticleType.MY_ARTICLE, true);
                        for (final Article article : scrap) {

                            asyncHelper.addTask(new SingleTask() {
                                @Override
                                public Object doJob(Object[] args) {
                                    Article result = mCrawlMgr.getArticle(article);
                                    LOG.i(TAG, "observing comment : " + result.articleId + ", title : " + result.title + ", result : " + result.comment);
                                    if (result.comment > article.comment) {
                                        result.isRead = false;
                                        mDataHelper.setIsReadScrap(result);
                                        CrawlUtil.showNewCommentNotification(result, result.comment - article.comment, mMeta.isVibrationAlarm());
                                    }

                                    return null;
                                }
                            });
                        }
                    }


                    asyncHelper.runTask(new TaskAsyncHelper.TaskCompleteListener() {
                        @Override
                        public void onFinish(boolean isSuccess, Object[] result, AzRuntimeException e) {
                            LOG.f(TAG, "onFinish Observing tasks.. ");
                            mIsObserving = false;
                            if (mDataHelper.isObserving())
                                mAlarmMgr.setObservingAlarm();
                        }
                    });

                }

                @Override
                public void postOperationWithResult(boolean result, Object value) {
                }

            });
        }

    }

    private void runVisitingBot(){

//        AutoCrawlManager autoCrawl = AutoCrawlManager.getInstance(this.getApplicationContext());
//
//        com.pangpang.autocrawl.LOG.LOG_LEVEL = AzApplication.APP_CONFIG.isLogEnabled()
//                ?com.pangpang.autocrawl.LOG.VERBOSE:com.pangpang.autocrawl.LOG.DISABLED;
//
//        autoCrawl.doCrawl(new com.pangpang.autocrawl.IAutoCrawler.CrawlListener() {
//            @Override
//            public void onCrawlStarted(WebView webView) {
//                Log.d("TAG", "onCrawlStarted ");
//            }
//
//            @Override
//            public void onCrawlCompleted(long duration, long nextSchedule) {
//                Log.d(TAG, "onCrawlCompleted : " + duration + ", next : " + nextSchedule);
//            }
//        });
    }

    private void reUpload() {
        LOG.f(TAG, "reUpload Setting : " + mDataHelper.isReUp() + ", onGoing : " + mIsReUploading);
        if (!mIsReUploading) {
            mIsReUploading = true;
            long condition = System.currentTimeMillis() - (mMeta.getReUpInterval() * CrawlConstants.RE_UPLOAD_INTERVAL);
            //long condition = System.currentTimeMillis();
            final List<Article> scrap = mDataHelper.getToReUploadList(condition);
            for (final Article article : scrap) {
                LOG.i(TAG, "reUp target - article : " + article.articleId + ", lastReUp : " + AzUtil.getDateStringFromMils(AzApplication.APP_CONTEXT, article.autoReUpload, true));
                mCrawlMgr.reUp(article);
            }

            mIsReUploading = false;
        }
    }

    private void loadCategory() {

        AzApplication.executeJobOnBackground(new AzSimpleWorker(60000) {
            boolean hasMore = true;
            boolean isSuc = true;

            @Override
            public void doInBackgroundAndResult() {
                mDataHelper.beginTransaction();
                try {
                    AssetManager assets = getAssets();
                    InputStream fis = assets.open("categoryList.json");
                    String categoryList = FileTool.toString(fis);

                    JSONArray arr = new JSONArray(categoryList);

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject json = arr.getJSONObject(i);
                        mDataHelper.insertCategory(json.getString("id"), json.getString("title"));
                    }
                } catch (Exception e) {
                    LOG.e(TAG, "error load category", e);
                    isSuc = false;
                }
                mDataHelper.endTransaction(isSuc);
                this.setResult(true && isSuc, null);
            }

            @Override
            public void postOperationWithResult(boolean result, Object value) {
                if (!result){
//                    mMeta.categoryLoaded(true);
//                }else {
                    mDataHelper.resetCategoryList();
                }

                mIsInitializing = false;
                startApp();
            }

        });

    }

}
