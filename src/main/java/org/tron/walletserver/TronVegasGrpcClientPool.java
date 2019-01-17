package org.tron.walletserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class TronVegasGrpcClientPool {
    private static final Logger logger = LoggerFactory.getLogger("TronVegasGrpcClientPool");

    private static final int QUERY_NODE_THREAD_NUMBER = 100;//查找节点线程总数
    private static final int CONNECTING_TIMEOUT = 2000;//测试连接超时时间(ms)
    private static final int DEFAULT_GRPC_PORT = 50051;//GRPC默认端口

    private static final long QUERY_LIMIT_TIME = 60000;//查询节点总时间(ms)

    private static final long MAX_QUERY_TIME = 1000;//请求节点最大响应时间限制(ms)
    private static final int MAX_ERROR_BLOCK_NUM = 2;//请求节点块最大误差范围

    private static final int MAX_NODE_LIMIT = 5;//保留节点数量


    public static TronVegasGrpcClientPool instance = new TronVegasGrpcClientPool();
    private SortedMap<Long, TronVegasNodeInfo> circle;
    private GrpcClient defaultClient;
    private String defaultFullNode = "";
    private String defaultSolidityNode = "";
    private ScheduledExecutorService scheduledExecutorService;

    public static TronVegasGrpcClientPool getInstance() {
        return instance;
    }

    /*
     * Overriding default InetAddress.isReachable() method to add 2 more arguments port and timeout value
     *
     * Address: www.google.com
     * port: 80 or 443
     * timeout: 2000 (in milliseconds)
     */
    public static long crunchifyAddressReachable(String address, int port, int timeout) {
        try {

            long time = System.currentTimeMillis();
            try (Socket crunchifySocket = new Socket()) {
                // Connects this socket to the server with a specified timeout value.
                crunchifySocket.connect(new InetSocketAddress(address, port), timeout);
            }
            // Return true if connection successful
            return (System.currentTimeMillis() - time);
        } catch (Exception exception) {
            // Return false if connection fails
            return -1;
        }
    }

    public void init(String fullNode, String solidityNode) {
        this.defaultFullNode = fullNode;
        this.defaultSolidityNode = solidityNode;
        this.defaultClient = new GrpcClient(this.defaultFullNode, this.defaultSolidityNode);
    }

    public void queryFastestNodes(QueryNodeCallback queryNodeCallback) {

        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
        }

        Optional<GrpcAPI.NodeList> opNodeList = TronVegasApi.listNodes();

        if (opNodeList == null || !opNodeList.isPresent()) {
            return;
        }

        GrpcAPI.NodeList nodeList = opNodeList.get();
        if (nodeList.getNodesCount() <= 0) {
            logger.info("No nodes is found");
            return;
        }

        final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(QUERY_NODE_THREAD_NUMBER);
        final ConcurrentSkipListSet<TronVegasNodeInfo> fullNodeSet = new ConcurrentSkipListSet<>();

        for (int index = 0; index < nodeList.getNodesCount(); index++) {
            final int i = index;
            fixedThreadPool.execute(() -> {
                try {
                    GrpcAPI.Node node = nodeList.getNodes(i);
                    String ip = node.getAddress().getHost().toStringUtf8();

                    if (TronVegasGrpcClientPool.crunchifyAddressReachable(node.getAddress().getHost().toStringUtf8(), DEFAULT_GRPC_PORT, CONNECTING_TIMEOUT) <= MAX_QUERY_TIME) {
                        try {
                            String host = ip + ":" + DEFAULT_GRPC_PORT;
                            long time = System.currentTimeMillis();
                            GrpcClient client = new GrpcClient(host, "");
                            GrpcAPI.BlockExtention block = client.getBlock2(-1);
                            long blockNum = block.getBlockHeader().getRawData().getNumber();
                            TronVegasNodeInfo tNode = new TronVegasNodeInfo();
                            tNode.setHost(host);
                            tNode.setBlockNum(blockNum);
                            tNode.setResponseTime(System.currentTimeMillis() - time);
                            tNode.setClient(client);
                            tNode.setWeight(TronVegasNodeInfo.DEFAULT_NODE_WEIGHT);
                            fullNodeSet.add(tNode);
                        } catch (Exception ex) {
                            logger.debug(ex.getMessage());
                        }
                    } else {
                        logger.debug(ip + " can't be connected");
                    }
                } catch (Exception ex) {
                    logger.debug(ex.getMessage());
                }
            });
        }

        scheduledExecutorService.schedule(() -> {
            try {

                fixedThreadPool.shutdownNow();

                Set<TronVegasNodeInfo> tempSet = new HashSet<>();

                for (TronVegasNodeInfo entry : fullNodeSet) {
                    if (entry.getResponseTime() >= MAX_QUERY_TIME) {
                        tempSet.add(entry);
                        safeReleaseNode(entry);
                    }
//                    logger.info("Host: " + entry.getHost() + " RTime:" + entry.getResponseTime() + " BlockNum:" + entry.getBlockNum());
                }
                fullNodeSet.removeAll(tempSet);
                tempSet.clear();
                logger.info("-------------------------------------------------------");

                long maxBlockNum = 0;
                for (TronVegasNodeInfo entry : fullNodeSet) {
                    if (entry.getBlockNum() > maxBlockNum) {
                        maxBlockNum = entry.getBlockNum();
                    }
                }
                for (TronVegasNodeInfo entry : fullNodeSet) {
                    if (entry.getBlockNum() < (maxBlockNum - MAX_ERROR_BLOCK_NUM)) {
                        tempSet.add(entry);
                        safeReleaseNode(entry);
                    }
                }
                fullNodeSet.removeAll(tempSet);
                tempSet.clear();

                int index = 0;
                for (TronVegasNodeInfo entry : fullNodeSet) {
                    index++;
                    if(index > MAX_NODE_LIMIT){
                        tempSet.add(entry);
                        safeReleaseNode(entry);
                    }
//                    logger.info("Host: " + entry.getHost() + " RTime:" + entry.getResponseTime() + " BlockNum:" + entry.getBlockNum());
                }
                fullNodeSet.removeAll(tempSet);
                tempSet.clear();

//                logger.info("-------------------------------------------------------");
//                for (TronVegasNodeInfo entry : fullNodeSet) {
//                    logger.info("Host: " + entry.getHost() + " RTime:" + entry.getResponseTime() + " BlockNum:" + entry.getBlockNum());
//                }
                initNodes(fullNodeSet);
                if(queryNodeCallback != null){
                    queryNodeCallback.finish(fullNodeSet);
                }
            } catch (Exception ex) {
                logger.error("QueryFastestNodes ERROR", ex);
            }
        }, QUERY_LIMIT_TIME, TimeUnit.MILLISECONDS);
    }

    private void safeReleaseNode(TronVegasNodeInfo nodeInfo) {
        if (nodeInfo == null || nodeInfo.getClient() == null) {
            return;
        }

        try {
            nodeInfo.getClient().shutdown();
        } catch (Exception ex) {
            logger.info("SafeReleaseNode ERROR", ex);
        }
    }

    public void initNodes(Collection<TronVegasNodeInfo> nodes) {
        if (nodes == null || nodes.size() <= 0) {
            return;
        }

        SortedMap<Long, TronVegasNodeInfo> newCircle = new TreeMap<>();
        for (TronVegasNodeInfo node : nodes) {
            add(newCircle, node);
        }

        SortedMap<Long, TronVegasNodeInfo> oldCircle = this.circle;
        this.circle = newCircle;
        release(oldCircle);
    }

    public GrpcClient borrow() {
        TronVegasNodeInfo proxy = get(UUID.randomUUID().toString());
        if (proxy != null && proxy.getClient() != null) {
            return proxy.getClient();
        }
        return defaultClient;
    }

    private TronVegasNodeInfo get(Object key) {
        if (circle == null || circle.isEmpty()) {
            return null;
        }

        SortedMap<Long, TronVegasNodeInfo> tail = circle.tailMap(hash(key.toString()));
        if (tail.size() == 0) {
            return circle.get(circle.firstKey());
        }
        return tail.get(tail.firstKey());
    }

    public void shutdown() {
        try {
            release(this.circle);
            if(scheduledExecutorService != null){
                scheduledExecutorService.shutdownNow();
            }
        }catch (Exception ex){
            logger.error("Shutdown ERROR", ex);
        }
    }

    private void add(SortedMap<Long, TronVegasNodeInfo> collection, TronVegasNodeInfo node) {
        for (int i = 0; i < node.getWeight(); i++) {
            collection.put(hash(node.toString() + i), node);
        }
    }

    private void release(SortedMap<Long, TronVegasNodeInfo> oldCircle) {
        if (oldCircle != null && oldCircle.size() > 0) {
            for (SortedMap.Entry<Long, TronVegasNodeInfo> entry : oldCircle.entrySet()) {
                safeReleaseNode(entry.getValue());
            }
            oldCircle.clear();
        }
    }

    private long hash(String key) {

        final int p = 16777619;
        int hash = (int) 2166136261L;

        for (byte b : key.getBytes())
            hash = (hash ^ b) * p;

        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        return hash;
    }

    public interface QueryNodeCallback {

        void finish(Collection<TronVegasNodeInfo> nodes);

    }
}
