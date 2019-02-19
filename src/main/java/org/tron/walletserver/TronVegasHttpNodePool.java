package org.tron.walletserver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

public class TronVegasHttpNodePool {

    private static final Logger logger = LoggerFactory.getLogger("TronVegasGrpcClientPool");

    private static final int MAX_NODE_HOST_CACHE_SIZE = 1000;//节点信息缓存总数

    private static final int QUERY_NODE_THREAD_NUMBER = 100;//查找节点线程总数
    private static final int CONNECTING_TIMEOUT = 2000;//测试连接超时时间(ms)
    private static final int DEFAULT_GRPC_PORT = 8090;//HTTP默认端口

    private static final long QUERY_LIMIT_TIME = 20000;//查询节点总时间(ms)

    private static final long MAX_QUERY_TIME = 2000;//请求节点最大响应时间限制(ms)
    private static final int MAX_ERROR_BLOCK_NUM = 2;//请求节点块最大误差范围

    private static final int MAX_NODE_LIMIT = 10;//保留节点数量

    private static final long FREQUENCY_QUERY_LIMIT_TIME = 60000 * 3;//查询节点频率限制(ms)

    private static ConcurrentSkipListSet<TronVegasNodeHost> nodeHostCacheSet = new ConcurrentSkipListSet<>();
    private static ScheduledExecutorService scheduledExecutorService;

    private final Object lock = new Object();

    private long lastQueryTime = 0;

    private static TronVegasHttpNodePool instance = new TronVegasHttpNodePool();

    public static TronVegasHttpNodePool getInstance(){
        return instance;
    }

    private static final JsonParser jsonParser = new JsonParser();

