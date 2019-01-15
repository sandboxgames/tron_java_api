package org.tron.walletserver;

import com.google.common.primitives.UnsignedLong;

public class TronVegasGrpcClientPool {

    public static TronVegasGrpcClientPool instance = new TronVegasGrpcClientPool();

    public static TronVegasGrpcClientPool getInstance(){
        return instance;
    }

    public void init(){

    }


}
