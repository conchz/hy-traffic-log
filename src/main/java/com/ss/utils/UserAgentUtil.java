package com.ss.utils;

import java.util.ResourceBundle;

/**
 * 
 * @description 根据参数获取资源文件里的useragent信息
 * @author ZhangHuaRong   
 * @update 2015年7月15日 上午10:27:05
 */

public class UserAgentUtil {
	
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("agent");


	public static String readProperties(String key) {
		return BUNDLE.getString(key);

	}


	
	public static String getUserAgent(String type,String model,String borwse,String osVersion){
		String key = Md5Util.getMD5Str(type,borwse);
		String template = readProperties(key);
		String result = doReplace(type,osVersion,template,model);
		return result;
	}

	private static String doReplace(String type, String osVersion,	String template,String model) {
		if(template!=null ){
			if(type.equals("iphone")){
				return template.replace("version", osVersion);
			}else if(type.equals("Android")){
				return template.replace("version", osVersion).replace("model", model);
			}
		}
		return null;
	}

	public static void main(String[] args) {
		System.out.println(BUNDLE.getObject("windowphone1"));

		System.out.println(getUserAgent("iphone","iphone","MicroMessenger","8_4"));
		System.out.println(getUserAgent("iphone","iphone","Weibo","7_2"));
		System.out.println(getUserAgent("iphone","iphone","baidubrowser","8_4"));
		System.out.println(getUserAgent("iphone","iphone","baiduboxapp","8_4"));
		System.out.println(getUserAgent("iphone","iphone","Safari","8_4"));
		System.out.println(getUserAgent("Android","MI 3","UCBrowser","4.4.4"));
		System.out.println(getUserAgent("Android","M032","Safari","4.4.4"));
		System.out.println(getUserAgent("Android","M032","MQQBrowser","4.4.4"));
		
		
	}

}
