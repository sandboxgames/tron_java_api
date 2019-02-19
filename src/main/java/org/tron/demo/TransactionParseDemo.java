package org.tron.demo;

import org.tron.walletserver.ContractUtils;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.ArrayList;
import java.util.List;


public class TransactionParseDemo {


    public static void main(String[] args) throws Exception {

        // testTransfer(address,uint256)

        String data1 = "a0e70267000000000000000000000041b603c845f91f3f928599e2b9b72292f5be1533690000000000000000000000000000000000000000000000000000000000000001";
        System.out.println("Data: " + data1);

        List<TypeReference<Type>> inputParameters = new ArrayList<>(1);
        inputParameters.add((TypeReference) new TypeReference<Address>() {});
        inputParameters.add((TypeReference) new TypeReference<Uint256>() {});

        System.out.println("MethodId: " + ContractUtils.getMethodId("testTransfer", inputParameters));

        List<Type> paramList = ContractUtils.decode(data1, inputParameters);
        for(Type t : paramList){
            System.out.println(t.getTypeAsString() + ":" + t.getValue());
        }

        System.out.println(" ");

        // rouletteVegas(bytes bets)
        String data2 = "80009a27000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000115b5b323030322c31303030303030305d5d000000000000000000000000000000";
        System.out.println("Data: " + data2);

        List<TypeReference<Type>> inputParameters2 = new ArrayList<>(1);
        inputParameters2.add((TypeReference) new TypeReference<DynamicBytes>() {});

        System.out.println("MethodId: " + ContractUtils.getMethodId("rouletteVegas", inputParameters2, true));

        List<Type> paramList2 = ContractUtils.decode(data2, inputParameters2);
        for(Type t : paramList2){
            System.out.println(t.getTypeAsString() + ":" + new String((byte[]) t.getValue()));
        }

    }

}
