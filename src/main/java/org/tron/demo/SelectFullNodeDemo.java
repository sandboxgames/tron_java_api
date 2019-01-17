package org.tron.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.walletserver.*;
import org.tron.walletserver.TronVegasApi;


public class SelectFullNodeDemo {
    private static final Logger logger = LoggerFactory.getLogger("SelectFullNodeDemo");

    private static boolean isFinished = false;

    public static void main(String[] args) throws Exception {

        // 注意
        // 初始化
        // TronVegasApi.initWithPrivateKey(privateKey);
        // TronVegasApi.queryFastestNodes();
        // 用定时调度调用
        // TronVegasApi.queryFastestNodes();



        // 以下为测试调试
        final String privateKey = "";
        TronVegasApi.initWithPrivateKey(privateKey);

        TronVegasGrpcClientPool.getInstance().queryFastestNodes(fullNodes -> {

            if (fullNodes != null) {
                for (TronVegasNodeInfo entry : fullNodes) {
                    logger.info("Host: " + entry.getHost() + " RTime:" + entry.getResponseTime() + " BlockNum:" + entry.getBlockNum());
                }
            }
            SelectFullNodeDemo.isFinished = true;
//            TronVegasGrpcClientPool.getInstance().shutdown();
        }, false);


        while (true){
//            if(isFinished){
                GrpcAPI.BlockExtention block = TronVegasApi.getNowBlock();
                if(block != null){
                    logger.info("Block:" + block.getBlockHeader().getRawData().getNumber());
                }
//            }
            Thread.sleep(1000);
        }
    }

}
