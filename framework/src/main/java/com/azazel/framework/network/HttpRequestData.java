package com.azazel.framework.network;

import android.text.TextUtils;

import com.azazel.framework.AzConstants;
import com.azazel.framework.AzException;
import com.azazel.framework.util.LOG;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HttpRequestData {
    private static final String LINE_FEED = "\r\n";
    private static final String TAG = "HttpRequestData";

    private boolean isRetryable = true;

    private String requestCode;
    private String charset;
    private int timeout;


    private String url;
    private String method;
    private String urlParams;
    private Map<String, String> header;

    private BodyPart payload;

    private String boundary;
    private List<BodyPart> multipartPayload;

    HttpRequestData(String requestCode, String url) {
        this.requestCode = requestCode;
        this.url = url;
    }

    String getRequestCode() {
        return requestCode;
    }

    void setURL(String url) {
        this.url = url;
    }

    public String getURL() {
        return url;
    }

    boolean isRetryable() {
        return isRetryable;
    }

    void setMethod(String method) {
        this.method = method;
    }

    void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    void addHeader(String key, String value) {
        if (header == null)
            header = new HashMap<>();
        header.put(key, value);
    }

    void addUrlParam(String key, String value){
        try {
            if(urlParams == null) urlParams = "";

            urlParams += "&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    void setPayload(String contentType, String content) {
        BodyPart part = new BodyPart();
        part.contentType = contentType;
        part.content = content;
        this.payload = part;
    }

    void setPayload(String contentType, File content, NetworkUtil.AzProgressListener listener) {
        BodyPart part = new BodyPart();
        part.contentType = contentType;
        part.content = content;
        this.payload = part;
    }

    void setPayload(String contentType, FileDescriptor content, NetworkUtil.AzProgressListener listener) {
        BodyPart part = new BodyPart();
        part.contentType = contentType;
        part.content = content;
        this.payload = part;
    }

    void setPayload(String contentType, InputStream content, NetworkUtil.AzProgressListener listener) {
        BodyPart part = new BodyPart();
        part.contentType = contentType;
        part.content = content;
        part.listener = listener;
        this.payload = part;
    }

    void setMultipartRequest(String boundary, String charset) {
        this.boundary = boundary;
        this.charset = charset;
        multipartPayload = new ArrayList<BodyPart>();
    }

    void addTextPart(String name, String contentType, String value) {
        BodyPart part = new BodyPart();
        part.name = name;
        part.contentType = contentType;
        part.content = value;
        multipartPayload.add(part);
    }

    void addFilePart(String name, String contentType, File uploadFile, NetworkUtil.AzProgressListener listener) {
        BodyPart part = new BodyPart();
        part.name = name;
        part.fileName = uploadFile.getName();
        part.contentType = contentType;
        part.content = uploadFile;
        part.listener = listener;
        multipartPayload.add(part);
    }

    void addBinaryPart(String name, String fileName, String contentType, InputStream stream, NetworkUtil.AzProgressListener listener) {
        BodyPart part = new BodyPart();
        part.name = name;
        part.fileName = fileName;
        part.contentType = contentType;
        part.content = stream;
        part.listener = listener;
        multipartPayload.add(part);
    }


    HttpURLConnection build() {
        LOG.d(TAG, "build - request : " + requestCode + ", method : " + method + ", boundary : " + boundary);
        HttpURLConnection conn = null;
        try {
            if(!TextUtils.isEmpty(urlParams)){
                url += "?" + urlParams.substring(1);
            }

            LOG.d(TAG, "final url : " + url);

            URL realUrl = new URL(url);
            conn = (HttpURLConnection) realUrl.openConnection();

            if (method != null)
                conn.setRequestMethod(method);

            if (header != null) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            conn.setReadTimeout(timeout);
            conn.setConnectTimeout(timeout);

            if (payload != null) {
                conn.setDoInput(true);
                conn.setRequestProperty("Content-Type", payload.contentType);
                conn.setChunkedStreamingMode(0);
                writeBody(conn.getOutputStream(), payload);
            } else if (boundary != null && multipartPayload != null && multipartPayload.size() > 0) {
                conn.setDoInput(true);
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);
                conn.setChunkedStreamingMode(0);
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), charset), true);
                writer.append(LINE_FEED).append("--" + boundary);

                for (BodyPart part : multipartPayload) {
                    writer.append(LINE_FEED);
                    writer.append("Content-Disposition: form-data; name=\"" + part.name + "\";");
                    if (part.fileName != null)
                        writer.append(" filename=\"" + part.fileName + "\";");
                    writer.append(LINE_FEED);
                    writer.append("Content-Type: " + part.contentType + "; charset=" + charset).append(
                            LINE_FEED);
                    writer.append(LINE_FEED);

                    if (part.content instanceof String) {
                        writer.append((String) part.content);
                    } else {
                        writer.flush();
                        writeBody(conn.getOutputStream(), part);
                    }

                    writer.append(LINE_FEED).append("--" + boundary);
                }

                writer.append("--");
                writer.append(LINE_FEED);
                writer.flush();
            }
        } catch (IOException e) {
            throw new AzException(AzConstants.ResultCode.FAIL_HTTP, e);
        }
        return conn;
    }

    private void writeBody(OutputStream stream, BodyPart part) {
        try {
            if (part.content instanceof String) {
                BufferedWriter br = new BufferedWriter(new OutputStreamWriter(stream));
                br.write((String) part.content);
                br.flush();
            } else if (part.content instanceof File) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream((File) part.content);
                    streaming(fis, stream, part.listener);
                } finally {
                    if (fis != null)
                        fis.close();
                }
            } else if (part.content instanceof FileDescriptor) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream((FileDescriptor) part.content);
                    streaming(fis, stream, part.listener);
                } finally {
                    if (fis != null)
                        fis.close();
                }
//            }else if(part.content instanceof  FileInputStream){
//                streaming((InputStream)part.content, stream, part.listener);
//                part.content = ((FileInputStream)part.content).getFD();
            } else if (part.content instanceof InputStream) {
                isRetryable = false;
                streaming((InputStream) part.content, stream, part.listener);
            }
        } catch (Exception e) {
            throw new AzException(AzConstants.ResultCode.FAIL_HTTP, e);
        }
    }

    private static void streaming(InputStream from, OutputStream to, NetworkUtil.AzProgressListener listener) throws IOException {
        int read = 0;
        long transferred = 0;
        long total = from.available();
        byte[] buff = new byte[4096];
        int len = buff.length;
        BufferedInputStream bis = new BufferedInputStream(from);
        BufferedOutputStream bos = new BufferedOutputStream(to);
        while ((read = bis.read(buff, 0, len)) > 0) {
            bos.write(buff, 0, read);
            if (listener != null) {
                transferred += read;
                listener.transferred(transferred, total);
            }
        }
        bos.flush();
    }


    class BodyPart {
        String name;
        String fileName;
        String contentType;
        Object content;
        NetworkUtil.AzProgressListener listener;
    }
}
