package com.azazel.framework;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;

import com.azazel.framework.util.LOG;

abstract public class AzObject implements IAzContext{

	public AzObject(){
		LOG.i(getName(), "AzObject");
		AzApplication.registerAzContext(this);
	}

	@Override
	public Handler getHandler() {
		return activityHandler;
	}

	protected final Handler activityHandler = new InnerClassHandler(this); 

	static class InnerClassHandler extends Handler{
		private final WeakReference<IAzContext> azContextRef;

		InnerClassHandler(IAzContext azContext){
			this.azContextRef = new WeakReference<IAzContext>(azContext);
		}

		@Override
		public void handleMessage(Message msg){
			IAzContext context = azContextRef.get();
			if(context != null)
				context.onEventReceived(msg);
		}
	}

	@Override
	public void registerEventType(int[] eventType) {
		AzApplication.subscribeAction(this, eventType);
	}

	@Override
	public int[] getSubscribeEventType() {
		return null;
	}

}
