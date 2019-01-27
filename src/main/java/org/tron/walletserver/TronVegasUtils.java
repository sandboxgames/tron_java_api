package org.tron.walletserver;

import org.tron.api.GrpcAPI;
import org.tron.common.crypto.Sha256Hash;
import org.tron.common.utils.ByteArray;

public class TronVegasUtils {

    public static String getTxid(GrpcAPI.TransactionExtention transactionExtention){
        if(transactionExtention == null){
            return "";
        }
        return ByteArray.toHexString(Sha256Hash.hash(transactionExtention.getTransaction().getRawData().toByteArray()));
    }

}
