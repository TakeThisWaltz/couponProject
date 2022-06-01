package com.azazel.framework.task;

import android.content.Context;
import android.content.Intent;

public interface ITaskHandler {
	public String getTag();
	public void onTimeout(final Context context, final Intent intent);
	
	public boolean hasPrepare();
	
	public boolean prepare(final int ctid, final ITaskTrigger trigger, final Context context, final Intent intent);

	public TaskResult doJob(final Context context, final Intent intent);
}
