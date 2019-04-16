package org.tron.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.walletserver.TronVegasApi;
import org.tron.walletserver.TronVegasGrpcClientPool;
import org.tron.walletserver.TronVegasNodeInfo;


public class SelectFullNodeDemo {

    private static final Logger logger = LoggerFactory.getLogger("SelectFullNodeDemo");

    private static boolean isFinished = false;

    private static long blockNum = -1;

    public static void main(String[] args) {

        TronVegasApi.isDebug = true;
        TronVegasApi.initWithPrivateKey("");

        TronVegasGrpcClientPool.getInstance().queryFastestNodes(fullNodes -> {

            if (fullNodes != null) {
                for (TronVegasNodeInfo entry : fullNodes) {
                    logger.info("Host: " + entry.getHost() + " RTime:" + entry.getResponseTime() + " BlockNum:" + entry.getBlockNum());
                }
            }

//            TronVegasGrpcClientPool.getInstance().shutdown();
        }, true);

        long time = 0;

        while (!isFinished){

            if(time >= 60000){
                time = 0;
                TronVegasGrpcClientPool.getInstance().queryFastestNodes(null, true);
            }

            try {
                GrpcAPI.BlockExtention block = TronVegasApi.getBlock2Safe(blockNum);
                if(block != null){
                    if(block.hasBlockHeader()){
                        logger.info("Block:" + block.getBlockHeader().getRawData().getNumber());
                        blockNum = block.getBlockHeader().getRawData().getNumber();
                    }else{
                        logger.info("Block: NULL");
                    }
                }else{
                    logger.info("Block: NULL");
                }
            }catch (Exception ex){
                logger.info("GetBlock2Safe ERROR: " + ex.getMessage());
            }

            try {
                Thread.sleep(3000);
                blockNum++;
            }catch (InterruptedException e){

            }

            time += 1000;
        }
//

        TronVegasGrpcClientPool.getInstance().shutdown();
    }

}
