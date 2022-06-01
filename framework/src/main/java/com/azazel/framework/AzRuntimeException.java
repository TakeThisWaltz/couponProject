package com.azazel.framework;

public class AzRuntimeException extends RuntimeException{
	private static final long serialVersionUID = 1L;	
	private  int mExceptionCode;	
	
	
	public AzRuntimeException(final int code) {		
		this.mExceptionCode = code;
	}	
	
	public AzRuntimeException(final int code, String msg) {		
		super(msg);		
		this.mExceptionCode = code;
	}	
	
	public AzRuntimeException(final int code, Throwable t) {		
		super(t);
		this.mExceptionCode = code;
	}	
	
	public AzRuntimeException(final int code, String msg, Throwable t) {		
		super(msg,t);
		this.mExceptionCode = code;
	}	
	
	public  int getExceptionCode() {
		return mExceptionCode;
	}

	@Override
	public String toString() {
		return super.toString() + ", eCode : " + mExceptionCode;
	}	

//	public  String getmExceptionDetails() {
//		return mExceptionDetails;
//	}

	
	
}
