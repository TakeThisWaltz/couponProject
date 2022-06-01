package com.azazel.framework;

public interface IAzWorker {
	
	public long getTimeout();

	public void preOperation();
	
	public boolean doInBackground();
	
	public void postOperation(boolean result);
}
