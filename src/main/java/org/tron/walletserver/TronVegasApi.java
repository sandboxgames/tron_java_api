package org.tron.walletserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.AddressPrKeyPairMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.BlockListExtention;
import org.tron.api.GrpcAPI.DelegatedResourceList;
import org.tron.api.GrpcAPI.EasyTransferResponse;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.TransactionListExtention;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.Sha256Hash;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.TransactionUtils;
import org.tron.core.config.Configuration;
import org.tron.core.config.Parameter.CommonConstant;
import org.tron.core.exception.CancelException;
import org.tron.core.exception.CipherException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.BuyStorageBytesContract;
import org.tron.protos.Contract.BuyStorageContract;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.SellStorageContract;
import org.tron.protos.Contract.UnfreezeAssetContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Contract.UpdateEnergyLimitContract;
import org.tron.protos.Contract.UpdateSettingContract;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.Proposal;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionSign;
import org.tron.protos.Protocol.Witness;


public class TronVegasApi {

    private static final Logger logger = LoggerFactory.getLogger("TronVegasApi");
    private static byte addressPreFixByte = CommonConstant.ADD_PRE_FIX_BYTE_TESTNET;
    private static int rpcVersion = 0;

    public static final String TRX_MESSAGE_HEADER = "\u0019TRON Signed Message:\n32";

    private static ECKey ecKey = null;

    public static boolean isSupportConstant = false;
    public static boolean isDebug = false;
    public static boolean isForceUseTrongrid = false;
    public static boolean isForceUseLocalIPS = false;

    public static void initWithPrivateKey(String privateKey) {
        ecKey = ECKey.fromPrivate(ByteArray.fromHexString(privateKey));
        init();
    }

    public static void init() {
        Config config = Configuration.getByPath("config.conf");

        String fullNode = "";
        String solidityNode = "";
        if (config.hasPath("soliditynode.ip.list")) {
            solidityNode = config.getStringList("soliditynode.ip.list").get(0);
        }
        if (config.hasPath("fullnode.ip.list")) {
            fullNode = config.getStringList("fullnode.ip.list").get(0);
        }
        if (config.hasPath("net.type") && "mainnet".equalsIgnoreCase(config.getString("net.type"))) {
            TronVegasApi.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
        } else {
            TronVegasApi.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_TESTNET);
        }
        if (config.hasPath("RPC_version")) {
            rpcVersion = config.getInt("RPC_version");
        }

        int maxNodeLimit = 0;
        if (config.hasPath("MAX_NODE_LIMIT")){
            maxNodeLimit = config.getInt("MAX_NODE_LIMIT");
        }
        if (config.hasPath("SUPPORT_CONSTANT")){
            isSupportConstant = config.getInt("SUPPORT_CONSTANT") > 0;
        }

        List<String> seedNodeList = null;
        if(config.hasPath("seed.node.ip.list")){
            seedNodeList = config.getStringList("seed.node.ip.list");
        }

        if (config.hasPath("FORCE_USE_TRONGRID")){
            isForceUseTrongrid = config.getInt("FORCE_USE_TRONGRID") > 0;
        }

