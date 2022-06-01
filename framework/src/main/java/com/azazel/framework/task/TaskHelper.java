package com.azazel.framework.task;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.azazel.framework.AzConstants;
import com.azazel.framework.task.TaskHelper.TaskResultListener;
import com.azazel.framework.util.LOG;

public class TaskHelper {
	private static final String TAG = "TaskHelper";

	private static ConcurrentMap<Integer, TaskObject> TASK_MAP = new ConcurrentHashMap<Integer, TaskObject>();

//	public static Handler MAIN_TASK_HANDLER = new Handler(Looper.getMainLooper());
	
	private static TaskResult COMMON_TIMEOUT_FAILED_RESULT = new TaskResult();
	static{ COMMON_TIMEOUT_FAILED_RESULT.setSucceeded(false);
	COMMON_TIMEOUT_FAILED_RESULT.setRcode(AzConstants.TASK_STATUS_CODE.PROCESS_TIMEOUT);}
	
	private static TaskResult COMMON_PREPARE_FAILED_RESULT = new TaskResult();
	static{ COMMON_PREPARE_FAILED_RESULT.setSucceeded(false);
	COMMON_PREPARE_FAILED_RESULT.setRcode(AzConstants.TASK_STATUS_CODE.PREPARE_FAILED);}
	
	private static Handler DEFAULT_TIMEOUT_HANDLER = new Handler(Looper.getMainLooper()) {
		public void handleMessage(final Message msg) {
			final int what=msg.what;
			Runnable runTimeOut = new Runnable() {
				@Override
				public void run() {
					TaskObject task = TASK_MAP.remove(what);
					if(task != null && task.handler != null){
						LOG.i(TAG, "doTask-timeout - " + task.handler.getTag() + " : " + task.hashCode() + ", retryCnt : " + task.retryCnt);
						task.handler.onTimeout(task.context, task.intent);
						task.listener.onCompleted(COMMON_TIMEOUT_FAILED_RESULT, task.context, task.intent);			
					}else
						LOG.i(TAG, "Already removed from TaskMap : " + what);
				}
			};
			
			Thread thread = new Thread(runTimeOut,"TIMEOUT_THREAD");
			thread.start();
		}
	};
	
	private static ITaskTrigger DEFAULT_TASK_TRIGGER = new ITaskTrigger() {
		
		@Override
		public void prepared(int ctid, final Context context, final Intent intent) {
			final TaskObject task = TASK_MAP.get(ctid);
			if(task!=null){
				task.context = context;
				task.intent = intent;
				LOG.i(TAG, "doTask-after prepare - " + task.handler.getTag() + " : " + task.hashCode());

				Runnable run = new Runnable() {
					@Override
					public void run() {
						LOG.i(TAG, "doTask-excute doJob - " + task.handler.getTag() + " : " + task.hashCode());
						TaskResult result = task.handler.doJob(task.context, task.intent);
						if(TASK_MAP.containsKey(task.hashCode()) && (result.isSucceeded() || task.retryCnt>=task.taskOption.getMaxRetryCount())){
							//Finished..
							TASK_MAP.remove(task.hashCode());
							LOG.i(TAG, "Remove from TaskMap - " + task.handler.getTag() + " : " + task.hashCode() + ", retryCnt : " + task.retryCnt);
							task.listener.onCompleted(result, context, intent);

						}else if(TASK_MAP.containsKey(task.hashCode()) && !result.isSucceeded() && task.taskOption.getMaxRetryCount() > 0){
							// Retry
							boolean isSuc = false;
							for(;task.retryCnt<task.taskOption.getMaxRetryCount();){
								task.retryCnt = task.retryCnt + 1;
								LOG.i(TAG, "doTask-retry prepare - " + task.handler.getTag() + " : " + task.hashCode() + " : " + task.retryCnt);
								if((isSuc = task.handler.prepare(task.hashCode(), DEFAULT_TASK_TRIGGER, context, intent)) 
										|| !TASK_MAP.containsKey(task.hashCode()))
									break;
							}

							if(!isSuc && TASK_MAP.containsKey(task.hashCode())){
								TASK_MAP.remove(task.hashCode());
								LOG.i(TAG, "Remove from TaskMap - " + task.handler.getTag() + " : " + task.hashCode() + ", retryCnt : " + task.retryCnt);
								task.listener.onCompleted(COMMON_PREPARE_FAILED_RESULT, context, intent);
							}

						}
					}
				};
				Thread thread = new Thread(run,"TRIGGER_THREAD");
				thread.start();
			}
		}
	};

