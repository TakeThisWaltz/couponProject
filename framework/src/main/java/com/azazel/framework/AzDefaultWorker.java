package com.azazel.framework;

public abstract class AzDefaultWorker implements IAzWorker{
	
	private long timeout;
	private boolean isSuccess;
	private Object value;
	
	public AzDefaultWorker(long timeout){
		this.timeout = timeout;
	}

	@Override
	public long getTimeout() {
		return timeout;
	}

	@Override
	public boolean doInBackground() {
		doInBackgroundAndResult();
		return isSuccess;
	}
	
	public void setResult(boolean isSuccess, Object value){
		this.isSuccess = isSuccess;
		this.value = value;
	}
	
	public void publishProgress(final long proc, final long total, final Object obj){
		AzApplication.executeUIJob(new Runnable() {
			
			@Override
			public void run() {
				onProgress(proc, total, obj);
			}
		});
	}
	
	public abstract void onProgress(long proc, long total, Object obj);
	
	public abstract void doInBackgroundAndResult();

	@Override
	public void postOperation(boolean result) {
		postOperationWithResult(result, value);
	}
	
	public abstract void postOperationWithResult(boolean result, Object value);

}
