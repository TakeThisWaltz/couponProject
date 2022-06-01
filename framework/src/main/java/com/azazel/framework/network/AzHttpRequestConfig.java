package com.azazel.framework.network;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.azazel.framework.AzApplication;

public class AzHttpRequestConfig implements HttpRequestConfig {

    private static final int CONNECTION_TIMEOUT = 29000;                                                                                                        // 60
    private static final int CONNECTION_TIMEOUT_60 = 60000;                                                                                                        // 60

    private int mTimeout;
    private HttpRequestConfig otherConfig;

    public static final HttpRequestConfig HTTP_AZ_CONFIG = new AzHttpRequestConfig(CONNECTION_TIMEOUT_60);
    public static final HttpRequestConfig HTTP_AZ_CONFIG_WITH_COOKIE = new AzHttpRequestConfig(CONNECTION_TIMEOUT_60, new HttpRequestBuilderWithCookie());

    private AzHttpRequestConfig(int timeout) {
        mTimeout = timeout;
    }

    private AzHttpRequestConfig(int timeout, HttpRequestConfig other) {
        mTimeout = timeout;
        otherConfig = other;
    }

    @Override
    public HttpRequestBuilder configure(HttpRequestBuilder builder) {
        if(otherConfig != null)
            builder = otherConfig.configure(builder);
        try {
            if(AzApplication.APP_CONTEXT != null) {
                PackageInfo packageInfo = AzApplication.APP_CONTEXT.getPackageManager().getPackageInfo(AzApplication.APP_CONTEXT.getPackageName(), 0);
                String clientInfoStr = "package=" + AzApplication.APP_CONTEXT.getPackageName() + ";version_name=" + packageInfo.versionName + ";version_code=" + packageInfo.versionCode + ";"
                        + "device=" + Build.DEVICE + ";android=" + Build.VERSION.SDK_INT;
                builder.addHeader("client_info", clientInfoStr);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        builder.setRequestTimeout(mTimeout);
        return builder;
    }
}
