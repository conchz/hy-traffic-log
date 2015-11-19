package com.ss.es;

import com.ss.main.Constants;
import com.ss.parser.GarbledCodeParser;
import com.ss.parser.KeywordExtractor;
import com.ss.parser.SearchEngineParser;
import com.ss.redis.JRedisPools;
import com.ss.utils.UrlUtils;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.Lists;

import redis.clients.jedis.Jedis;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by yousheng on 15/3/16.
 */
public class EsForward implements Constants {

    private static final int ONE_DAY_SECONDS = 86_400;

    private final int HANDLER_WORKERS = Runtime.getRuntime().availableProcessors() * 2;

    private final BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();

    private final ExecutorService preHandlerExecutor = Executors.newFixedThreadPool(HANDLER_WORKERS, new DataPreHandleThreadFactory());

    private final ExecutorService requestHandlerExecutor = Executors.newFixedThreadPool(HANDLER_WORKERS, new EsRequestThreadFactory());
    
	private final GaProcessor gaProcessor;
    
    private final PageConversionProcessor pageConversionProcessor;
    
    
    public EsForward(TransportClient client) {
        this.pageConversionProcessor = new PageConversionProcessor(client);
        this.gaProcessor = new GaProcessor();
        BlockingQueue<IndexRequest> requestQueue = new LinkedBlockingQueue<>();
        preHandle(client, requestQueue);
        handleRequest(client, requestQueue);
    }

    public void add(Map<String, Object> obj) {
        queue.add(obj);
    }

    private void preHandle(TransportClient client, BlockingQueue<IndexRequest> requestQueue) {
        for (int i = 0; i < HANDLER_WORKERS; i++)
            preHandlerExecutor.execute(new PreHandleWorker(client, requestQueue));
    }

    private void handleRequest(TransportClient client, BlockingQueue<IndexRequest> requestQueue) {
        for (int i = 0; i < HANDLER_WORKERS; i++)
            requestHandlerExecutor.execute(new RequestHandleWorker(client, requestQueue));
    }

    private void submitRequest(BulkRequestBuilder bulkRequestBuilder) {
        BulkResponse responses = bulkRequestBuilder.get();
        if (responses.hasFailures()) {
            System.out.println("Failure: " + responses.buildFailureMessage());
//            MonitorService.getService().es_data_error();
        }
    }

    private void addRequest(TransportClient client, BlockingQueue<IndexRequest> requestQueue, Map<String, Object> source) {
        IndexRequestBuilder builder = client.prepareIndex();
        builder.setIndex(source.remove(INDEX).toString());
        builder.setType(source.remove(TYPE).toString());
        builder.setSource(source);
        requestQueue.add(builder.request());
    }


