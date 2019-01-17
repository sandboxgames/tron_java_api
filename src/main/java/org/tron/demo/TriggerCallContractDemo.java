package org.tron.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.utils.AbiUtil;
import org.tron.common.utils.ByteArray;
import org.tron.walletserver.TronVegasApi;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TriggerCallContractDemo {
    private static final Logger logger = LoggerFactory.getLogger("TriggerCallContractDemo");

    public static boolean isFinished = false;
    public static AtomicInteger successCount = new AtomicInteger(0);
    public static AtomicInteger count = new AtomicInteger(0);

    public static final int TOTAL_COUNT = 10000;

    public static void main(String[] args) throws Exception {
        final String privateKey = "3B79F0B570C0669617DB9B627027DCAD46758834B47691FEDA62A4C0FB85E586";
        final String toAddress = "TSZcWmjrgfNpEjWC8yVVqTwHcjBttp16di";
        final String contractAddress = "41fab4a0696766af4b9f989fabf0505b306109406c";


        TronVegasApi.initWithPrivateKey(privateKey);

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1000);

        long time1 = System.currentTimeMillis();
        for (int i = 1; i <= TOTAL_COUNT; i++) {
            final int amount = i;
            fixedThreadPool.execute(() -> {

                try {
                    long time = System.currentTimeMillis();
                    Function function = new Function("testTransfer",
                            Arrays.asList(
                                    new Address(Numeric.toHexString(TronVegasApi.decodeFromBase58Check(toAddress))),
                                    new Uint256(BigInteger.valueOf(amount))),
                            Collections.<TypeReference<?>>emptyList());
                    boolean result = TronVegasApi.triggerContract(
                            ByteArray.fromHexString(contractAddress),
                            0L,
                                    Numeric.hexStringToByteArray(FunctionEncoder.encode(function)),
                            100000000L,
                            0,
                            null
                    );
                    if (result) {
                        successCount.incrementAndGet();
                    }
                    System.out.println("Process Time: " + (System.currentTimeMillis() - time));
                } catch (Exception ex) {
                    logger.error("ERROR:" + ex.getMessage());
                }
                count.incrementAndGet();
            });
        }

        while (!isFinished) {
            if (count.get() >= TOTAL_COUNT) {
                isFinished = true;
            }
            Thread.sleep(1);
        }
        long totalTime = System.currentTimeMillis() - time1;


        fixedThreadPool.shutdown();

        logger.info("Total Count:" + count.get());
        logger.info("Success Count:" + successCount.get());
        logger.info("Total Time: " + totalTime + "ms");
        logger.info("TPS: " + (TOTAL_COUNT * 1000 / totalTime));

    }
}
