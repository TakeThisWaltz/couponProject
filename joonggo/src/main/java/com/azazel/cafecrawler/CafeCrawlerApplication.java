package com.azazel.cafecrawler;

import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import com.azazel.framework.AzApplication;
import com.azazel.framework.IAzAppConfig;
import com.azazel.framework.network.NetworkUtil.AzProgressListener;
import com.azazel.framework.util.FileTool;
import com.azazel.framework.util.LOG;

import java.io.File;
import java.io.InputStream;

//@ReportsCrashes
//(
//		formKey = "",
//		resToastText = R.string.toast_crash,
//		mode = ReportingInteractionMode.DIALOG,
//		resDialogIcon = android.R.drawable.ic_dialog_info,
//		resDialogTitle = R.string.toast_crash_dialog_title,
//		resDialogText = R.string.toast_crash_dialog_text,
//		mailTo = "risma825j@gmail.com"
//
//		)
public class CafeCrawlerApplication extends AzApplication{
	private static final String TAG = "CafeCrawlerApplication";
	
	static{
		Log.d(TAG, "Init config");
		AzApplication.APP_CONFIG = new IAzAppConfig() {
			
			@Override
			public boolean isServerMaintenanceMode() {
				return CafeCrawlerConfig.SERVER_MAINTENANCE_MODE;
			}
			
			@Override
			public boolean isLogEnabled() {
				return CafeCrawlerConfig.LOG_ENABLED;
			}
			
			@Override
			public boolean isFileLogEnabled() {
				return CafeCrawlerConfig.FILE_LOG_ENABLED;
			}
			
			@Override
			public String getVersionString() {
				return CafeCrawlerConfig.VERSION;
			}
			
			@Override
			public int getVersionCode() {
				return CafeCrawlerConfig.VERSION_CODE;
			}
			
			@Override
			public String getPublisher() {
				return CafeCrawlerConfig.PUBLISHER;
			}
			
			@Override
			public String getName() {
				return CafeCrawlerConfig.NAME;
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
	
	private void copyAsset(String fileName){
		try {
			AssetManager am = this.getResources().getAssets();
			InputStream is = am.open(fileName);
			String filePath = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/" + AzApplication.APP_CONFIG.getPublisher() + "/" + AzApplication.APP_CONFIG.getName() + "/" + fileName;
			
			File file = new File(filePath);
			if(file.exists())
				LOG.i(TAG, "remove old file : " + file.delete());
			FileTool.writeToFile(is, 0, filePath, new AzProgressListener() {
				
				@Override
				public void transferred(long now, long total) {
					LOG.i(TAG, "transferred : " + now + " / " + total);
				}
			});
			LOG.i(TAG, "copy complete : " + filePath);
		} catch (Exception e) {
			LOG.e(TAG, "copyAsset err", e);
		}

	}

}
