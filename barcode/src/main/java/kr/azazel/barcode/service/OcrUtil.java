package kr.azazel.barcode.service;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.azazel.framework.AzApplication;
import com.azazel.framework.network.AzHttpRequestConfig;
import com.azazel.framework.network.HttpRequestBuilder;
import com.azazel.framework.network.NetworkUtil;
import com.azazel.framework.util.LOG;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class OcrUtil {
    private static final String TAG = "OcrService";

    private static final AtomicBoolean IN_PROGRESS = new AtomicBoolean(false);

    private static void downloadLanguageFile(String dir) {
        IN_PROGRESS.set(true);
        String url = "https://d1hm6ppxabjxtj.cloudfront.net/kor.traineddata";

        String destFile = dir + "/kor.traineddata";

        HttpRequestBuilder.create(url, url, AzHttpRequestConfig.HTTP_AZ_CONFIG)
                .executeInNewThread(new NetworkUtil.FileResponseHandler() {
                    @Override
                    public void handleResponse(long size, InputStream stream) {
                        try {
//                            ZipInputStream zis = new ZipInputStream(stream);
                            FileOutputStream outputStream = new FileOutputStream(destFile);
                            IOUtils.copy(stream, outputStream);
                        } catch (Exception e) {
                            LOG.e(TAG, "downloadLanguageFile err", e);
                        } finally {
                            IN_PROGRESS.set(false);
                        }
                    }
                }, new NetworkUtil.AzThreadExceptionHandler() {
                    @Override
                    public void onException(Exception e) {
                        LOG.e(TAG, "share detail ", e);
                        IN_PROGRESS.set(false);
                    }
                });
    }


    public static synchronized boolean checkLanguageFile() {
        if (IN_PROGRESS.get()) {
            return false;
        }
        String dir = AzApplication.APP_CONTEXT.getFilesDir().getAbsolutePath() + "/tessdata";

        File file = new File(dir);
        if (!file.exists() && file.mkdirs()) {
            downloadLanguageFile(dir);
            return false;
        } else if (file.exists()) {
            String filePath = dir + "/kor.traineddata";
            File langDataFile = new File(filePath);
            if (!langDataFile.exists()) {
                downloadLanguageFile(dir);
                return false;
            } else if (langDataFile.length() != 15317715) {
                if (langDataFile.delete()) {
                    downloadLanguageFile(dir);
                }
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private static void createFiles(String dir) {
        AssetManager assetMgr = AzApplication.APP_CONTEXT.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = assetMgr.open("kor.traineddata");

            String destFile = dir + "/kor.traineddata";

            outputStream = new FileOutputStream(destFile);

            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            LOG.e(TAG, "createFiles err", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String extractText(Uri uri) {
        try (InputStream imageStream = AzApplication.APP_CONTEXT.getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            return extractText(bitmap);
        } catch (Exception e) {
            LOG.e(TAG, "extractText err", e);
            return null;
        }
    }

    public static String extractText(Bitmap image) {
        TessBaseAPI tess = new TessBaseAPI();
        String dir = AzApplication.APP_CONTEXT.getFilesDir().getAbsolutePath();
        if (checkLanguageFile()) {
            String dataPath = new File(dir).getAbsolutePath();

            tess.init(dataPath, "kor");

            tess.setImage(image);
            String text = tess.getUTF8Text();
            tess.recycle();
            return text;
        } else {
            return null;
        }
    }


}
