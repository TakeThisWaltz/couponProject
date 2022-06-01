package com.azazel.framework.network;

import com.azazel.framework.AzConstants;
import com.azazel.framework.AzException;
import com.azazel.framework.util.LOG;

import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;

public class HttpRequestBuilder {
    private static final String TAG = "HttpRequestBuilder_";

    public interface HttpMethod {
        static final String GET = "GET";
        static final String POST = "POST";
        static final String PUT = "PUT";
        static final String DELETE = "DELETE";
    }


    private HttpRequestData mRequest;

    public static HttpRequestBuilder create(String reqCode, String url, HttpRequestConfig... creator) {

        HttpRequestBuilder builder = new HttpRequestBuilder(reqCode, url);
        //  BASE_CONFIG.configure(builder);
        for (HttpRequestConfig c : creator)
            c.configure(builder);
        return builder;
    }

    public String getUrl() {
        return mRequest.getURL();
    }

    private HttpRequestBuilder(String reqCode, String url) {
        mRequest = new HttpRequestData(reqCode, url);
    }

    public HttpRequestBuilder setMethod(String method) {
        mRequest.setMethod(method);
        return this;
    }

    public HttpRequestBuilder setRequestTimeout(int timeout) {
        mRequest.setTimeout(timeout);
        return this;
    }

    public HttpRequestBuilder addHeader(String name, String value) {
        mRequest.addHeader(name, value);
        return this;
    }

    public HttpRequestBuilder addUrlParam(String name, String value){
        mRequest.addUrlParam(name, value);
        return this;
    }

    public HttpRequestBuilder setPayload(String contentType, String payload) {
        mRequest.setPayload(contentType, payload);
        return this;
    }

    public HttpRequestBuilder setPayload(String contentType, File file, NetworkUtil.AzProgressListener listener) {
        mRequest.setPayload(contentType, file, listener);
        return this;
    }

    public HttpRequestBuilder setPayload(String contentType, FileDescriptor file, NetworkUtil.AzProgressListener listener) {
        mRequest.setPayload(contentType, file, listener);
        return this;
    }

    public HttpRequestBuilder setPayload(String contentType, InputStream is, NetworkUtil.AzProgressListener listener) {
        mRequest.setPayload(contentType, is, listener);
        return this;
    }

    public HttpRequestBuilder setMultipartRequest(String boundary, String charset) {
        mRequest.setMultipartRequest(boundary, charset);
        return this;
    }

    public HttpRequestBuilder addTextPart(String name, String contentType, String value) {
        mRequest.addTextPart(name, contentType, value);
        return this;
    }

    public HttpRequestBuilder addFilePart(String fileName, String contentType, File uploadFile) {
        return addFilePart(fileName, contentType, uploadFile, null);
    }

    public HttpRequestBuilder addBinaryPart(String name, String filename, String contentType, InputStream stream) {
        mRequest.addBinaryPart(name, filename, contentType, stream, null);
        return this;
    }


    public HttpRequestBuilder addFilePart(String fileName, String contentType, File uploadFile, NetworkUtil.AzProgressListener listener) {
        mRequest.addFilePart(fileName, contentType, uploadFile, listener);
        return this;
    }


    public void execute(NetworkUtil.AzResponseHandler handler) {
        try {
            NetworkUtil.execute(mRequest, handler);
        } catch (AzException e) {
            if (e.getExceptionCode() == AzConstants.ResultCode.FAIL_AND_RETRY && mRequest.isRetryable()) {
                LOG.e(TAG + mRequest.getRequestCode(), mRequest.hashCode() + " : Retry this http request");
                NetworkUtil.execute(mRequest, handler);
                return;
            } else
                throw e;
        } catch (Exception e) {
            throw new AzException(AzConstants.ResultCode.FAIL_HTTP, e);
        }
    }

    public void executeInNewThread(final NetworkUtil.AzResponseHandler handler, final NetworkUtil.AzThreadExceptionHandler exceptionHandler){
        new Thread(){
            @Override
            public void run(){
                try {
                    NetworkUtil.execute(mRequest, handler);
                } catch (AzException e) {
                    if (e.getExceptionCode() == AzConstants.ResultCode.FAIL_AND_RETRY && mRequest.isRetryable()) {
                        LOG.e(TAG + mRequest.getRequestCode(), mRequest.hashCode() + " : Retry this http request");
                        NetworkUtil.execute(mRequest, handler);
                        return;
                    } else
                        exceptionHandler.onException(e);
                } catch (Exception e) {
                    exceptionHandler.onException(e);
                }
            }
        }.start();

    }
}
