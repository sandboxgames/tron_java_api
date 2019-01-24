package org.tron.walletserver;

import java.util.Objects;

public class TronVegasNodeHost implements Comparable<TronVegasNodeHost> {

    private String ip;

    private int port;

    private String host;

    @Override
    public int compareTo(TronVegasNodeHost o) {
        return ip.compareTo(o.getIp());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TronVegasNodeHost)) return false;
        TronVegasNodeHost that = (TronVegasNodeHost) o;
        return Objects.equals(getHost(), that.getHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(host);
    }

    public String getHost() {
        return host;
    }

    public String getIp() {
        return ip;
    }

    public void init(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.host = this.ip + ":" + this.port;
    }

    public int getPort() {
        return port;
    }

}
