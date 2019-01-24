package org.tron.demo;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.TronVegasApi;
import org.web3j.utils.Numeric;

import java.util.List;

import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TriggerSmartContract;

public class GetBlock2ScanDemo {

    private static final Logger logger = LoggerFactory.getLogger("GetBlock2ScanDemo");

    private static boolean isFinished = false;

    public static void main(String[] args) throws Exception {

        final String privateKey = "";
        TronVegasApi.initWithPrivateKey(privateKey);

        ByteString contractAddress = ByteString.copyFrom(Numeric.hexStringToByteArray("0x41fab4a0696766af4b9f989fabf0505b306109406c"));


        while (!isFinished){
            GrpcAPI.BlockExtention block = TronVegasApi.getBlock2Safe(-1);
            if(block != null){
                logger.info("Block:" + block.getBlockHeader().getRawData().getNumber());
            }

            List<GrpcAPI.TransactionExtention> transactionsList = block.getTransactionsList();
            if(transactionsList == null || transactionsList.size() <= 0){
//                logger.info("No Transactions");
                continue;
            }

            transactionsList.stream().forEach(transaction -> {
                if(transaction == null || (transaction.getTransaction().getRetCount() > 0 && transaction.getTransaction().getRet(0).getContractRet() != Protocol.Transaction.Result.contractResult.SUCCESS)){
                    logger.info("Transaction is fail");
                    return;
                }
                transaction.getTransaction().getRawData().getContractList().stream().forEach(contract -> {
                    try{
                        Any contractParameter = contract.getParameter();
                        if(TriggerSmartContract == contract.getType()){
                            Contract.TriggerSmartContract triggerSmartContract = contractParameter
                                    .unpack(Contract.TriggerSmartContract.class);
                            if(contractAddress.equals(triggerSmartContract.getContractAddress())){
                                logger.info("Get Transaction " + System.currentTimeMillis());
                                logger.info(transaction.toString());
                                logger.info("" + transaction.getTransaction().getRet(0).getContractRet().toString());
                                isFinished = true;
                            }
                        }
                    }catch (Exception ex){
                        logger.error("Contract ERROR", ex);
                    }
                });
            });

            Thread.sleep(500);
        }

    }
}
