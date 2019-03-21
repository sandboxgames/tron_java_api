package org.tron.demo;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.walletserver.TronVegasApi;
import org.web3j.utils.Numeric;

import java.util.List;

import static org.tron.protos.Protocol.Transaction.Contract.ContractType.TriggerSmartContract;

public class GetBlockScanDemo {

    private static final Logger logger = LoggerFactory.getLogger("GetBlockScanDemo");

    private static boolean isFinished = false;

    public static void main(String[] args) throws Exception {


        // 以下为测试调试
        final String privateKey = "";
        TronVegasApi.initWithPrivateKey(privateKey);

        ByteString contractAddress = ByteString.copyFrom(Numeric.hexStringToByteArray("0x41fab4a0696766af4b9f989fabf0505b306109406c"));

        while (!isFinished){

            Protocol.Block block = TronVegasApi.getBlockSafe(-1);
            if(block != null){
                logger.info("Block:" + block.getBlockHeader().getRawData().getNumber());
            }

            List<Protocol.Transaction> transactionsList = block.getTransactionsList();
            if(transactionsList == null || transactionsList.size() <= 0){
                logger.info("No Transactions");
                continue;
            }

            transactionsList.stream().forEach(transaction -> {
                if(transaction == null || (transaction.getRetCount() > 0 && transaction.getRet(0).getRet() != Protocol.Transaction.Result.code.SUCESS)){
                    logger.info("Transaction is revert");
                    return;
                }
                transaction.getRawData().getContractList().stream().forEach(contract -> {
                    try{
                        Any contractParameter = contract.getParameter();
                        if(TriggerSmartContract == contract.getType()){
                            Contract.TriggerSmartContract triggerSmartContract = contractParameter
                                    .unpack(Contract.TriggerSmartContract.class);
//                                logger.info(Numeric.toHexString(triggerSmartContract.getContractAddress().toByteArray()));
                            if(contractAddress.equals(triggerSmartContract.getContractAddress())){
                                logger.info("Get Transaction " + System.currentTimeMillis());
                                isFinished = true;
                            }
                        }else{

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
