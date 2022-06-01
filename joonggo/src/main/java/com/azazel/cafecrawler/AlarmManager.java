package com.azazel.cafecrawler;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.framework.AzApplication;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;

import java.util.Calendar;

/**
 * Created by JI on 2015-05-20.
 */
public class AlarmManager {
    private static final String TAG = "AlarmManager";

    private static AlarmManager INSTANCE = new AlarmManager();

    private PendingIntent mObservingIntent;
    private PendingIntent mReuploadIntent;

    private CrawlDataHelper mDataHelper;
    private MetaManager mMeta;

    public static AlarmManager getInstance() {
        return INSTANCE;
    }

    private AlarmManager() {
        mDataHelper = CrawlDataHelper.getInstance();
        mMeta = MetaManager.getInstance();

        Intent intent = new Intent(CrawlConstants.Intents.OBSERVING);
        intent.setPackage(AzApplication.APP_CONTEXT.getPackageName());
        intent.setClass(AzApplication.APP_CONTEXT, CrawlService.class);
        mObservingIntent = PendingIntent.getService(AzApplication.APP_CONTEXT, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentReup = new Intent(CrawlConstants.Intents.RE_UPLOAD);
        intentReup.setPackage(AzApplication.APP_CONTEXT.getPackageName());
        intentReup.setClass(AzApplication.APP_CONTEXT, CrawlService.class);
        mReuploadIntent = PendingIntent.getService(AzApplication.APP_CONTEXT, 1, intentReup, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    public void setSearchAlarm(long searchId, boolean alarm) {
        if (alarm && !mDataHelper.isObserving()) {
            setObservingAlarm();
        }
        mDataHelper.updateSearchAlarm(searchId, alarm);

        if (!alarm && !mDataHelper.isObserving())
            cancelObservingAlarm();
    }

    public void setCommentAlarm(long articleId, boolean alarm) {
        if (alarm && !mDataHelper.isObserving()) {
            setObservingAlarm();
        }
        mDataHelper.updateCommentAlarm(articleId, alarm);

        if (!alarm && !mDataHelper.isObserving())
            cancelObservingAlarm();
    }

    public void setReUpAlarm(long articleId, boolean reup) {
        if (reup && !mDataHelper.isReUp()) {
            setReUpAlarm();
        }
        mDataHelper.updateAutoReUpload(articleId, reup);

        if (!reup && !mDataHelper.isReUp())
            cancelReUpAlarm();
    }

    public void setHasAlarmTimezone(boolean value) {
        mMeta.setHasAlarmTimezone(value);
        setObservingAlarm();
    }

    public void setAlarmTimezoneFrom(int hour, int min) {
        mMeta.setAlarmTimezoneFrom(hour, min);
        setObservingAlarm();
    }

    public void setAlarmTimezoneTo(int hour, int min) {
        mMeta.setAlarmTimezoneTo(hour, min);
        setObservingAlarm();
    }

    public void setObservingAlarm() {
        long schedule = 0;
        Calendar alarm = Calendar.getInstance();
        alarm.add(Calendar.MILLISECOND, CrawlConstants.OBSERVING_INTERVAL);
        Calendar from = null;
        Calendar to = null;
        if (mMeta.hasAlarmTimezone()) {
            from = mMeta.getAlarmTimezoneFrom();
            to = mMeta.getAlarmTimezoneTo();

            if (from.compareTo(to) > 0) {
                if (alarm.compareTo(to) < 0)
                    from.add(Calendar.DAY_OF_MONTH, -1);
                else
                    to.add(Calendar.DAY_OF_MONTH, 1);
            } else if (to.compareTo(alarm) < 0) {
                from.add(Calendar.DAY_OF_MONTH, 1);
                to.add(Calendar.DAY_OF_MONTH, 1);
            }
            LOG.f(TAG, "setObservingAlarm - timezone - " + mMeta.hasAlarmTimezone() + " : " + from.getTime() + " ~ " + to.getTime());
        }


        if (mMeta.hasAlarmTimezone()) {
            if (alarm.compareTo(from) > 0 && alarm.compareTo(to) < 0)
                schedule = alarm.getTimeInMillis();
            else
                schedule = from.getTimeInMillis();
        } else {
            schedule = alarm.getTimeInMillis();
        }

        LOG.f(TAG, "setObservingAlarm - schedule : " + AzUtil.getDateStringFromMils(AzApplication.APP_CONTEXT, schedule, true));
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) AzApplication.APP_CONTEXT.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(mObservingIntent);
        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, schedule, mObservingIntent);//, CrawlConstants.OBSERVING_INTERVAL, mObservingIntent);
    }

    private void cancelObservingAlarm() {
        LOG.f(TAG, "cancelObservingAlarm ");
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) AzApplication.APP_CONTEXT.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(mObservingIntent);
    }

    public void setReUpAlarm() {
        LOG.f(TAG, "setReUpAlarm ");
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) AzApplication.APP_CONTEXT.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(mReuploadIntent);
        alarmManager.setInexactRepeating(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + CrawlConstants.OBSERVING_INTERVAL, CrawlConstants.RE_UPLOAD_INTERVAL, mReuploadIntent);
    }

    private void cancelReUpAlarm() {
        LOG.f(TAG, "cancelReUpAlarm ");
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) AzApplication.APP_CONTEXT.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(mReuploadIntent);
    }

    public CrawlDataHelper.Search addSearchKeyword(String categoryId, String categoryTitle, String keyword, long lastestArticleId) {
        boolean isObserving = mDataHelper.isObserving();
        CrawlDataHelper.Search search = mDataHelper.insertSearchList(categoryId, categoryTitle, keyword, lastestArticleId);
        if (!isObserving)
            setObservingAlarm();
        return search;
    }

    public long addScrap(CrawlDataHelper.Article article) {
        boolean isObserving = mDataHelper.isObserving();
        long id = mDataHelper.insertScrap(article);
        if (!isObserving && mDataHelper.isObserving())
            setObservingAlarm();
        return id;
    }

    public void deleteKeyword(long id) {
        boolean isObserving = mDataHelper.isObserving();
        mDataHelper.deleteKeyword(id);
        if (isObserving && !mDataHelper.isObserving())
            cancelObservingAlarm();
    }

    public void deleteScrap(long articleId) {
        boolean isObserving = mDataHelper.isObserving();
        mDataHelper.deleteScrap(articleId);
        if (isObserving && !mDataHelper.isObserving())
            cancelObservingAlarm();
    }

}
