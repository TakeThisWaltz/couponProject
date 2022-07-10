package com.azazel.cafecrawler;

import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.cafecrawler.data.CrawlDataHelper.Article;
import com.azazel.cafecrawler.data.CrawlDataHelper.Search;
import com.azazel.framework.AzApplication;
import com.azazel.framework.network.AzHttpRequestConfig;
import com.azazel.framework.network.HttpRequestBuilder;
import com.azazel.framework.network.NetworkUtil;
import com.azazel.framework.network.NetworkUtil.StringResponseHandler;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.DelayedWebViewClient;
import com.azazel.framework.util.LOG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CrawlManager {
    public static final String TAG = "CrawlManager";

    private MetaManager mMeta;
    private CrawlDataHelper mDataHelper;

    private boolean loginRequestFinished;

    private static final CrawlManager INSTANCE = new CrawlManager();

    public static CrawlManager getInstance() {
        return INSTANCE;
    }

    private CrawlManager() {
        mMeta = MetaManager.getInstance();
        mDataHelper = CrawlDataHelper.getInstance();
    }


    public List<Article> search(final Search search, final int page, long lastArticle) {
        if (search == null) return search(null, null, page, lastArticle);
        else return search(search.categoryId, search.keyword, page, lastArticle);
    }

    public List<Article> search(final String categoryId, final String keyword, final int page) {
        return search(categoryId, keyword, page, 0);
    }

    public List<Article> search(final String categoryId, final String keyword, final int page, final long lastArticle) {
        LOG.f(TAG, "search - category : " + categoryId + ", keyword : " + keyword + ", page : " + page + ", last : " + lastArticle);

        final List<Article> result = new ArrayList<Article>();
        String url = null;
        if (TextUtils.isEmpty(keyword))
            return recentList();
        else
            url = String.format(CrawlConstants.Urls.SEARCH, categoryId, URLEncoder.encode(keyword), page);

        LOG.d(TAG, "url : " + url);
        HttpRequestBuilder.create(TAG, url, AzHttpRequestConfig.HTTP_AZ_CONFIG_WITH_COOKIE)
                .execute(new NetworkUtil.JSONResponseHandler() {
                    @Override
                    public void handleResponse(int statusCode, JSONObject body) throws JSONException {
                        if (statusCode == 200) {
                            JSONArray items = body.getJSONObject("message").getJSONObject("result").getJSONArray("articleList");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject json = (JSONObject) items.get(i);
                                if (json.getString("type").equals("ARTICLE")) {
                                    JSONObject item = json.getJSONObject("item");
                                    long articleId = item.getLong("articleId");
                                    String title = item.getString("subject");
                                    String date = item.getString("currentSecTime");
                                    String writer = item.getString("memberNickName");
                                    String thumb = item.has("thumbnailImageUrl") ? item.getString("thumbnailImageUrl") : null;

                                    Article article = new Article(articleId, title, writer, thumb, date, CrawlConstants.ArticleType.SEARCH_RESULT);
                                    LOG.i(TAG, "recentList : " + article);
                                    if (!result.contains(article))
                                        result.add(article);
                                }
                            }
                        }
                    }
                });


        return result;//this.setResult(true, result);
    }

    public List<Article> recentList() {
        LOG.f(TAG, "search - recentList ");
        final List<Article> result = new ArrayList<Article>();
        String url = String.format(CrawlConstants.Urls.ALL_LIST, 1);
        HttpRequestBuilder.create(url, url, AzHttpRequestConfig.HTTP_AZ_CONFIG_WITH_COOKIE)
                .execute(new NetworkUtil.JSONResponseHandler() {
                    @Override
                    public void handleResponse(int statusCode, JSONObject body) throws JSONException {
                        if (statusCode == 200) {
                            JSONArray items = body.getJSONObject("message").getJSONObject("result").getJSONArray("articleList");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = (JSONObject) items.get(i);
                                long articleId = item.getLong("articleId");
                                String title = item.getString("subject");
                                String date = item.getLong("writeDateTimestamp") + "";
                                if (item.has("writeDateTimestamp")) {
                                    date = LocalDateTime.ofInstant(Instant.ofEpochMilli(item.getLong("writeDateTimestamp")), TimeZone.getDefault().toZoneId()).toLocalDate().toString();
                                }
                                String writer = item.getString("writerId");
                                String thumb = item.has("representImage") ? item.getString("representImage") : null;

                                Article article = new Article(articleId, title, writer, thumb, date, CrawlConstants.ArticleType.SEARCH_RESULT);
                                LOG.i(TAG, "recentList : " + article);
                                if (!result.contains(article))
                                    result.add(article);
                            }
                        }
                    }
                });


        return result;//this.setResult(true, result);
    }

    public List<Article> getMyArticle(final String userId) {
        LOG.d(TAG, "getMyArticle id : " + userId);
        final List<Article> result = new ArrayList<Article>();

        final int[] page = {1, 0};
//        do {
        HttpRequestBuilder.create(TAG, String.format(CrawlConstants.Urls.MY_ARTICLE, URLEncoder.encode(userId), page[0]), AzHttpRequestConfig.HTTP_AZ_CONFIG_WITH_COOKIE)
                .setMethod("POST")
                .execute(new NetworkUtil.JSONResponseHandler() {
                    @Override
                    public void handleResponse(int statusCode, JSONObject body) throws JSONException {
                        if (statusCode == 200) {
                            JSONArray items = body.getJSONObject("message").getJSONObject("result").getJSONArray("articleList");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);

                                long articleId = item.getLong("articleid");
                                String title = item.getString("subject").replaceAll("<(/)?[^>]+>", "").replaceAll("\r\n|\n|\r", "");
                                String date = item.getString("writedt").replaceAll("<(/)?[^>]+>", "").replaceAll("\r\n|\n|\r", "");
                                String comment = item.getString("commentcount");
                                String thumb = item.has("productSale") && item.getJSONObject("productSale").has("imgUrl") ? item.getJSONObject("productSale").getString("imgUrl") : null;
                                Article my = new Article(articleId, title, "", null, date, CrawlConstants.ArticleType.MY_ARTICLE);
                                if (comment != null && !comment.isEmpty())
                                    my.comment = Integer.parseInt(comment);
                                result.add(my);
                                LOG.i(TAG, "getMyArticle : " + my);
                            }
//                                if (page[1] == 0) {
//                                    page[1] = doc.select("div.prev-next tr>td").size();
//                                    LOG.i(TAG, "getMyArticle - total page : " + page[1]);
//                                }
//
//                                page[0]++;
                        }
                    }
                });

