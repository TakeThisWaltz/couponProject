package com.azazel.framework;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
//import android.support.multidex.MultiDexApplication;
import android.util.SparseArray;
import android.view.View;

import com.azazel.framework.util.LOG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AzApplication extends Application//MultiDexApplication
{
    private static final String TAG = "AzApplication";

    public static Context APP_CONTEXT;
    public static Activity ACTIVATED_ACTIVITY;

    private static Handler APP_HANDLER;

    private static Dialog mProgress;

    public static IAzAppConfig APP_CONFIG;

    @Override
    public void onCreate() {
        APP_HANDLER = new Handler();
        super.onCreate();
        LOG.i(TAG, "onCreate");
        APP_CONTEXT = this.getApplicationContext();
    }

    static Map<String, Handler> HANDLERMAP = new HashMap<String, Handler>();
    static SparseArray<List<String>> STATIC_ACTION_MAP = new SparseArray<List<String>>();

    static SparseArray<List<String>> DYNAMIC_ACTION_MAP = new SparseArray<List<String>>();

    // static Map<String, List<Integer>> DYNAMIC_ACTION_MAP = new
    // HashMap<String, List<Integer>>();

    static void registerAzContext(IAzContext context) {
        if (context.getName() == null)
            throw new AzRuntimeException(AzConstants.ResultCode.FAIL_INITIALIZE);
        if (!HANDLERMAP.containsKey(context.getName())) {
            HANDLERMAP.put(context.getName(), context.getHandler());
            int[] svcArr = context.getSubscribeEventType();
            if (svcArr != null) {
                for (int svc : svcArr) {
                    List<String> ctxList = STATIC_ACTION_MAP.get(svc);
                    if (ctxList == null) {
                        ctxList = new ArrayList<String>();
                        STATIC_ACTION_MAP.put(svc, ctxList);
                    }
                    ctxList.add(context.getName());
                    LOG.i(TAG, "registerAzContext : " + context.getName() + " - > " + svc);
                }
            }
        }
    }

    static void deRegisterAzContext(String name) {
        if (HANDLERMAP.containsKey(name)) {
            HANDLERMAP.remove(name);
            for (int i = 0; i < STATIC_ACTION_MAP.size(); i++) {
                List<String> list = STATIC_ACTION_MAP.valueAt(i);
                if (list != null && list.contains(name))
                    list.remove(name);
            }
            for (int i = 0; i < DYNAMIC_ACTION_MAP.size(); i++) {
                List<String> list = DYNAMIC_ACTION_MAP.valueAt(i);
                if (list != null && list.contains(name))
                    list.remove(name);
            }
        }
    }

    static void subscribeAction(IAzContext context, int[] service) {
        if (service != null) {
            for (int svc : service) {
                List<String> staticList = STATIC_ACTION_MAP.get(svc);
                List<String> dynamicList = DYNAMIC_ACTION_MAP.get(svc);
                if (staticList != null
                        && !staticList.contains(context.getName())) {
                    if (dynamicList == null)
                        DYNAMIC_ACTION_MAP.put(svc,
                                (dynamicList = new ArrayList<String>()));
                    if (!dynamicList.contains(context.getName()))
                        dynamicList.add(context.getName());
                }
            }
        }
    }

    public static boolean isRegistered(String name) {
        return name != null && HANDLERMAP.containsKey(name);
    }

    public static void sendMessage(String name, Message msg) {
        Handler handler = HANDLERMAP.get(name);
        if (handler != null)
            handler.sendMessage(msg);
    }

    public static void sendMessageDelayed(String name, Message msg, long delayed) {
        Handler handler = HANDLERMAP.get(name);
        if (handler != null)
            handler.sendMessageDelayed(msg, delayed);
    }

    public static void sendEmptyMessage(String name, int what) {
        Handler handler = HANDLERMAP.get(name);
        if (handler != null)
            handler.sendEmptyMessage(what);
    }

    public static void sendEmptyMessageDelayed(String name, int what,
                                               long delayMillis) {
        Handler handler = HANDLERMAP.get(name);
        if (handler != null)
            handler.sendEmptyMessageDelayed(what, delayMillis);
    }

    public static void sendBroadcastMessage(int service, Message msg) {
        List<String> list = STATIC_ACTION_MAP.get(service);
        if (list != null)
            for (String name : list)
                HANDLERMAP.get(name).sendMessage(msg);

        list = DYNAMIC_ACTION_MAP.get(service);
        if (list != null)
            for (String name : list)
                HANDLERMAP.get(name).sendMessage(msg);
    }

    public static void sendBroadcastEmptyMessage(int service, int what) {
        List<String> list = STATIC_ACTION_MAP.get(service);
        if (list != null)
            for (String name : list)
                HANDLERMAP.get(name).sendEmptyMessage(what);

        list = DYNAMIC_ACTION_MAP.get(service);
        if (list != null)
            for (String name : list)
                HANDLERMAP.get(name).sendEmptyMessage(what);
    }


    public static void sendBroadcastEmptyMessageDelayed(int service, int what, long delayMillis) {
        List<String> list = STATIC_ACTION_MAP.get(service);
        if (list != null)
            for (String name : list)
                HANDLERMAP.get(name).sendEmptyMessageDelayed(what, delayMillis);

        list = DYNAMIC_ACTION_MAP.get(service);
        if (list != null)
            for (String name : list)
                HANDLERMAP.get(name).sendEmptyMessageDelayed(what, delayMillis);
    }


    public static void executeJobOnBackground(final IAzWorker worker) {

        final Thread thread = new Thread() {

            private boolean isFinish = false;
            private boolean isSuccess = false;

            @Override
            public void run() {
                final int threadCode = this.hashCode();

                LOG.i(TAG, "Thread[" + threadCode + "] : start background operation. handler : " + APP_HANDLER);

                Runnable timeoutTask = null;

                if (worker.getTimeout() > 0) {
                    timeoutTask = new Runnable() {

                        @Override
                        public void run() {
                            synchronized (this) {
                                if (!isFinish) {
                                    isFinish = true;
                                    LOG.f(TAG, "Thread[" + threadCode + "] : start post operation with timeout ");
                                    worker.postOperation(false);
                                    LOG.i(TAG, "Thread[" + threadCode + "] : finish post operation with timeout");
                                }
                            }
                        }

                    };
                    APP_HANDLER.postDelayed(timeoutTask, worker.getTimeout());
                    LOG.i(TAG, "Thread[" + threadCode + "] : set Timer : after " + worker.getTimeout());
                }


                try {
                    isSuccess = worker.doInBackground();
                } catch (Exception e) {
                    LOG.e(TAG, "Thread[" + threadCode + "] : err on background operation", e);
                    synchronized (this) {
                        isSuccess = false;
                    }
                }
                LOG.i(TAG, "Thread[" + threadCode + "] : finish background operation");

                if (timeoutTask != null)
                    APP_HANDLER.removeCallbacks(timeoutTask);

                APP_HANDLER.post(new Runnable() {

                    @Override
                    public void run() {
                        synchronized (this) {
                            LOG.i(TAG, "Thread[" + threadCode + "] : start post operation. isFinished : " + isFinish + " isSuccess : " + isSuccess);
                            if (!isFinish) {
                                isFinish = true;
                                worker.postOperation(isSuccess);
                                LOG.i(TAG, "Thread[" + threadCode + "] : finish post operation. isSuccess : " + isSuccess);
                            }
                        }
                    }

                });
            }

        };

        LOG.i(TAG, "Thread[" + thread.hashCode() + "] : start pre operation");
        worker.preOperation();
        LOG.i(TAG, "Thread[" + thread.hashCode() + "] : finish pre operation");

        thread.start();

    }

    public static void executeUIJob(final Runnable job) {
        APP_HANDLER.sendMessage(Message.obtain(APP_HANDLER, job));
    }

    public static void executeUIJob(final Runnable job, long delayed) {
        APP_HANDLER.sendMessageDelayed(Message.obtain(APP_HANDLER, job), delayed);
    }


    public static boolean isRegistered(IAzContext context) {
        return HANDLERMAP.containsKey(context.getName());
    }
    public static void setInterval(final RepeatableRunnable job, final boolean newThread, final long interval) {
        setInterval(job, newThread, interval, 0);
    }

    public static void setInterval(final RepeatableRunnable job, final boolean newThread, final long interval, final int maxTry) {
        LOG.i(TAG, "setInterval [" + job.hashCode() + "] - newThread : " + newThread + ", interval : " + interval + ", maxTry : " + maxTry);
        final Runnable task = new Runnable() {
            private Runnable mTask = this;
            private int tryCnt = 0;

            @Override
            public void run() {
                tryCnt++;
                if(maxTry > 0 && tryCnt > maxTry) {
                    LOG.e(TAG, "job is failed by max retries : " + maxTry);
                    job.onFailedByMaxRetry();
                }else if (newThread) {
                    new Thread() {
                        @Override
                        public void run() {
                            LOG.d(TAG, "job will be executed now in new thread.. tryCnt : " + tryCnt);
                            if (job.doJob()) {
                                LOG.i(TAG, "setInterval [" + job.hashCode() + "] - job was executed and it will be executed again after : " + interval);
                                APP_HANDLER.sendMessageDelayed(Message.obtain(APP_HANDLER, mTask), interval);
                            } else {
                                LOG.i(TAG, "setInterval [" + job.hashCode() + "] - job was executed and interval finished");
                            }
                        }
                    }.start();
                } else {
                    LOG.d(TAG, "job will be executed now.. tryCnt : " + tryCnt);
                    if (job.doJob()) {
                        LOG.i(TAG, "setInterval [" + job.hashCode() + "] - job was executed and it will be executed again after : " + interval);
                        APP_HANDLER.sendMessageDelayed(Message.obtain(APP_HANDLER, mTask), interval);
                    } else {
                        LOG.i(TAG, "setInterval [" + job.hashCode() + "] - job was executed and interval finished");
                    }
                }
            }
        };

        APP_HANDLER.sendMessage(Message.obtain(APP_HANDLER, task));
    }

    public interface RepeatableRunnable {
        public boolean doJob();
        public void onFailedByMaxRetry();
    }

    public static void showProgress() {
        if (mProgress != null && mProgress.isShowing()) {
            try {
                mProgress.cancel();
            } catch (Exception e) {
                LOG.e(TAG, "error on process dialog cancelling.. it's not fatal.", e);
            }
        }

        if (ACTIVATED_ACTIVITY == null) {
            LOG.e(TAG, "UI is not activated!! unable to show progress dialog..", null);
            return;
        }
        mProgress = new Dialog(ACTIVATED_ACTIVITY) {

            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                //setTheme(android.R.style.Theme_Translucent_NoTitleBar);
                //requestWindowFeature(Window.FEATURE_NO_TITLE);
                setContentView(R.layout.dialog_progress);
                getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                this.setCanceledOnTouchOutside(false);
                this.setCancelable(false);

                int divierId = getContext().getResources()
                        .getIdentifier("android:id/titleDivider", null, null);
                View divider = findViewById(divierId);
                if(divider != null)
                    divider.setBackgroundColor(getContext().getResources().getColor(android.R.color.transparent));
            }

        };
        mProgress.show();
    }

    //	public static void showProgress(){
    //		showProgress(APP_CONTEXT);
    //	}
    public static void stopProgress() {
        if (mProgress != null && mProgress.isShowing()) {
            try {
                mProgress.cancel();
            } catch (Exception e) {
                LOG.e(TAG, "error on process dialog cancelling.. it's not fatal.", e);
            }
        }
    }

}