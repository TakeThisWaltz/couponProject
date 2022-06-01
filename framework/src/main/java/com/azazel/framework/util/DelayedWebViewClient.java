package com.azazel.framework.util;

import android.graphics.Bitmap;
import android.os.Handler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by JJ on 2016-04-14.
 */
public abstract class DelayedWebViewClient extends WebViewClient {
    private static final String TAG = "DelayedWebViewClient";

    private long delay;

    private Handler handler = new Handler();
    private Runnable task;
    private Runnable timeTask;

    private WebView webView;
    private String url;

    private long timeout;

    public DelayedWebViewClient(long delay) {
        super();

        this.delay = delay;

        task = new Runnable() {
            @Override
            public void run() {
                onDelayedPageFinished(webView, url);
            }
        };
    }

    public DelayedWebViewClient(long delay, long timeout) {
        super();

        this.delay = delay;
        this.timeout = timeout;

        task = new Runnable() {
            @Override
            public void run() {
                onDelayedPageFinished(webView, url);
            }
        };

        timeTask = new Runnable() {
            @Override
            public void run() {
                if(handler != null) {
                    handler.removeCallbacks(task);
                    handler.removeCallbacks(this);
                }
                task = null;
                onTimeout(webView, url);
            }
        };
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        LOG.d(TAG, "onPageStarted - url : " + url);
        this.webView = view;
        this.url = url;
        if(timeout > 0){
            handler.removeCallbacks(timeTask);
            handler.postDelayed(timeTask, timeout);
       }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        LOG.d(TAG, "onPageFinished - url : " + url);
        if(handler != null) {
            this.webView = view;
            this.url = url;

            if (timeTask != null)
                handler.removeCallbacks(timeTask);

            if (task != null) {
                handler.removeCallbacks(task);
                handler.postDelayed(task, delay);
            }
        }
    }

    public void release() {
        handler.removeCallbacks(task);

        if(timeTask != null)
            handler.removeCallbacks(timeTask);

        task = null;
        timeout = 0;
        timeTask = null;
        handler = null;
        webView = null;
    }

    public void onTimeout(WebView webview, String url){

    }

    public abstract void onDelayedPageFinished(WebView view, String url);
}
