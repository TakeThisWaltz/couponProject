package com.azazel.framework.util;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import com.azazel.framework.AzApplication;
import com.azazel.framework.AzConstants;
import com.azazel.framework.AzException;

import java.util.Random;


public class AzUtil {
    private static String TAG = "AzUtil";

    private static boolean ctidWait = false;


    public static String generateCTID(int length) {
        StringBuilder sb = new StringBuilder();

        Random generator = new Random();
        for (int i = 0; i < length; i++) {

            sb.append(generator.nextInt(10));
            /*sb.append((int)(Math.random()*10));*/
        }
        return sb.toString();
    }


    public static void pause(final String ctid) {
        synchronized (ctid) {
            try {
                LOG.i(TAG, "Thread - wait : " + ctid);
                ctidWait = true;

                while (ctidWait) {
                    ctid.wait();
                }
                LOG.i(TAG, "Thread - notified : " + ctid);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void notify(final String ctid) {
        synchronized (ctid) {

            ctidWait = false;
            ctid.notifyAll();
            LOG.i(TAG, "Thread - notify : " + ctid);
        }
    }

    public static String getMacAddress(Context context) {
        WifiManager mngr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String addrMAC = mngr.getConnectionInfo().getMacAddress();

        return addrMAC;
    }

    //	public static String getClientDeviceId(final Context context){
    //		String clientDeviceId = null;
    //		final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    //
    //		final int phoneType = telephonyManager.getPhoneType();
    //		if(phoneType != TelephonyManager.PHONE_TYPE_NONE)
    //		{
    //			//there is a radio.
    //			clientDeviceId =telephonyManager.getDeviceId();
    //		}
    //		else
    //		{
    //			//there is no radio.
    //			clientDeviceId = Build.SERIAL;
    //		}
    //
    //		if(clientDeviceId== null || clientDeviceId.isEmpty() || clientDeviceId.equals("0"))
    //		{
    //			clientDeviceId = "";
    //		}
    //		return clientDeviceId;
    //	}

    public static boolean checkPackageByName(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            LOG.i(TAG, "checkPackageByName : " + packageName + ", OK");
            return true;
        } catch (NameNotFoundException e) {
            LOG.i(TAG, "checkPackageByName : " + packageName + ", It is not installed.");
            return false;
        }
    }


    public static String getStringWithLength(String org, int len) {
        String result = org;
        if (result.length() > len)
            result = result.substring(0, len);
        else {
            while (result.length() < len)
                result = result.concat("0");

        }
        return result;
    }


    public interface BitmapLoadListener {
        public void onLoad(Bitmap bitmap);
    }

    //	public static void bitmapLoaderFromNetwork(final String url, final BitmapLoadListener listener){
    //		LOG.i(TAG, "bitmapLoaderFromNetwork url : " + url);
    //		AzApplication.executeJobOnBackground(new IAzWorker() {
    //			Bitmap bitmap;
    //
    //			@Override
    //			public void preOperation() {
    //				LOG.i(TAG, "bitmapLoaderFromNetwork preOperation");
    //			}
    //
    //			@Override
    //			public void postOperation(boolean isError) {
    //				LOG.i(TAG, "bitmapLoaderFromNetwork postOperation : " + isError + ", bitmap : " + bitmap);
    //				listener.onLoad(bitmap);
    //			}
    //
    //			@Override
    //			public long getTimeout() {
    //				return 10000;
    //			}
    //
    //			@Override
    //			public void doInBackground() {
    //				LOG.i(TAG, "bitmapLoaderFromNetwork doInBackground ");
    //
    //				ServerManager.loadImageFromUrl(url, new FileResponseHandler() {
    //
    //					@Override
    //					public void handleResponse(String key, long size, InputStream stream) {
    //						LOG.i(TAG, "loadImageFromUrl handleResponse size : " + size);
    //						bitmap = BitmapFactory.decodeStream(stream);
    //					}
    //				});
    //
    //			}
    //		});
    //	}
    //

    public static String getDateStringFromMils(Context context, long time,
                                               boolean timeInclude) {
        String strDate = android.text.format.DateFormat
                .getMediumDateFormat(context).format(time).toString();
        if (timeInclude)
            strDate += " "
                    + android.text.format.DateFormat.getTimeFormat(context)
                    .format(time).toString();
        return strDate;
    }


    public static String getLongDateStringFromMils(Context context, long time,
                                                   boolean timeInclude) {
        String strDate = android.text.format.DateFormat
                .getLongDateFormat(context).format(time).toString();
        if (timeInclude)
            strDate += " "
                    + android.text.format.DateFormat.getTimeFormat(context)
                    .format(time).toString();
        return strDate;
    }


    public static String getTimeStringFromMils(Context context, long time) {
        return android.text.format.DateFormat.getTimeFormat(context)
                .format(time).toString();
    }


    public static boolean isNullOrEmpty(String str) {
        return str == null || "".equals(str) || str.equals("null");
    }

    public static boolean isNullOrEmptyWithTrim(String str) {
        if (str != null)
            return isNullOrEmpty(str.trim().replace("\n", ""));
        else return true;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static boolean checkNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) AzApplication.APP_CONTEXT.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) return false;

        boolean isMobileAvailable = false;
        boolean isMobileConnect = false;
        boolean isWifiAvailable = false;
        boolean isWifiConnect = false;

        NetworkInfo networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (networkInfo != null) {
            isMobileAvailable = networkInfo.isAvailable();
            isMobileConnect = networkInfo.isAvailable();
        }

        networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null) {
            isWifiAvailable = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isAvailable();
            isWifiConnect = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
        }

