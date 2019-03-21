package org.tron.walletserver;

import org.tron.api.GrpcAPI;
import org.tron.common.crypto.Sha256Hash;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;

public class TronVegasUtils {

    public static String getTxid(GrpcAPI.TransactionExtention transactionExtention){
        if(transactionExtention == null){
            return "";
        }
        return ByteArray.toHexString(Sha256Hash.hash(transactionExtention.getTransaction().getRawData().toByteArray()));
    }

    public static String getTxid(Protocol.Transaction transaction){
        if(transaction == null){
            return "";
        }
        return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    }

    public static String getBlockId(Protocol.Block block){
        if(block == null){
            return "";
        }
        BlockId blockId = new BlockId(Sha256Hash.of(block.getBlockHeader().getRawData().toByteArray()),
                block.getBlockHeader().getRawData().getNumber());
        return ByteArray.toHexString(blockId.getBytes());
    }

}
