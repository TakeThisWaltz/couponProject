package com.azazel.framework;

public abstract class AzSimpleWorker implements IAzWorker{

	private long timeout;
	private boolean isSuccess;
	private Object value;

	public AzSimpleWorker(){
		this.timeout = AzConstants.DEFAULT_TIMEOUT;
	}

	public AzSimpleWorker(long timeout){
		this.timeout = timeout;
	}

	@Override
	public long getTimeout() {
		return timeout;
	}

	@Override
	public boolean doInBackground() {
		try {
			doInBackgroundAndResult();
		}catch(AzRuntimeException e){
			setResult(false, e);
		}
		return isSuccess;
	}
	
	public void setResult(boolean isSuccess, Object value){
		this.isSuccess = isSuccess;
		this.value = value;
	}

    @Override
    public void preOperation(){}
	
	public abstract void doInBackgroundAndResult();

	@Override
	public void postOperation(boolean result) {
		if(!result && value == null)
			value = new AzRuntimeException(AzConstants.ResultCode.FAIL);
		postOperationWithResult(result, value);
	}
	
	public abstract void postOperationWithResult(boolean result, Object value);

}
