package com.azazel.framework.task;

import java.util.HashMap;
import java.util.Map;

public class TaskResult {

	private boolean isSucceeded = false;
	private int rcode = -1;
	private String resultMsg;

	private Map<String, String> dataMap;

	public boolean isSucceeded() {
		return isSucceeded;
	}

	public void setSucceeded(boolean isSucceeded) {
		this.isSucceeded = isSucceeded;
	}

	public int getRcode() {
		return rcode;
	}

	public void setRcode(int rcode) {
		this.rcode = rcode;
	}

	public String getResultMsg() {
		return resultMsg;
	}

	public void setResultMsg(String resultMsg) {
		this.resultMsg = resultMsg;
	}

	public void putStringData(String key, String value){
		checkDataMap();
		dataMap.put(key, value);
	}

	public String getStringData(String key){
		if(dataMap!= null)
			return dataMap.get(key);
		return null;
	}

	private void checkDataMap(){
		if(dataMap == null)
			dataMap = new HashMap<String, String>();
	}

	@Override
	public String toString(){
		StringBuilder result = new StringBuilder("TaskResult - isSuc: " + isSucceeded + ", rcode: " + rcode + ", resultMsg: " + resultMsg + ", ");
		if(dataMap != null){
			result.append("\nDataMap : ");
			for(String key : dataMap.keySet()){
				result.append(key + "-" + dataMap.get(key) + ", ");
			}
		}
		return result.toString();
	}

}
