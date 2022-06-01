package kr.azazel.barcode;

import android.util.Log;

import com.azazel.framework.AzApplication;
import com.azazel.framework.IAzAppConfig;
import com.azazel.framework.util.LOG;

/**
 * Created by ji on 2016. 10. 12..
 */

public class BarcodeApplication extends AzApplication {
    private static final String TAG = "BarcodeApplication";

    static{
        Log.d(TAG, "Init config");
        AzApplication.APP_CONFIG = new IAzAppConfig() {

            @Override
            public boolean isServerMaintenanceMode() {
                return AzAppConfig.SERVER_MAINTENANCE_MODE;
            }

            @Override
            public boolean isLogEnabled() {
                return AzAppConfig.LOG_ENABLED;
            }

            @Override
            public boolean isFileLogEnabled() {
                return AzAppConfig.FILE_LOG_ENABLED;
            }

            @Override
            public String getVersionString() {
                return AzAppConfig.VERSION;
            }

            @Override
            public int getVersionCode() {
                return AzAppConfig.VERSION_CODE;
            }

            @Override
            public String getPublisher() {
                return AzAppConfig.PUBLISHER;
            }

            @Override
            public String getName() {
                return AzAppConfig.NAME;
            }
        };

        Log.d(TAG, "Init config finished");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LOG.f(TAG, "onCreate");
        //ACRA.init(this);

        //copyAsset("lucia.dat");

    }
}
