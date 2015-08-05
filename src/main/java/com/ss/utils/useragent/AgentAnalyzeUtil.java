package com.ss.utils.useragent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.lang3.StringUtils;


/**
 * 
 * @description 解析user-agent工具类
 * @author ZhangHuaRong   
 */
public class AgentAnalyzeUtil {

	public static void main(String[] args) {
		
		String str = "Mozilla/5.0 (Linux; U; Android 4.4.4; zh-CN; MI 3 Build/KTU84P) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/10.5.1.597 U3/0.8.0 Mobile Safari/534.30";
		String MicroMessenger = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_4 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H143 MicroMessenger/6.2.2 NetType/WIFI Language/zh_CN";
		String Weibo ="Mozilla/5.0 (iPhone; CPU iPhone OS 8_4 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H143 Weibo (iPhone7,1__weibo__5.3.0__iphone__os8.4)";
		String baidubrowser = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_4 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.4 Mobile/12H143 Safari/600.1.4 baidubrowser/5.1.6.5 (Baidu; P2 8.4)";
		String baiduboxapp = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_4 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H143 rabbit%2F1.0 baiduboxapp/0_0.0.6.6_enohpi_4331_057/4.8_2C2%257enohPi/1099a/893E895E67C946569B0A05008C44C8A0AC3014D21ORNBRGCJPO/1";
		String str1 = "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1 ";
	
		
		
           
		Map<String,String> result = analyze(str);
		
		System.out.println(result);

	}
	/**
	 * 
	 * @param useragent
	 * @description 根据useragent判断是否来自移动端
	 */
	public static boolean judgeIsMoblie(String useragent) {
		boolean isMoblie = false;
		String[] mobileAgents = { "iphone", "android", "phone", "mobile", "wap", "netfront", "java", "opera mobi",
				"opera mini", "ucweb", "windows ce", "symbian", "series", "webos", "sony", "blackberry", "dopod",
				"nokia", "samsung", "palmsource", "xda", "pieplus", "meizu", "midp", "cldc", "motorola", "foma",
				"docomo", "up.browser", "up.link", "blazer", "helio", "hosin", "huawei", "novarra", "coolpad", "webos",
				"techfaith", "palmsource", "alcatel", "amoi", "ktouch", "nexian", "ericsson", "philips", "sagem",
				"wellcom", "bunjalloo", "maui", "smartphone", "iemobile", "spice", "bird", "zte-", "longcos",
				"pantech", "gionee", "portalmmm", "jig browser", "hiptop", "benq", "haier", "^lct", "320x320",
				"240x320", "176x220", "w3c ", "acs-", "alav", "alca", "amoi", "audi", "avan", "benq", "bird", "blac",
				"blaz", "brew", "cell", "cldc", "cmd-", "dang", "doco", "eric", "hipt", "inno", "ipaq", "java", "jigs",
				"kddi", "keji", "leno", "lg-c", "lg-d", "lg-g", "lge-", "maui", "maxo", "midp", "mits", "mmef", "mobi",
				"mot-", "moto", "mwbp", "nec-", "newt", "noki", "oper", "palm", "pana", "pant", "phil", "play", "port",
				"prox", "qwap", "sage", "sams", "sany", "sch-", "sec-", "send", "seri", "sgh-", "shar", "sie-", "siem",
				"smal", "smar", "sony", "sph-", "symb", "t-mo", "teli", "tim-", "tosh", "tsm-", "upg1", "upsi", "vk-v",
				"voda", "wap-", "wapa", "wapi", "wapp", "wapr", "webc", "winw", "winw", "xda", "xda-",
				"Googlebot-Mobile" };
		if (useragent != null) {
			for (String mobileAgent : mobileAgents) {
				if (useragent.toLowerCase().indexOf(mobileAgent) >= 0) {
					isMoblie = true;
					break;
				}
			}
		}
		return isMoblie;
	}
	/**
	 * 
	 * @param useragent
	 * @description 获取机型 适用于android系列
	 */
	public static String getModel(String useragent){
		  Pattern pattern = Pattern.compile(";\\s?(\\S*?\\s?\\S*?)\\s?(Build)?/");  
          Matcher matcher = pattern.matcher(useragent);  
          String model = null;  
          if (matcher.find()) {  
              model = matcher.group(1).trim();  
          }
          return model;
	}
	/**
	 * 
	 * @param agent
	 * @description 解析useragent
	 */
	public static Map<String,String> analyze(String agent){
		Map<String,String> result = new HashMap<String,String>();
		 Pattern pattern = Pattern.compile("\\([^\\(]+\\)");
         Matcher matcher = pattern.matcher(agent);  
         String newstr = agent.replaceAll("\\([^\\(]+\\)", "");
         String browse = BrowseTool.checkBrowse(agent);
 		 result.put("browse", browse);
         while (matcher.find()) {  
        	 String item = matcher.group();
        	 if(item.contains("KHTML") || item.contains("Gecko")){
        		 result.put("LayoutEngine", item);
        	 }else{
        		 analyzeOsModel(item,result);
        	 }
         }
         analyzeCommon(result,newstr);
		
		return result;
	}

	private static void analyzeOsModel(String item, Map<String, String> result) {
		if(item.contains("Mac") && item.contains("OS")){
			if(item.contains("iPhone")){
				result.put("model", "iPhone");
				result.put("os", "Mac OS X");
			}
			if(item.contains("iPod")){
				result.put("model", "iPod");
				result.put("os", "Mac OS X");
			}
			
			
		}else if(item.contains("Android")){
			String model = getModel(item);
			result.put("model", model);
			result.put("os", "Android");
		}
	}

	private static void analyzeCommon(Map<String, String> result, String newstr) {
          String[] ss = newstr.split(" ");
          for(String str : ss){
        	  if(StringUtils.isNotBlank(str)){
        		  if(str.contains("/")){
        			  String[] kv = str.split("/");
        			  result.put(kv[0], kv[1]);
        		  }else{
        			  result.put(str, null);
        		  }
        	  }
        	 
          }

		
	}
}
