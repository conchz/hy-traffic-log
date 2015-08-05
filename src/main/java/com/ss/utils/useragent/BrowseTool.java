package com.ss.utils.useragent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 
 * @description 判断浏览器的类型
 * @author ZhangHuaRong   
 * @update 2015年7月15日 上午10:28:41
 */
public class BrowseTool {

	
	private final static String IE9="MSIE 9.0";
	private final static String IE8="MSIE 8.0";
	private final static String IE7="MSIE 7.0";
	private final static String IE6="MSIE 6.0";
	private final static String MAXTHON="Maxthon";
	private final static String QQ="QQBrowser";
	private final static String GREEN="GreenBrowser";
	private final static String SE360="360SE";
	private final static String FIREFOX="Firefox";
	private final static String OPERA="Opera";
	private final static String CHROME="Chrome";
	private final static String SAFARI="Safari";
	private final static String OTHER="其它";
	
	
	private final static String MicroMessenger="MicroMessenger";
	private final static String Weibo="Weibo";
	private final static String baidubrowser="baidubrowser";
	private final static String baiduboxapp="baiduboxapp";
	private final static String UC="UCBrowser";
	
	public static boolean regex(String regex,String str){
		Pattern p =Pattern.compile(regex,Pattern.MULTILINE);
		Matcher m=p.matcher(str);
		return m.find();
	}
	
	public static String checkBrowse(String userAgent){
		if(regex(UC,userAgent))return UC;
		if(regex(OPERA, userAgent))return OPERA;
		if(regex(CHROME, userAgent))return CHROME;
		if(regex(FIREFOX, userAgent))return FIREFOX;
		if(regex(SAFARI, userAgent))return SAFARI;
		if(regex(SE360, userAgent))return SE360;
		if(regex(GREEN,userAgent))return GREEN;
		if(regex(QQ,userAgent))return QQ;
		if(regex(MAXTHON, userAgent))return MAXTHON;
		if(regex(IE9,userAgent))return IE9;
		if(regex(IE8,userAgent))return IE8;
		if(regex(IE7,userAgent))return IE7;
		if(regex(IE6,userAgent))return IE6;
		
		if(regex(MicroMessenger,userAgent))return MicroMessenger;
		if(regex(Weibo,userAgent))return Weibo;
		if(regex(baidubrowser,userAgent))return baidubrowser;
		if(regex(baiduboxapp,userAgent))return baiduboxapp;
		return OTHER;
	}
	
	
	
	
	  public static void main(String[] args) {  
	        String ie9    ="Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)";  
	        String ie8    ="Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322)";  
	        String ie7    ="Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.2; .NET CLR 1.1.4322)";  
	        String ie6    ="Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322)";  
	        String aoyou  ="Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322; Maxthon 2.0)";  
	        String qq     ="Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322) QQBrowser/6.8.10793.201";  
	        String green  ="Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322; GreenBrowser)";  
	        String se360  ="Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322; 360SE)";  
	          
	        String chrome ="Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US) AppleWebKit/534.11 (KHTML, like Gecko) Chrome/9.0.570.0 Safari/534.11";                 
	        String safari ="Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN) AppleWebKit/533.17.8 (KHTML, like Gecko) Version/5.0.1 Safari/533.17.8";  
	        String fireFox="Mozilla/5.0 (Windows NT 5.2; rv:7.0.1) Gecko/20100101 Firefox/7.0.1";  
	        String opera  ="Opera/9.80  (Windows NT 5.2; U; zh-cn) Presto/2.9.168 Version/11.51";  
	        String other  ="(Windows NT 5.2; U; zh-cn) Presto/2.9.168 Version/11.51";  
	        
	        
	        String MicroMessenger = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_4 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H143 MicroMessenger/6.2.2 NetType/WIFI Language/zh_CN";
			String Weibo ="Mozilla/5.0 (iPhone; CPU iPhone OS 8_4 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H143 Weibo (iPhone7,1__weibo__5.3.0__iphone__os8.4)";
			String baidubrowser = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_4 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.4 Mobile/12H143 Safari/600.1.4 baidubrowser/5.1.6.5 (Baidu; P2 8.4)";
			String baiduboxapp = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_4 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H143 rabbit%2F1.0 baiduboxapp/0_0.0.6.6_enohpi_4331_057/4.8_2C2%257enohPi/1099a/893E895E67C946569B0A05008C44C8A0AC3014D21ORNBRGCJPO/1";
			String str = "Mozilla/5.0 (Linux; U; Android 4.4.4; zh-CN; MI 3 Build/KTU84P) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/10.5.1.597 U3/0.8.0 Mobile Safari/534.30";

			
			
	        BrowseTool b=new BrowseTool();  
	        System.out.println(b.checkBrowse(ie9));  
	        System.out.println(b.checkBrowse(ie8));  
	        System.out.println(b.checkBrowse(ie7));  
	        System.out.println(b.checkBrowse(ie6));  
	        System.out.println(b.checkBrowse(aoyou));  
	        System.out.println(b.checkBrowse(qq));  
	        System.out.println(b.checkBrowse(green));  
	        System.out.println(b.checkBrowse(se360));  
	        System.out.println(b.checkBrowse(chrome));  
	        System.out.println(b.checkBrowse(safari));  
	        System.out.println(b.checkBrowse(fireFox));  
	        System.out.println(b.checkBrowse(opera));  
	        System.out.println(b.checkBrowse(other));
	        System.out.println("====================================");
	        System.out.println(b.checkBrowse(MicroMessenger));  
	        System.out.println(b.checkBrowse(Weibo));  
	        System.out.println(b.checkBrowse(baidubrowser));  
	        System.out.println(b.checkBrowse(baiduboxapp));
	        System.out.println(b.checkBrowse(str));
	    }  
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
