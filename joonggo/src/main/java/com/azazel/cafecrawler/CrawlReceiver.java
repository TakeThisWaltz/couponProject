package com.azazel.cafecrawler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.framework.util.LOG;

/**
 * Created by JI on 2015-05-20.
 */
public class CrawlReceiver extends BroadcastReceiver {
    private static final String TAG = "CrawlReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.f(TAG, "onReceive : " + intent.getAction() + ", type : " + intent.getType());

        if (CrawlConstants.Intents.BOOT_COMPLETED.equals(intent.getAction())) {
            if(CrawlDataHelper.getInstance().isObserving())
                AlarmManager.getInstance().setObservingAlarm();
            if(CrawlDataHelper.getInstance().isReUp())
                AlarmManager.getInstance().setReUpAlarm();
        }
    }
}
