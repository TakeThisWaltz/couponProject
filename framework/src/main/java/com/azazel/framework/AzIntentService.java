package com.azazel.framework;

import java.lang.ref.WeakReference;

import android.app.IntentService;
import android.os.Handler;
import android.os.Message;

import com.azazel.framework.util.LOG;

abstract public class AzIntentService extends IntentService implements IAzContext {

	public AzIntentService(String name) {
		super(name);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		LOG.i(getName(), "onCreate");
		AzApplication.registerAzContext(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LOG.i(getName(), "onDestroy");
		AzApplication.deRegisterAzContext(this.getName());
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

	@Override
	public Handler getHandler() {
		return activityHandler;
	}

}