    public void queryFastestNodes(QueryNodeCallback queryNodeCallback, boolean forceQuery) {
        if(!forceQuery && System.currentTimeMillis() - lastQueryTime < FREQUENCY_QUERY_LIMIT_TIME){
            logger.info("Query fastest node in limit time");
            return;
        }

        synchronized (lock){
            if(!forceQuery && System.currentTimeMillis() - lastQueryTime < FREQUENCY_QUERY_LIMIT_TIME){
                logger.info("Query fastest node in limit time");
                return;
            }
            lastQueryTime = System.currentTimeMillis();
        }

        try {

            if (scheduledExecutorService == null) {
                scheduledExecutorService = Executors.newScheduledThreadPool(1);
            }

            try{
                Optional<GrpcAPI.NodeList> opNodeList = TronVegasApi.listNodesByDefault();
                GrpcAPI.NodeList nodeList = opNodeList.get();
                if (nodeList.getNodesCount() > 0) {
                    for (int index = 0; index < nodeList.getNodesCount(); index++) {
                        GrpcAPI.Node node = nodeList.getNodes(index);
                        TronVegasNodeHost nodeHost = new TronVegasNodeHost();
                        nodeHost.init(node.getAddress().getHost().toStringUtf8(), DEFAULT_GRPC_PORT);
                        addNodeHost(nodeHost);
                    }
                }
            }catch (Exception ex){
                logger.error("TronVegasApi.listNodes ERROR", ex);
            }

            if(nodeHostCacheSet.size() <= 0){
                return;
            }

            final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(QUERY_NODE_THREAD_NUMBER);
            final ConcurrentSkipListSet<TronVegasNodeInfo> fullNodeSet = new ConcurrentSkipListSet<>();

            for(TronVegasNodeHost nodeHost : nodeHostCacheSet)
                fixedThreadPool.execute(() -> {
                    try {
                        if (HttpUtils.crunchifyAddressReachable(nodeHost.getIp(), nodeHost.getPort(), CONNECTING_TIMEOUT) <= MAX_QUERY_TIME && HttpUtils.crunchifyAddressReachable(nodeHost.getIp(), nodeHost.getPort() + 1, CONNECTING_TIMEOUT) <= MAX_QUERY_TIME) {
                            try {
                                long time = System.currentTimeMillis();
                                String result = sendPost("http://" + nodeHost.getHost() + "/wallet/getnowblock", "");
                                if(!StringUtils.isBlank(result)){
                                    JsonObject jsonData = jsonParser.parse(result).getAsJsonObject();
                                    TronVegasNodeInfo tNode = new TronVegasNodeInfo();
                                    tNode.setHost(nodeHost.getHost());
                                    tNode.setBlockNum(jsonData.getAsJsonObject("block_header").getAsJsonObject("raw_data").get("number").getAsLong());
                                    tNode.setResponseTime(System.currentTimeMillis() - time);
                                    tNode.setWeight(TronVegasNodeInfo.DEFAULT_NODE_WEIGHT);
                                    fullNodeSet.add(tNode);
                                }
                            } catch (Exception ex) {
                                logger.debug(ex.getMessage());
                            }
                        } else {
                            logger.debug(nodeHost.getIp() + " can't be connected");
                        }
                    } catch (Exception ex) {
                        logger.debug(ex.getMessage());
                    }
                });

            scheduledExecutorService.schedule(() -> {
                try {

                    fixedThreadPool.shutdownNow();

                    Set<TronVegasNodeInfo> tempSet = new HashSet<>();

                    for (TronVegasNodeInfo entry : fullNodeSet) {
                        if (entry.getResponseTime() >= MAX_QUERY_TIME) {
                            tempSet.add(entry);
                        }
                        if(TronVegasApi.isDebug){
                            logger.info("Host: " + entry.getHost() + " RTime:" + entry.getResponseTime() + " BlockNum:" + entry.getBlockNum());
                        }
                    }
                    fullNodeSet.removeAll(tempSet);
                    tempSet.clear();

                    long maxBlockNum = 0;
                    for (TronVegasNodeInfo entry : fullNodeSet) {
                        if (entry.getBlockNum() > maxBlockNum) {
                            maxBlockNum = entry.getBlockNum();
                        }
                    }
                    for (TronVegasNodeInfo entry : fullNodeSet) {
                        if (entry.getBlockNum() < (maxBlockNum - MAX_ERROR_BLOCK_NUM)) {
                            tempSet.add(entry);
                        }
                    }
                    fullNodeSet.removeAll(tempSet);
                    tempSet.clear();

                    int index = 0;
                    for (TronVegasNodeInfo entry : fullNodeSet) {
                        index++;
                        if (index > MAX_NODE_LIMIT) {
                            tempSet.add(entry);
                        }
                    }
                    fullNodeSet.removeAll(tempSet);
                    tempSet.clear();

                    if(TronVegasApi.isDebug){
                        logger.info("FINAL FASTEST NODE LISTS");
                        for (TronVegasNodeInfo entry : fullNodeSet) {
                            logger.info("Host: " + entry.getHost() + " RTime:" + entry.getResponseTime() + " BlockNum:" + entry.getBlockNum());
                        }
                    }
                } catch (Exception ex) {
                    logger.error("QueryFastestNodes Schedule ERROR", ex);
                }
            }, QUERY_LIMIT_TIME, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            logger.error("QueryFastestNodes ERROR", ex);
        }
    }

    private void addNodeHost(TronVegasNodeHost nodeHost){
        nodeHostCacheSet.add(nodeHost);
        if(nodeHostCacheSet.size() > MAX_NODE_HOST_CACHE_SIZE){
            nodeHostCacheSet.pollFirst();
        }
    }

    public void shutdown() {
        try {
            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (Exception ex) {
            logger.error("Shutdown ERROR", ex);
        }
    }

    public interface QueryNodeCallback {

        void finish(Collection<TronVegasNodeHost> nodes);

    }

    public static String sendPost(String strURL, String params) {
        BufferedReader reader = null;
        try {
            URL url = new URL(strURL);// 创建连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("POST"); // 设置请求方式
            // connection.setRequestProperty("Accept", "application/json"); // 设置接收数据的格式
            connection.setRequestProperty("Content-Type", "application/json"); // 设置发送数据的格式
            connection.connect();
            //一定要用BufferedReader 来接收响应， 使用字节来接收响应的方法是接收不到内容的
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8"); // utf-8编码
            out.append(params);
            out.flush();
            out.close();
            // 读取响应
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String line;
            String res = "";
            while ((line = reader.readLine()) != null) {
                res += line;
            }
            reader.close();

            return res;
        } catch (IOException e) {
            logger.debug("POST ERROR", e);
        }
        return null; // 自定义错误信息
    }
}
