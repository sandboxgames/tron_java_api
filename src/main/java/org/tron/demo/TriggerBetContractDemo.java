package org.tron.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.common.crypto.Sha256Hash;
import org.tron.common.utils.ByteArray;
import org.tron.walletserver.TronVegasApi;
import org.tron.walletserver.TronVegasUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

public class TriggerBetContractDemo {
    private static final Logger logger = LoggerFactory.getLogger("TriggerCallContractDemo");

    public static boolean isFinished = false;

    public static void main(String[] args) throws Exception {
        final String privateKey = "";

        final String contractAddress = "4117178f94e8f24698ed59d54678b04966856381cb";


        TronVegasApi.initWithPrivateKey(privateKey);
        Function function = new Function("luckVegas",
                Arrays.asList(
                        new Uint256(BigInteger.valueOf(1))),
                Collections.<TypeReference<?>>emptyList());
        GrpcAPI.TransactionExtention result = TronVegasApi.triggerContractForTxid(
                ByteArray.fromHexString(contractAddress),
                0L,
                Numeric.hexStringToByteArray(FunctionEncoder.encode(function)),
                100000000L,
                0,
                null
        );
        System.out.println(TronVegasUtils.getTxid(result));

    }
}
