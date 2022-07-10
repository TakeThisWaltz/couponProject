package com.azazel.cafecrawler;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.app.NotificationCompat;

import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.cafecrawler.data.CrawlDataHelper.Search;
import com.azazel.framework.AzApplication;
import com.azazel.framework.util.LOG;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

public class CrawlUtil {
    private static final String TAG = "CrawlUtil";

    public static synchronized void initImageLoader(Context context) {
        if (!ImageLoader.getInstance().isInited()) {
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                    .threadPriority(Thread.NORM_PRIORITY - 2)
                    .denyCacheImageMultipleSizesInMemory()
                    .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                    .diskCacheSize(50 * 1024 * 1024) // 50 Mb
                    .tasksProcessingOrder(QueueProcessingType.LIFO)
                    .writeDebugLogs() // Remove for release app
                    .memoryCache(new WeakMemoryCache())
                    .build();
            // Initialize ImageLoader with configuration.
            ImageLoader.getInstance().init(config);
        }
    }

    public static void showNewNotification(Search search, int size, CrawlDataHelper.Article lastArticle, boolean isVibrate) {
        LOG.i(TAG, "showNotification - Search : id : " + search.id + ", " + search.keyword + ", : " + size + ", isVibrate : " + isVibrate);
        Intent intent = new Intent(AzApplication.APP_CONTEXT, MainActivity.class);
        intent.putExtra("search_id", search.id);
        intent.setData(Uri.parse("crawl://com.azazel.cafecrawler/search/" + search.id));

        showNotification((int) search.id, intent, AzApplication.APP_CONTEXT.getString(R.string.noti_new_article, search.keyword), lastArticle.title, size, isVibrate);
    }

    public static void showNewCommentNotification(CrawlDataHelper.Article article, int size, boolean isVibrate) {
        LOG.i(TAG, "showNewCommentNotification - Article articleId : " + article.articleId + ", : " + size + ", isVibrate : " + isVibrate);
        Intent intent = new Intent(AzApplication.APP_CONTEXT, MainActivity.class);
        intent.putExtra("article_id", article.articleId);
        intent.setData(Uri.parse("crawl://com.azazel.cafecrawler/article/" + article.articleId));

        showNotification((int) article.articleId, intent, AzApplication.APP_CONTEXT.getString(R.string.noti_new_comment), article.title, size, isVibrate);
    }

    public static void showNewMyArticleNotification(long articleId, String title , boolean isVibrate) {
        LOG.i(TAG, "showNewCommentNotification - Article articleId : " + articleId + ", : " + title + ", isVibrate : " + isVibrate);
        Intent intent = new Intent(AzApplication.APP_CONTEXT, MainActivity.class);
        intent.putExtra("article_id", articleId);
        intent.setData(Uri.parse("crawl://com.azazel.cafecrawler/article/" + articleId));

        showNotification((int) articleId, intent, AzApplication.APP_CONTEXT.getString(R.string.noti_new_my_article), title, 1, isVibrate);
    }

    public static void showNotification(int notiId, Intent intent, String title, String content, int number, boolean isVibrate) {
        PendingIntent contentIntent = PendingIntent.getActivity(AzApplication.APP_CONTEXT, 0,
                intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(AzApplication.APP_CONTEXT)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(content))
                        .setContentText(content).setNumber(number)
                        .setAutoCancel(true).setLights(0xff5f00ff, 2000, 1000);


        if (isVibrate)
            mBuilder.setVibrate(CrawlConstants.VIBRATE);

        mBuilder.setContentIntent(contentIntent);
        ((NotificationManager) AzApplication.APP_CONTEXT.getSystemService(Context.NOTIFICATION_SERVICE)).notify(notiId, mBuilder.build());
    }

    public static void cancelNewNotification(Search search) {
        ((NotificationManager) AzApplication.APP_CONTEXT.getSystemService(Context.NOTIFICATION_SERVICE)).cancel((int) search.id);
    }


    public static void sendSMS(Activity activity, String receiver, String message){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra("address", receiver);
        intent.putExtra("sms_body", message);
        intent.setType("vnd.android-dir/mms-sms");
        activity.startActivity(intent);
    }
}
