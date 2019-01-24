package org.tron.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.walletserver.TronVegasApi;
import org.tron.walletserver.TronVegasGrpcClientPool;
import org.tron.walletserver.TronVegasNodeInfo;

public class HttpBlockScanDemo {

    private static final Logger logger = LoggerFactory.getLogger("HttpBlockScanDemo");

    private static boolean isFinished = false;

    private static boolean isLoaded = false;

    public static void main(String[] args) throws Exception {

        TronVegasApi.initWithPrivateKey("");

        TronVegasGrpcClientPool.getInstance().queryFastestNodes(fullNodes -> {

            if (fullNodes != null) {
                for (TronVegasNodeInfo entry : fullNodes) {
                    logger.info("Host: " + entry.getHost() + " RTime:" + entry.getResponseTime() + " BlockNum:" + entry.getBlockNum());
                }
            }
            isLoaded = true;
        }, true);


        while (!isFinished){

            if(!isLoaded){
                continue;
            }

            try {
                GrpcAPI.BlockExtention block = TronVegasApi.getBlock2Safe(-1);
                if(block != null){
                    if(block.hasBlockHeader()){
                        logger.info("Block:" + block.toString());
                    }else{
                        logger.info("Block: NULL");
                    }
                }else{
                    logger.info("Block: NULL");
                }
            }catch (Exception ex){
                logger.error("GetBlock2Safe ERROR", ex);
            }
            Thread.sleep(500);
        }

        TronVegasGrpcClientPool.getInstance().shutdown();
    }

}
