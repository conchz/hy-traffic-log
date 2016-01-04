package com.ss.es;

import com.ss.main.RelogConfig;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.net.InetSocketAddress;
import java.util.*;

import static com.ss.main.Constants.DEV_MODE;
import static com.ss.main.Constants.PROD_MODE;

/**
 * Created by dolphineor on 2015-3-20.
 */
public class EsPools {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("elasticsearch");

    private static int bulkRequestNumber;

    private static List<TransportClient> clients = new ArrayList<>();
    private static Map<String, String> esMap = new HashMap<>();


    private EsPools() {
    }

    public static List<TransportClient> getEsClient() {
        if (clients.isEmpty()) {
            synchronized (EsPools.class) {
                if (clients.isEmpty()) {
                    String[] hosts = BUNDLE.getString("es.host").split(";");
                    String[] clusters = BUNDLE.getString("es.cluster").split(";");
                    switch (RelogConfig.getMode()) {
                        case DEV_MODE:
                            clients.addAll(initEsClient(hosts[0], clusters[0]));
                            break;
                        case PROD_MODE:
                            clients.addAll(initEsClient(hosts[1], clusters[1]));
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        return clients;
    }

    private static List<TransportClient> initEsClient(String host, String cluster) {
        List<TransportClient> clients = new ArrayList<>();
        try {
            String[] hostArr = host.split("\\|");
            String[] clusterArr = cluster.split("\\|");
            for (int i = 0, l = hostArr.length; i < l; i++) {
                List<InetSocketTransportAddress> addressList = new ArrayList<>();
                for (String _host : hostArr[i].split(",")) {
                    String[] arr = _host.split(":");
                    if (arr.length == 1)
                        addressList.add(new InetSocketTransportAddress(new InetSocketAddress(arr[0], 9300)));
                    else if (arr.length == 2)
                        addressList.add(new InetSocketTransportAddress(new InetSocketAddress(arr[0], Integer.parseInt(arr[1]))));
                }
                String clusterName = clusterArr[i];

                Settings settings = Settings.builder().put(esMap)
                        .put("cluster.name", clusterName)
                        .put("client.transport.sniff", true)
                        .put("client.transport.ignore_cluster_name", false)
                        .put("client.transport.ping_timeout", "10s")
                        .put("client.transport.nodes_sampler_interval", "15s").build();
                TransportClient client = TransportClient.builder().settings(settings).build();
                client.addTransportAddresses(addressList.toArray(new InetSocketTransportAddress[addressList.size()]));
                clients.add(client);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return clients;
    }


    public static int getBulkRequestNumber() {
        return bulkRequestNumber;
    }

    public static void setBulkRequestNumber(int bulkRequestNumber) {
        EsPools.bulkRequestNumber = bulkRequestNumber;
    }
}