	public static void doTask(final Context context, final Intent intent, final ITaskHandler handler, final ITaskOption taskOption
			, final TaskResultListener listener){

		final TaskObject task = new TaskObject(context, intent, handler, taskOption, listener);

		if(handler.hasPrepare()){

			Runnable runPrepare = new Runnable() {
				@Override
				public void run() {
					LOG.i(TAG, "doTask-excute prepare - " + task.handler.getTag() + " : " + task.hashCode());
					boolean isSuc = handler.prepare(task.hashCode(), DEFAULT_TASK_TRIGGER, context, intent);
					if(!isSuc && TASK_MAP.containsKey(task.hashCode()) && taskOption.getMaxRetryCount() > 0){
						for(;task.retryCnt<taskOption.getMaxRetryCount();){
							task.retryCnt = task.retryCnt + 1;
							LOG.i(TAG, "doTask-retry prepare - " + task.handler.getTag() + " : " + task.hashCode() + " : " + task.retryCnt);
							if((isSuc = handler.prepare(task.hashCode(), DEFAULT_TASK_TRIGGER, context, intent))
									|| !TASK_MAP.containsKey(task.hashCode()))
								break;
						}
					}
					
					if(!isSuc && TASK_MAP.containsKey(task.hashCode())){
						TASK_MAP.remove(task.hashCode());
						LOG.i(TAG, "Remove from TaskMap - " + task.handler.getTag() + " : " + task.hashCode() + ", retryCnt : " + task.retryCnt);
						listener.onCompleted(COMMON_PREPARE_FAILED_RESULT, context, intent);
					}
				}
			};
			Thread threadPrepare = new Thread(runPrepare,"PREPARED_THREAD");

			TASK_MAP.put(task.hashCode(), task);
			if(taskOption.getTimeout() > 0){
				LOG.i(TAG, "doTask-setTimeout - " + task.handler.getTag() + " : " + task.hashCode() + ", after "  + taskOption.getTimeout());
				DEFAULT_TIMEOUT_HANDLER.sendEmptyMessageDelayed(task.hashCode(), taskOption.getTimeout());
			}
			threadPrepare.start();
		}else{

			Runnable run = new Runnable() {
				@Override
				public void run() {
					LOG.i(TAG, "doTask-excute doJob - " + task.handler.getTag() + " : " + task.hashCode());
					TaskResult result = handler.doJob(context, intent);
					if(!result.isSucceeded() && TASK_MAP.containsKey(task.hashCode()) && taskOption.getMaxRetryCount() > 0){
						for(;task.retryCnt<taskOption.getMaxRetryCount();){
							task.retryCnt = task.retryCnt + 1;
							LOG.i(TAG, "doTask-retry doJob - " + task.handler.getTag() + " : " + task.hashCode() + " : " + task.retryCnt);
							if((result = handler.doJob(context, intent)).isSucceeded() 
									|| !TASK_MAP.containsKey(task.hashCode()))
								break;
						}
					}
					
					if(TASK_MAP.containsKey(task.hashCode())){
						TASK_MAP.remove(task.hashCode());
						LOG.i(TAG, "Remove from TaskMap - " + task.handler.getTag() + " : " + task.hashCode() + ", retryCnt : " + task.retryCnt);
						listener.onCompleted(result, context, intent);
					}
				}
			};
			Thread thread = new Thread(run,"EXCUTE_THREAD");
			TASK_MAP.put(task.hashCode(), task);
			if(taskOption.getTimeout() > 0){
				LOG.i(TAG, "doTask-setTimeout - " + task.handler.getTag() + " : " + task.hashCode() + ", after "  + taskOption.getTimeout());
				DEFAULT_TIMEOUT_HANDLER.sendEmptyMessageDelayed(task.hashCode(), taskOption.getTimeout());
			}
			thread.start();
		}
	}

	public interface TaskResultListener{
		public void onCompleted(final TaskResult result, final Context context, final Intent intent);
	}
	
	
}
class TaskObject{
	
	Context context;
	Intent intent;
	ITaskHandler handler;
	ITaskOption taskOption;
	TaskResultListener listener;
	int retryCnt;

	TaskObject(Context context, Intent intent, ITaskHandler handler,
			ITaskOption taskOption, TaskResultListener listener) {
		this.retryCnt = 0;
		this.context = context;
		this.intent = intent;
		this.handler = handler;
		this.taskOption = taskOption;
		this.listener = listener;
	}


}
