package com.azazel.framework.task;

import com.azazel.framework.AzConstants;
import com.azazel.framework.AzRuntimeException;
import com.azazel.framework.util.LOG;

/**
 * Created by JI on 2015-03-24.
 */
public abstract class SingleTask{
    private static final String TAG = "SingleTask";

    private Object[] args;

    abstract public Object doJob(Object[] args);

    public SingleTask(){}

    public SingleTask(Object... args){
        this.args = args;
    }

    public void run(final int index, final SingleTaskCompleteListener taskListener){
        Thread thread = new Thread(){
            @Override
            public void run(){
                try{
                    Object result = doJob(args);
                    taskListener.onFinish(true, index, result, null);
                }catch(AzRuntimeException e){
                    LOG.e(TAG, "run err1 - " + e.getMessage(), e);
                    taskListener.onFinish(false, index, null, e);
                }catch(Exception e){
                    LOG.e(TAG, "run err2 - " + e.getMessage(), e);
                    taskListener.onFinish(false, index, null, new AzRuntimeException(AzConstants.ResultCode.FAIL_THREAD, e));
                }
            }
        };

        thread.start();

    }


    interface SingleTaskCompleteListener{
        void onFinish(boolean isSuccess, int index, Object result, AzRuntimeException e);
    }
}
