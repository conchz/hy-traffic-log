package com.ss.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
/**
 * 
 * @description md5工具类
 * @author ZhangHuaRong   
 * @update 2015年7月15日 上午10:25:00
 */
public class Md5Util {
	/**  
     * MD5 加密  
     */   
    public static String getMD5Str(String str) {   
        MessageDigest messageDigest = null;   
   
        try {   
            messageDigest = MessageDigest.getInstance("MD5");   
   
            messageDigest.reset();   
   
            messageDigest.update(str.toUpperCase().getBytes("UTF-8"));   
        } catch (NoSuchAlgorithmException e) {   
            System.out.println("NoSuchAlgorithmException caught!");   
            System.exit(-1);   
        } catch (UnsupportedEncodingException e) {   
            e.printStackTrace();   
        }   
   
        byte[] byteArray = messageDigest.digest();   
   
        StringBuffer md5StrBuff = new StringBuffer();   
   
        for (int i = 0; i < byteArray.length; i++) {               
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)   
                md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));   
            else   
                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));   
        }   
   
        return md5StrBuff.toString();   
    }  

	/**  
     * MD5 加密   类型+browse
     */   
    public static String getMD5Str(Object...objects){
    	StringBuilder stringBuilder=new StringBuilder();
    	for (Object object : objects) {
			stringBuilder.append(object.toString());
		}
    	return getMD5Str(stringBuilder.toString());
    }
    
    
    public static void main(String[] args) {
    	System.out.println(getMD5Str("iphone","MicroMessenger"));
    	System.out.println(getMD5Str("iphone","Weibo"));
    	System.out.println(getMD5Str("iphone","baidubrowser"));
    	System.out.println(getMD5Str("iphone","baiduboxapp"));
    	System.out.println(getMD5Str("iphone","Safari"));
    	System.out.println(getMD5Str("android","UCBrowser"));
    	System.out.println(getMD5Str("android","Safari"));
    	System.out.println(getMD5Str("android","MQQBrowser"));
	}
}
