package com.azazel.framework.util;

import java.io.File;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.os.Environment;
import android.util.Log;

import com.azazel.framework.AzApplication;

public class LOG {

	private static final String LOG_EXT = ".log";
	private static final String TAG = AzApplication.APP_CONFIG==null?"Azazel":AzApplication.APP_CONFIG.getName();

	final static String logPath = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/" + (AzApplication.APP_CONFIG==null?"Azazel":AzApplication.APP_CONFIG.getPublisher()) + "/" + TAG + "/";
	final static SimpleDateFormat formatter = new SimpleDateFormat(
			"MM.dd_HH.mm.ss", Locale.KOREA);
	
	final static boolean LOG_ENABLE = AzApplication.APP_CONFIG==null?true:AzApplication.APP_CONFIG.isLogEnabled();
	final static boolean FILE_LOG = AzApplication.APP_CONFIG==null?false:AzApplication.APP_CONFIG.isFileLogEnabled();

	final static int MAX_FILE_SIZE = 5 * 1024 * 1024;
	static File mFile;
	static PrintWriter mWriter;

	public static String getLogPath(){
		return logPath;
	}
	
	private static PrintWriter getLogWriter() {
		if (mWriter == null) {
			synchronized (LOG_EXT) {
				if (mWriter == null) {
					try {
						final File folderPath = new File(logPath);
						if (!folderPath.exists()) {
							folderPath.mkdirs();
							Log.i(TAG, "create dir : " + logPath);
						}

						mFile = new File(logPath
								+ formatter.format(System.currentTimeMillis())
								+ LOG_EXT);
						mWriter = new PrintWriter(mFile);
						Log.i(TAG, "create writer : " + logPath);
					} catch (Exception e) {
						Log.i(TAG, "create error : " + e.getMessage());
						e.printStackTrace();
						mWriter.close();
						mWriter = null;
					}
				}
			}
		} else if (mFile.length() > MAX_FILE_SIZE) {
			synchronized (LOG_EXT) {
				if (mFile.length() > MAX_FILE_SIZE) {
					try {
						mWriter.close();
						mFile = new File(logPath
								+ formatter.format(System.currentTimeMillis())
								+ LOG_EXT);
						mWriter = new PrintWriter(mFile);
						Log.i(TAG, "create writer : " + logPath);
					} catch (Exception e) {
						Log.i(TAG, "create error : " + e.getMessage());
						e.printStackTrace();
						mWriter.close();
						mWriter = null;
					}
				}
			}
		}
		return mWriter;
	}

	private static void writeLog(final String tag, final String msg) {
		writeLog(tag, msg, null);
	}

	private static void writeLog(final String tag, final String msg,
			final Throwable tr) {
		if (FILE_LOG) {
			try {
				PrintWriter writer = getLogWriter();
				if (writer != null) {
					writer.write("["
							+ formatter.format(System.currentTimeMillis())
							+ "][" + tag + "]" + msg + "\n");
					if (tr != null)
						tr.printStackTrace(writer);
					writer.flush();
				}
			} catch (Exception e) {
				Log.i(TAG, "write error : " + e.getMessage());
				e.printStackTrace();
				mWriter.close();
				mWriter = null;
			}
		}
	}

	public static void i(final String tag, final String msg) {
		if (LOG_ENABLE && msg != null) {
			Log.i(tag, msg);
			writeLog(tag, msg);
		}
	}

	public static void d(final String tag, final String msg) {
		if (LOG_ENABLE && msg != null) {
			Log.d(tag, msg);
			writeLog(tag, msg);
		}
	}

	public static void v(final String Tag, final String msg) {
		if (LOG_ENABLE && msg != null) {
			Log.v(Tag, msg);
			writeLog(Tag, msg);
		}
	}

	public static void w(final String Tag, final String msg) {
		if (LOG_ENABLE && msg != null) {
			Log.w(Tag, msg);
			writeLog(Tag, msg);
		}
	}

	// force logging.
	public static void e(final String Tag, String msg, final Throwable tr) {
		if (msg != null) {
            if(tr instanceof UnknownHostException)
                msg += "UnknownHostException : " +  tr.getMessage();
			Log.e(Tag, msg, tr);
			writeLog(Tag, msg, tr);
		}
	}

	// force logging.
	public static void e(final String Tag, final String msg) {
		if (msg != null) {
			Log.e(Tag, msg);
			writeLog(Tag, msg);
		}
	}

	// force logging.
	public static void f(final String Tag, final String msg) {
		if (msg != null) {
			Log.i(Tag, msg);
			writeLog(Tag, msg);
		}
	}
}
