package org.tron.walletserver;

import java.util.Objects;

public class TronVegasNodeInfo implements Comparable<TronVegasNodeInfo> {
    public static final int DEFAULT_NODE_WEIGHT = 10;//各节点默认权重

    private String host;

    private long blockNum;

    private long responseTime;

    private int weight;

    private GrpcClient client;

    @Override
    public int compareTo(TronVegasNodeInfo o) {

        if(responseTime < o.getResponseTime()){
            return -1;
        }else if(responseTime > o.getResponseTime()){
            return 1;
        }

        if(blockNum > o.getBlockNum()){
            return -1;
        }else if(blockNum < o.getBlockNum()){
            return 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TronVegasNodeInfo)) return false;
        TronVegasNodeInfo that = (TronVegasNodeInfo) o;
        return Objects.equals(getHost(), that.getHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost());
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public long getBlockNum() {
        return blockNum;
    }

    public void setBlockNum(long blockNum) {
        this.blockNum = blockNum;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public GrpcClient getClient() {
        return client;
    }

    public void setClient(GrpcClient client) {
        this.client = client;
    }
}
