package com.ss.es;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.elasticsearch.common.lang3.StringUtils;

import com.mongodb.DBObject;
import com.ss.main.Constants;
import com.ss.mongo.MongoDBUtil;
import com.ss.utils.GaDateUtils;

public class GaProcessor implements Constants {

	private final BlockingQueue<Map<String, Object>> gaQueue = new LinkedBlockingQueue<>();

	GaProcessor() {

		Thread requestThread = new Thread(() -> {
			while (true) {
				Map<String, Object> gaMap = null;
				try {
					gaMap = gaQueue.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (gaMap == null)
					continue;
				// 保存数据
				saveDate(gaMap);
			}
		});

		requestThread.start();
	}

	private void saveDate(Map<String, Object> source) {

		if (source.size() == 0) {
			return;
		}

		String type = source.get(TYPE).toString();
		String userId = source.get(VID).toString();
		String isNew = source.get(VISITOR_IDENTIFIER).toString();

		if (StringUtils.isBlank(type) || StringUtils.isBlank(userId)) {
			return;
		}

		Map<String, Object> querier = new HashMap<String, Object>();
		querier.put(MONGODB_TYPE, type);
		querier.put(MONGODB_USER_ID, userId);
	
		
		Map<String, Object> store = new HashMap<String, Object>();
		store.put(MONGODB_IS_NEW, isNew);

		// 保存数据----》每日记录
		String collName = getDayOfCollName();
		saveData(collName, querier,store);
		// 保存数据----》每周记录
		collName = getWeekOfCollName();
		saveData(collName, querier,store);
		// 保存数据----》每月记录
		collName = getMonthOfCollName();
		saveData(collName, querier,store);

	}

	public void add(Map<String, Object> source) {
		Map<String, Object> map = new HashMap<>(source);
		gaQueue.add(map);
	}

	public String getDayOfCollName() {

		String dateString = GaDateUtils.getCurrentDate();
		String collName = MONGODB_PREFIX + dateString;
		return collName;
	}

	public String getWeekOfCollName() {

		String dateString = GaDateUtils.getWeekMonday();
		String collName = MONGODB_PREFIX_WEEK + dateString;
		return collName;
	}

	public String getMonthOfCollName() {

		String dateString = GaDateUtils.getCurrentMonth();
		String collName = MONGODB_PREFIX_MONTH + dateString;
		return collName;
	}

	public void saveData(String collName, Map<String, Object> querier,Map<String,Object> store) {

		DBObject dBObject = MongoDBUtil.findOne(querier, collName);

		//有记录存在,更新PV
		if (dBObject != null) {
			Integer pv = Integer.valueOf(dBObject.get(MONGODB_PV).toString()) + 1;
			dBObject.put(MONGODB_PV, pv);
			MongoDBUtil.update(MongoDBUtil.getMapped(querier), dBObject, false,
					false, collName);
			
		//无记录存在,新增数据
		} else {
			store.put(MONGODB_PV, 1);
			store.put(MONGODB_USER_ID, querier.get(MONGODB_USER_ID));
			store.put(MONGODB_TYPE, querier.get(MONGODB_TYPE));
			
			dBObject = MongoDBUtil.getMapped(store);
			MongoDBUtil.insert(dBObject, collName);
		}
	}

}
