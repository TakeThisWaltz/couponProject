package kr.azazel.barcode;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.util.Base64;
import android.view.Display;
import android.view.WindowManager;

import com.azazel.framework.AzApplication;
import com.azazel.framework.util.AES256Cipher;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;

import kr.azazel.barcode.vo.BarcodeSort;
import kr.azazel.barcode.vo.MyBarcode;

public class MetaManager {
    private static final String TAG = "MetaManager";
    private static final String EMPTY = "";

    private static final boolean ENCRYPT = false;

    private static MetaManager INSTANCE;

    private String key = null;
    private byte[] ivBytes = null;

    private SharedPreferences mMeta;
    private static final String PREF_NAME = AzAppConfig.NAME;

    private Display mDisplay;
    private Point mSize;

    private boolean isUIActivated;
    private boolean isAppForeground;
    private boolean isServiceAvailable;
    private boolean isServiceConnecting;

    private long uiActivatedTime;

    public static String SERVER_API_ADDRESS = "http://jweb.kr:9000";
    public static String SERVER_SOCKET_ADDRESS = null;


    private interface Key {
        static final String USER_ID = "USER_ID";
        static final String NICK_NAME = "NICK_NAME";
        static final String AUTH_TOKEN = "AUTH_TOKEN";
        static final String ACCESS_TOKEN = "ACCESS_TOKEN";
        static final String BARCODE_SORT_ = "BARCODE_SORT_";
    }

    private MetaManager() {
        mMeta = AzApplication.APP_CONTEXT.getSharedPreferences(PREF_NAME, 0);


        if (ENCRYPT) {
            key = AzUtil.getStringWithLength(PREF_NAME + AzUtil.getMacAddress(AzApplication.APP_CONTEXT), 32);
            ivBytes = AES256Cipher.generateByteArr((byte) 0x00, 16);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            mSize = new Point();
            ((WindowManager) AzApplication.APP_CONTEXT.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(mSize);
        } else {
            mDisplay = ((WindowManager) AzApplication.APP_CONTEXT.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        }
    }

    public static MetaManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new MetaManager();
        return INSTANCE;
    }

    public boolean isLoggedIn() {
        return getUserId() >= 0 && getAccessToken() != null;
    }

    public int getUserId() {
        return mMeta.getInt(Key.USER_ID, -1);
    }

    public void setUserId(int userId) {
        mMeta.edit().putInt(Key.USER_ID, userId).commit();
    }

    public String getNickName() {
        return mMeta.getString(Key.NICK_NAME, null);
    }

    public void setNickName(String nickName) {
        mMeta.edit().putString(Key.NICK_NAME, nickName).commit();
    }

    public String getAuthToken() {
        return mMeta.getString(Key.AUTH_TOKEN, null);
    }

    public void setAuthToken(String authToken) {
        mMeta.edit().putString(Key.AUTH_TOKEN, authToken).commit();
    }

    public String getAccessToken() {
        return mMeta.getString(Key.ACCESS_TOKEN, null);
    }

    public void setAccessToken(String accessToken) {
        mMeta.edit().putString(Key.ACCESS_TOKEN, accessToken).commit();
    }

    public String getExtractedExpireDate() {
        return mMeta.getString("ExtractedExpireDate", null);
    }

    public void setExtractedExpireDate(String value) {
        mMeta.edit().putString("ExtractedExpireDate", value).commit();
    }

    public void setBarcodeSortValue(MyBarcode.Category category, BarcodeSort sort) {
        mMeta.edit().putString(Key.BARCODE_SORT_ + category.name(), sort.name()).commit();
    }

    public BarcodeSort getBarcodeSortValue(MyBarcode.Category category) {
        return BarcodeSort.valueOf(mMeta.getString(Key.BARCODE_SORT_ + category.name(), BarcodeSort.REG_ASC.name()));
    }


    public int getDisplayWidth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            return mSize.x;
        } else {
            return mDisplay.getWidth();
        }
    }

    public int getDisplayHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            return mSize.y;
        } else {
            return mDisplay.getHeight();
        }
    }


    public void startServiceConnecting() {
        if (!isServiceConnecting) {
            AzApplication.sendBroadcastEmptyMessage(AzAppConstants.MAIN_SERVICE
                    , AzAppConstants.Event.SERVICE_CONNECTING_START);
            this.isServiceConnecting = true;
        }
    }

    public void finishServiceConnecting() {
        //AzUtil.printNowStackTrace();
        if (isServiceConnecting) {
            AzApplication.sendBroadcastEmptyMessage(AzAppConstants.MAIN_SERVICE
                    , AzAppConstants.Event.SERVICE_CONNECTING_END);
            this.isServiceConnecting = false;
        }
    }

    public boolean isServiceConnecting() {
        return isServiceConnecting;
    }

    public void setIsServiceAvailable(boolean value) {
        LOG.f(TAG, "setIsServiceAvailable : " + value);
        if (isServiceAvailable != value) {
            this.isServiceAvailable = value;
            AzApplication.sendBroadcastEmptyMessage(AzAppConstants.MAIN_SERVICE
                    , (value ? AzAppConstants.Event.SERVICE_AVAILABLE : AzAppConstants.Event.SERVICE_NOT_AVAILABLE));
        }
    }

    public boolean isServiceAvailable() {
        return isServiceAvailable;
    }
