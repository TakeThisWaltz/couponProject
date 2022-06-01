package com.azazel.framework;

public interface IAzAppConfig {

	public String getPublisher();
	public String getName();
	public String getVersionString();
	public int getVersionCode();
	
	public boolean isLogEnabled();
	public boolean isFileLogEnabled();
	
	public boolean isServerMaintenanceMode();
}
