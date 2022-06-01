package com.azazel.cafecrawler;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.util.Base64;
import android.view.Display;
import android.view.WindowManager;

import com.azazel.cafecrawler.data.CrawlDataHelper;
import com.azazel.framework.AzApplication;
import com.azazel.framework.util.AES256Cipher;
import com.azazel.framework.util.AzUtil;
import com.azazel.framework.util.LOG;

import java.util.Calendar;
import java.util.List;

public class MetaManager {
    private static final String TAG = "MetaManager";
    private static final String EMPTY = "";

    private static MetaManager INSTANCE = new MetaManager();

    private String key = null;
    private byte[] ivBytes = null;
    private SharedPreferences mMeta;
    private static final String PREF_NAME = "tag_gallery";

    private Display mDisplay;
    private Point mSize;

    private CrawlDataHelper mDataHelper;
    private List<CrawlDataHelper.Category> mCategoryList;

    private interface Key {
        static final String IS_CATEGORY_LOADED = "IS_CATEGORY_LOADED_NEW";
        static final String USER_ID = "USER_ID";
        static final String HAS_ALARM_TIMEZONE = "HAS_ALARM_TIMEZONE";
        static final String VIBRATION_ALARM = "VIBRATION_ALARM";
        static final String ALARM_TIMEZONE_FROM = "ALARM_TIMEZONE_FROM";
        static final String ALARM_TIMEZONE_TO = "ALARM_TIMEZONE_TO";
        static final String RE_UPLOAD_INTERVAL = "RE_UPLOAD_INTERVAL";
    }

    private MetaManager() {
        mMeta = AzApplication.APP_CONTEXT.getSharedPreferences(PREF_NAME, 0);

        key = AzUtil.getStringWithLength(PREF_NAME + AzUtil.getMacAddress(AzApplication.APP_CONTEXT), 32);
        ivBytes = AES256Cipher.generateByteArr((byte) 0x00, 16);

        mDataHelper = CrawlDataHelper.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            mSize = new Point();
            ((WindowManager) AzApplication.APP_CONTEXT.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(mSize);
        } else {
            mDisplay = ((WindowManager) AzApplication.APP_CONTEXT.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        }


//        if (isCategoryLoaded())
//            mCategoryList = mDataHelper.getCategoryList();

    }

    public static MetaManager getInstance() {
        return INSTANCE;
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

    public boolean isCategoryLoaded() {
        //mMeta.getBoolean(Key.IS_CATEGORY_LOADED, false);
        List<CrawlDataHelper.Category> categories = mDataHelper.getCategoryList();
        LOG.d(TAG, "isCategoryLoaded - size : " + categories.size());
        if(categories.size() > 0){
            mCategoryList = categories;
            return true;
        }else return false;
    }

    public List<CrawlDataHelper.Category> getCategoryList() {
        if (mCategoryList == null)
            mCategoryList = mDataHelper.getCategoryList();

        return mCategoryList;
    }

    public boolean isAvailableCategory(String category) {
        return getCategoryList().contains(category);
    }

//    public void categoryLoaded(boolean loaded) {
//        mMeta.edit().putBoolean(Key.IS_CATEGORY_LOADED, loaded).commit();
//    }

    public boolean hasAlarmTimezone() {
        return mMeta.getBoolean(Key.HAS_ALARM_TIMEZONE, false)
                && !(mMeta.getInt(Key.ALARM_TIMEZONE_FROM + "_HOUR", 0) == mMeta.getInt(Key.ALARM_TIMEZONE_TO + "_HOUR", 0)
                && mMeta.getInt(Key.ALARM_TIMEZONE_FROM + "_MIN", 0) == mMeta.getInt(Key.ALARM_TIMEZONE_TO + "_MIN", 0));
    }

    public void setHasAlarmTimezone(boolean value) {
        mMeta.edit().putBoolean(Key.HAS_ALARM_TIMEZONE, value).commit();
    }

    public boolean isVibrationAlarm() {
        return mMeta.getBoolean(Key.VIBRATION_ALARM, true);
    }

    public void setVibrationAlarm(boolean value) {
        mMeta.edit().putBoolean(Key.VIBRATION_ALARM, value).commit();
    }

    public Calendar getAlarmTimezoneFrom() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, mMeta.getInt(Key.ALARM_TIMEZONE_FROM + "_HOUR", 0));
        cal.set(Calendar.MINUTE, mMeta.getInt(Key.ALARM_TIMEZONE_FROM + "_MIN", 0));
        return cal;
    }

    public Calendar getAlarmTimezoneTo() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, mMeta.getInt(Key.ALARM_TIMEZONE_TO + "_HOUR", 0));
        cal.set(Calendar.MINUTE, mMeta.getInt(Key.ALARM_TIMEZONE_TO + "_MIN", 0));
        return cal;
    }

    public void setAlarmTimezoneFrom(int hour, int min) {
        LOG.i(TAG, "setAlarmTimezoneFrom - " + hour + " : " + min);
        mMeta.edit().putInt(Key.ALARM_TIMEZONE_FROM + "_HOUR", hour)
                .putInt(Key.ALARM_TIMEZONE_FROM + "_MIN", min)
                .commit();
    }

    public void setAlarmTimezoneTo(int hour, int min) {
        LOG.i(TAG, "setAlarmTimezoneTo - " + hour + " : " + min);
        mMeta.edit().putInt(Key.ALARM_TIMEZONE_TO + "_HOUR", hour)
                .putInt(Key.ALARM_TIMEZONE_TO + "_MIN", min)
                .commit();
    }

    public int getReUpInterval() {
        return mMeta.getInt(Key.RE_UPLOAD_INTERVAL, 3);
    }

    public void setReUpInterval(int value) {
        LOG.i(TAG, "setReUpInterval : " + value);
        mMeta.edit().putInt(Key.RE_UPLOAD_INTERVAL, value).commit();
    }

    public String getUserId() {
        return mMeta.getString(Key.USER_ID, null);
    }

    public void setUserId(String userId) {
        LOG.i(TAG, "setUserId : " + userId);
        mMeta.edit().putString(Key.USER_ID, userId).commit();
    }


}
