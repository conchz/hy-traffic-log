package com.ss.es;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.ss.main.Constants;
import com.ss.mongo.MongoDBUtil;
import com.ss.utils.GaDateUtils;
import com.ss.vo.ExitCountObject;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ExitStatisticsProcessor implements Constants {

	private final BlockingQueue<Map<String, Object>> exitQueue = new LinkedBlockingQueue<>();

	ExitStatisticsProcessor() {

		Thread requestThread = new Thread(() -> {
			while (true) {
				Map<String, Object> exitMap = null;
				try {
					exitMap = exitQueue.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (exitMap == null)
					continue;
				// 保存数据
				saveDate(exitMap);
			}
		});

		requestThread.start();
	}

	private void saveDate(Map<String, Object> source) {

		// 退出次数对象
		ExitCountObject exitCount = createExitCount(source);
		if (exitCount == null) {
			return;
		}
		// 连接表名称
		String collName = getDayOfCollName();
		DBCollection collTransatcion = MongoDBUtil.getCollection(DB_EXIT_NAME,
				"transatcion");

		DBCollection collExit = MongoDBUtil.getCollection(DB_EXIT_NAME,
				collName);

		if (!PLACEHOLDER.equals(exitCount.getRf())) { // 来源路径为 "-".
			DBObject transatcionObject = getTransatcionObject(
					exitCount.getLoc(), exitCount.getRf());

			// 开启事物
			WriteResult transatcionWR = collTransatcion
					.insert(transatcionObject);

			if (transatcionWR.getError() == null) {

				// 更新事物状态
				transatcionWR = collTransatcion
						.update(getUpdateTransatcionObject(transatcionObject
								.get("_id")),
								getUpdateTransatcionStatus("pending"));

				if (transatcionWR.getError() == null) {

					// 第一次提交
					boolean isSuccess = firstSumbit(transatcionObject,
							exitCount, collExit);

					if (isSuccess) {

						// 更新事物状态
						transatcionWR = collTransatcion.update(
								getUpdateTransatcionObject(transatcionObject
										.get("_id")),
								getUpdateTransatcionStatus("applied"));

						if (transatcionWR.getError() == null) {

							// 第二次提交
							isSuccess = sencondSubmit(transatcionObject,
									exitCount, collExit);

							if (isSuccess) {

								// 完成
								transatcionWR = collTransatcion
										.update(getUpdateTransatcionObject(transatcionObject
												.get("_id")),
												getUpdateTransatcionStatus("done"));

							}
						}
					}
				}

			}

		}

	}

	private boolean sencondSubmit(DBObject transatcionObject,
			ExitCountObject exitCount, DBCollection collExit) {
		// 第二次提交
		DBObject rfPullQuery = getPullExitCountObject(transatcionObject,
				exitCount, true);

		DBObject rfPullUpdate = new BasicDBObject();
		rfPullUpdate.put("$pull", new BasicDBObject("pendingTransactions",
				transatcionObject.get("_id")));

		WriteResult rfPullWR = collExit.update(rfPullQuery, rfPullUpdate);
		String rfPullWRError = rfPullWR.getError();

		DBObject locPullQuery = getPullExitCountObject(transatcionObject,
				exitCount, false);

		DBObject locPullUpdate = new BasicDBObject();
		locPullUpdate.put("$pull", new BasicDBObject("pendingTransactions",
				transatcionObject.get("_id")));

		WriteResult locPullWR = collExit.update(locPullQuery, locPullUpdate);

		if (rfPullWRError == null && locPullWR.getError() == null) {
			return true;
		}
		return false;

	}

	private boolean firstSumbit(DBObject transatcionObject,
			ExitCountObject exitCount, DBCollection collExit) {

		DBObject rfQuery = getExitCountObject(transatcionObject, exitCount,
				true);

		DBObject rfUpdate = new BasicDBObject();
		rfUpdate.put("$push", new BasicDBObject("pendingTransactions",
				transatcionObject.get("_id")));

		DBObject dBObject = collExit.findOne(rfQuery);
		// 是否存在
		if (dBObject == null) {
			rfUpdate.put("$inc",
					new BasicDBObject("exitCount", EXIT_COUNT_ZERO));
		} else {
			Integer eCount = Integer.valueOf(dBObject.get("exitCount")
					.toString());
			if (eCount > 0) {
				rfUpdate.put("$inc", new BasicDBObject("exitCount",
						EXIT_COUNT_DECREASE));
			} else {
				rfUpdate.put("$inc", new BasicDBObject("exitCount",
						EXIT_COUNT_ZERO));
			}
		}

		WriteResult rfWR = collExit.update(rfQuery, rfUpdate, true, false);

		String rfWRError = rfWR.getError();

		DBObject locQuery = getExitCountObject(transatcionObject, exitCount,
				false);
		DBObject locUpdate = new BasicDBObject();
		locUpdate.put("$inc", new BasicDBObject("exitCount",
				EXIT_COUNT_INCREASE));
		locUpdate.put("$push", new BasicDBObject("pendingTransactions",
				transatcionObject.get("_id")));

		WriteResult locWR = collExit.update(locQuery, locUpdate, true, false);

		if (rfWRError == null && locWR.getError() == null) {
			return true;
		}
		return false;

	}

	private DBObject getPullExitCountObject(DBObject transatcionObject,
			ExitCountObject exitCount, boolean isRf) {

		DBObject queryObject = new BasicDBObject();
		queryObject.put("type", exitCount.getType());
		queryObject.put("userId", exitCount.getUserId());
		if (isRf) {
			queryObject.put("url", exitCount.getRf());
		} else {
			queryObject.put("url", exitCount.getLoc());
		}

		queryObject.put("rfType", exitCount.getRfType());
		queryObject.put("isNew", exitCount.getIsNew());
		if (StringUtils.isNotBlank(exitCount.getSe())) {
			queryObject.put("se", exitCount.getSe());
		}

		return queryObject;

	}

	private DBObject getExitCountObject(DBObject transatcionObject,
			ExitCountObject exitCount, boolean isRf) {

		DBObject queryObject = new BasicDBObject();
		queryObject.put("type", exitCount.getType());
		queryObject.put("userId", exitCount.getUserId());
		if (isRf) {
			queryObject.put("url", exitCount.getRf());
		} else {
			queryObject.put("url", exitCount.getLoc());
		}

		queryObject.put("rfType", exitCount.getRfType());
		queryObject.put("isNew", exitCount.getIsNew());
		if (StringUtils.isNotBlank(exitCount.getSe())) {
			queryObject.put("se", exitCount.getSe());
		}
		queryObject.put("pendingTransactions", new BasicDBObject("$ne",
				transatcionObject.get("_id")));

		return queryObject;

	}

	private DBObject getUpdateTransatcionObject(Object id) {
		DBObject dBObjectId = new BasicDBObject();
		dBObjectId.put("_id", id);
		return dBObjectId;
	}

	private DBObject getUpdateTransatcionStatus(Object status) {
		DBObject dBObject = new BasicDBObject();
		DBObject parameterObject = new BasicDBObject("state", status);
		parameterObject.put("lastModified", GaDateUtils.getCurrentTime());
		dBObject.put("$set", parameterObject);
		return dBObject;
	}

	private DBObject getTransatcionObject(String loc, String rf) {
		DBObject transatcionObject = new BasicDBObject();
		transatcionObject.put("from", rf);
		transatcionObject.put("to", loc);
		transatcionObject.put("score", EXIT_COUNT_INCREASE);
		transatcionObject.put("state", "initial");
		transatcionObject.put("lastModified", GaDateUtils.getCurrentTime());

		return transatcionObject;
	}

	private ExitCountObject createExitCount(Map<String, Object> source) {

		// 用户类型
		String type = source.containsKey(TYPE) ? source.get(TYPE).toString()
				: "";
		if (StringUtils.isBlank(type)) {
			return null;
		}

		// 用户ID
		String userId = source.containsKey(TT) ? source.get(TT).toString() : "";
		if (StringUtils.isBlank(userId)) {
			return null;
		}

		// 当前路径
		String loc = source.containsKey(CURR_ADDRESS) ? source
				.get(CURR_ADDRESS).toString() : "";
		if (StringUtils.isBlank(loc)) {
			return null;
		}
		// 来源路径
		String rf = source.containsKey(RF) ? source.get(RF).toString() : "";
		// 来源类型
		String rfType = source.containsKey(RF_TYPE) ? source.get(RF_TYPE)
				.toString() : "";
		// 搜素引擎名字
		String se = source.containsKey(SE) ? source.get(SE).toString() : "";
		// 新老访客
		String isNew = source.containsKey(VISITOR_IDENTIFIER) ? source.get(
				VISITOR_IDENTIFIER).toString() : "";

		return new ExitCountObject(type, userId, loc, rf, rfType, se, isNew);
	}

	public DBObject getLocQueryObject(ExitCountObject exitCount) {
		DBObject queryObject = new BasicDBObject();
		queryObject.put("url", exitCount.getLoc());

		if (StringUtils.isNotBlank(exitCount.getRfType())) {
			queryObject.put("rfType", exitCount.getRfType());
		}
		if (StringUtils.isNotBlank(exitCount.getSe())) {
			queryObject.put("se", exitCount.getSe());
		}
		if (StringUtils.isNotBlank(exitCount.getIsNew())) {
			queryObject.put("isNew", exitCount.getIsNew());
		}
		return queryObject;
	}

	public DBObject getLocUpdateObject(int value) {
		DBObject updateObject = new BasicDBObject();
		updateObject.put("$inc", new BasicDBObject("exitCount", value));
		updateObject
				.put("$set",
						new BasicDBObject("lastModified", GaDateUtils
								.getCurrentTime()));

		return updateObject;
	}

	public void add(Map<String, Object> source) {
		Map<String, Object> map = new HashMap<>(source);
		exitQueue.add(map);
	}

	public String getDayOfCollName() {

		String dateString = GaDateUtils.getCurrentDate();
		String collName = MONGODB_EXIT_PREFIX + dateString;
		return collName;
	}

	public void saveData(String collName, Map<String, Object> querier,
			Map<String, Object> store) {

	}

}
