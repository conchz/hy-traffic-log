package com.ss.mongo;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;


public class MongoDBUtil {
	private static Mongo connection = null;
	private static DB db = null;
	private static Settings settings;

	static {
		try {
			settings = ImmutableSettings.settingsBuilder()
					.loadFromClasspath("mongo.yml").build();
			connection = new Mongo(settings.get("IP") + ":"
					+ settings.get("PORT"));
			db = connection.getDB(settings.get("DB_NAME"));
		} catch (MongoException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	private MongoDBUtil() {

	}

	/**
	 * 根据条件判断是否存在数据
	 */
	public static boolean existByDbs(DBObject dbs, String collName) {
		DBCollection coll = db.getCollection(collName);
		long count = coll.count(dbs);
		return count > 0;
	}

	/**
	 * 判断集合是否存在
	 * 
	 * @param collectionName
	 * @return
	 */
	public static boolean collectionExists(String collectionName) {
		return db.collectionExists(collectionName);
	}

	/**
	 * 查询单个,按主键查询
	 * 
	 * @param id
	 * @param collectionName
	 */
	public static void findById(String id, String collectionName) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("_id", new ObjectId(id));
		findOne(map, collectionName);
	}

	/**
	 * 查询单个 <br>
	 * ------------------------------<br>
	 * 
	 * @param map
	 * @param collectionName
	 */
	public static DBObject findOne(Map<String, Object> map, String collectionName) {
		DBObject dbObject = getMapped(map);
		DBObject object = getCollection(collectionName).findOne(dbObject);
		return object;
	}

	/**
	 * count
	 * 
	 * @param dbs
	 * @param collectionName
	 * @return
	 */
	public static long count(DBObject dbs, String collectionName) {
		return getCollection(collectionName).count(dbs);
	}

	/**
	 * 查询
	 * 
	 * @param dbObject
	 * @param cursor
	 * @param cursorPreparer
	 * @param collectionName
	 */
	public static void find(DBObject dbObject, String collectionName) {
		DBCursor dbCursor = getCollection(collectionName).find(dbObject);
		Iterator<DBObject> iterator = dbCursor.iterator();
		while (iterator.hasNext()) {
			print(iterator.next());
		}
	}

	/**
	 * 获取集合(表)
	 * 
	 * @param collectionName
	 * @return
	 */
	public static DBCollection getCollection(String collectionName) {
		return db.getCollection(collectionName);
	}

	/**
	 * 获取所有集合(表)名称
	 * 
	 * @return
	 */
	public static Set<String> getCollection() {
		return db.getCollectionNames();
	}

	/**
	 * 创建集合(表)
	 * 
	 * @param collectionName
	 * @param options
	 */
	public static void createCollection(String collectionName, DBObject options) {
		if (!collectionExists(collectionName)) {
			db.createCollection(collectionName, options);
		}
	}

	/**
	 * 删除
	 * 
	 * @param collectionName
	 */
	public static void dropCollection(String collectionName) {
		DBCollection collection = getCollection(collectionName);
		collection.drop();
	}

	/**
	 * 
	 * @param map
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static DBObject getMapped(Map<String, Object> map) {
		DBObject dbObject = new BasicDBObject();
		Iterator<Entry<String, Object>> iterable = map.entrySet().iterator();
		while (iterable.hasNext()) {
			Entry<String, Object> entry = iterable.next();
			Object value = entry.getValue();
			String key = entry.getKey();
			if (key.startsWith("$") && value instanceof Map) {
				BasicBSONList basicBSONList = new BasicBSONList();
				Map<String, Object> conditionsMap = ((Map<String, Object>) value);
				Set<String> keys = conditionsMap.keySet();
				for (String k : keys) {
					Object conditionsValue = conditionsMap.get(k);
					if (conditionsValue instanceof Collection) {
						conditionsValue = convertArray(conditionsValue);
					}
					DBObject dbObject2 = new BasicDBObject(k, conditionsValue);
					basicBSONList.add(dbObject2);
				}
				value = basicBSONList;
			} else if (value instanceof Collection) {
				value = convertArray(value);
			} else if (!key.startsWith("$") && value instanceof Map) {
				value = getMapped(((Map<String, Object>) value));
			}
			dbObject.put(key, value);
		}
		return dbObject;
	}

	@SuppressWarnings("unchecked")
	private static Object[] convertArray(Object value) {
		Object[] values = ((Collection<Object>) value).toArray();
		return values;
	}

	private static void print(DBObject object) {
		Set<String> keySet = object.keySet();
		for (String key : keySet) {
			print(object.get(key));
		}
	}

	private static void print(Object object) {
		System.out.println(object.toString());
	}

	/**
	 * 插入数据
	 * 
	 * @param dbs
	 * @param collName
	 */
	public static void insert(DBObject dbs, String collName) {
		db.getCollection(collName).insert(dbs);
	}

	/**
	 * 批量插入数据
	 * 
	 * @param dbses
	 * @param collName
	 */
	public static void insertBatch(List<DBObject> dbses, String collName) {
		// EG
		// List<DBObject> dbObjects = new ArrayList<DBObject>();
		// DBObject jim = new BasicDBObject("name", "jim");
		// DBObject lisi = new BasicDBObject("name", "lisi");
		// dbObjects.add(jim);
		// dbObjects.add(lisi);
		// mongoDb.insertBatch(dbObjects, "users");
		// 1.得到集合
		// 2.插入操作
		db.getCollection(collName).insert(dbses);
	}

	/**
	 * 根据id删除数据
	 * 
	 * @param id
	 * @param collName
	 * @return 返回影响的数据条数
	 */
	public static int deleteById(String id, String collName) {
		// EG
		// mongoDb.deleteById("55a778f9f7fe06b241f54b10", "users");
		// 1.得到集合
		DBObject dbs = new BasicDBObject("_id", new ObjectId(id));
		int count = db.getCollection(collName).remove(dbs).getN();
		return count;
	}

	/**
	 * 根据条件删除数据
	 * 
	 * @param id
	 * @param collName
	 * @return 返回影响的数据条数
	 */
	public static int deleteByDbs(DBObject dbs, String collName) {
		// EG
		// DBObject lisi = new BasicDBObject();
		// lisi.put("name", "lisi");
		// int count = mongoDb.deleteByDbs(lisi, "users");
		// System.out.println("删除数据的条数是: " + count);
		// 1.得到集合
		DBCollection coll = db.getCollection(collName);
		int count = coll.remove(dbs).getN();
		return count;
	}

	/**
	 * 更新数据
	 * 
	 * @param find
	 *            查询器
	 * @param update
	 *            更新器
	 * @param upsert
	 *            更新或插入
	 * @param multi
	 *            是否批量更新
	 * @param collName
	 *            集合名称
	 * @return 返回影响的数据条数
	 */
	public static int update(DBObject find, DBObject update, boolean upsert,
			boolean multi, String collName) {
		// EG
		// DBObject find = new BasicDBObject();
		// find.put("name", "小明");
		// DBObject update = new BasicDBObject();
		// update.put("$set", new BasicDBObject("eamil", "test1111@126.com"));
		// mongoDb.update(find, update, false, true, "users");
		// 1.得到集合
		DBCollection coll = db.getCollection(collName);
		int count = coll.update(find, update, upsert, multi).getN();
		return count;
	}

	/**
	 * 查询器(分页)
	 * 
	 * @param ref
	 * @param keys
	 * @param start
	 * @param limit
	 * @return
	 */
	public DBCursor find(DBObject ref, DBObject keys, int start, int limit,
			String collName) {
		DBCursor cur = find(ref, keys, collName);
		return cur.limit(limit).skip(start);
	}

	/**
	 * 查询器(不分页)
	 * 
	 * @param ref
	 * @param keys
	 * @param start
	 * @param limit
	 * @param collName
	 * @return
	 */
	public DBCursor find(DBObject ref, DBObject keys, String collName) {
		// 1.得到集合
		DBCollection coll = db.getCollection(collName);
		DBCursor cur = coll.find(ref, keys);
		return cur;
	}

	/**
	 * 判断集合中是否存在特定条件的记录
	 * 
	 * @param map
	 * @param collection
	 * @return
	 */
	public static boolean dataExists(Map<String, Object> map, String collection) {
		DBObject dbObject = getMapped(map);
		return getCollection(collection).findOne(dbObject) != null;
	}

	/**
	 * 查询特定条件的所有记录
	 * 
	 * @param map
	 * @param collection
	 * @return
	 */
	public static List<DBObject> findByRefs(Map<String, Object> map,
			String collection) {
		return findByRefs(map, collection, new String[] {});
	}

	/**
	 * 查询特定条件的所有记录,并返回特定的字段
	 * 
	 * @param map
	 * @param fields
	 * @param collection
	 * @return
	 */
	public static List<DBObject> findByRefs(Map<String, Object> map,
			String collection, String... fields) {
		DBObject dbObject = getMapped(map);
		DBObject fieldObject = new BasicDBObject();
		// 永远不显示_id字段
		fieldObject.put("_id", false);
		for (String field : fields) {
			fieldObject.put(field, true);
		}
		return getCollection(collection).find(dbObject, fieldObject).toArray();
	}
}
