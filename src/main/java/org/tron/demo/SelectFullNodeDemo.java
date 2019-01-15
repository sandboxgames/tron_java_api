package org.tron.demo;

import org.tron.api.GrpcAPI;
import org.tron.walletserver.GrpcClient;
import org.tron.walletserver.TronVegasApi;
import org.tron.walletserver.WalletApi;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SelectFullNodeDemo {

    public static void main(String[] args) throws Exception {
        final String privateKey = "3B79F0B570C0669617DB9B627027DCAD46758834B47691FEDA62A4C0FB85E586";

        Map<String, Long> fullnodeList = new ConcurrentHashMap<>();

        TronVegasApi.initWithPrivateKey(privateKey);

        Optional<GrpcAPI.NodeList> opNodeList = WalletApi.listNodes();
        System.out.println("Getting Node List");
        if(opNodeList.isPresent()){
            GrpcAPI.NodeList nodeList = opNodeList.get();
            for(int index = 0; index < nodeList.getNodesCount(); index++){
                GrpcAPI.Node node = nodeList.getNodes(index);
                System.out.println("Node" + index + ":" + node.getAddress().toString());
                String host = node.getAddress().getHost().toStringUtf8() + ":50051";
                GrpcClient c = new GrpcClient(host, "");

                long time = System.currentTimeMillis();
                try{
                    GrpcAPI.BlockList blocks = c.getBlockByLatestNum(1).get();
                    System.out.println(blocks.toString());
                }catch (Exception ex){
                    System.out.println(ex.getMessage());
                }
                fullnodeList.put(host, System.currentTimeMillis() - time);
            }
        }

        for (Map.Entry<String, Long> entry : fullnodeList.entrySet()) {
            System.out.println("K: " + entry.getKey() + " Time:" + entry.getValue());
        }


//        Optional<GrpcAPI.WitnessList> opNodeList = WalletApi.listWitnesses();
//        System.out.println("Getting Node List");
//        if(opNodeList.isPresent()){
//            GrpcAPI.WitnessList nodeList = opNodeList.get();
//            for(int index = 0; index < nodeList.getWitnessesCount(); index++){
//                Protocol.Witness node = nodeList.getWitnesses(index);
//                System.out.println("Node" + index + ":" + node.toString());
//            }
//        }
    }
}