        if (config.hasPath("FORCE_USE_LOCALIPS")){
            isForceUseLocalIPS = config.getInt("FORCE_USE_LOCALIPS") > 0;
        }
        TronVegasGrpcClientPool.getInstance().init(fullNode, solidityNode, maxNodeLimit, seedNodeList);
    }

    public static void queryFastestNodes(){
        TronVegasGrpcClientPool.getInstance().queryFastestNodes(null, false);
    }

    public static byte getAddressPreFixByte() {
        return addressPreFixByte;
    }

    public static void setAddressPreFixByte(byte addressPreFixByte) {
        TronVegasApi.addressPreFixByte = addressPreFixByte;
    }

    public static int getRpcVersion() {
        return rpcVersion;
    }


    public static boolean verifySign(String publicKey, byte data[], String signDataHex){
        boolean result = false;
        try{
            byte[] messageHash = Hash.sha3(getTRONMessageHash(data));
            byte[] address = ECKey.signatureToAddress(messageHash, TransactionUtils.getBase64FromByteString(ByteString.copyFrom(ByteArray.fromHexString(signDataHex))));
            result = Arrays.equals(TronVegasApi.decodeFromBase58Check(publicKey), address);
        }catch (Exception ex){
            logger.error("verifySign", ex);
        }
        return result;
    }

    public static byte[] signByte(byte[] data){
        return signByteWithECKey(data, ecKey);
    }

    public static byte[] signByte(byte[] data, String privateKey){
        ECKey key = ECKey.fromPrivate(ByteArray.fromHexString(privateKey));
        return signByteWithECKey(data, key);
    }

    private static byte[] signByteWithECKey(byte[] data, ECKey key){
        byte[] hash = Hash.sha3(getTRONMessageHash(data));
        return key.sign(hash).toByteArray();
    }

    private static byte[] getTRONMessageHash(byte[] message) {
        byte[] prefix = TRX_MESSAGE_HEADER.getBytes();

        byte[] result = new byte[prefix.length + message.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(message, 0, result, prefix.length, message.length);

        return result;
    }

    public static Account queryAccount(byte[] address) {
        return TronVegasGrpcClientPool.getInstance().borrow().queryAccount(address);//call rpc
    }

    public static Account queryAccountById(String accountId) {
        return TronVegasGrpcClientPool.getInstance().borrow().queryAccountById(accountId);
    }

    //Warning: do not invoke this interface provided by others.
    public static Transaction signTransactionByApi(Transaction transaction, byte[] privateKey) {
        TransactionSign.Builder builder = TransactionSign.newBuilder();
        builder.setPrivateKey(ByteString.copyFrom(privateKey));
        builder.setTransaction(transaction);
        return TronVegasGrpcClientPool.getInstance().borrow().signTransaction(builder.build());
    }

    //Warning: do not invoke this interface provided by others.
    public static TransactionExtention signTransactionByApi2(Transaction transaction,
                                                             byte[] privateKey) {
        TransactionSign.Builder builder = TransactionSign.newBuilder();
        builder.setPrivateKey(ByteString.copyFrom(privateKey));
        builder.setTransaction(transaction);
        return TronVegasGrpcClientPool.getInstance().borrow().signTransaction2(builder.build());
    }

    //Warning: do not invoke this interface provided by others.
    public static byte[] createAdresss(byte[] passPhrase) {
        return TronVegasGrpcClientPool.getInstance().borrow().createAdresss(passPhrase);
    }

    //Warning: do not invoke this interface provided by others.
    public static EasyTransferResponse easyTransfer(byte[] passPhrase, byte[] toAddress,
                                                    long amount) {
        return TronVegasGrpcClientPool.getInstance().borrow().easyTransfer(passPhrase, toAddress, amount);
    }

    //Warning: do not invoke this interface provided by others.
    public static EasyTransferResponse easyTransferByPrivate(byte[] privateKey, byte[] toAddress,
                                                             long amount) {
        return TronVegasGrpcClientPool.getInstance().borrow().easyTransferByPrivate(privateKey, toAddress, amount);
    }

    public static boolean broadcastTransaction(byte[] transactionBytes)
            throws InvalidProtocolBufferException {
        Transaction transaction = Transaction.parseFrom(transactionBytes);
        return TransactionUtils.validTransaction(transaction)
                && TronVegasGrpcClientPool.getInstance().borrow().broadcastTransaction(transaction);
    }

    //Warning: do not invoke this interface provided by others.
    public static AddressPrKeyPairMessage generateAddress() {
        EmptyMessage.Builder builder = EmptyMessage.newBuilder();
        return TronVegasGrpcClientPool.getInstance().borrow().generateAddress(builder.build());
    }

    public static Block getBlockSafe(long blockNum){
        Block block = null;

        TronVegasNodeInfo node = TronVegasGrpcClientPool.getInstance().get(UUID.randomUUID().toString());
        GrpcClient client = null;

        if(node != null && node.getClient() != null){
            client = node.getClient();
        }else {
            client = TronVegasGrpcClientPool.getInstance().borrow();
        }

        try{
            block = client.getBlock(blockNum);
        }catch (Exception ex){
            if(node != null){
                logger.error("Error Node IP: " + node.getHost());
            }
            logger.error("getBlock ERROR", ex);
            if(node != null && node.incErrorCount() > TronVegasNodeInfo.DEFAULT_NODE_MAX_ERROR_COUNT){
                TronVegasGrpcClientPool.getInstance().remove(node);
            }
        }
        return block;
    }

    public static BlockExtention getBlock2Safe(long blockNum){
        BlockExtention block = null;

        TronVegasNodeInfo node = TronVegasGrpcClientPool.getInstance().get(UUID.randomUUID().toString());
        GrpcClient client = null;

        if(node != null && node.getClient() != null){
            client = node.getClient();
        }else {
            client = TronVegasGrpcClientPool.getInstance().borrow();
        }

        try{
            block = client.getBlock2(blockNum);
        }catch (Exception ex){
            if(node != null){
                logger.error("Error Node IP: " + node.getHost());
            }
            logger.error("getBlock2Safe ERROR", ex);
            if(node != null && node.incErrorCount() > TronVegasNodeInfo.DEFAULT_NODE_MAX_ERROR_COUNT){
                TronVegasGrpcClientPool.getInstance().remove(node);
            }
        }
        return block;
    }

    public static Block getBlock(long blockNum) {
        return TronVegasGrpcClientPool.getInstance().borrow().getBlock(blockNum);
    }

    public static BlockExtention getBlock2(long blockNum) {
        return TronVegasGrpcClientPool.getInstance().borrow().getBlock2(blockNum);
    }

    public static long getTransactionCountByBlockNum(long blockNum) {
        return TronVegasGrpcClientPool.getInstance().borrow().getTransactionCountByBlockNum(blockNum);
    }

    public static Contract.TransferContract createTransferContract(byte[] to, byte[] owner,
                                                                   long amount) {
        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Contract.TransferAssetContract createTransferAssetContract(byte[] to,
                                                                             byte[] assertName, byte[] owner,
                                                                             long amount) {
        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Contract.ParticipateAssetIssueContract participateAssetIssueContract(byte[] to,
                                                                                       byte[] assertName, byte[] owner,
                                                                                       long amount) {
        Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
                .newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Contract.AccountUpdateContract createAccountUpdateContract(byte[] accountName,
                                                                             byte[] address) {
        Contract.AccountUpdateContract.Builder builder = Contract.AccountUpdateContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        ByteString bsAccountName = ByteString.copyFrom(accountName);
        builder.setAccountName(bsAccountName);
        builder.setOwnerAddress(basAddreess);

        return builder.build();
    }

    public static Contract.SetAccountIdContract createSetAccountIdContract(byte[] accountId,
                                                                           byte[] address) {
        Contract.SetAccountIdContract.Builder builder = Contract.SetAccountIdContract.newBuilder();
        ByteString bsAddress = ByteString.copyFrom(address);
        ByteString bsAccountId = ByteString.copyFrom(accountId);
        builder.setAccountId(bsAccountId);
        builder.setOwnerAddress(bsAddress);

        return builder.build();
    }

    public static Contract.UpdateAssetContract createUpdateAssetContract(
            byte[] address,
            byte[] description,
            byte[] url,
            long newLimit,
            long newPublicLimit
    ) {
        Contract.UpdateAssetContract.Builder builder =
                Contract.UpdateAssetContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        builder.setDescription(ByteString.copyFrom(description));
        builder.setUrl(ByteString.copyFrom(url));
        builder.setNewLimit(newLimit);
        builder.setNewPublicLimit(newPublicLimit);
        builder.setOwnerAddress(basAddreess);

        return builder.build();
    }

    public static Contract.AccountCreateContract createAccountCreateContract(byte[] owner,
                                                                             byte[] address) {
        Contract.AccountCreateContract.Builder builder = Contract.AccountCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setAccountAddress(ByteString.copyFrom(address));

        return builder.build();
    }

    public static Contract.WitnessCreateContract createWitnessCreateContract(byte[] owner,
                                                                             byte[] url) {
        Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setUrl(ByteString.copyFrom(url));

        return builder.build();
    }

    public static Contract.WitnessUpdateContract createWitnessUpdateContract(byte[] owner,
                                                                             byte[] url) {
        Contract.WitnessUpdateContract.Builder builder = Contract.WitnessUpdateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setUpdateUrl(ByteString.copyFrom(url));

        return builder.build();
    }

    public static Contract.VoteWitnessContract createVoteWitnessContract(byte[] owner,
                                                                         HashMap<String, String> witness) {
        Contract.VoteWitnessContract.Builder builder = Contract.VoteWitnessContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        for (String addressBase58 : witness.keySet()) {
            String value = witness.get(addressBase58);
            long count = Long.parseLong(value);
            Contract.VoteWitnessContract.Vote.Builder voteBuilder = Contract.VoteWitnessContract.Vote
                    .newBuilder();
            byte[] address = TronVegasApi.decodeFromBase58Check(addressBase58);
            if (address == null) {
                continue;
            }
            voteBuilder.setVoteAddress(ByteString.copyFrom(address));
            voteBuilder.setVoteCount(count);
            builder.addVotes(voteBuilder.build());
        }

        return builder.build();
    }

    public static boolean addressValid(byte[] address) {
        if (ArrayUtils.isEmpty(address)) {
            logger.warn("Warning: Address is empty !!");
            return false;
        }
        if (address.length != CommonConstant.ADDRESS_SIZE) {
            logger.warn(
                    "Warning: Address length need " + CommonConstant.ADDRESS_SIZE + " but " + address.length
                            + " !!");
            return false;
        }
        byte preFixbyte = address[0];
        if (preFixbyte != TronVegasApi.getAddressPreFixByte()) {
            logger
                    .warn("Warning: Address need prefix with " + TronVegasApi.getAddressPreFixByte() + " but "
                            + preFixbyte + " !!");
            return false;
        }
        //Other rule;
        return true;
    }

    public static String encode58Check(byte[] input) {
        byte[] hash0 = Sha256Hash.hash(input);
        byte[] hash1 = Sha256Hash.hash(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }

    private static byte[] decode58Check(String input) {
        byte[] decodeCheck = Base58.decode(input);
        if (decodeCheck.length <= 4) {
            return null;
        }
        byte[] decodeData = new byte[decodeCheck.length - 4];
        System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
        byte[] hash0 = Sha256Hash.hash(decodeData);
        byte[] hash1 = Sha256Hash.hash(hash0);
        if (hash1[0] == decodeCheck[decodeData.length] &&
                hash1[1] == decodeCheck[decodeData.length + 1] &&
                hash1[2] == decodeCheck[decodeData.length + 2] &&
                hash1[3] == decodeCheck[decodeData.length + 3]) {
            return decodeData;
        }
        return null;
    }

    public static byte[] decodeFromBase58Check(String addressBase58) {
        if (StringUtils.isEmpty(addressBase58)) {
            logger.warn("Warning: Address is empty !!");
            return null;
        }
        byte[] address = decode58Check(addressBase58);
        if (!addressValid(address)) {
            return null;
        }
        return address;
    }

    public static boolean priKeyValid(byte[] priKey) {
        if (ArrayUtils.isEmpty(priKey)) {
            logger.warn("Warning: PrivateKey is empty !!");
            return false;
        }
        if (priKey.length != 32) {
            logger.warn("Warning: PrivateKey length need 64 but " + priKey.length + " !!");
            return false;
        }
        //Other rule;
        return true;
    }

    public static Optional<WitnessList> listWitnesses() {
        Optional<WitnessList> result = TronVegasGrpcClientPool.getInstance().borrow().listWitnesses();
        if (result.isPresent()) {
            WitnessList witnessList = result.get();
            List<Witness> list = witnessList.getWitnessesList();
            List<Witness> newList = new ArrayList<>();
            newList.addAll(list);
            newList.sort(new Comparator<Witness>() {
                @Override
                public int compare(Witness o1, Witness o2) {
                    return Long.compare(o2.getVoteCount(), o1.getVoteCount());
                }
            });
            WitnessList.Builder builder = WitnessList.newBuilder();
            newList.forEach(witness -> builder.addWitnesses(witness));
            result = Optional.of(builder.build());
        }
        return result;
    }

    public static Optional<AssetIssueList> getAssetIssueList() {
        return TronVegasGrpcClientPool.getInstance().borrow().getAssetIssueList();
    }

    public static Optional<AssetIssueList> getAssetIssueList(long offset, long limit) {
        return TronVegasGrpcClientPool.getInstance().borrow().getAssetIssueList(offset, limit);
    }

    public static Optional<ProposalList> getProposalListPaginated(long offset, long limit) {
        return TronVegasGrpcClientPool.getInstance().borrow().getProposalListPaginated(offset, limit);
    }

    public static Optional<ExchangeList> getExchangeListPaginated(long offset, long limit) {
        return TronVegasGrpcClientPool.getInstance().borrow().getExchangeListPaginated(offset, limit);
    }

    public static Optional<NodeList> listNodes() {
        return TronVegasGrpcClientPool.getInstance().borrow().listNodes();
    }

    public static Optional<NodeList> listNodesByDefault() {
        return TronVegasGrpcClientPool.getInstance().getDefaultClient().listNodes();
    }

    public static Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
        return TronVegasGrpcClientPool.getInstance().borrow().getAssetIssueByAccount(address);
    }

    public static AccountNetMessage getAccountNet(byte[] address) {
        return TronVegasGrpcClientPool.getInstance().borrow().getAccountNet(address);
    }

    public static AccountResourceMessage getAccountResource(byte[] address) {
        return TronVegasGrpcClientPool.getInstance().borrow().getAccountResource(address);
    }

    public static AssetIssueContract getAssetIssueByName(String assetName) {
        return TronVegasGrpcClientPool.getInstance().borrow().getAssetIssueByName(assetName);
    }

    public static Optional<AssetIssueList> getAssetIssueListByName(String assetName) {
        return TronVegasGrpcClientPool.getInstance().borrow().getAssetIssueListByName(assetName);
    }

    public static AssetIssueContract getAssetIssueById(String assetId) {
        return TronVegasGrpcClientPool.getInstance().borrow().getAssetIssueById(assetId);
    }

    public static GrpcAPI.NumberMessage getTotalTransaction() {
        return TronVegasGrpcClientPool.getInstance().borrow().getTotalTransaction();
    }

    public static GrpcAPI.NumberMessage getNextMaintenanceTime() {
        return TronVegasGrpcClientPool.getInstance().borrow().getNextMaintenanceTime();
    }

    public static Optional<TransactionList> getTransactionsFromThis(byte[] address, int offset,
                                                                    int limit) {
        return TronVegasGrpcClientPool.getInstance().borrow().getTransactionsFromThis(address, offset, limit);
    }

    public static Optional<TransactionListExtention> getTransactionsFromThis2(byte[] address,
                                                                              int offset,
                                                                              int limit) {
        return TronVegasGrpcClientPool.getInstance().borrow().getTransactionsFromThis2(address, offset, limit);
    }

    public static Optional<TransactionList> getTransactionsToThis(byte[] address, int offset,
                                                                  int limit) {
        return TronVegasGrpcClientPool.getInstance().borrow().getTransactionsToThis(address, offset, limit);
    }

    public static Optional<TransactionListExtention> getTransactionsToThis2(byte[] address,
                                                                            int offset,
                                                                            int limit) {
        return TronVegasGrpcClientPool.getInstance().borrow().getTransactionsToThis2(address, offset, limit);
    }

    public static Optional<Transaction> getTransactionById(String txID) {
        return TronVegasGrpcClientPool.getInstance().borrow().getTransactionById(txID);
    }

    public static Optional<TransactionInfo> getTransactionInfoById(String txID) {
        return TronVegasGrpcClientPool.getInstance().borrow().getTransactionInfoById(txID);
    }

    public static Optional<Block> getBlockById(String blockID) {
        return TronVegasGrpcClientPool.getInstance().borrow().getBlockById(blockID);
    }

    public static Optional<BlockList> getBlockByLimitNext(long start, long end) {
        return TronVegasGrpcClientPool.getInstance().borrow().getBlockByLimitNext(start, end);
    }

    public static Optional<BlockListExtention> getBlockByLimitNext2(long start, long end) {
        return TronVegasGrpcClientPool.getInstance().borrow().getBlockByLimitNext2(start, end);
    }

    public static Optional<BlockList> getBlockByLatestNum(long num) {
        return TronVegasGrpcClientPool.getInstance().borrow().getBlockByLatestNum(num);
    }

    public static Optional<BlockListExtention> getBlockByLatestNum2(long num) {
        return TronVegasGrpcClientPool.getInstance().borrow().getBlockByLatestNum2(num);
    }

    public static Optional<ProposalList> listProposals() {
        return TronVegasGrpcClientPool.getInstance().borrow().listProposals();
    }

    public static Optional<Proposal> getProposal(String id) {
        return TronVegasGrpcClientPool.getInstance().borrow().getProposal(id);
    }

    public static Optional<DelegatedResourceList> getDelegatedResource(String fromAddress,
                                                                       String toAddress) {
        return TronVegasGrpcClientPool.getInstance().borrow().getDelegatedResource(fromAddress, toAddress);
    }

    public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndex(String address) {
        return TronVegasGrpcClientPool.getInstance().borrow().getDelegatedResourceAccountIndex(address);
    }

    public static Optional<ExchangeList> listExchanges() {
        return TronVegasGrpcClientPool.getInstance().borrow().listExchanges();
    }

    public static Optional<Exchange> getExchange(String id) {
        return TronVegasGrpcClientPool.getInstance().borrow().getExchange(id);
    }

    public static Optional<ChainParameters> getChainParameters() {
        return TronVegasGrpcClientPool.getInstance().borrow().getChainParameters();
    }

    public static Contract.ProposalCreateContract createProposalCreateContract(byte[] owner,
                                                                               HashMap<Long, Long> parametersMap) {
        Contract.ProposalCreateContract.Builder builder = Contract.ProposalCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.putAllParameters(parametersMap);
        return builder.build();
    }

    public static Contract.ProposalApproveContract createProposalApproveContract(byte[] owner,
                                                                                 long id, boolean is_add_approval) {
        Contract.ProposalApproveContract.Builder builder = Contract.ProposalApproveContract
                .newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setProposalId(id);
        builder.setIsAddApproval(is_add_approval);
        return builder.build();
    }

    public static Contract.ProposalDeleteContract createProposalDeleteContract(byte[] owner,
                                                                               long id) {
        Contract.ProposalDeleteContract.Builder builder = Contract.ProposalDeleteContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setProposalId(id);
        return builder.build();
    }

    public static Contract.ExchangeCreateContract createExchangeCreateContract(byte[] owner,
                                                                               byte[] firstTokenId, long firstTokenBalance,
                                                                               byte[] secondTokenId, long secondTokenBalance) {
        Contract.ExchangeCreateContract.Builder builder = Contract.ExchangeCreateContract.newBuilder();
        builder
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setFirstTokenId(ByteString.copyFrom(firstTokenId))
                .setFirstTokenBalance(firstTokenBalance)
                .setSecondTokenId(ByteString.copyFrom(secondTokenId))
                .setSecondTokenBalance(secondTokenBalance);
        return builder.build();
    }

    public static Contract.ExchangeInjectContract createExchangeInjectContract(byte[] owner,
                                                                               long exchangeId, byte[] tokenId, long quant) {
        Contract.ExchangeInjectContract.Builder builder = Contract.ExchangeInjectContract.newBuilder();
        builder
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setExchangeId(exchangeId)
                .setTokenId(ByteString.copyFrom(tokenId))
                .setQuant(quant);
        return builder.build();
    }

    public static Contract.ExchangeWithdrawContract createExchangeWithdrawContract(byte[] owner,
                                                                                   long exchangeId, byte[] tokenId, long quant) {
        Contract.ExchangeWithdrawContract.Builder builder = Contract.ExchangeWithdrawContract
                .newBuilder();
        builder
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setExchangeId(exchangeId)
                .setTokenId(ByteString.copyFrom(tokenId))
                .setQuant(quant);
        return builder.build();
    }

    public static Contract.ExchangeTransactionContract createExchangeTransactionContract(byte[] owner,
                                                                                         long exchangeId, byte[] tokenId, long quant, long expected) {
        Contract.ExchangeTransactionContract.Builder builder = Contract.ExchangeTransactionContract
                .newBuilder();
        builder
                .setOwnerAddress(ByteString.copyFrom(owner))
                .setExchangeId(exchangeId)
                .setTokenId(ByteString.copyFrom(tokenId))
                .setQuant(quant)
                .setExpected(expected);
        return builder.build();
    }

    public static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
        switch (type) {
            case "constructor":
                return SmartContract.ABI.Entry.EntryType.Constructor;
            case "function":
                return SmartContract.ABI.Entry.EntryType.Function;
            case "event":
                return SmartContract.ABI.Entry.EntryType.Event;
            case "fallback":
                return SmartContract.ABI.Entry.EntryType.Fallback;
            default:
                return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
        }
    }

    public static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
            String stateMutability) {
        switch (stateMutability) {
            case "pure":
                return SmartContract.ABI.Entry.StateMutabilityType.Pure;
            case "view":
                return SmartContract.ABI.Entry.StateMutabilityType.View;
            case "nonpayable":
                return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
            case "payable":
                return SmartContract.ABI.Entry.StateMutabilityType.Payable;
            default:
                return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
        }
    }

    public static SmartContract.ABI jsonStr2ABI(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
        JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
        SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
        for (int index = 0; index < jsonRoot.size(); index++) {
            JsonElement abiItem = jsonRoot.get(index);
            boolean anonymous = abiItem.getAsJsonObject().get("anonymous") != null ?
                    abiItem.getAsJsonObject().get("anonymous").getAsBoolean() : false;
            boolean constant = abiItem.getAsJsonObject().get("constant") != null ?
                    abiItem.getAsJsonObject().get("constant").getAsBoolean() : false;
            String name = abiItem.getAsJsonObject().get("name") != null ?
                    abiItem.getAsJsonObject().get("name").getAsString() : null;
            JsonArray inputs = abiItem.getAsJsonObject().get("inputs") != null ?
                    abiItem.getAsJsonObject().get("inputs").getAsJsonArray() : null;
            JsonArray outputs = abiItem.getAsJsonObject().get("outputs") != null ?
                    abiItem.getAsJsonObject().get("outputs").getAsJsonArray() : null;
            String type = abiItem.getAsJsonObject().get("type") != null ?
                    abiItem.getAsJsonObject().get("type").getAsString() : null;
            boolean payable = abiItem.getAsJsonObject().get("payable") != null ?
                    abiItem.getAsJsonObject().get("payable").getAsBoolean() : false;
            String stateMutability = abiItem.getAsJsonObject().get("stateMutability") != null ?
                    abiItem.getAsJsonObject().get("stateMutability").getAsString() : null;
            if (type == null) {
                logger.error("No type!");
                return null;
            }
            if (!type.equalsIgnoreCase("fallback") && null == inputs) {
                logger.error("No inputs!");
                return null;
            }

            SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
            entryBuilder.setAnonymous(anonymous);
            entryBuilder.setConstant(constant);
            if (name != null) {
                entryBuilder.setName(name);
            }

            /* { inputs : optional } since fallback function not requires inputs*/
            if (null != inputs) {
                for (int j = 0; j < inputs.size(); j++) {
                    JsonElement inputItem = inputs.get(j);
                    if (inputItem.getAsJsonObject().get("name") == null ||
                            inputItem.getAsJsonObject().get("type") == null) {
                        logger.error("Input argument invalid due to no name or no type!");
                        return null;
                    }
                    String inputName = inputItem.getAsJsonObject().get("name").getAsString();
                    String inputType = inputItem.getAsJsonObject().get("type").getAsString();
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
                            .newBuilder();
                    paramBuilder.setIndexed(false);
                    paramBuilder.setName(inputName);
                    paramBuilder.setType(inputType);
                    entryBuilder.addInputs(paramBuilder.build());
                }
            }

            /* { outputs : optional } */
            if (outputs != null) {
                for (int k = 0; k < outputs.size(); k++) {
                    JsonElement outputItem = outputs.get(k);
                    if (outputItem.getAsJsonObject().get("name") == null ||
                            outputItem.getAsJsonObject().get("type") == null) {
                        logger.error("Output argument invalid due to no name or no type!");
                        return null;
                    }
                    String outputName = outputItem.getAsJsonObject().get("name").getAsString();
                    String outputType = outputItem.getAsJsonObject().get("type").getAsString();
                    SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
                            .newBuilder();
                    paramBuilder.setIndexed(false);
                    paramBuilder.setName(outputName);
                    paramBuilder.setType(outputType);
                    entryBuilder.addOutputs(paramBuilder.build());
                }
            }

            entryBuilder.setType(getEntryType(type));
            entryBuilder.setPayable(payable);
            if (stateMutability != null) {
                entryBuilder.setStateMutability(getStateMutability(stateMutability));
            }

            abiBuilder.addEntrys(entryBuilder.build());
        }

        return abiBuilder.build();
    }

    public static Contract.UpdateSettingContract createUpdateSettingContract(byte[] owner,
                                                                             byte[] contractAddress, long consumeUserResourcePercent) {

        Contract.UpdateSettingContract.Builder builder = Contract.UpdateSettingContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
        return builder.build();
    }

    public static Contract.UpdateEnergyLimitContract createUpdateEnergyLimitContract(
            byte[] owner,
            byte[] contractAddress, long originEnergyLimit) {

        Contract.UpdateEnergyLimitContract.Builder builder = Contract.UpdateEnergyLimitContract
                .newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setOriginEnergyLimit(originEnergyLimit);
        return builder.build();
    }

    public static CreateSmartContract createContractDeployContract(String contractName,
                                                                   byte[] address,
                                                                   String ABI, String code, long value, long consumeUserResourcePercent, long originEnergyLimit, long tokenValue, String tokenId,
                                                                   String libraryAddressPair) {
        SmartContract.ABI abi = jsonStr2ABI(ABI);
        if (abi == null) {
            logger.error("abi is null");
            return null;
        }

        SmartContract.Builder builder = SmartContract.newBuilder();
        builder.setName(contractName);
        builder.setOriginAddress(ByteString.copyFrom(address));
        builder.setAbi(abi);
        builder.setConsumeUserResourcePercent(consumeUserResourcePercent)
                .setOriginEnergyLimit(originEnergyLimit);

        if (value != 0) {

            builder.setCallValue(value);
        }
        byte[] byteCode;
        if (null != libraryAddressPair) {
            byteCode = replaceLibraryAddress(code, libraryAddressPair);
        } else {
            byteCode = Hex.decode(code);
        }

        builder.setBytecode(ByteString.copyFrom(byteCode));
        CreateSmartContract.Builder createSmartContractBuilder = CreateSmartContract.newBuilder();
        createSmartContractBuilder.setOwnerAddress(ByteString.copyFrom(address)).
                setNewContract(builder.build());
        if (tokenId != null && !tokenId.equalsIgnoreCase("") && !tokenId.equalsIgnoreCase("#")) {
            createSmartContractBuilder.setCallTokenValue(tokenValue).setTokenId(Long.parseLong(tokenId));
        }
        return createSmartContractBuilder.build();
    }

    private static byte[] replaceLibraryAddress(String code, String libraryAddressPair) {

        String[] libraryAddressList = libraryAddressPair.split("[,]");

        for (int i = 0; i < libraryAddressList.length; i++) {
            String cur = libraryAddressList[i];

            int lastPosition = cur.lastIndexOf(":");
            if (-1 == lastPosition) {
                throw new RuntimeException("libraryAddress delimit by ':'");
            }
            String libraryName = cur.substring(0, lastPosition);
            String addr = cur.substring(lastPosition + 1);
            String libraryAddressHex;
            try {
                libraryAddressHex = (new String(Hex.encode(TronVegasApi.decodeFromBase58Check(addr)),
                        "US-ASCII")).substring(2);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);  // now ignore
            }
            String repeated = new String(new char[40 - libraryName.length() - 2]).replace("\0", "_");
            String beReplaced = "__" + libraryName + repeated;
            Matcher m = Pattern.compile(beReplaced).matcher(code);
            code = m.replaceAll(libraryAddressHex);
        }

        return Hex.decode(code);
    }

    public static Contract.TriggerSmartContract triggerCallContract(byte[] address,
                                                                    byte[] contractAddress,
                                                                    long callValue, byte[] data, long tokenValue, String tokenId) {
        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(address));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(data));
        builder.setCallValue(callValue);
        if (tokenId != null && tokenId != "") {
            builder.setCallTokenValue(tokenValue);
            builder.setTokenId(Long.parseLong(tokenId));
        }
        return builder.build();
    }

    public static SmartContract getContract(byte[] address) {
        return TronVegasGrpcClientPool.getInstance().borrow().getContract(address);
    }


    public static byte[] getAddress() {
        return ecKey.getAddress();
    }

    public static Account queryAccount() {
        return queryAccount(getAddress());
    }

    private static Transaction signTransaction(Transaction transaction) {
        if (transaction.getRawData().getTimestamp() == 0) {
            transaction = TransactionUtils.setTimestamp(transaction);
        }

        transaction = TransactionUtils.sign(transaction, ecKey);
        return transaction;
    }

    private static boolean processTransactionExtention(TransactionExtention transactionExtention)
            throws IOException, CipherException, CancelException {
        if (transactionExtention == null) {
            return false;
        }
        Return ret = transactionExtention.getResult();
        if (!ret.getResult()) {
            logger.debug("Code = " + ret.getCode());
            logger.debug("Message = " + ret.getMessage().toStringUtf8());
            return false;
        }
        Transaction transaction = transactionExtention.getTransaction();
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            logger.debug("Transaction is empty");
            return false;
        }
        logger.debug(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = signTransaction(transaction);
        return TronVegasGrpcClientPool.getInstance().borrow().broadcastTransaction(transaction);
    }

    private static boolean processTransaction(Transaction transaction)
            throws IOException, CipherException, CancelException {
        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }
        transaction = signTransaction(transaction);
        return TronVegasGrpcClientPool.getInstance().borrow().broadcastTransaction(transaction);
    }

    public static boolean sendCoin(byte[] to, long amount)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.TransferContract contract = createTransferContract(to, owner, amount);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
            return processTransaction(transaction);
        }
    }

    public static String sendCoinForTxid(byte[] to, long amount)
        throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.TransferContract contract = createTransferContract(to, owner, amount);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction2(contract);
            boolean ret = processTransactionExtention(transactionExtention);
            if (ret) {
                return ByteArray.toHexString(transactionExtention.getTxid().toByteArray());
            }
            return null;
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
            boolean ret = processTransaction(transaction);
            if (ret) {
                return ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
            }
            return null;
        }
    }

    public static boolean updateAccount(byte[] accountNameBytes)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.AccountUpdateContract contract = createAccountUpdateContract(accountNameBytes, owner);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean setAccountId(byte[] accountIdBytes)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        GrpcClient client = TronVegasGrpcClientPool.getInstance().borrow();
        Contract.SetAccountIdContract contract = createSetAccountIdContract(accountIdBytes, owner);
        Transaction transaction = client.createTransaction(contract);

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            return false;
        }

        transaction = signTransaction(transaction);
        return client.broadcastTransaction(transaction);
    }

    public static boolean updateAsset(byte[] description, byte[] url, long newLimit,
                                      long newPublicLimit)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.UpdateAssetContract contract
                = createUpdateAssetContract(owner, description, url, newLimit, newPublicLimit);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean transferAsset(byte[] to, byte[] assertName, long amount)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.TransferAssetContract contract = createTransferAssetContract(to, assertName, owner,
                amount);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransferAssetTransaction2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createTransferAssetTransaction(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean participateAssetIssue(byte[] to, byte[] assertName, long amount)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.ParticipateAssetIssueContract contract = participateAssetIssueContract(to, assertName,
                owner, amount);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow()
                    .createParticipateAssetIssueTransaction2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createParticipateAssetIssueTransaction(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean createAssetIssue(Contract.AssetIssueContract contract)
            throws CipherException, IOException, CancelException {
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createAssetIssue2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createAssetIssue(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean createAccount(byte[] address)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.AccountCreateContract contract = createAccountCreateContract(owner, address);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createAccount2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createAccount(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean createWitness(byte[] url) throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.WitnessCreateContract contract = createWitnessCreateContract(owner, url);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createWitness2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createWitness(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean updateWitness(byte[] url) throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.WitnessUpdateContract contract = createWitnessUpdateContract(owner, url);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().updateWitness2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().updateWitness(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean voteWitness(HashMap<String, String> witness)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().voteWitnessAccount2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().voteWitnessAccount(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean freezeBalance(long frozen_balance, long frozen_duration, int resourceCode,
                                        String receiverAddress)
            throws CipherException, IOException, CancelException {
        Contract.FreezeBalanceContract contract = createFreezeBalanceContract(frozen_balance,
                frozen_duration, resourceCode, receiverAddress);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
            return processTransaction(transaction);
        }
    }

    public static boolean buyStorage(long quantity)
            throws CipherException, IOException, CancelException {
        Contract.BuyStorageContract contract = createBuyStorageContract(quantity);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
        return processTransactionExtention(transactionExtention);
    }

    public static boolean buyStorageBytes(long bytes)
            throws CipherException, IOException, CancelException {
        Contract.BuyStorageBytesContract contract = createBuyStorageBytesContract(bytes);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
        return processTransactionExtention(transactionExtention);
    }

    public static boolean sellStorage(long storageBytes)
            throws CipherException, IOException, CancelException {
        Contract.SellStorageContract contract = createSellStorageContract(storageBytes);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
        return processTransactionExtention(transactionExtention);

    }

    private static FreezeBalanceContract createFreezeBalanceContract(long frozen_balance,
                                                                     long frozen_duration, int resourceCode, String receiverAddress) {
        byte[] address = getAddress();
        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddress = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddress).setFrozenBalance(frozen_balance)
                .setFrozenDuration(frozen_duration).setResourceValue(resourceCode);

        if (receiverAddress != null && !receiverAddress.equals("")) {
            ByteString receiverAddressBytes = ByteString.copyFrom(
                    Objects.requireNonNull(TronVegasApi.decodeFromBase58Check(receiverAddress)));
            builder.setReceiverAddress(receiverAddressBytes);
        }
        return builder.build();
    }

    private static BuyStorageContract createBuyStorageContract(long quantity) {
        byte[] address = getAddress();
        Contract.BuyStorageContract.Builder builder = Contract.BuyStorageContract.newBuilder();
        ByteString byteAddress = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddress).setQuant(quantity);

        return builder.build();
    }

    private static BuyStorageBytesContract createBuyStorageBytesContract(long bytes) {
        byte[] address = getAddress();
        Contract.BuyStorageBytesContract.Builder builder = Contract.BuyStorageBytesContract
                .newBuilder();
        ByteString byteAddress = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddress).setBytes(bytes);

        return builder.build();
    }

    private static SellStorageContract createSellStorageContract(long storageBytes) {
        byte[] address = getAddress();
        Contract.SellStorageContract.Builder builder = Contract.SellStorageContract.newBuilder();
        ByteString byteAddress = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddress).setStorageBytes(storageBytes);

        return builder.build();
    }

    public static boolean unfreezeBalance(int resourceCode, String receiverAddress)
            throws CipherException, IOException, CancelException {
        Contract.UnfreezeBalanceContract contract = createUnfreezeBalanceContract(resourceCode, receiverAddress);
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
            return processTransaction(transaction);
        }
    }

    private static UnfreezeBalanceContract createUnfreezeBalanceContract(int resourceCode, String receiverAddress) {
        byte[] address = getAddress();
        Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddreess).setResourceValue(resourceCode);

        if (receiverAddress != null && !receiverAddress.equals("")) {
            ByteString receiverAddressBytes = ByteString.copyFrom(
                    Objects.requireNonNull(TronVegasApi.decodeFromBase58Check(receiverAddress)));
            builder.setReceiverAddress(receiverAddressBytes);
        }

        return builder.build();
    }

    public static boolean unfreezeAsset() throws CipherException, IOException, CancelException {
        Contract.UnfreezeAssetContract contract = createUnfreezeAssetContract();
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
            return processTransaction(transaction);
        }
    }

    private static UnfreezeAssetContract createUnfreezeAssetContract() {
        byte[] address = getAddress();
        Contract.UnfreezeAssetContract.Builder builder = Contract.UnfreezeAssetContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddreess);
        return builder.build();
    }

    public static boolean withdrawBalance() throws CipherException, IOException, CancelException {
        Contract.WithdrawBalanceContract contract = createWithdrawBalanceContract();
        if (rpcVersion == 2) {
            TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().createTransaction2(contract);
            return processTransactionExtention(transactionExtention);
        } else {
            Transaction transaction = TronVegasGrpcClientPool.getInstance().borrow().createTransaction(contract);
            return processTransaction(transaction);
        }
    }

    private static WithdrawBalanceContract createWithdrawBalanceContract() {
        byte[] address = getAddress();
        Contract.WithdrawBalanceContract.Builder builder = Contract.WithdrawBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(address);
        builder.setOwnerAddress(byteAddreess);

        return builder.build();
    }

    public static boolean createProposal(HashMap<Long, Long> parametersMap)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.ProposalCreateContract contract = createProposalCreateContract(owner, parametersMap);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().proposalCreate(contract);
        return processTransactionExtention(transactionExtention);
    }

    public static boolean approveProposal(long id, boolean is_add_approval)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.ProposalApproveContract contract = createProposalApproveContract(owner, id,
                is_add_approval);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().proposalApprove(contract);
        return processTransactionExtention(transactionExtention);
    }

    public static boolean deleteProposal(long id)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.ProposalDeleteContract contract = createProposalDeleteContract(owner, id);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().proposalDelete(contract);
        return processTransactionExtention(transactionExtention);
    }

    public static boolean exchangeCreate(byte[] firstTokenId, long firstTokenBalance,
                                         byte[] secondTokenId, long secondTokenBalance)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.ExchangeCreateContract contract = createExchangeCreateContract(owner, firstTokenId,
                firstTokenBalance, secondTokenId, secondTokenBalance);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().exchangeCreate(contract);
        return processTransactionExtention(transactionExtention);
    }

    public static boolean exchangeInject(long exchangeId, byte[] tokenId, long quant)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.ExchangeInjectContract contract = createExchangeInjectContract(owner, exchangeId,
                tokenId, quant);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().exchangeInject(contract);
        return processTransactionExtention(transactionExtention);
    }

    public static boolean exchangeWithdraw(long exchangeId, byte[] tokenId, long quant)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.ExchangeWithdrawContract contract = createExchangeWithdrawContract(owner, exchangeId,
                tokenId, quant);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().exchangeWithdraw(contract);
        return processTransactionExtention(transactionExtention);
    }

    public static boolean exchangeTransaction(long exchangeId, byte[] tokenId, long quant, long expected)
            throws CipherException, IOException, CancelException {
        byte[] owner = getAddress();
        Contract.ExchangeTransactionContract contract = createExchangeTransactionContract(owner,
                exchangeId, tokenId, quant, expected);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().exchangeTransaction(contract);
        return processTransactionExtention(transactionExtention);
    }

    public static byte[] generateContractAddress(Transaction trx) {

        // get owner address
        // this address should be as same as the onweraddress in trx, DONNOT modify it
        byte[] ownerAddress = getAddress();

        // get tx hash
        byte[] txRawDataHash = Sha256Hash.of(trx.getRawData().toByteArray()).getBytes();

        // combine
        byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
        System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
        System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

        return Hash.sha3omit12(combined);

    }

    public static boolean updateSetting(byte[] contractAddress, long consumeUserResourcePercent)
            throws IOException, CipherException, CancelException {
        byte[] owner = getAddress();
        UpdateSettingContract updateSettingContract = createUpdateSettingContract(owner,
                contractAddress, consumeUserResourcePercent);

        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().updateSetting(updateSettingContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            logger.debug("RPC create trx failed!");
            if (transactionExtention != null) {
                logger.debug("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return false;
        }

        return processTransactionExtention(transactionExtention);

    }

    public static boolean updateEnergyLimit(byte[] contractAddress, long originEnergyLimit)
            throws IOException, CipherException, CancelException {
        byte[] owner = getAddress();
        UpdateEnergyLimitContract updateEnergyLimitContract = createUpdateEnergyLimitContract(
                owner,
                contractAddress, originEnergyLimit);

        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow()
                .updateEnergyLimit(updateEnergyLimitContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            logger.debug("RPC create trx failed!");
            if (transactionExtention != null) {
                logger.debug("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return false;
        }

        return processTransactionExtention(transactionExtention);

    }

    public static boolean deployContract(String contractName, String ABI, String code,
                                         long feeLimit, long value, long consumeUserResourcePercent, long originEnergyLimit, long tokenValue, String tokenId, String libraryAddressPair)
            throws IOException, CipherException, CancelException {
        byte[] owner = getAddress();
        CreateSmartContract contractDeployContract = createContractDeployContract(contractName, owner,
                ABI, code, value, consumeUserResourcePercent, originEnergyLimit, tokenValue, tokenId, libraryAddressPair);

        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().deployContract(contractDeployContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            logger.debug("RPC create trx failed!");
            if (transactionExtention != null) {
                logger.debug("Code = " + transactionExtention.getResult().getCode());
                System.out
                        .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            }
            return false;
        }

        TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();

        byte[] contractAddress = generateContractAddress(transactionExtention.getTransaction());
        logger.debug(
                "Your smart contract address will be: " + TronVegasApi.encode58Check(contractAddress));
        return processTransactionExtention(transactionExtention);

    }

    public static TransactionExtention triggerContractForTxid(byte[] contractAddress, long callValue, byte[] data, long feeLimit, long tokenValue, String tokenId)
        throws IOException, CipherException, CancelException {
        byte[] owner = ecKey.getAddress();
        Contract.TriggerSmartContract triggerContract = triggerCallContract(owner, contractAddress,
            callValue, data, tokenValue, tokenId);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().triggerContract(triggerContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            logger.debug("RPC create call trx failed!");
            logger.debug("Code = " + transactionExtention.getResult().getCode());
            logger.debug("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            return null;
        }

        Transaction transaction = transactionExtention.getTransaction();
        if (transaction.getRetCount() != 0 &&
            transactionExtention.getConstantResult(0) != null &&
            transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
            logger.debug("message:" + transaction.getRet(0).getRet());
            logger.debug(":" + ByteArray
                .toStr(transactionExtention.getResult().getMessage().toByteArray()));
            logger.debug("Result:" + Hex.toHexString(result));
            return transactionExtention;
        }

        TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
            .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();

        if (processTransactionExtention(transactionExtention)) {
            return transactionExtention;
        }
        return null;
    }

    public static boolean triggerContract(byte[] contractAddress, long callValue, byte[] data, long feeLimit, long tokenValue, String tokenId)
            throws IOException, CipherException, CancelException {
        byte[] owner = ecKey.getAddress();
        Contract.TriggerSmartContract triggerContract = triggerCallContract(owner, contractAddress,
                callValue, data, tokenValue, tokenId);
        TransactionExtention transactionExtention = TronVegasGrpcClientPool.getInstance().borrow().triggerContract(triggerContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            logger.debug("RPC create call trx failed!");
            logger.debug("Code = " + transactionExtention.getResult().getCode());
            logger.debug("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            return false;
        }

        Transaction transaction = transactionExtention.getTransaction();
        if (transaction.getRetCount() != 0 &&
                transactionExtention.getConstantResult(0) != null &&
                transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
            logger.debug("message:" + transaction.getRet(0).getRet());
            logger.debug(":" + ByteArray
                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
            logger.debug("Result:" + Hex.toHexString(result));
            return true;
        }

        TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
        Transaction.Builder transBuilder = Transaction.newBuilder();
        Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
        transBuilder.setRawData(rawBuilder);
        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }
        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }
        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();

        return processTransactionExtention(transactionExtention);
    }
}