//        } while (page[0] <= page[1]);
        return result;

    }

    public List<Article> getMyComment(final String userId) {
        LOG.d(TAG, "getMyComment id : " + userId);
        final List<Article> result = new ArrayList<Article>();

        final int[] page = {1, 0};
        do {
            HttpRequestBuilder.create(TAG, String.format(CrawlConstants.Urls.MY_COMMENT, URLEncoder.encode(userId), page[0]), AzHttpRequestConfig.HTTP_AZ_CONFIG_WITH_COOKIE)
                    .execute(new StringResponseHandler() {

                        @Override
                        public void handleResponse(int status, String body) {
                            //LOG.i(TAG, "category : " + body);
                            Document doc = Jsoup.parse(body);
                            Elements rows = doc.select("div.article-board>table>tbody>tr[align=center]");//doc.select("a[href^=javascript:goArticle]");
                            for (int i = 1; i < rows.size(); i++) {
                                Element row = rows.get(i);
                                LOG.i(TAG, "row : " + row);
                                String articleStr = row.select("td span.m-tcol-c").html();
                                long articleId = Long.parseLong(articleStr);
                                String title = row.select("td.board-list>span>a").html().replaceAll("<(/)?[^>]+>", "").replaceAll("\r\n|\n|\r", "");
                                String date = row.select("td.view-count").get(0).html();
                                String writer = row.select("td>div.pers_nick_area a>div.m-tcol-c").html();
                                String comment = row.select("td.board-list>a>span>strong").html();
                                Article my = new Article(articleId, title, writer, null, date, CrawlConstants.ArticleType.MY_COMMENT);
                                if (comment != null && !comment.isEmpty())
                                    my.comment = Integer.parseInt(comment);
                                result.add(my);
                                LOG.i(TAG, "getMyComment : " + my);

                            }

                            if (page[1] == 0) {
                                page[1] = doc.select("div.prev-next tr>td").size();
                                LOG.i(TAG, "getMyComment - total page : " + page[1]);
                            }

                            page[0]++;

                        }
                    });
        } while (page[0] <= page[1]);

        return result;

    }

    public List<Article> getMyComment2(final String userId) {
        LOG.d(TAG, "getMyComment id : " + userId);
        final List<Article> result = new ArrayList<Article>();

        final int[] page = {1, 0};
        do {
            HttpRequestBuilder.create(TAG, String.format(CrawlConstants.Urls.MY_COMMENT2, URLEncoder.encode(userId), URLEncoder.encode(userId), URLEncoder.encode(userId), page[0]), AzHttpRequestConfig.HTTP_AZ_CONFIG_WITH_COOKIE)
                    .execute(new StringResponseHandler() {

                        @Override
                        public void handleResponse(int status, String body) {
                            //LOG.i(TAG, "category : " + body);
                            Document doc = Jsoup.parse(body);
                            Elements rows = doc.select("div.article-board>table>tbody>tr[align=center]");//doc.select("a[href^=javascript:goArticle]");
                            for (int i = 1; i < rows.size(); i++) {
                                Element row = rows.get(i);
                                LOG.i(TAG, "row : " + row);
                                String articleStr = row.select("td span.m-tcol-c").html();
                                long articleId = Long.parseLong(articleStr);
                                String title = row.select("td.v_top a").attr("title").replaceAll("<(/)?[^>]+>", "").replaceAll("\r\n|\n|\r", "");

                                Article my = new Article(articleId, title, CrawlConstants.ArticleType.MY_COMMENT);

                                result.add(my);
                                LOG.i(TAG, "getMyComment : " + my);

                            }

                            if (page[1] == 0) {
                                page[1] = doc.select("div.prev-next tr>td").size();
                                LOG.i(TAG, "getMyComment - total page : " + page[1]);
                            }

                            page[0]++;

                        }
                    });
        } while (page[0] <= page[1]);

        return result;

    }


    public Article getArticle(final Article article) {
        LOG.i(TAG, "getArticle : " + article);
        final Article result = new Article(article.articleId, article.title, article.writer, article.thumb, article.date, article.comment, article.isRead, article.type, article.alarm);
        HttpRequestBuilder.create(TAG, String.format(CrawlConstants.Urls.COMMENT_VIEW, article.articleId), AzHttpRequestConfig.HTTP_AZ_CONFIG_WITH_COOKIE)
                .execute(new StringResponseHandler() {

                    @Override
                    public void handleResponse(int status, String body) {
                        LOG.i(TAG, "category : " + body);
                        Document doc = Jsoup.parse(body);
                        Element main = doc.getElementById("ct");
                        if (main == null) {
                            LOG.i(TAG, "invalid request .. id : " + article.articleId);
                            return;
                        }
                        if (main.hasClass("error_content")) {
                            LOG.i(TAG, "invalid article .. id : " + article.articleId);
                            if (main.toString().contains(AzApplication.APP_CONTEXT.getString(R.string.str_deleted)))
                                result.isDeleted = true;
                            return;
                        }
                        String title = main.select("div.main_text>h2>span").html().replaceAll("\r\n|\n|\r", "");
                        //String writer = row.select("a.nick").html();
                        //String date = main.select("span.if_inr>span.im").get(0).childNode(5).toString();
                        String comment = doc.select("div.cafe_name>h1").html().replaceAll("(댓글)|\r\n|\n|\r", "").trim();
                        result.title = title;
                        //result.writer = writer;
                        //result.info = result.writer + " | " + "[" + comment + "]";
                        result.comment = Integer.parseInt(comment);
                        LOG.i(TAG, "getArticle result : " + result);
                    }
                });

        return result;
    }

    public void checkLogin() {
        LOG.f(TAG, "checkLogin - loginRequestFinished : " + loginRequestFinished + ", id : " + mMeta.getUserId());

        if (TextUtils.isEmpty(mMeta.getUserId())) {
            LOG.f(TAG, "user id is empty.. request login..");
            AzApplication.sendEmptyMessage(SplashActivity.TAG, CrawlConstants.Event.REQUEST_LOGIN);
            return;
        }

        loginRequestFinished = false;

        final WebView webView = new WebView(AzApplication.APP_CONTEXT);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            boolean mLoggedIn = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                LOG.f(TAG, "onPageFinished url : " + url + ", isCorrect : " + (view == webView));
                if (view != webView) return;

                loginRequestFinished = true;

                if (url.contains("login.login")) {
                    LOG.f(TAG, "request login..");
                    webView.loadUrl("about:blank");
                    AzApplication.sendEmptyMessage(SplashActivity.TAG, CrawlConstants.Event.REQUEST_LOGIN);

                } else if (!mLoggedIn && url.equals("https://m.cafe.naver.com/cafe-home/cafes/join")) {
                    mLoggedIn = true;
                    onLoginCompleted(null);
                    webView.loadUrl("about:blank");
                }

                webView.setWebViewClient(null);
            }
        });

        webView.loadUrl(CrawlConstants.Urls.LOGIN_CHECK);

        AzApplication.executeUIJob(new Runnable() {
            @Override
            public void run() {
                LOG.f(TAG, "LOGIN TIMEOUT - loginRequestFinished : " + loginRequestFinished + ", enable : " + webView.isEnabled() + ", " + webView.isActivated());
                if (!loginRequestFinished)
                    checkLogin();
            }
        }, 30000);
    }

    public void testReUp() {
        LOG.d(TAG, "testReUp");
        long condition = System.currentTimeMillis();
        final List<Article> scrap = mDataHelper.getToReUploadList(condition);
        for (final Article article : scrap) {
            LOG.i(TAG, "reUp target - article : " + article.articleId + ", lastReUp : " + AzUtil.getDateStringFromMils(AzApplication.APP_CONTEXT, article.autoReUpload, true));
            reUp(article);
        }
    }

    public void reUp(final Article article) {
        reUp(article, false);
    }

    public void reUp(final Article article, final boolean instant) {
        LOG.i(TAG, "reUp : " + article.articleId);

        mDataHelper.updateAutoReUploadProcessing(article.articleId);

        final WebView webView = new WebView(AzApplication.APP_CONTEXT);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.addJavascriptInterface(new CrawlManager.MyJavaScriptInterface() {
            @Override
            @JavascriptInterface
            public void receiveFromWeb(String value) {
                LOG.i(TAG, "receiveFromWeb : " + value);
            }
        }, "ActiveXObject");

        webView.setWebViewClient(new DelayedWebViewClient(1000, 5000) {
            @Override
            public void onDelayedPageFinished(WebView view, String url) {

                LOG.i(TAG, "reUp - onPageFinished [" + article.articleId + "] url : " + url);
                if (url.contains("ArticleWrite.nhn"))
                    view.loadUrl("javascript:sWriteMode = \"write\";document.querySelector(\"input[name='articleid']\").value = \"\";oCafeWrite.preCafeWriteContents();setTimeout(function(){console.log('try again');oCafeWrite.preCafeWriteContents();}, 2000)");
                else if (url.contains("ArticleRead.nhn")) {
                    LOG.f(TAG, "reUp finished.. [" + article.articleId + "] ");
                    if (!instant)
                        mDataHelper.updateAutoReUpload(article.articleId, true);
                    else {
                        mDataHelper.updateAutoReUpload(article.articleId, false);
                        article.autoReUpload = 0;
                        Toast.makeText(AzApplication.APP_CONTEXT, "바로 끌어올리기가 성공했습니다.", Toast.LENGTH_SHORT).show();
                    }

                    view.setWebViewClient(null);

                    Pattern p = Pattern.compile("articleid=([0-9]+)");
                    Matcher m = p.matcher(url);
                    if (m.find()) {
                        String articleIdStr = m.group(1);
                        long newArticleId = Long.parseLong(articleIdStr);
                        CrawlUtil.showNewMyArticleNotification(newArticleId, article.title, mMeta.isVibrationAlarm());

                    }


                    startRefreshMyArticle();
                }

            }

            @Override
            public void onTimeout(WebView view, String url) {
                LOG.i(TAG, "reUp - onTimeout  url : " + url);
                webView.stopLoading();
                webView.loadUrl("about:blank");
                this.release();
                mDataHelper.updateAutoReUploadFailed(article.articleId);
                Toast.makeText(AzApplication.APP_CONTEXT, R.string.toast_reup_failed, Toast.LENGTH_LONG).show();
                System.exit(1);
            }

        });

        webView.loadUrl(String.format(CrawlConstants.Urls.REUP_ARTICLE, article.articleId));
    }

    public void onLoginCompleted(String userId) {
        LOG.i(TAG, "login completed.. userId : " + userId);
        //AzUtil.printNowStackTrace();
        if (userId != null)
            mMeta.setUserId(userId);
        AzApplication.sendEmptyMessage(CrawlService.TAG, CrawlConstants.Event.LOGIN_COMPLETED);

    }

    public void startRefreshMyArticle() {
        LOG.i(TAG, "startRefreshMyArticle");

        AzApplication.sendEmptyMessage(CrawlService.TAG, CrawlConstants.Event.REFRESH_MY_ARTICLE);

    }


    public interface MyJavaScriptInterface {
        @JavascriptInterface
        public void receiveFromWeb(String value);
    }

    public interface LoginCompleteListener {
        public void onLoginCompleted(String userId);
    }

}