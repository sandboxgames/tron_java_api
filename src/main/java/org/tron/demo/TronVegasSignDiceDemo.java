package org.tron.demo;

import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.walletserver.TronVegasApi;

public class TronVegasSignDiceDemo {

    public static void main(String[] args) throws Exception {

        final String privateKey = "3B79F0B570C0669617DB9B627027DCAD46758834B47691FEDA62A4C0FB85E586";
        final String pubKey = "TZHspmV9EtRiXKmyoReYPFRNQbf2ekoUCX";

        TronVegasApi.initWithPrivateKey(privateKey);

        String message = "Hello World";

        String signDataHex = ByteArray.toHexString(TronVegasApi.signByte(message.getBytes()));

        System.out.println("签名公钥：" + pubKey);
        System.out.println("源数据：" + message);
        System.out.println("源数据Hex：" + ByteArray.toHexString(message.getBytes()));
        System.out.println("源数据Hash：" + Hash.sha3(ByteArray.toHexString(message.getBytes())));

        System.out.println("签名数据：" + signDataHex);
        System.out.println("签名数据Hash：" + Hash.sha3(signDataHex));

        System.out.println(" ");

        boolean match = TronVegasApi.verifySign(pubKey, message.getBytes(), signDataHex);

        System.out.println("验证签名结果：" + match);

        System.out.println(" ");


        System.out.println("客户端签名验证");

        String clientSignDataHex = "0x0ac38a85b1921800e3735b73bd3059e9bec162997bc766ea81114a9d2baf16981d0576c0ac369ca1045664e3113c5a5282d6a00ef1cb011b53e74e76f736f9281b";

        boolean clientVerifyResult = TronVegasApi.verifySign(pubKey, message.getBytes(), clientSignDataHex);
        System.out.println("验证签名结果：" + clientVerifyResult);
    }

}
