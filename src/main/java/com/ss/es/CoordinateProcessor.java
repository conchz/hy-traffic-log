package com.ss.es;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ss.main.Constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dolphineor on 2015-6-9.
 * <p>
 * 坐标处理器
 */
@SuppressWarnings("unchecked")
class CoordinateProcessor implements Constants {

    public static Map<String, Object> handle(Map<String, Object> source) {
        if (source.isEmpty())
            return Collections.emptyMap();

        Map<String, Object> sourceMap = new HashMap<>();

        // 解析xy数组
        JSONArray xyJsonArray = (JSONArray) JSON.parse(source.get(XY).toString());
        Object[] xyArr = xyJsonArray.stream().map(xy -> (Map<String, Double>) xy).toArray();

        sourceMap.put(INDEX, source.get(INDEX).toString());
        sourceMap.put(TYPE, source.get(TYPE).toString());
        sourceMap.put(TT, source.get(TT).toString());
        sourceMap.put(VID, source.get(VID).toString());
        sourceMap.put(CURR_ADDRESS, source.get(CURR_ADDRESS).toString());
        sourceMap.put(XY, xyArr);
        sourceMap.put(DH, source.get(DH));
        source.clear();
        return sourceMap;
    }

}
