package org.tron.demo;

import com.google.protobuf.ByteString;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Sha256Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.TransactionUtils;
import org.tron.walletserver.TronVegasApi;

import java.util.Arrays;

public class TronVegasSignDiceDemo {

    public static void main(String[] args) throws Exception {

        final String privateKey = "3B79F0B570C0669617DB9B627027DCAD46758834B47691FEDA62A4C0FB85E586";
        final String pubKey = "TZHspmV9EtRiXKmyoReYPFRNQbf2ekoUCX";

        TronVegasApi.initWithPrivateKey(privateKey);

        byte[] srcData = "Hello World".getBytes();

        byte[] signData = TronVegasApi.signByte(srcData);

        System.out.println("签名公钥：" + pubKey);
        System.out.println("源数据：" + new String(srcData));
        System.out.println("源数据Hash：" + ByteArray.toHexString(Sha256Hash.hash(srcData)));
        System.out.println("签名数据：" + ByteArray.toHexString(signData));
        System.out.println("签名数据Hash：" + ByteArray.toHexString(Sha256Hash.hash(signData)));

        System.out.println(" ");

        byte[] address = ECKey.signatureToAddress(Sha256Hash.hash(srcData), TransactionUtils.getBase64FromByteString(ByteString.copyFrom(signData)));
        System.out.println("验证签名地址：" + TronVegasApi.encode58Check(address));
        System.out.println("验证签名结果：" + Arrays.equals(TronVegasApi.decodeFromBase58Check(pubKey), address));
    }
}
