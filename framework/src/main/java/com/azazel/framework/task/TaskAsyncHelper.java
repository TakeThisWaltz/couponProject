package com.azazel.framework.task;

import com.azazel.framework.AzRuntimeException;
import com.azazel.framework.util.LOG;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by JI on 2015-03-24.
 */
public class TaskAsyncHelper {
    private static final String TAG = "TaskAsyncHelper";

    private Object mLock = new Object();

    private int mMaxRun;
    private AtomicInteger mNowRun = new AtomicInteger();
    private int mIndex;
    private ConcurrentLinkedQueue<SingleTask> mQueue;
    private AzRuntimeException mError;

    private SingleTask.SingleTaskCompleteListener mSingleTaskListener;
    private TaskCompleteListener mAllTaskCompletedListener;

    private Object[] mResultArr;
    private AtomicBoolean mIsQueueClosed = new AtomicBoolean();
    private boolean isBlockedBeforeCompleted = false;

    public TaskAsyncHelper(int maxAsyncRun, final boolean isBlockedBeforeCompleted) {
        mMaxRun = maxAsyncRun;
        mQueue = new ConcurrentLinkedQueue<SingleTask>();
        this.isBlockedBeforeCompleted = isBlockedBeforeCompleted;
        mSingleTaskListener = new SingleTask.SingleTaskCompleteListener() {

            @Override
            public void onFinish(boolean isSuccess, int index, Object result, AzRuntimeException e) {

                mResultArr[index] =  result;

                if (e != null && mError == null)
                    mError = e;

                LOG.i(TAG, "task finished - [" + index + "], mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);


                if (mIsQueueClosed.get() && mNowRun.decrementAndGet() == 0 && mQueue.size() == 0) {
                    if (isBlockedBeforeCompleted) {
                        LOG.i(TAG, "runTask notify1 wait , mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);
                        synchronized (mLock) {
                            LOG.i(TAG, "runTask notify2 wait , mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);
                            mLock.notifyAll();
                        }
                    } else {
                        LOG.i(TAG, "runTask finished without waiting , mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);
                        if (mAllTaskCompletedListener != null)
                            mAllTaskCompletedListener.onFinish((mError == null), mResultArr, mError);
                    }
                }else
                    run();
            }
        };
    }

    public void addTask(SingleTask task) {
        LOG.i(TAG, "addTask , mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);
        if (!mIsQueueClosed.get()) {
            mQueue.add(task);
        }
    }

    public void runTask(final TaskCompleteListener taskListener) {
        LOG.i(TAG, "runTask , mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);
        if (mIsQueueClosed.compareAndSet(false, true)) {
            mResultArr = new Object[mQueue.size()];
            mAllTaskCompletedListener = taskListener;
            if (mQueue.size() > 0 || mNowRun.get() > 0) {
                startTask();
            } else {
                if (mAllTaskCompletedListener != null)
                    mAllTaskCompletedListener.onFinish((mError == null), mResultArr, mError);
            }
        }
    }

    private void startTask(){
        run();
        if(isBlockedBeforeCompleted){
            synchronized (mLock) {
                try {
                    LOG.i(TAG, "runTask before wait, mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);
                    mLock.wait();
                    LOG.i(TAG, "runTask after wait , mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mAllTaskCompletedListener != null)
                    mAllTaskCompletedListener.onFinish((mError == null), mResultArr, mError);
            }
        }
    }

    private synchronized void run() {
        LOG.i(TAG, "task Run!! mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);
        while (mNowRun.get() < mMaxRun && mQueue.size() > 0) {
            SingleTask task = mQueue.poll();
            mNowRun.incrementAndGet();
            LOG.i(TAG, "task start - [" + mIndex + "], mNowRun : " + mNowRun + ", mQueue size : " + mQueue.size() + ", closed : " + mIsQueueClosed);
            task.run(mIndex++, mSingleTaskListener);
        }
    }

    public interface TaskCompleteListener {
        void onFinish(boolean isSuccess, Object[] result, AzRuntimeException e);
    }
}