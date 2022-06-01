package com.azazel.framework.network;

import com.azazel.framework.AzConstants;
import com.azazel.framework.AzException;
import com.azazel.framework.util.LOG;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class NetworkUtil {
    private static final String TAG = "NetworkUtil_";

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_OCTET_STREAM = "application/octet-stream";
    public static final String CONTENT_MULTIPART_FORM_DATA = "multipart/form-data";

    private static final Object REQUEST_LOCK = new Object();
    private static final HashMap<String, Queue<HttpURLConnection>> REQUEST_MAP = new HashMap<String, Queue<HttpURLConnection>>();


    public static void cancelRequestes(String requestCode) {
        synchronized (REQUEST_LOCK) {
            LOG.i(TAG + requestCode, "cancelRequestes - start : " + REQUEST_MAP.size());
            try {

                Queue<HttpURLConnection> queue = REQUEST_MAP.get(requestCode);
                if (queue != null) {
                    HttpURLConnection request = null;
                    while ((request = queue.poll()) != null) {
                        request.disconnect();
                        LOG.d(TAG + requestCode, request.hashCode() + " : aborted, remains - " + queue.size());
                    }
                    REQUEST_MAP.remove(requestCode);
                }
            } catch (Exception e) {
                LOG.e(TAG + requestCode, "cancelRequestes err ", e);
                throw new AzException(AzConstants.ResultCode.FAIL_HTTP, e);
            }
            LOG.i(TAG + requestCode, "cancelRequestes - end : " + REQUEST_MAP.size());
        }
    }


    static void execute(HttpRequestData request, AzResponseHandler handler) {
        LOG.i(TAG + request.getRequestCode(), request.hashCode() + " : execute : " + request.getURL());

        InputStream response = null;
        Queue<HttpURLConnection> requestQueue = null;
        HttpURLConnection connection = null;

        try {

            connection = request.build();

            synchronized (REQUEST_LOCK) {
                requestQueue = REQUEST_MAP.get(request.getRequestCode());
                if (requestQueue == null) {
                    requestQueue = new LinkedList<HttpURLConnection>();
                    REQUEST_MAP.put(request.getRequestCode(), requestQueue);
                }
                requestQueue.add(connection);
            }

            if (!HttpRequestBuilder.HttpMethod.GET.equals(connection.getRequestMethod()))
                connection.getOutputStream().close();

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                String redirectURL = connection.getHeaderField("Location");
                request.setURL(redirectURL);
                throw new AzException(AzConstants.ResultCode.FAIL_AND_RETRY, "request is redirected : " + redirectURL);
            } else if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST)
                response = connection.getErrorStream();
            else response = connection.getInputStream();

            if (handler instanceof NetworkUtil.StringResponseHandler)
                extractResponse(request.getRequestCode(), connection.getHeaderFields(), responseCode, response, (NetworkUtil.StringResponseHandler) handler);
            else if (handler instanceof NetworkUtil.FileResponseHandler)
                extractResponse(request.getRequestCode(), connection.getHeaderFields(), responseCode, connection.getContentLength(), response, (NetworkUtil.FileResponseHandler) handler);
            else
                throw new AzException(AzConstants.ResultCode.FAIL_SERVER_ERR, "invalid response handler");

        } catch (AzException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            LOG.e(TAG + request.getRequestCode(), request.hashCode() + " : " + request.getURL() + " - executeRequest time-out err", e);
            throw new AzException(AzConstants.ResultCode.FAIL_HTTP_TIMEOUT, e);
        } catch (Exception e) {
            LOG.e(TAG + request.getRequestCode(), request.hashCode() + " : " + request.getURL() + " - executeRequest err", e);
            throw new AzException(AzConstants.ResultCode.FAIL_HTTP, e);
        } finally {
            synchronized (REQUEST_LOCK) {
                if (requestQueue != null) {
                    requestQueue.remove(connection);
                    if (requestQueue.size() == 0)
                        REQUEST_MAP.remove(requestQueue);
                }
            }
            if (response != null)
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            if (connection != null)
                connection.disconnect();
        }
    }

    private static void extractResponse(String requestCode, Map<String, List<String>> headers, int responseCode, InputStream response, NetworkUtil.StringResponseHandler handler) {
        if (response != null) {
            try {
                String encoding = null;
                if (headers.get("Content-Type") != null) {
                    String contentType = headers.get("Content-Type").get(0);
                    if (contentType.contains("charset=")) {
                        encoding = contentType.substring(contentType.indexOf("charset=") + 8);
                        LOG.i(TAG, "extractResponse - encoding : " + encoding);
                    }
                }
                String responseStr = toString(response, encoding);

                LOG.d(TAG + requestCode, responseStr);

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    handler.handleResponse(responseCode, responseStr);
                } else {
                    LOG.f(TAG + requestCode, "There was a problem on the Backup Server. RESULT CODE: " + responseCode);
                    throw new AzException(AzConstants.ResultCode.FAIL_SERVER_ERR, "status error : " + responseCode + ", response =" + responseStr);
                }
            } catch (IOException e) {
                LOG.e(TAG + requestCode, "extractResponse err", e);
                throw new AzException(AzConstants.ResultCode.FAIL_HTTP, "extractResponse err", e);
            } catch (OutOfMemoryError e) {
                LOG.e(TAG + requestCode, "extractResponse err - Converting HTTPEntity to String returns out of Memory", e);
                throw new AzException(AzConstants.ResultCode.FAIL_HTTP, "extractResponse OutOfMemoryError", e);
            }
        }
    }

    private static void extractResponse(String requestCode, Map<String, List<String>> headers, int responseCode, long contentLength, InputStream response, NetworkUtil.FileResponseHandler handler) {
        if (response != null) {
            if (responseCode == HttpURLConnection.HTTP_OK) {
                LOG.f(TAG + requestCode, "Header : " + responseCode);
                handler.handleResponse(contentLength, response);
            } else {
                if (responseCode != HttpURLConnection.HTTP_BAD_REQUEST) {
                    LOG.f(TAG + requestCode, "There was a problem on the Backup Server. RESULT CODE: " + responseCode);
                }
                String responseStr = null;
                try {
                    responseStr = toString(response);
                } catch (Exception e) {
                    LOG.e(TAG + requestCode, "handleResponse err", e);
                }
                throw new AzException(AzConstants.ResultCode.FAIL_SERVER_ERR, "status error : " + responseCode + ", response =" + responseStr);
            }
        }
    }

    public static String toString(InputStream is, String charSet) throws IOException {
        InputStreamReader isr = null;
        if (charSet == null)
            isr = new InputStreamReader(is);
        else isr = new InputStreamReader(is, charSet);
        BufferedReader r = new BufferedReader(isr);
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }

    private static String toString(InputStream is) throws IOException {
        return toString(is, null);
    }

    public interface AzResponseHandler {

    }

    public static abstract class JSONResponseHandler implements StringResponseHandler {
        public void handleResponse(int statusCode, String body) {
            try {
                JSONObject json = new JSONObject(body);
                handleResponse(statusCode, json);
            } catch (JSONException jsone) {
                throw new AzException(AzConstants.ResultCode.FAIL_JSON, jsone);
            }
        }

        public abstract void handleResponse(int statusCode, JSONObject body) throws JSONException;
    }

    public interface StringResponseHandler extends AzResponseHandler {
        public void handleResponse(int statusCode, String body);
    }

    public interface FileResponseHandler extends AzResponseHandler {
        public void handleResponse(long size, InputStream stream);
    }

    public interface AzProgressListener {
        public void transferred(long transferred, long total);
    }


    public interface AzThreadExceptionHandler {
        public void onException(Exception e);
    }
}