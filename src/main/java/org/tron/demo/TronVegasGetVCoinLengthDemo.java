package org.tron.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.walletserver.TronVegasApi;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class TronVegasGetVCoinLengthDemo {
    private static final Logger logger = LoggerFactory.getLogger("TronVegasGetVCoinLengthDemo");

    public static void main(String[] args) throws Exception {
        final String privateKey = "";

        final String contractAddress = "4117178f94e8f24698ed59d54678b04966856381cb";

        TronVegasApi.initWithPrivateKey(privateKey);

        int start = 1;
        int end = 100;

        Function function = new Function("getVCoinStakerArray",
                Arrays.asList(
                        new Uint256(BigInteger.valueOf(start)),
                        new Uint256(BigInteger.valueOf(end))),
                Collections.singletonList(new TypeReference<Uint256>(){}));

        GrpcAPI.TransactionExtention transaction = TronVegasApi.triggerContractForTxid(
                ByteArray.fromHexString(contractAddress),
                0L,
                Numeric.hexStringToByteArray(FunctionEncoder.encode(function)),
                100000000L,
                0,
                null
        );

        if(transaction != null && transaction.getConstantResultList().size() > 0){
            List<Type> result = FunctionReturnDecoder.decode(
                    Numeric.toHexString(transaction.getConstantResult(0).toByteArray()),
                    function.getOutputParameters());
            if(result.size() > 0){
                Uint256 val = (Uint256)result.get(0).getValue();
                System.out.println("Return: " + val.getValue());
            }
        }


    }
}
