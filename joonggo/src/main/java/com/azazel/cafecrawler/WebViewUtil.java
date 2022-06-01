package com.azazel.cafecrawler;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.azazel.framework.AzApplication;
import com.azazel.framework.util.LOG;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ji on 2017. 6. 15..
 */

public class WebViewUtil {
    private static final String TAG = "WebViewUtil";

    public interface ScreenCaptureCallback{
        public void OnScreenCaptured(Bitmap bitmap);
    }
    public interface ScreenCaptureJavascript {
        @JavascriptInterface
        public void init();
        @JavascriptInterface
        public void capture(int curStep, int stepSize, int totalHeight);
        @JavascriptInterface
        public void finish();
    }

    public static String enableCaptureFullScreen(final WebView webView, final ScreenCaptureCallback callback){
        webView.addJavascriptInterface(new WebViewUtil.ScreenCaptureJavascript() {
            List<Bitmap> bitmapList = new ArrayList<Bitmap>();

            @Override
            @JavascriptInterface
            public void init() {
                AzApplication.executeUIJob(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl("javascript:" +
                                "function azScrollAndCapture(curStep, stepSize, totalHeight){" +
                                "console.log('azScrollAndCapture - curStep : ' + curStep + ', stepSize : ' + stepSize + ', totalHeight : ' + totalHeight);" +
                                "var nextHeight = stepSize * curStep;" +
                                "if(totalHeight >= nextHeight){" +
                                "   window.scrollTo(0, stepSize * curStep);" +
                                "   setTimeout(function(){window.azScreenCaptureObject.capture(curStep, stepSize, totalHeight);}, 500);" +
                                "}else{" +
                                "   window.azScreenCaptureObject.finish();" +
                                "}" +
                                "}" +
                                "azScrollAndCapture(0, window.innerHeight, document.body.scrollHeight);" +
                                ""
                        );
                    }
                });
            }

            @Override
            @JavascriptInterface
            public void capture(final int curStep, final int stepSize, final int totalHeight) {
                LOG.d(TAG, "capture - curStep : " + curStep);
                AzApplication.executeUIJob(new Runnable() {
                    @Override
                    public void run() {
                        webView.setDrawingCacheEnabled(true);
                        Bitmap org = webView.getDrawingCache();
                        LOG.d(TAG, "capture - curStep : " + curStep + ", orgImg : " + org.getWidth() + "/" + org.getHeight());
                        Bitmap bm = Bitmap.createScaledBitmap(org, org.getWidth()/3, org.getHeight()/3, true);
                        org.recycle();
                        webView.setDrawingCacheEnabled(false);
                        bitmapList.add(bm);
                        webView.loadUrl("javascript:" +
                                "if(" + curStep + "==0){document.querySelector('div.gnb').style.display = 'none';}" +
                                "azScrollAndCapture(" + (curStep + 1) + ", " + stepSize + ", " + totalHeight + ");");
                    }
                });
            }

            @Override
            @JavascriptInterface
            public void finish() {
                LOG.d(TAG, "finish");

                Bitmap cs = Bitmap.createBitmap(bitmapList.get(0).getWidth(), bitmapList.get(0).getHeight() * bitmapList.size()
                        , Bitmap.Config.ARGB_8888);


                Canvas comboImage = new Canvas(cs);

                for(int i=0;i<bitmapList.size();i++){
                    Bitmap bitmap = bitmapList.get(i);
                    comboImage.drawBitmap(bitmap, 0, bitmap.getHeight() * i, null);
                }
                comboImage.save();
                bitmapList.clear();
                callback.OnScreenCaptured(cs);


//                        try {
//                            File file = new File(webView.getContext().getCacheDir(), "image.png");
//                            FileOutputStream fos = new FileOutputStream(file); // overwrites this image every time
//                            if (fos != null) {
//                                cs.compress(Bitmap.CompressFormat.PNG, 90, fos);
//                                fos.close();
//                            }
//                            file.setReadable(true, false);
//                            final Intent intent = new Intent(     android.content.Intent.ACTION_SEND);
//                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
//                            intent.setType("image/jpg");
//                            webView.getContext().startActivity(intent);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
            }

        }, "azScreenCaptureObject");

        return "window.azScreenCaptureObject.init();";
    }

    public static void captureFullScreen(final WebView webView){



        webView.loadUrl("javascript: window.azScreenCaptureObject.init(); ");
    }
}
