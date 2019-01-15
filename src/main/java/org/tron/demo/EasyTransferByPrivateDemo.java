package org.tron.demo;

import org.tron.api.GrpcAPI.EasyTransferResponse;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol.Transaction;
import org.tron.walletserver.WalletApi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class EasyTransferByPrivateDemo {


    public static boolean isFinished = false;
    public static AtomicInteger successCount  = new AtomicInteger(0);
    public static AtomicInteger count  = new AtomicInteger(0);

    public static final int TOTAL_COUNT = 10000;

    public static void main(String[] args) throws Exception {
        String privateKey = "53DA77E817FC18BDDED706DFD242B3F1DA86CFF38A385DB4B0B223D73A5B8A09";
        String toAddress = "TZHspmV9EtRiXKmyoReYPFRNQbf2ekoUCX";


        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1000);

        long time1 = System.currentTimeMillis();
        for (int i = 1; i <= TOTAL_COUNT; i++) {
            fixedThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        EasyTransferResponse response = WalletApi
                                .easyTransferByPrivate(ByteArray.fromHexString(privateKey),
                                        WalletApi.decodeFromBase58Check(toAddress), 1L);

//                        if (response.getResult().getResult()) {
//                            Transaction transaction = response.getTransaction();
//                            System.out.println("Easy transfer successful!!!");
//                            System.out.println(
//                                    "Receive txid = " + ByteArray.toHexString(response.getTxid().toByteArray()));
//                            System.out.println(Utils.printTransaction(transaction));
//                        } else {
//                            System.out.println("Easy transfer failed!!!");
//                            System.out.println("Code = " + response.getResult().getCode());
//                            System.out.println("Message = " + response.getResult().getMessage().toStringUtf8());
//                        }
                        successCount.incrementAndGet();
                    } catch (Exception ex) {
                        System.out.println("ERROR:" + ex.getMessage());
                    }
                    count.incrementAndGet();
                }
            });
        }

        while (!isFinished){
            if(count.get() >= TOTAL_COUNT){
                isFinished = true;
            }
            Thread.sleep(1);
        }
        long totalTime = System.currentTimeMillis() - time1;


        fixedThreadPool.shutdown();

        System.out.println("Total Count:" + count.get());
        System.out.println("Success Count:" + successCount.get());
        System.out.println("Total Time: " + totalTime + "ms");
        System.out.println("TPS: " + (TOTAL_COUNT * 1000 / totalTime));


//        EasyTransferResponse response = WalletApi
//                .easyTransferByPrivate(ByteArray.fromHexString(privateKey),
//                        WalletApi.decodeFromBase58Check(toAddress), 1L);
//
//        if (response.getResult().getResult()) {
//            Transaction transaction = response.getTransaction();
//            System.out.println("Easy transfer successful!!!");
//            System.out.println(
//                    "Receive txid = " + ByteArray.toHexString(response.getTxid().toByteArray()));
//            System.out.println(Utils.printTransaction(transaction));
//        } else {
//            System.out.println("Easy transfer failed!!!");
//            System.out.println("Code = " + response.getResult().getCode());
//            System.out.println("Message = " + response.getResult().getMessage().toStringUtf8());
//        }
    }
}
