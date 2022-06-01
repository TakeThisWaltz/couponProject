package com.azazel.framework.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import com.azazel.framework.AzConstants;
import com.azazel.framework.AzRuntimeException;

public class UriTool {
//	public static StringBuilder addUrlParameter(final StringBuilder sb, final String key, final String value) {
//		return addUrlParameter(sb, key, value, false);
//	}
	
	public static StringBuilder addUrlParameter(final StringBuilder sb, final String key, String value, final boolean isFirstParam) {
		//LOG.i("UriTool", "addUrlParameter - key : " + key + ", value : " + value);
		try {
            if(value == null) value = "";
			if(isFirstParam) {
				sb.append(URLEncoder.encode(key, "UTF_8") + "=" + URLEncoder.encode(value, "UTF_8"));
			}else
				sb.append("&" + URLEncoder.encode(key, "UTF_8") +"=" + URLEncoder.encode(value, "UTF_8"));
		} catch (UnsupportedEncodingException e) {
			LOG.e("UriTool", "addUrlParameter - key : " + key + ", value : " + value, e);
			throw new AzRuntimeException(AzConstants.ResultCode.FAIL_HTTP, e);
		} catch (NullPointerException e){
			LOG.i("UriTool", "NullPointerException : addUrlParameter - key : " + key + ", value : " + value);
			throw new AzRuntimeException(AzConstants.ResultCode.FAIL_HTTP, e);
		}
		return sb;
	}
	
	public static String addUrlParameters(final String baseUrl, final Map<String, String> params){
		if(params == null)
			return baseUrl;
		
		StringBuilder sb = new StringBuilder(baseUrl);
		if(!baseUrl.endsWith("?"))
			sb.append("?");
		boolean first = true;
		if(!sb.toString().endsWith("&"))
			first = false;
		for(String key : params.keySet()){
			addUrlParameter(sb, key, params.get(key), first);
			first = false;
		}
		return sb.toString();
	}
}
