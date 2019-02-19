package org.tron.walletserver;

import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.util.List;

public class ContractUtils {

    public static List<Type> decode(String data, List<TypeReference<Type>> parameters) {
        if (StringUtils.isBlank(data)) {
            return null;
        }
        return FunctionReturnDecoder.decode(data.substring(8), parameters);
    }

    public static String getMethodId(String method, List<TypeReference<Type>> inputParameters) {
        return getMethodId(method, inputParameters, false);
    }

    public static String getMethodId(String method, List<TypeReference<Type>> inputParameters, boolean hasHexPrefix) {
        String methodSignature = buildMethodSignature(method, inputParameters);
        System.out.println(methodSignature);
        return buildMethodId(methodSignature, hasHexPrefix);
    }

    private static String buildMethodSignature(String methodName, List<TypeReference<Type>> parameters) {
        StringBuilder result = new StringBuilder();
        result.append(methodName);
        result.append("(");
        try {
            int lastIndex = parameters.size() - 1;
            for (int index = 0; index <= lastIndex; index++) {
                result.append(getSolidityType(parameters.get(index)));
                if(index < lastIndex){
                    result.append(",");
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        result.append(")");
        return result.toString();
    }

    private static String getSolidityType(TypeReference<? extends Type> typeReference) throws Exception {
        Class<?> clz = typeReference.getClassType();

        if(DynamicBytes.class.isAssignableFrom(clz)){
            return "bytes";
        } if(Utf8String.class.isAssignableFrom(clz)){
            return "string";
        }

        return clz.getSimpleName().toLowerCase();
    }

    private static String buildMethodId(String methodSignature, boolean hasHexPrefix) {
        byte[] input = methodSignature.getBytes();
        byte[] hash = Hash.sha3(input);
        return hasHexPrefix ? Numeric.toHexString(hash).substring(0, 10) : Numeric.toHexString(hash).substring(2, 10);
    }


}
