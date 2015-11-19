package com.ss.es;

import com.ss.main.Constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dolphineor on 2015-6-9.
 * <p>
 * 事件处理器
 */
class EventProcessor implements Constants {

	public static Map<String, Object> handle(Map<String, Object> source) {
		if (source.isEmpty())
			return Collections.emptyMap();

		Map<String, Object> sourceMap = new HashMap<>();

		String[] eventArr = source.get(ET).toString().split("\\*");
		sourceMap.put(ET_CATEGORY, eventArr[0]);
		sourceMap.put(ET_ACTION, eventArr[1]);
		if (eventArr.length == 3) {
			sourceMap.put(ET_TARGET, eventArr[2]);
		} else if (eventArr.length == 4) {
			sourceMap.put(ET_TARGET, eventArr[2]);
			sourceMap.put(ET_LABEL, eventArr[3]);
		} else if (eventArr.length == 5) {
			sourceMap.put(ET_TARGET, eventArr[2]);
			sourceMap.put(ET_LABEL, eventArr[3]);
			sourceMap.put(ET_VALUE, eventArr[4]);
		} else {
			sourceMap.put(ET_LABEL, EMPTY_STRING);
			sourceMap.put(ET_VALUE, EMPTY_STRING);
		}

		sourceMap.put(INDEX, source.get(INDEX).toString());
		sourceMap.put(TYPE, source.get(TYPE).toString());
		sourceMap.put(TT, source.get(TT).toString());
		sourceMap.put(VID, source.get(VID).toString());
		sourceMap.put(CURR_ADDRESS, source.get(CURR_ADDRESS).toString());
		sourceMap.put(UNIX_TIME, Long.parseLong(source.get(UNIX_TIME).toString()));
		sourceMap.put(VISITOR_IDENTIFIER, Integer.parseInt(source.get(VISITOR_IDENTIFIER).toString()));
		sourceMap.put(REGION, source.get(REGION).toString());
		sourceMap.put(CITY, source.get(CITY).toString());
		sourceMap.put(RF, source.get(RF).toString());

		source.clear();
		return sourceMap;
	}
}