    class DataPreHandleThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            PreHandlerThread thread = new PreHandlerThread(r);
            thread.setName("thread-relog-preHandler-" + counter.incrementAndGet());
            return thread;
        }
    }

    class EsRequestThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            RequestHandlerThread thread = new RequestHandlerThread(r);
            thread.setName("thread-relog-requestHandler-" + counter.incrementAndGet());
            return thread;
        }
    }

    /**
     * 数据预处理线程
     */
    class PreHandlerThread extends Thread {
        public PreHandlerThread(Runnable target) {
            super(target);
        }
    }

    /**
     * es请求处理线程
     */
    class RequestHandlerThread extends Thread {
        public RequestHandlerThread(Runnable target) {
            super(target);
        }
    }

    class PreHandleWorker implements Runnable {
        private final TransportClient client;
        private final BlockingQueue<IndexRequest> requestQueue;

        PreHandleWorker(TransportClient client, BlockingQueue<IndexRequest> requestQueue) {
            this.client = client;
            this.requestQueue = requestQueue;
        }

        @Override
        public void run() {
            while (true) {
                Map<String, Object> mapSource = null;
                try {
                    mapSource = queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mapSource == null || !mapSource.containsKey(T) || !mapSource.containsKey(TT))
                    continue;

                long clientTime = Long.parseLong(mapSource.remove(DT).toString());
                mapSource.put(CLIENT_TIME, clientTime);

                Jedis jedis = null;
                try {
                    jedis = JRedisPools.getConnection();
                    String trackId = mapSource.getOrDefault(T, EMPTY_STRING).toString();
                    String esType = jedis.get(TYPE_ID_PREFIX + trackId);
                    if (esType == null)
                        continue;
                    mapSource.remove(T);

                    // 网站代码安装的正确性检测
                    String siteUrl = jedis.get(SITE_URL_PREFIX + trackId);
                    if (Strings.isEmpty(siteUrl) || !UrlUtils.match(siteUrl, mapSource.get(CURR_ADDRESS).toString()))
                        continue;

                    /**
                     * 检测当天对同一网站访问的重复性
                     * key: trackId:2015-07-01
                     */
                    long time = Long.parseLong(mapSource.get(UNIX_TIME).toString());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(time);
                    String dateString = DATE_FORMAT.format(calendar.getTime());
                    String ipDupliKey = trackId + DELIMITER + dateString;
                    long statusCode = jedis.sadd(ipDupliKey, mapSource.get(REMOTE).toString());
                    mapSource.put(IP_DUPLICATE, statusCode);
                    // 设置过期时间
                    jedis.expire(ipDupliKey, ONE_DAY_SECONDS + 3600);

                    //页面转化
                    pageConversionProcessor.add(mapSource, esType);
                    
                   
//                    // TEST CODE
//                    if (TEST_TRACK_ID.equals(trackId)) {
//                        MonitorService.getService().data_ready();
//                    }

                    // 区分普通访问, 事件跟踪, xy坐标, 推广URL, 指定广告跟踪统计信息
                    String eventInfo = mapSource.getOrDefault(ET, EMPTY_STRING).toString();
                    String xyCoordinateInfo = mapSource.getOrDefault(XY, EMPTY_STRING).toString();
                    String promotionUrlInfo = mapSource.getOrDefault(UT, EMPTY_STRING).toString();
                    String adTrackInfo = mapSource.getOrDefault(AD_TRACK, EMPTY_STRING).toString();
                    Map<String, Object> adTrackMap = new HashMap<>();
                    if (!eventInfo.isEmpty()) {
                        mapSource.put(TYPE, esType + ES_TYPE_EVENT_SUFFIX);
                        addRequest(client, requestQueue, EventProcessor.handle(mapSource));
                        continue;
                    } else if (!xyCoordinateInfo.isEmpty()) {
                        mapSource.put(TYPE, esType + ES_TYPE_XY_SUFFIX);
                        addRequest(client, requestQueue, CoordinateProcessor.handle(mapSource));
                        continue;
                    } else if (!promotionUrlInfo.isEmpty()) {
                        if (!mapSource.get(CURR_ADDRESS).toString().contains(SEM_KEYWORD_IDENTIFIER))
                            continue;
                        mapSource.put(TYPE, esType + ES_TYPE_PROMOTION_URL_SUFFIX);
                        mapSource.put(HOST, trackId);
                        addRequest(client, requestQueue, PromotionUrlProcessor.handle(mapSource));
                        continue;
                    } else if (!adTrackInfo.isEmpty()) {
                        adTrackMap.put(INDEX, mapSource.get(INDEX).toString());
                        adTrackMap.put(TYPE, esType + ES_TYPE_AD_TRACK);
                        adTrackMap.put(AD_SOURCE, mapSource.get(AD_SOURCE).toString());
                        adTrackMap.put(AD_MEDIA, mapSource.get(AD_MEDIA).toString());
                        adTrackMap.put(AD_CAMPAIGN, mapSource.get(AD_CAMPAIGN).toString());
                        adTrackMap.put(AD_KEYWORD, mapSource.get(AD_KEYWORD).toString());
                        adTrackMap.put(AD_CREATIVE, mapSource.get(AD_CREATIVE).toString());
                        adTrackMap.put(REMOTE, mapSource.get(REMOTE).toString());
                        adTrackMap.put(UNIX_TIME, Long.parseLong(mapSource.get(UNIX_TIME).toString()));

                    }
                    mapSource.put(TYPE, esType);
                   /**
                    * Cache  - 保存访问信息
                    */
                    gaProcessor.add(mapSource);
                    
                    // 检测是否是一次的新的访问(1->新的访问, 0->同一次访问)
                    int identifier = Integer.valueOf(mapSource.getOrDefault(NEW_VISIT, 0).toString());

                    String locTemp = mapSource.get(CURR_ADDRESS).toString();
                    int lastIndex = locTemp.length() - 1;
                    if (Objects.equals('/', locTemp.charAt(lastIndex))) {
                        locTemp = locTemp.substring(0, lastIndex);
                        mapSource.put(CURR_ADDRESS, locTemp);
                        locTemp = null;
                    }

                    if (identifier == 1) {
                        mapSource.put(ENTRANCE, 1);
                        mapSource.remove(NEW_VISIT);
                        String _location = mapSource.get(CURR_ADDRESS).toString();

                        boolean hasPromotion = false;
                        if (_location.contains(SEM_KEYWORD_IDENTIFIER)) {
                            // keyword extract
                            Map<String, Object> keywordInfoMap = KeywordExtractor.parse(_location);
                            if (!keywordInfoMap.isEmpty())
                                mapSource.putAll(keywordInfoMap);

                            hasPromotion = true;
                        }

                        mapSource.put(DESTINATION_URL, hasPromotion ? _location : PLACEHOLDER);
                    } else {
                        mapSource.put(ENTRANCE, 0);
                    }

                    // 来源类型解析
                    String refer = mapSource.get(RF).toString();
                    String tt = mapSource.get(TT).toString();
                    String rf_type;
                    if (PLACEHOLDER.equals(refer) || UrlUtils.match(siteUrl, refer)) {  // 直接访问
                        mapSource.put(RF, PLACEHOLDER);
                        mapSource.put(SE, PLACEHOLDER);
                        mapSource.put(KW, PLACEHOLDER);

                        rf_type = jedis.get(tt);
                        if (rf_type == null) {
                            mapSource.put(RF_TYPE, VAL_RF_TYPE_DIRECT);
                            jedis.setex(tt, ONE_DAY_SECONDS, VAL_RF_TYPE_DIRECT);
                        } else {
                            mapSource.put(RF_TYPE, rf_type);
                        }

                        mapSource.put(DOMAIN, PLACEHOLDER);
                    } else {
                        List<String> skList = Lists.newArrayList();
                        boolean found = SearchEngineParser.getSK(java.net.URLDecoder.decode(refer, StandardCharsets.UTF_8.name()), skList);
                        // extract domain from rf
                        URL url = new URL(refer);
                        mapSource.put(DOMAIN, url.getProtocol() + DOUBLE_SLASH + url.getHost());
                        if (found) {    // 搜索引擎
                            mapSource.put(SE, skList.remove(0));

                            // 搜索词乱码识别
                            String searchWord = skList.remove(0);
                            if (GarbledCodeParser.isGarbledCode(searchWord))
                                searchWord = GARBLED_VALUE;
                            mapSource.put(KW, searchWord);

                            rf_type = jedis.get(tt);
                            if (rf_type == null) {
                                mapSource.put(RF_TYPE, VAL_RF_TYPE_SE);
                                jedis.setex(tt, ONE_DAY_SECONDS, VAL_RF_TYPE_SE);
                            } else {
                                mapSource.put(RF_TYPE, rf_type);
                            }
                        } else {    // 外部链接
                            rf_type = jedis.get(tt);
                            if (rf_type == null) {
                                mapSource.put(RF_TYPE, VAL_RF_TYPE_OUTLINK);
                                jedis.setex(tt, ONE_DAY_SECONDS, VAL_RF_TYPE_OUTLINK);
                            } else {
                                mapSource.put(RF_TYPE, rf_type);
                            }
                        }
                    }

                    // 访问URL路径解析
                    String location = UrlUtils.removeProtocol(mapSource.get(CURR_ADDRESS).toString());
                    if (location.contains(QUESTION_MARK))
                        location = location.substring(0, location.indexOf(QUESTION_MARK));

                    Map<String, String> pathMap = new HashMap<>();

                    final AtomicInteger integer = new AtomicInteger(0);
                    Consumer<String> pathConsumer = (String c) -> pathMap.put(HTTP_PATH + (integer.getAndIncrement()), c);
                    Arrays.asList(location.split("/")).stream().filter((p) -> !p.isEmpty() || !p.startsWith(HTTP_PREFIX)).forEach(pathConsumer);
                    mapSource.put(PATHS, pathMap);
                    
                    if(!adTrackInfo.isEmpty()){
                    	 fillingDate(mapSource,adTrackMap);
                    	 addRequest(client, requestQueue, adTrackMap);
                    }

                  
                    addRequest(client, requestQueue, mapSource);

                   
                } catch (NullPointerException | UnsupportedEncodingException | MalformedURLException e) {
                    e.printStackTrace();
//                    MonitorService.getService().data_error();
                } finally {
                    if (jedis != null) {
                        jedis.close();
                    }
                }

            }
        }

		private void fillingDate(Map<String, Object> mapSource, Map<String, Object> adTrackMap) {
            adTrackMap.put(TT, mapSource.get(TT).toString());
            adTrackMap.put(CURR_ADDRESS, mapSource.get(CURR_ADDRESS).toString());
            adTrackMap.put(UCV, mapSource.get(UCV).toString());
            adTrackMap.put(CITY, mapSource.get(CITY).toString());
            adTrackMap.put(ISP, mapSource.get(ISP).toString());
            adTrackMap.put(IP_DUPLICATE, mapSource.get(IP_DUPLICATE).toString());
            adTrackMap.put(VID, mapSource.get(VID).toString());
            adTrackMap.put(SE, mapSource.get(SE).toString());
            adTrackMap.put(AD_TRACK, mapSource.get(AD_TRACK).toString());
            adTrackMap.put(CLIENT_TIME, mapSource.get(CLIENT_TIME).toString());
            adTrackMap.put(ENTRANCE, mapSource.get(ENTRANCE).toString());
            adTrackMap.put(RF_TYPE, mapSource.get(RF_TYPE).toString());
            adTrackMap.put(HOST, mapSource.get(HOST).toString());
            adTrackMap.put(KW, mapSource.get(KW).toString());
            adTrackMap.put(VERSION, mapSource.get(VERSION).toString());
            adTrackMap.put(METHOD, mapSource.get(METHOD).toString());
            adTrackMap.put(VISITOR_IDENTIFIER, mapSource.get(VISITOR_IDENTIFIER).toString());
            adTrackMap.put(RF, mapSource.get(RF).toString());
            adTrackMap.put(REGION, mapSource.get(REGION).toString());
            adTrackMap.put(DOMAIN, mapSource.get(DOMAIN).toString());
            adTrackMap.put(ENTRANCE, mapSource.get(ENTRANCE).toString());
		}

    }

    class RequestHandleWorker implements Runnable {
        private final TransportClient client;
        private final BlockingQueue<IndexRequest> requestQueue;

        public RequestHandleWorker(TransportClient client, BlockingQueue<IndexRequest> requestQueue) {
            this.client = client;
            this.requestQueue = requestQueue;
        }

        @Override
        public void run() {
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
                    submitRequest(bulkRequestBuilder);
                    bulkRequestBuilder = client.prepareBulk();
                    continue;
                }

                if (bulkRequestBuilder.numberOfActions() == EsPools.getBulkRequestNumber()) {
                    submitRequest(bulkRequestBuilder);
                    bulkRequestBuilder = client.prepareBulk();
                }

            }
        }

    }

}
