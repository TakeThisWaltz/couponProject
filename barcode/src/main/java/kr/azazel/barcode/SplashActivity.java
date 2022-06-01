package kr.azazel.barcode;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.Toast;

import com.azazel.framework.AzActivity;
import com.azazel.framework.AzApplication;
import com.azazel.framework.util.LOG;

/**
 * Created by JI on 2015-03-17.
 */
public class SplashActivity extends AzActivity {
    public static final String TAG = "SplashActivity";

    private boolean isInitCompleted = false;

    @Override
    public int[] getSubscribeEventType() {
        return null;
    }

    @Override
    public boolean pauseListenWhenActivityOnPause() {
        return false;
    }

    @Override
    public String getName() {
        return TAG;
    }

    int RETRY_CNT = 0;

    @Override
    public void onEventReceived(Message msg) {
        LOG.i(TAG, "onEventReceived : " + msg.what + ", obj : " + msg.obj);
        switch (msg.what) {
            case AzAppConstants.Event.EXIT: {
                LOG.i(TAG, "EXIT!!");
                isInitCompleted = true;
                finish();
                break;
            }

            case AzAppConstants.Event.TRY_EXIT: {
                LOG.i(TAG, "TRY_EXIT!!");
                exitFlag = false;
                break;
            }
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash);

        AzApplication.sendEmptyMessageDelayed(TAG, AzAppConstants.Event.EXIT, 1000);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    private boolean exitFlag = false;
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (exitFlag == false) {
                Toast.makeText(this, R.string.toast_exit,
                        Toast.LENGTH_SHORT).show();
                exitFlag = true;
                AzApplication.sendEmptyMessageDelayed(TAG, AzAppConstants.Event.TRY_EXIT, 2000);
                return false;
            } else {
                AzApplication.sendEmptyMessage(MainActivity.TAG, AzAppConstants.Event.EXIT);
                AzApplication.sendEmptyMessage(TAG, AzAppConstants.Event.EXIT);
                return false;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

}

