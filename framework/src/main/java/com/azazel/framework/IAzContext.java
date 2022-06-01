package com.azazel.framework;

import android.os.Handler;
import android.os.Message;

public interface IAzContext {
	public String getName();

	public Handler getHandler();

	public void onEventReceived(Message msg);

	public void registerEventType(int[] eventType);

	public int[] getSubscribeEventType();

}
