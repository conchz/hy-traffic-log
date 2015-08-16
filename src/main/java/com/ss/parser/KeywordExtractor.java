package com.ss.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.ss.main.Constants;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dolphineor on 2015-5-14.
 */
public class KeywordExtractor implements Constants {

    public static Map<String, Object> parse(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String domain = url.getHost().replace(WWW_PREFIX, EMPTY_STRING);
            QueryStringDecoder decoder = new QueryStringDecoder(urlStr);
            String transferId = decoder.parameters().get(SEM_KEYWORD_IDENTIFIER.replace("=", EMPTY_STRING)).get(0);
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : transferId.split(EMPTY_STRING)) {
                stringBuilder.append(9 - Integer.valueOf(s));
            }
            Long keywordId = Long.valueOf(stringBuilder.toString());

            Map<String, Object> infoMap = new HashMap<>();

            HttpURLConnection conn = (HttpURLConnection) new URL(String.format(KEYWORD_INFO_REQUEST_URL, domain, keywordId))
                    .openConnection();
            String chartSet = StandardCharsets.UTF_8.name();
            conn.setRequestProperty("Charset", chartSet);
            conn.setUseCaches(false);
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                StringBuilder contentBuilder = new StringBuilder();
                String line;
                try (BufferedReader responseReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), chartSet))) {
                    // 处理响应流, 必须与服务器响应流输出的编码一致
                    while ((line = responseReader.readLine()) != null) {
                        contentBuilder.append(line).append("\n");
                    }
                }

                JSONObject jsonObject = JSON.parseObject(contentBuilder.toString());
                infoMap.put(ES_ACCOUNT_ID, jsonObject.getLong(SEM_ACCOUNT_ID));
                infoMap.put(ES_CAMPAIGN_ID, jsonObject.getLong(SEM_CAMPAIGN_ID));
                infoMap.put(ES_CAMPAIGN_NAME, jsonObject.getString(SEM_CAMPAIGN_NAME));
                infoMap.put(ES_ADGROUP_ID, jsonObject.getLong(SEM_ADGROUP_ID));
                infoMap.put(ES_ADGROUP_NAME, jsonObject.getString(SEM_ADGROUP_NAME));
                infoMap.put(ES_KEYWORD_ID, keywordId);
                infoMap.put(ES_KEYWORD_NAME, jsonObject.getString(SEM_KEYWORD_NAME));
            }
            conn.disconnect();

            return infoMap;
        } catch (IOException | NullPointerException | JSONException ignored) {

        }

        return Collections.emptyMap();
    }

}
