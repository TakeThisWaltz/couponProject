package com.azazel.cafecrawler;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.cafecrawler.fragment.ArticleAdapter;
import com.azazel.framework.AzApplication;
import com.azazel.framework.AzSimpleWorker;
import com.azazel.framework.network.AzHttpRequestConfig;
import com.azazel.framework.network.HttpRequestBuilder;
import com.azazel.framework.network.NetworkUtil;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.DelayedWebViewClient;
import com.azazel.framework.util.LOG;
import com.baoyz.widget.PullRefreshLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by ji on 2017. 6. 17..
 */

public class DialogUtil {
    private static final String TAG = "DialogUtil";

    private static Dialog SEARCH_DIALOG;

    public static void showSearchListDialog(final Activity activity, final CrawlDataHelper.Search search) {

        if (SEARCH_DIALOG != null && SEARCH_DIALOG.isShowing()) {
            SEARCH_DIALOG.cancel();
            SEARCH_DIALOG = null;
        }
        AzUtil.makeTransparentBackgroundDialog(activity, R.layout.popup_search, new AzUtil.OnAzDialogCreateListener() {

            private ListView mListView;
            private ArticleAdapter mAdapter;
            private List<CrawlDataHelper.Article> mArticles;
            private PullRefreshLayout mRefreshLayout;

            @Override
            public void onAzDialogCreated(Dialog dialog) {
                SEARCH_DIALOG = dialog;
                mListView = (ListView) dialog.findViewById(R.id.list_alarm_search);

                final AdView adView = (AdView) dialog.findViewById(R.id.adView_popup);
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);

                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        final CrawlDataHelper.Article article = (CrawlDataHelper.Article) parent.getAdapter().getItem(position);
                        showArticleDetailDialog(activity, article);
                    }
                });

