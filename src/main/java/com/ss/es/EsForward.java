package com.ss.es;

import com.google.common.collect.Lists;
import com.ss.log.RelogLog;
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

import redis.clients.jedis.Jedis;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
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
    
    private final ExitStatisticsProcessor exitStatisticsProcessor;

    private final PageConversionProcessor pageConversionProcessor;


    public EsForward(TransportClient client) {
        this.pageConversionProcessor = new PageConversionProcessor(client);
        this.gaProcessor = new GaProcessor();
        this.exitStatisticsProcessor = new ExitStatisticsProcessor();
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
                    RelogLog.record(e.getMessage());
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
                    String pcname = mapSource.getOrDefault(PAGE_CONVERSION_NAME, EMPTY_STRING).toString();//页面转化

                    
                    String tt = mapSource.get(TT).toString();
                    
                    Map<String, Object> adTrackMap = new HashMap<>();
                    if (!eventInfo.isEmpty()) {
                        mapSource.put(TYPE, esType + ES_TYPE_EVENT_SUFFIX);
                        String locTemp = mapSource.get(CURR_ADDRESS).toString();
                        int lastIndex = locTemp.length() - 1;
                        if (Objects.equals('/', locTemp.charAt(lastIndex))) {
                            locTemp = locTemp.substring(0, lastIndex);
                            mapSource.put(CURR_ADDRESS, locTemp);
                            locTemp = null;
                        }
                        addRequest(client, requestQueue, EventProcessor.handle(mapSource, jedis.get(tt), siteUrl));
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
                        String ipAdsDupliKey = trackId + ":ads" + DELIMITER + dateString;
                        long adSstatusCode = jedis.sadd(ipAdsDupliKey, mapSource.get(REMOTE).toString());
                        jedis.expire(ipAdsDupliKey, ONE_DAY_SECONDS + 3600);
                        adTrackMap.put(IP_DUPLICATE, adSstatusCode);

                    }
                    mapSource.put(TYPE, esType);
                  

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
                    String rf_type=VAL_RF_TYPE_OUTLINK;
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

                    if (!adTrackInfo.isEmpty()) {
                        fillingDate(mapSource, adTrackMap);
                        addRequest(client, requestQueue, adTrackMap);
                    }
                    if (!pcname.isEmpty()) {
                        mapSource.put(TYPE, esType + ES_TYPE_PAGE_SUFFIX);
                    }


                    /**
                     * Cache  - 保存访问信息
                     */
                    gaProcessor.add(mapSource);
                    
                    /**
                     * exitStatistics :退出次数统计
                     * */
                    exitStatisticsProcessor.add(mapSource);
                    
                    addRequest(client, requestQueue, mapSource);
                } catch (NullPointerException | UnsupportedEncodingException | MalformedURLException e) {
                    e.printStackTrace();
                    RelogLog.record(e.getMessage() + "[  "+mapSource+"   ]");
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

            if (mapSource.containsKey(AD_RF)) {
                adTrackMap.put(RF, mapSource.get(AD_RF).toString());
                adTrackMap.put(AD_RF, mapSource.get(AD_RF).toString());
            } else {
                Map<String, String> params = URLRequest(mapSource.get(CURR_ADDRESS).toString());
                if (params.containsKey(AD_RF)) {
                    adTrackMap.put(RF, params.get(AD_RF));
                } else {
                    adTrackMap.put(RF, PLACEHOLDER);
                }

            }
            if (mapSource.containsKey(CURR_ADDRESS)) {
                adTrackMap.put(CURR_ADDRESS, mapSource.get(CURR_ADDRESS).toString());
            }

            if (mapSource.containsKey(UCV)) {
                adTrackMap.put(UCV, mapSource.get(UCV).toString());
            }

            if (mapSource.containsKey(CITY)) {
                adTrackMap.put(CITY, mapSource.get(CITY).toString());
            }

            if (mapSource.containsKey(ISP)) {
                adTrackMap.put(ISP, mapSource.get(ISP).toString());
            }

            if (mapSource.containsKey(VID)) {
                adTrackMap.put(VID, mapSource.get(VID).toString());
            }

            if (mapSource.containsKey(SE)) {
                adTrackMap.put(SE, mapSource.get(SE).toString());
            }
            if (mapSource.containsKey(AD_TRACK)) {
                adTrackMap.put(AD_TRACK, mapSource.get(AD_TRACK).toString());
            }
            if (mapSource.containsKey(CLIENT_TIME)) {
                adTrackMap.put(CLIENT_TIME, mapSource.get(CLIENT_TIME).toString());
            }
            if (mapSource.containsKey(ENTRANCE)) {
                adTrackMap.put(ENTRANCE, mapSource.get(ENTRANCE).toString());
            }
            if (mapSource.containsKey(RF_TYPE)) {
                adTrackMap.put(RF_TYPE, mapSource.get(RF_TYPE).toString());
            }
            if (mapSource.containsKey(HOST)) {
                adTrackMap.put(HOST, mapSource.get(HOST).toString());
            }
            if (mapSource.containsKey(KW)) {
                adTrackMap.put(KW, mapSource.get(KW).toString());
            }
            if (mapSource.containsKey(VERSION)) {
                adTrackMap.put(VERSION, mapSource.get(VERSION).toString());
            }
            if (mapSource.containsKey(METHOD)) {
                adTrackMap.put(METHOD, mapSource.get(METHOD).toString());
            }
            if (mapSource.containsKey(VISITOR_IDENTIFIER)) {
                adTrackMap.put(VISITOR_IDENTIFIER, mapSource.get(VISITOR_IDENTIFIER).toString());
            }

            if (mapSource.containsKey(REGION)) {
                adTrackMap.put(REGION, mapSource.get(REGION).toString());
            }
            if (mapSource.containsKey(DOMAIN)) {
                adTrackMap.put(DOMAIN, mapSource.get(DOMAIN).toString());
            }
            if (mapSource.containsKey(ENTRANCE)) {
                adTrackMap.put(ENTRANCE, mapSource.get(ENTRANCE).toString());
            }
            if (PLACEHOLDER.equals(adTrackMap.get(RF).toString().trim())) {
                adTrackMap.put(RF, "直接访问");
            }
        }

        /**
         * @param URL
         * @description 解析url获取参数
         * @author ZhangHuaRong
         * @update 2015年10月28日 下午2:19:24
         */
        private Map<String, String> URLRequest(String URL) {
            Map<String, String> mapRequest = new HashMap<String, String>();
            String[] arrSplit = null;
            String strUrlParam = TruncateUrlPage(URL);
            if (strUrlParam == null) {
                return mapRequest;
            }
            // 每个键值为一组
            arrSplit = strUrlParam.split("[&]");
            for (String strSplit : arrSplit) {
                String[] arrSplitEqual = null;
                arrSplitEqual = strSplit.split("[=]");
                // 解析出键值
                if (arrSplitEqual.length > 1) {
                    // 正确解析
                    mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);
                } else {
                    if (arrSplitEqual[0] != "") {
                        // 只有参数没有值，不加入
                        mapRequest.put(arrSplitEqual[0], "");
                    }
                }
            }
            return mapRequest;
        }

        private String TruncateUrlPage(String strURL) {
            String strAllParam = null;
            String[] arrSplit = null;
            strURL = strURL.trim().toLowerCase();
            arrSplit = strURL.split("[?]");
            if (strURL.length() > 1) {
                if (arrSplit.length > 1) {
                    if (arrSplit[1] != null) {
                        strAllParam = arrSplit[1];
                    }
                }
            }
            return strAllParam;
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