        if ((isWifiAvailable && isWifiConnect) || (isMobileAvailable && isMobileConnect)) {
            return true;
        } else {
            return false;
        }
    }


    public static Dialog makeTransparentBackgroundDialog(final Context activityContext, final int layout, final OnAzDialogCreateListener createListener) {
        return new Dialog(activityContext) {

            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                //setTheme(android.R.style.Theme_Translucent_NoTitleBar);
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                setContentView(layout);
                this.setCanceledOnTouchOutside(true);
                this.setCancelable(true);

//				int divierId = getContext().getResources()
//						.getIdentifier("android:id/titleDivider", null, null);
//				View divider = findViewById(divierId);
                // divider.setVisibility(View.GONE);
//				divider.setBackgroundColor(getContext().getResources().getColor(android.R.color.transparent));

                createListener.onAzDialogCreated(this);

            }

        };

    }

    public interface OnAzDialogCreateListener {
        public void onAzDialogCreated(Dialog dialog);
    }

    public static void printNowStackTrace() {
        try {
            throw new AzException(AzConstants.ResultCode.DO_NOTHING, "get stacktrace");
        } catch (Exception e) {
            LOG.e(TAG, "stacktrace", e);
        }

    }

    public static String printArray(int[] arr) {
        if (arr == null) {
            return "[printArray] array is null..";
        } else if (arr.length == 0) {
            return "[printArray] array is empty..";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[printArray] [0] : " + arr[0]);
        for (int i = 1; i < arr.length; i++) {
            sb.append(", [" + i + "] : " + arr[i]);

        }
        return sb.toString();
    }

    public static void printCursor(String tag, Cursor cs, boolean moveToFirst) {
        if (cs != null && cs.getCount() > 0) {
            int colCnt = cs.getColumnCount();
            while (cs.moveToNext()) {
                for (int i = 0; i < colCnt; i++) {
                    LOG.i(tag, "col - " + cs.getColumnName(i) + "\t : " + cs.getString(i));
                }
            }

            if (moveToFirst)
                cs.moveToFirst();
        } else
            LOG.d(tag, "printCursor - no data : " + cs);
    }

    public static String removeSpecialChars(String str) {
        String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
        str = str.replaceAll(match, " ");
        return str;
    }

    /**
     * hide KeyPad 키보드를 숨긴다
     */
    public static void hideKeyboard(EditText et) {
        if (et != null) {
            InputMethodManager imm = (InputMethodManager) AzApplication.APP_CONTEXT.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        }
    }
}
