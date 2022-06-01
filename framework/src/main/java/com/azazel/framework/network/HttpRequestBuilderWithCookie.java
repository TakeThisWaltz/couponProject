package com.azazel.framework.network;

import android.webkit.CookieManager;

import com.azazel.framework.util.LOG;

import java.net.URI;

public class HttpRequestBuilderWithCookie implements HttpRequestConfig {

    private static final String TAG = "HttpRequestBuilderWithCookie";

    @Override
    public HttpRequestBuilder configure(HttpRequestBuilder builder) {
        builder.addHeader("Cookie", getCookieFromAppCookieManager(URI.create(builder.getUrl())));
        return builder;
    }

    private String getCookieFromAppCookieManager(URI url) {
        CookieManager cookieMgr = CookieManager.getInstance();

        String rawCookieHeader = cookieMgr.getCookie(url.getHost());
        LOG.i(TAG, "cookie : " + rawCookieHeader);
        return rawCookieHeader;

    }

}
