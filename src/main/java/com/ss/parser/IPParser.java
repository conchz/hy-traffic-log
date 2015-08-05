package com.ss.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ss.main.Constants.*;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Created by dolphineor on 2015-3-18.
 */
public class IPParser {

    private static final String IP_URL_TEMPLATE = "http://ip.taobao.com/service/getIpInfo.php?ip=%s";
    private static final String GLOBAL_IP_URL_TEMPLATE = "https://sp0.baidu.com/8aQDcjqpAAV3otqbppnN2DJv/api.php?query=%s&resource_id=6006&ie=utf8&oe=utf8&format=json&tn=baidu";
    private static final String IP_CHECK_ADDRESS = "http://city.ip138.com/ip2city.asp";
    private static final String IP_REGION_SUFFIX1 = "自治区";
    private static final String IP_REGION_SUFFIX2 = "内蒙古自治区";
    private static final String IP_REGION_SUFFIX3 = "特别行政区";
    private static final String COUNTY_LEVEL_CITY = "省直辖县级行政区划"; // 县级市
    private static final String ABROAD = "国外";


    public static Map<String, Object> getIpInfo(String ip) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(String.format(IP_URL_TEMPLATE, ip));
            conn = (HttpURLConnection) url.openConnection();
            conn.connect();

            Map<String, Object> ipInfoMap = null;

            if (conn.getResponseCode() == HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String jsonStr = reader.lines().findFirst().get();
                ipInfoMap = handleIpInfoResponse(jsonStr);
            } else if (conn.getResponseCode() == HTTP_BAD_GATEWAY) {
                conn.disconnect();
                TimeUnit.SECONDS.sleep(1);
                conn.connect();

                if (conn.getResponseCode() == HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    String jsonStr = reader.lines().findFirst().get();
                    ipInfoMap = handleIpInfoResponse(jsonStr);
                }
            }

            if (ipInfoMap == null)
                return Collections.emptyMap();

            if (ipInfoMap.containsValue(ABROAD)) {
                ipInfoMap.replace(TRAFFIC_CATEGORY, 1);
                ipInfoMap.replace(REGION, getGlobalIpLocation(ip));
            }
            return ipInfoMap;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return Collections.emptyMap();
    }

    private static Map<String, Object> handleIpInfoResponse(String jsonStr) {
        JSONObject jsonObject = JSON.parseObject(jsonStr).getJSONObject("data");

        Map<String, Object> ipInfoMap = new HashMap<>();
        ipInfoMap.put(TRAFFIC_CATEGORY, 0);
        String region = jsonObject.getString(REGION);
        if (region.isEmpty()) {
            ipInfoMap.put(REGION, ABROAD);
            ipInfoMap.put(CITY, PLACEHOLDER);
            ipInfoMap.put(ISP, PLACEHOLDER);
        } else {
            if (IP_REGION_SUFFIX2.equals(region))
                ipInfoMap.put(REGION, region.substring(0, 3));
            else {
                if (region.contains(IP_REGION_SUFFIX1) || region.contains(IP_REGION_SUFFIX3))
                    ipInfoMap.put(REGION, region.substring(0, 2));
                else
                    ipInfoMap.put(REGION, region.replace("省", EMPTY_STRING));
            }

            if (jsonObject.getString(CITY).isEmpty())
                ipInfoMap.put(CITY, PLACEHOLDER);
            else
                ipInfoMap.put(CITY, jsonObject.getString(CITY));


            if (jsonObject.getString(ISP).isEmpty())
                ipInfoMap.put(ISP, PLACEHOLDER);
            else
                ipInfoMap.put(ISP, jsonObject.getString(ISP));

        }

        return ipInfoMap;
    }

    public static String getGlobalIpLocation(String ip) {
        String location = PLACEHOLDER;
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(String.format(GLOBAL_IP_URL_TEMPLATE, ip));
            connection = (HttpsURLConnection) url.openConnection();
            String chartSet = StandardCharsets.UTF_8.name();
            connection.setRequestProperty("Charset", chartSet);
            connection.setUseCaches(false);
            connection.connect();

            if (connection.getResponseCode() == HTTP_OK) {
                StringBuilder contentBuilder = new StringBuilder();
                String line;
                try (BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), chartSet))) {
                    while ((line = responseReader.readLine()) != null) {
                        contentBuilder.append(line).append("\n");
                    }
                }

                JSONObject jsonObject = JSON.parseObject(contentBuilder.toString());
                location = jsonObject.getJSONArray("data").getJSONObject(0).getString("location");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return location;
    }

    // 获取当前主机在web上的真实IP(该主机有可能没有配置外网IP, 外网不能对当前主机进行访问)
    public static String getWebIp() {
        try {
            URL url = new URL(IP_CHECK_ADDRESS);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");

            StringBuilder response = new StringBuilder("");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
            br.lines().forEach(s -> response.append(s).append("\r\n"));

            String content = response.toString();
            return content.substring(content.indexOf("[") + 1, content.indexOf("]"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getRealIp() throws SocketException {
        String localIp = null;// 内网IP, 如果没有配置外网IP则返回它
        String netIp = null;// 外网IP

        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        InetAddress ip;
        boolean isFound = false;// 是否找到外网IP
        while (netInterfaces.hasMoreElements() && !isFound) {
            NetworkInterface ni = netInterfaces.nextElement();
            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                ip = address.nextElement();
                if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && !ip.getHostAddress().contains(":")) {// 外网IP
                    netIp = ip.getHostAddress();
                    isFound = true;
                    break;
                } else if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && !ip.getHostAddress().contains(":")) {// 内网IP
                    localIp = ip.getHostAddress();
                }
            }
        }

        if (netIp != null && !"".equals(netIp)) {
            return netIp;
        } else {
            return localIp;
        }
    }

}
