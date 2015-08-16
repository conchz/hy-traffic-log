package com.ss.es;

import com.alibaba.fastjson.JSON;
import com.ss.main.Constants;
import com.ss.parser.KeywordExtractor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dolphineor on 2015-6-9.
 * <p>
 * 推广URL统计信息处理器
 */
@SuppressWarnings("unchecked")
class PromotionUrlProcessor implements Constants {

    public static Map<String, Object> handle(Map<String, Object> source) {
        if (source.isEmpty())
            return Collections.emptyMap();

        String location = source.get(CURR_ADDRESS).toString();
        String ut = source.remove(UT).toString();
        Map<String, Object> promotionUrlMap = new HashMap<>();

        // 提取推广URL统计信息
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) JSON.parse(ut)).entrySet()) {
            promotionUrlMap.put(entry.getKey(), entry.getValue());
        }

        // 提取关键词信息
        Map<String, Object> keywordInfoMap = KeywordExtractor.parse(location);
        if (!keywordInfoMap.isEmpty())
            source.putAll(keywordInfoMap);

        promotionUrlMap.put(INDEX, source.get(INDEX).toString());
        promotionUrlMap.put(TYPE, source.get(TYPE).toString());
        promotionUrlMap.put(HOST, source.get(HOST).toString()); // 存储的是网站的trackId
        promotionUrlMap.put(TT, source.get(TT).toString());
        promotionUrlMap.put(VID, source.get(VID).toString());
        promotionUrlMap.put(CURR_ADDRESS, location);
        promotionUrlMap.put(REGION, source.get(REGION).toString());
        promotionUrlMap.put(CITY, source.get(CITY).toString());
        promotionUrlMap.put(ISP, source.get(ISP).toString());

        source.clear();
        return promotionUrlMap;
    }
}
