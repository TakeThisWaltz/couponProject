package com.azazel.framework.task;

import android.content.Context;
import android.content.Intent;

public interface ITaskTrigger {

	public void prepared(int ctid, Context context, Intent intent);
}
