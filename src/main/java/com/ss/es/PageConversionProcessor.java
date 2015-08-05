package com.ss.es;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ss.main.Constants;
import com.ss.redis.JRedisPools;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by perfection on 15-7-16.
 */
public class PageConversionProcessor implements Constants {

    private final BlockingQueue<Map<String, Object>> pageConversionQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<IndexRequest> requestQueue = new LinkedBlockingQueue<>();


    PageConversionProcessor(TransportClient client) {

        Thread requestThread = new Thread(() -> {
            while (true) {
                Map<String, Object> pageConversionMap = null;
                try {
                    pageConversionMap = pageConversionQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (pageConversionMap == null)
                    continue;

                pageConversionMap = pageConversionHandle(pageConversionMap);

                if (pageConversionMap == null)
                    continue;

                addRequest(client, requestQueue, pageConversionMap);
            }
        });

        Thread esThread = new Thread(() -> {
            BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();
            while (true) {
                IndexRequest request = null;
                try {
                    request = requestQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (request == null)
                    continue;

                bulkRequestBuilder.add(request);

                if (requestQueue.isEmpty() && bulkRequestBuilder.numberOfActions() > 0) {
                    bulkRequestBuilder.get();
                    bulkRequestBuilder = client.prepareBulk();
                    continue;
                }

                if (bulkRequestBuilder.numberOfActions() == EsPools.getBulkRequestNumber()) {
                    bulkRequestBuilder.get();
                    bulkRequestBuilder = client.prepareBulk();
                }

            }
        });

        requestThread.setName("thread-relog-pageconversion-pre");
        esThread.setName("thread-relog-pageconversion-es");

        requestThread.start();
        esThread.start();
    }

    public void add(Map<String, Object> source, String esType) {
        Map<String, Object> map = new HashMap<>(source);
        map.put(TYPE, esType + ES_TYPE_PAGE_CONVERSION_SUFFIX);
        pageConversionQueue.add(map);
    }


    private void addRequest(TransportClient client, BlockingQueue<IndexRequest> requestQueue, Map<String, Object> source) {
        IndexRequestBuilder builder = client.prepareIndex();
        builder.setIndex(source.remove(INDEX).toString());
        builder.setType(source.remove(TYPE).toString());
        builder.setSource(source);
        requestQueue.add(builder.request());
    }

    //统计页面转化数据
    private Map<String, Object> pageConversionHandle(Map<String, Object> source) {
        //测试参数包
//        String target = "{paths:[{steps:[{step_urls:[{url:'http://www.best-ad.cn/Quick_vote.html'},{url:'http://www.best-ad.cn/search.html'}],step_urls:[{url: 'http://www.best-ad.cn/Eye.html'}]}]},{steps:[{step_urls:[{url:'http://www.best-ad.cn/Wireless.html'}]}]}],target_urls:[ { url: 'http://www.best-ad.cn/', _id: '55a8c253d3f947e810942193' } ]}";
//        String strings = "[{url:'http://www.best-ad.cn/Quick_vote.html'},{url:'http://www.best-ad.cn/Wireless.html'}]";
//        String string1 = "[{url:'http://www.best-ad.cn/search.html'}]";
//        String string2 = "[{url:'http://www.best-ad.cn/Eye.html'}]";

        Jedis redis = JRedisPools.getConnection();//获取redis连接
        String spacer = "ss_pv_=";//初始化参数pv记录间隔符
        if (source.size() == 0) {
            closeRedis(redis);
            return null;
        }
        String loc_url = source.get("loc").toString();
        //去掉http://和网址末端的/
        loc_url = loc_url.split(DOUBLE_SLASH)[1];
        if (loc_url.substring(loc_url.length() - 1, loc_url.length()).equals("/")) {
            loc_url = loc_url.substring(0, loc_url.length() - 1);
        }

        redis.append(source.getOrDefault(T, EMPTY_STRING).toString() + ":" + source.get("tt"), "{" + loc_url + "}" + spacer);//向该key的value的值后添加该值
        //当key永久存在或者不存在时间限制的返回值为-1
        if (redis.ttl(source.getOrDefault(T, EMPTY_STRING).toString() + ":" + source.get("tt")) == -1) {
            redis.expire(source.getOrDefault(T, EMPTY_STRING).toString() + ":" + source.get("tt"), 60 * 60);
        }
        String configureData = redis.get("pc:" + source.getOrDefault(T, EMPTY_STRING).toString());//从redis读取页面转化配置信息
        if (configureData == null || Objects.equals("", configureData)) {
            closeRedis(redis);
            return null;
        }

        JSONObject jsonObject = JSONObject.parseObject(configureData);
        JSONArray target_urls = JSON.parseArray(jsonObject.getString("target_urls"));
        JSONArray paths_jsonArray = JSON.parseArray(jsonObject.getString("paths"));
        boolean isTarget_url = false;
        //判断是否是转化目标页面
        for (int i = 0, l = target_urls.size(); i < l; i++) {
            Map target_url = (Map) target_urls.get(i);
            if (loc_url.equals(target_url.get("url"))) {
                isTarget_url = true;
                break;
            }
        }
        if (isTarget_url) {
            String pvSaves = redis.get(source.getOrDefault(T, EMPTY_STRING).toString() + ":" + source.get("tt"));
            if (pvSaves == null || Objects.equals("", pvSaves)) {
                closeRedis(redis);
                return null;
            }
            pvSaves = pvSaves.substring(0, pvSaves.length() - spacer.length());
            String[] pvs = pvSaves.split(spacer);
            boolean isLeaf = false;
            boolean isConversion = false;//初始化参数是否为转化成功
            for (int l = 0; l < paths_jsonArray.size(); l++) {
                JSONObject paths_ = JSON.parseObject(paths_jsonArray.get(l).toString());
                JSONArray path_ = JSON.parseArray(paths_.get("steps").toString());
                //判断是否是转化路径是否一次相等
                for (int c = 0; c < path_.size(); c++) {
                    //获取配置的c步骤的url信息
//                    String steps = redis.get(jsonObject.getString("_id")+":pcu:"+c);
                    String steps = (JSON.parseObject(path_.get(c).toString())).getString("step_urls");
                    JSONArray step = JSON.parseArray(steps);
                    isLeaf = false;//初始化节点是否匹配阀值
                    //对于该节点的url数组取值判断
                    for (int i = step.size() - 1; i >= 0; i--) {
                        JSONObject stepOne = JSONObject.parseObject(step.get(i).toString());
                        for (int k = pvs.length - path_.size() - 1 + c; k <= pvs.length + c - path_.size() - 1; k++) {
                            if (k < pvs.length && k >= 1) {
                                if ((stepOne.getString("url").equals(pvs[k].substring(1, pvs[k].length() - 1)))) {
                                    isLeaf = true;
                                    break;
                                }
                            }
                        }
                        if (isLeaf) {
                            break;
                        }
                    }
                    //节点url是否匹配进而是否终止判断
                    if (!isLeaf) {
                        break;
                    } else if (c == path_.size() - 1) {//判断循环判断都结束且都通过
                        isConversion = true;
                    }
                }
                if (isConversion) {
                    closeRedis(redis);
                    return handle(source, loc_url, jsonObject);
                }
            }
            closeRedis(redis);
            return null;
        } else {
            closeRedis(redis);
            return null;
        }
    }

    private Map<String, Object> handle(Map<String, Object> source, String loc_url, JSONObject jsonObject) {
        if (source.isEmpty())
            return null;

        Map<String, Object> sourceMap = new HashMap<>();

        sourceMap.put(INDEX, source.get(INDEX).toString());//index索引
//                    sourceMap.put(TYPE, source.get(TYPE).toString());//索引type
        sourceMap.put(TT, source.get(TT).toString()); //访问次数标识符
        sourceMap.put(VID, source.get(VID).toString());//访客唯一标识符
        sourceMap.put(CURR_ADDRESS, loc_url);//loc当前访问的页面
        sourceMap.put(UNIX_TIME, Long.parseLong(source.get(UNIX_TIME).toString()));//当前系统时间
        sourceMap.put(VISITOR_IDENTIFIER, Integer.parseInt(source.get(VISITOR_IDENTIFIER).toString()));//新老客户
        sourceMap.put(REGION, source.get(REGION).toString());//地域
        sourceMap.put(CITY, source.get(CITY).toString());//城市
        sourceMap.put("pm", source.get("pm").toString());//pc,移动设备
        sourceMap.put(REMOTE, source.get(REMOTE).toString());//ip地址
        sourceMap.put(UCV, source.get(UCV).toString());//cookie标识用于统计uv
        sourceMap.put(PAGE_CONVERSION_NAME, jsonObject.getString("target_name"));//记录页面转化目标名称
        sourceMap.put(PAGE_CONVERSION_RECORD, jsonObject.getString("record_type"));//页面转化记录方式

//                    sourceMap.put(PAGE_CONVERSION_ORDERID,jsonObject.getString("target_name"));//订单号

        sourceMap.put(PAGE_CONVERSION_TYPE, jsonObject.getString("conv_tpye"));//页面转化类型
        sourceMap.put(PAGE_CONVERSION_TYPETEXT, jsonObject.getString("conv_text"));
        sourceMap.put(PAGE_CONVERSION_INCOME, jsonObject.getString("expected_yield"));//页面转化预期收益
        sourceMap.put(PAGE_CONVERSION_CONVERSIONRATE, jsonObject.getString("pecent_yield"));//页面转化预期转化率

        source.clear();
        return sourceMap;
    }

    private void closeRedis(Jedis jedis) {
        try {
            if (jedis != null)
                jedis.close();
        } catch (JedisException ignored) {

        }
    }

}
