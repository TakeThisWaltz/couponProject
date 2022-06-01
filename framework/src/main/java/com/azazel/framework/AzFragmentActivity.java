package com.azazel.framework;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;

import com.azazel.framework.util.LOG;

import java.lang.ref.WeakReference;

abstract public class AzFragmentActivity extends FragmentActivity implements IAzActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG.i(getName(), "onCreate");
        if (!this.pauseListenWhenActivityOnPause())
            AzApplication.registerAzContext(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LOG.i(getName(), "onDestroy");
        if (!this.pauseListenWhenActivityOnPause())
            AzApplication.deRegisterAzContext(this.getName());
    }

    @Override
    protected void onPause() {
        super.onPause();
        AzApplication.ACTIVATED_ACTIVITY = null;
        LOG.i(getName(), "onPause");
        if (this.pauseListenWhenActivityOnPause())
            AzApplication.deRegisterAzContext(this.getName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        AzApplication.ACTIVATED_ACTIVITY = this;
        LOG.i(getName(), "onResume");
        if (this.pauseListenWhenActivityOnPause())
            AzApplication.registerAzContext(this);
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

    @Override
    public boolean isListening() {
        boolean result = AzApplication.isRegistered(this);
        LOG.i(getName(), "isListening : " + result);
        return result;
    }

}