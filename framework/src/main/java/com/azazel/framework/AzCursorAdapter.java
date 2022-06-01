package com.azazel.framework;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.CursorAdapter;

import com.azazel.framework.util.LOG;

abstract public class AzCursorAdapter extends CursorAdapter implements IAzContext{
	protected String mName;
	
	public AzCursorAdapter(Context context, String name, Cursor c, int flag) {
		super(context, c, flag);
		this.mName = name;
		LOG.i(getName(), "AzCursorAdapter");
		AzApplication.registerAzContext(this);
	}
	
	@Override
	public String getName(){
		return mName;
	}

	@Override
	public Handler getHandler() {
		return activityHandler;
	}
	
	@Override
	public void registerEventType(int[] eventType) {
		AzApplication.subscribeAction(this, eventType);
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
	
}