                mRefreshLayout = (PullRefreshLayout) dialog.findViewById(R.id.refresh_alarm);
                mRefreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        loadItems();
                    }
                });

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        LOG.i(TAG, "onDismiss");
                        if(adView != null)
                            adView.destroy();
                    }
                });

                loadItems();

            }

            private void loadItems() {
                mRefreshLayout.setRefreshing(true);
                AzApplication.executeJobOnBackground(new AzSimpleWorker(CrawlConstants.DEFAULT_NETWORK_TIMEOUT) {
                    @Override
                    public void doInBackgroundAndResult() {
                        List<CrawlDataHelper.Article> result = CrawlManager.getInstance().search(search.categoryId, search.keyword, 1);

                        Collections.sort(result);

                        this.setResult(true, result);
                    }

                    @Override
                    public void postOperationWithResult(boolean result, Object value) {
                        if (result) {
                            List<CrawlDataHelper.Article> resultArr = (List<CrawlDataHelper.Article>) value;
                            if (mArticles == null)
                                mArticles = resultArr;
                            else {
                                mArticles.clear();
                                mArticles.addAll(resultArr);
                            }

//                            new PriceParser(mArticles).startParse(new PriceParser.PriceFoundListener() {
//                                @Override
//                                public void onPriceFound(CrawlDataHelper.Article article, int[] prices) {
//                                    LOG.d(TAG, "onPriceFound - article : " + article.articleId + ", price : " + AzUtil.printArray(prices));
//                                }
//                            });

                            long beforeMaxArticleId = CrawlDataHelper.getInstance().getBeforeMaxArticleId(search.id);

                            if (mAdapter == null) {
                                mAdapter = new ArticleAdapter(activity, R.layout.search_list_item, R.id.tv_list_title, mArticles, R.id.tv_list_title, R.id.tv_list_writer, R.id.img_list_thmb);
                                mAdapter.setBeforeMaxArticleId(beforeMaxArticleId);
                                mListView.setAdapter(mAdapter);
                            } else {
                                mAdapter.setBeforeMaxArticleId(beforeMaxArticleId);
                                mAdapter.notifyDataSetChanged();
                            }

                            if (resultArr.size() > 0) {
                                LOG.i(TAG, "search result - first : " + resultArr.get(0).articleId + ", last : " + resultArr.get(resultArr.size() - 1).articleId);
                                search.newArticle = resultArr.get(0).articleId;
                                search.oldArticle = resultArr.get(resultArr.size() - 1).articleId;
                                CrawlDataHelper.getInstance().updateNewArticle(search.id, resultArr.get(resultArr.size() - 1).articleId, resultArr.get(0).articleId);
                            }


                        }
                        mRefreshLayout.setRefreshing(false);
                    }

                });
            }

            private void parsePrice(){

            }
        }).show();
    }

    private static Dialog DETAIL_DIALOG;

    public static void showArticleDetailDialog(final Activity activity, final CrawlDataHelper.Article article) {

        if (DETAIL_DIALOG != null && DETAIL_DIALOG.isShowing()) {
            DETAIL_DIALOG.cancel();
            DETAIL_DIALOG = null;
        }

        AzUtil.makeTransparentBackgroundDialog(activity, R.layout.popup_item_detail, new AzUtil.OnAzDialogCreateListener() {
            private Dialog dialog;

            @Override
            public void onAzDialogCreated(final Dialog dialog) {
                this.dialog = dialog;
                DETAIL_DIALOG = dialog;

                final WebView webView = (WebView) dialog.findViewById(R.id.web_detail_popup);
                final String detailUrl = String.format(CrawlConstants.Urls.DETAIL_VIEW, article.articleId);

                dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        LOG.d(TAG, "onKey : " + event.getKeyCode());
                        if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                            webView.loadUrl("javascript:console.log('back key pressed.. history : ' + document.referrer); " +
                                    "if(document.referrer) history.back(); else window.azWebObject.receiveFromWeb('finish');");
                            return true;
                        }

                        return false;
                    }
                });



                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setUseWideViewPort(true);
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        LOG.d(TAG, "shouldOverrideUrlLoading 1 - " + url);
                        //webView.loadUrl("javascript:window.azWebObject.receiveFromWeb('"+url+"');");
                        return parsePhoneNumber(url);
                    }

                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        LOG.d(TAG, "shouldOverrideUrlLoading 2 - " + request.getUrl());
                        //webView.loadUrl("javascript:window.azWebObject.receiveFromWeb('"+request.getUrl()+"');");
                        return parsePhoneNumber(request.getUrl().toString());
                    }


                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        LOG.d(TAG, "onPageFinished - " + url);
                        webView.setEnabled(false);
                        webView.loadUrl("javascript:" +
                                "try{" +
                                "document.querySelector('div.footer_fix').style.display = 'none';" +
                                "}catch(err){}" +
                                "window.azWebObject.receiveFromWeb((document.querySelector('div.post_com')?document.querySelector('div.post_com').innerHTML:'') + document.querySelector('#postContent').innerHTML.replace(/(<([^>]+)>)/gi, ''));" +
                                "var azSmsLink = document.querySelector('#smsLink');" +
                                "var azCallLink = document.querySelector('#dialLink');" +
                                "if(azCallLink){ console.log('azCallLink : ' + azCallLink.href); if(azCallLink.href.endsWith('#')) azCallLink.click(); }" +
                                "if(azSmsLink){ console.log('smsLink : ' + azSmsLink.href); if(azSmsLink.href.endsWith('#')) azSmsLink.click(); }" +
                                "");
                    }
                });
                final View fab = dialog.findViewById(R.id.btn_add_scrap);
                if(CrawlDataHelper.getInstance().getArticle(article.articleId) == null) {
                    fab.setVisibility(View.VISIBLE);
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            article.type = CrawlConstants.ArticleType.SCRAP;
                            long id = AlarmManager.getInstance().addScrap(article);
                            if (id > -1) {
                                fab.setVisibility(View.GONE);
                                AzApplication.sendEmptyMessage(MainActivity.TAG, CrawlConstants.Event.SCRAP_CHANGED);
                                Toast.makeText(activity, R.string.toast_new_scrap, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                dialog.findViewById(R.id.btn_share).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        LOG.d(TAG, "share clicked..");

                        HttpRequestBuilder.create(detailUrl, detailUrl, AzHttpRequestConfig.HTTP_AZ_CONFIG)
                                .executeInNewThread(new NetworkUtil.StringResponseHandler() {
                                    @Override
                                    public void handleResponse(int statusCode, String body) {
                                        LOG.d(TAG, "share detail " + body);
                                        if(body.contains("카페 멤버만 볼 수 있습니다.")){
                                            AzApplication.executeUIJob(new Runnable() {
                                                @Override
                                                public void run() {
                                                    AlertDialog.Builder alert_confirm = new AlertDialog.Builder(view.getContext());
                                                    alert_confirm.setMessage("카페회원에게만 공개된 글입니다. 스크린샷으로 공유하시겠습니까?").setCancelable(false).setPositiveButton("확인",
                                                            new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    AzApplication.showProgress();
                                                                    WebViewUtil.captureFullScreen(webView);
                                                                }
                                                            }).setNegativeButton("취소",
                                                            new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    // 'No'
                                                                    return;
                                                                }
                                                            });
                                                    AlertDialog alert = alert_confirm.create();
                                                    alert.show();

                                                }
                                            });

                                        }else{
                                            Intent intent = new Intent(Intent.ACTION_SEND);
                                            intent.setType("text/plain");
                                            intent.putExtra(Intent.EXTRA_TEXT, "[중고나라 알리미 - 중GO!]\n" + article.title + "\n" + detailUrl);
                                            view.getContext().startActivity(Intent.createChooser(intent, "Share"));
                                        }
                                    }
                                }, new NetworkUtil.AzThreadExceptionHandler() {
                                    @Override
                                    public void onException(Exception e) {
                                        LOG.e(TAG, "share detail ",e);
                                    }
                                });


                    }
                });



                webView.addJavascriptInterface(new CrawlManager.MyJavaScriptInterface() {
                    @Override
                    @JavascriptInterface
                    public void receiveFromWeb(String value) {
                        //LOG.i(TAG, "receiveFromWeb : " + value);
                        if("finish".equals(value)){
                            dialog.cancel();
                        }else
                            parsePhoneNumber(value);

                    }
                }, "azWebObject");

                WebViewUtil.enableCaptureFullScreen(webView, new WebViewUtil.ScreenCaptureCallback() {
                    @Override
                    public void OnScreenCaptured(Bitmap bitmap) {
                        LOG.d(TAG, "OnScreenCaptured");

                        try {

                            File cachePath = new File(webView.getContext().getCacheDir(), "images");
                            cachePath.mkdirs(); // don't forget to make the directory
                            FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            stream.close();


                            File imagePath = new File(webView.getContext().getCacheDir(), "images");
                            File newFile = new File(imagePath, "image.png");
                            Uri contentUri = FileProvider.getUriForFile(webView.getContext(), "com.azazel.cafecrawler.fileprovider", newFile);

                            if (contentUri != null) {
                                Intent shareIntent = new Intent();
                                shareIntent.setAction(Intent.ACTION_SEND);
                                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                                shareIntent.setDataAndType(contentUri, webView.getContext().getContentResolver().getType(contentUri));
                                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                                shareIntent.putExtra(Intent.EXTRA_TITLE, "[중고나라 알리미 - 중GO!]\n" + article.title + "\n");
                                webView.getContext().startActivity(Intent.createChooser(shareIntent, "Choose an app"));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        AzApplication.stopProgress();
                    }
                });


                webView.loadUrl(detailUrl);

                if (!(article.type == CrawlConstants.ArticleType.SEARCH_RESULT) && !article.isRead) {
                    article.isRead = true;
                    CrawlDataHelper.getInstance().setIsReadScrap(article);
                }

            }

            private boolean parsePhoneNumber(String value){
                LOG.i(TAG, "parsePhoneNumber : " + value);
                final String phoneNumber = PhoneNumberParser.searchPhoneNumber(value);
                LOG.d(TAG, "phone : " + phoneNumber);

                if(!TextUtils.isEmpty(phoneNumber)){
                    AzApplication.executeUIJob(new Runnable() {
                        @Override
                        public void run() {
                            View btnSend = dialog.findViewById(R.id.btn_send_sms);
                            btnSend.setVisibility(View.VISIBLE);
                            btnSend.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
//                                            ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
//
//                                            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, "우리은행 1002-729-654148입니다 이리로 5만원"));


                                    CrawlUtil.sendSMS(activity, phoneNumber, "중고나라보고 연락드립니다.");
                                }
                            });
                        }
                    });
                    return true;
                }else{
                    return (value.startsWith("sms:") || value.startsWith("tel:") || value.contains("=sms%3A") || value.contains("=tel%3A"));
                }
            }
        }).show();
    }



    public static void showLoginPopup(final Activity activity, final CrawlManager.LoginCompleteListener listener) {
        LOG.i(TAG, "showLoginPopup");
        AzUtil.makeTransparentBackgroundDialog(activity, R.layout.popup_login, new AzUtil.OnAzDialogCreateListener() {
            private String mTmpUserId;

            @Override
            public void onAzDialogCreated(final Dialog dialog) {
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(false);

                dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK){
                            AzApplication.sendEmptyMessage(CrawlService.TAG, CrawlConstants.Event.LOGIN_FAILED);
                        }
                        return false;
                    }
                });

                final WebView webView = (WebView) dialog.findViewById(R.id.web_login_popup);

                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setUseWideViewPort(true);

                webView.setWebViewClient(new DelayedWebViewClient(1000) {
                    @Override
                    public void onDelayedPageFinished(WebView view, String url) {
                        LOG.i(TAG, "onPageFinished - url : " + url);
                        if (url.contains("login.login")) {
                            LOG.i(TAG, "request login..");

                            webView.loadUrl("javascript:document.querySelector('input.btn_global').onclick = function(event){window.LoginEvent.receiveFromWeb(document.getElementById('id').value); nclks('log.login', this, event);}");
                        } else if (url.equals("https://m.cafe.naver.com/cafe-home/cafes/join")) {
                            if (mTmpUserId != null) {
                                webView.loadUrl("about:blank");
                                dialog.dismiss();
                                listener.onLoginCompleted(mTmpUserId);
                            }
                        }
                    }
                });

                webView.addJavascriptInterface(new CrawlManager.MyJavaScriptInterface() {

                    @Override
                    @JavascriptInterface
                    public void receiveFromWeb(String value) {
                        LOG.i(TAG, "userId : " + value);
                        mTmpUserId = value;

                    }
                }, "LoginEvent");

                webView.loadUrl(CrawlConstants.Urls.LOGIN);

            }
        }).show();
    }





    public static void showWebViewPopup(final Activity activity, final String url) {
        LOG.i(TAG, "showWebViewPopup - url : " + url);
        AzUtil.makeTransparentBackgroundDialog(activity, R.layout.popup_login, new AzUtil.OnAzDialogCreateListener() {
            private String mTmpUserId;

            @Override
            public void onAzDialogCreated(final Dialog dialog) {
                LOG.i(TAG, "onAzDialogCreated - url : " + url);

                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(false);


                final WebView webView = (WebView) dialog.findViewById(R.id.web_login_popup);
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
                        LOG.i(TAG, "reUp - onPageFinished  url : " + url);
                        if (url.contains("ArticleWrite.nhn"))
                            view.loadUrl("javascript:sWriteMode = \"write\";document.querySelector(\"input[name='articleid']\").value = \"\";oCafeWrite.preCafeWriteContents();setTimeout(function(){oCafeWrite.preCafeWriteContents();}, 1000)");
                        else if (url.contains("ArticleRead.nhn")) {
                            LOG.f(TAG, "reUp finished..  ");
                        }
                    }

                    @Override
                    public void onTimeout(WebView view, String url) {
                        LOG.i(TAG, "reUp - onTimeout  url : " + url);
                        webView.stopLoading();
                        webView.loadUrl("about:blank");
                    }
                });



                webView.loadUrl(url);

            }
        }).show();
    }


}