//
//    public boolean isServiceAvailable(boolean showToast) {
//        if (!isServiceAvailable && showToast) {
//            if (AzUtil.checkNetworkAvailable()) {
//                if (isServiceConnecting)
//                    RSUtil.showServerConnectingToast();
//                else
//                    RSUtil.showServerErrorToast();
//            } else
//                RSUtil.showNetworkErrorToast();
//        }
//        return isServiceAvailable;
//    }
//
//    public boolean isServiceAvailableAndStartService(boolean showToast) {
//        if (!isServiceAvailable) {
//            if (showToast) {
//                if (AzUtil.checkNetworkAvailable()) {
//                    if (isServiceConnecting)
//                        RSUtil.showServerConnectingToast();
//                    else
//                        RSUtil.showServerErrorToast();
//                } else
//                    RSUtil.showNetworkErrorToast();
//            }
//            AzApplication.APP_CONTEXT.startService(new Intent(AzApplication.APP_CONTEXT, RadioStarsService.class).setAction(AzAppConstants.Intents.START_APP));
//        }
//        return isServiceAvailable;
//    }

    public void setIsUIActivated(boolean isActivated) {
        this.isUIActivated = isActivated;
        setIsAppForeground(isActivated);
        if (isActivated)
            uiActivatedTime = System.currentTimeMillis();
    }

    public boolean isUIActivated() {
        return isUIActivated;
    }

    public void setIsAppForeground(boolean isAppForeground) {
        this.isAppForeground = isAppForeground;
    }

    public boolean isAppForeground() {
        return isAppForeground;
    }


    private String encrypt(String data) {
        try {
            if (EMPTY.equals(data))
                return data;
            byte[] cipherData = AES256Cipher.encrypt(ivBytes, key.getBytes("UTF-8"), data.getBytes("UTF-8"));
            String enData = Base64.encodeToString(cipherData, Base64.DEFAULT);

            LOG.d(TAG, "encrypt : " + data + " -> " + enData);
            return enData;
        } catch (Exception e) {
            LOG.e(TAG, "Encrypt Error !!", e);
            data = "";
            return data;
        }
    }

    private String decrypt(String data) {
        try {
            if (EMPTY.equals(data))
                return data;
            byte[] cipherData = AES256Cipher.decrypt(ivBytes, key.getBytes("UTF-8"), Base64.decode(data.getBytes("UTF-8"), Base64.DEFAULT));
            String deData = new String(cipherData, "UTF-8");
            LOG.d(TAG, "decrypt : " + data + " -> " + deData);
            return deData;
        } catch (Exception e) {
            LOG.e(TAG, "Decrypt Error !!", e);
            data = "";
            return data;
        }
    }

}
