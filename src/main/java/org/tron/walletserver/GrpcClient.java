package org.tron.walletserver;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountPaginated;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.AddressPrKeyPairMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.BlockLimit;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.BlockListExtention;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.DelegatedResourceList;
import org.tron.api.GrpcAPI.DelegatedResourceMessage;
import org.tron.api.GrpcAPI.EasyTransferAssetByPrivateMessage;
import org.tron.api.GrpcAPI.EasyTransferAssetMessage;
import org.tron.api.GrpcAPI.EasyTransferByPrivateMessage;
import org.tron.api.GrpcAPI.EasyTransferMessage;
import org.tron.api.GrpcAPI.EasyTransferResponse;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.NodeList;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.PaginatedMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.TransactionListExtention;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletExtensionGrpc;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.Proposal;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionSign;

public class GrpcClient {

  private static final Logger logger = LoggerFactory.getLogger("GrpcClient");
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;

//  public GrpcClient(String host, int port) {
//    channel = ManagedChannelBuilder.forAddress(host, port)
//        .usePlaintext(true)
//        .build();
//    blockingStub = WalletGrpc.newBlockingStub(channel);
//  }

  public GrpcClient(String fullnode, String soliditynode) {
    if (!StringUtils.isEmpty(fullnode)) {
      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    }
    if (!StringUtils.isEmpty(soliditynode)) {
      channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
          .usePlaintext(true)
          .build();
      blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
      blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelSolidity);
    }
  }

  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public void shutdownNow() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdownNow();
    }
    if (channelSolidity != null) {
      channelSolidity.shutdownNow();
    }
  }

  public Protocol.NodeInfo getNodeInfo() {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getNodeInfo(EmptyMessage.newBuilder().build());
  }

  public Account queryAccount(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAccount(request);
    } else {
      return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAccount(request);
    }
  }

  public Account queryAccountById(String accountId) {
    ByteString bsAccountId = ByteString.copyFromUtf8(accountId);
    Account request = Account.newBuilder().setAccountId(bsAccountId).build();
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAccountById(request);
    } else {
      return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAccountById(request);
    }
  }

  //Warning: do not invoke this interface provided by others.
  public Transaction signTransaction(TransactionSign transactionSign) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getTransactionSign(transactionSign);
  }

  //Warning: do not invoke this interface provided by others.
  public TransactionExtention signTransaction2(TransactionSign transactionSign) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getTransactionSign2(transactionSign);
  }

  //Warning: do not invoke this interface provided by others.
  public TransactionExtention addSign(TransactionSign transactionSign) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).addSign(transactionSign);
  }

  public TransactionSignWeight getTransactionSignWeight(Transaction transaction) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getTransactionSignWeight(transaction);
  }

  public TransactionApprovedList getTransactionApprovedList(Transaction transaction) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getTransactionApprovedList(transaction);
  }

  //Warning: do not invoke this interface provided by others.
  public byte[] createAdresss(byte[] passPhrase) {
    BytesMessage.Builder builder = BytesMessage.newBuilder();
    builder.setValue(ByteString.copyFrom(passPhrase));

    BytesMessage result = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).createAddress(builder.build());
    return result.getValue().toByteArray();
  }

  //Warning: do not invoke this interface provided by others.
  public EasyTransferResponse easyTransfer(byte[] passPhrase, byte[] toAddress, long amount) {
    EasyTransferMessage.Builder builder = EasyTransferMessage.newBuilder();
    builder.setPassPhrase(ByteString.copyFrom(passPhrase));
    builder.setToAddress(ByteString.copyFrom(toAddress));
    builder.setAmount(amount);

    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).easyTransfer(builder.build());
  }

  //Warning: do not invoke this interface provided by others.
  public EasyTransferResponse easyTransferByPrivate(byte[] privateKey, byte[] toAddress,
      long amount) {
    EasyTransferByPrivateMessage.Builder builder = EasyTransferByPrivateMessage.newBuilder();
    builder.setPrivateKey(ByteString.copyFrom(privateKey));
    builder.setToAddress(ByteString.copyFrom(toAddress));
    builder.setAmount(amount);

    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).easyTransferByPrivate(builder.build());
  }

  //Warning: do not invoke this interface provided by others.
  public EasyTransferResponse easyTransferAsset(byte[] passPhrase, byte[] toAddress,
      String assetId, long amount) {
    EasyTransferAssetMessage.Builder builder = EasyTransferAssetMessage.newBuilder();
    builder.setPassPhrase(ByteString.copyFrom(passPhrase));
    builder.setToAddress(ByteString.copyFrom(toAddress));
    builder.setAssetId(assetId);
    builder.setAmount(amount);

    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).easyTransferAsset(builder.build());
  }

  //Warning: do not invoke this interface provided by others.
  public EasyTransferResponse easyTransferAssetByPrivate(byte[] privateKey, byte[] toAddress,
      String assetId, long amount) {
    EasyTransferAssetByPrivateMessage.Builder builder = EasyTransferAssetByPrivateMessage
        .newBuilder();
    builder.setPrivateKey(ByteString.copyFrom(privateKey));
    builder.setToAddress(ByteString.copyFrom(toAddress));
    builder.setAssetId(assetId);
    builder.setAmount(amount);

    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).easyTransferAssetByPrivate(builder.build());
  }

  public Transaction createTransaction(Contract.AccountUpdateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).updateAccount(contract);
  }

  public TransactionExtention createTransaction2(Contract.AccountUpdateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).updateAccount2(contract);
  }

  public Transaction createTransaction(Contract.SetAccountIdContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).setAccountId(contract);
  }

  public Transaction createTransaction(Contract.UpdateAssetContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).updateAsset(contract);
  }

  public TransactionExtention createTransaction2(Contract.UpdateAssetContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).updateAsset2(contract);
  }

  public Transaction createTransaction(Contract.TransferContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).createTransaction(contract);
  }

  public TransactionExtention createTransaction2(Contract.TransferContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).createTransaction2(contract);
  }

  public Transaction createTransaction(Contract.FreezeBalanceContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).freezeBalance(contract);
  }

  public TransactionExtention createTransaction(Contract.BuyStorageContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).buyStorage(contract);
  }

  public TransactionExtention createTransaction(Contract.BuyStorageBytesContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).buyStorageBytes(contract);
  }

  public TransactionExtention createTransaction(Contract.SellStorageContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).sellStorage(contract);
  }

  public TransactionExtention createTransaction2(Contract.FreezeBalanceContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).freezeBalance2(contract);
  }

  public Transaction createTransaction(Contract.WithdrawBalanceContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).withdrawBalance(contract);
  }

  public TransactionExtention createTransaction2(Contract.WithdrawBalanceContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).withdrawBalance2(contract);
  }

  public Transaction createTransaction(Contract.UnfreezeBalanceContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).unfreezeBalance(contract);
  }

  public TransactionExtention createTransaction2(Contract.UnfreezeBalanceContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).unfreezeBalance2(contract);
  }

  public Transaction createTransaction(Contract.UnfreezeAssetContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).unfreezeAsset(contract);
  }

  public TransactionExtention createTransaction2(Contract.UnfreezeAssetContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).unfreezeAsset2(contract);
  }

  public Transaction createTransferAssetTransaction(Contract.TransferAssetContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).transferAsset(contract);
  }

  public TransactionExtention createTransferAssetTransaction2(
      Contract.TransferAssetContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).transferAsset2(contract);
  }

  public Transaction createParticipateAssetIssueTransaction(
      Contract.ParticipateAssetIssueContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).participateAssetIssue(contract);
  }

  public TransactionExtention createParticipateAssetIssueTransaction2(
      Contract.ParticipateAssetIssueContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).participateAssetIssue2(contract);
  }

  public Transaction createAssetIssue(Contract.AssetIssueContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).createAssetIssue(contract);
  }

  public TransactionExtention createAssetIssue2(Contract.AssetIssueContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).createAssetIssue2(contract);
  }

  public Transaction voteWitnessAccount(Contract.VoteWitnessContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).voteWitnessAccount(contract);
  }

  public TransactionExtention voteWitnessAccount2(Contract.VoteWitnessContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).voteWitnessAccount2(contract);
  }

  public TransactionExtention proposalCreate(Contract.ProposalCreateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).proposalCreate(contract);
  }

  public Optional<ProposalList> listProposals() {
    ProposalList proposalList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).listProposals(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(proposalList);
  }

  public Optional<Proposal> getProposal(String id) {
    BytesMessage request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
        ByteArray.fromLong(Long.parseLong(id))))
        .build();
    Proposal proposal = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getProposalById(request);
    return Optional.ofNullable(proposal);
  }

  public Optional<DelegatedResourceList> getDelegatedResource(String fromAddress,
      String toAddress) {

    ByteString fromAddressBS = ByteString.copyFrom(
        Objects.requireNonNull(TronVegasApi.decodeFromBase58Check(fromAddress)));
    ByteString toAddressBS = ByteString.copyFrom(
        Objects.requireNonNull(TronVegasApi.decodeFromBase58Check(toAddress)));

    DelegatedResourceMessage request = DelegatedResourceMessage.newBuilder()
        .setFromAddress(fromAddressBS)
        .setToAddress(toAddressBS)
        .build();
    DelegatedResourceList delegatedResource = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
        .getDelegatedResource(request);
    return Optional.ofNullable(delegatedResource);
  }

  public Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndex(String address) {

    ByteString addressBS = ByteString.copyFrom(
        Objects.requireNonNull(TronVegasApi.decodeFromBase58Check(address)));

    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(addressBS).build();

    DelegatedResourceAccountIndex accountIndex = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
        .getDelegatedResourceAccountIndex(bytesMessage);
    return Optional.ofNullable(accountIndex);
  }


  public Optional<ExchangeList> listExchanges() {
    ExchangeList exchangeList;
    if (blockingStubSolidity != null) {
      exchangeList = blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).listExchanges(EmptyMessage.newBuilder().build());
    } else {
      exchangeList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).listExchanges(EmptyMessage.newBuilder().build());
    }

    return Optional.ofNullable(exchangeList);
  }

  public Optional<Exchange> getExchange(String id) {
    BytesMessage request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
        ByteArray.fromLong(Long.parseLong(id))))
        .build();

    Exchange exchange;
    if (blockingStubSolidity != null) {
      exchange = blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getExchangeById(request);
    } else {
      exchange = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getExchangeById(request);
    }

    return Optional.ofNullable(exchange);
  }

  public Optional<ChainParameters> getChainParameters() {
    ChainParameters chainParameters = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
        .getChainParameters(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(chainParameters);
  }

  public TransactionExtention proposalApprove(Contract.ProposalApproveContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).proposalApprove(contract);
  }

  public TransactionExtention proposalDelete(Contract.ProposalDeleteContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).proposalDelete(contract);
  }

  public TransactionExtention exchangeCreate(Contract.ExchangeCreateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).exchangeCreate(contract);
  }

  public TransactionExtention exchangeInject(Contract.ExchangeInjectContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).exchangeInject(contract);
  }

  public TransactionExtention exchangeWithdraw(Contract.ExchangeWithdrawContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).exchangeWithdraw(contract);
  }

  public TransactionExtention exchangeTransaction(Contract.ExchangeTransactionContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).exchangeTransaction(contract);
  }

  public Transaction createAccount(Contract.AccountCreateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).createAccount(contract);
  }

  public TransactionExtention createAccount2(Contract.AccountCreateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).createAccount2(contract);
  }

  public AddressPrKeyPairMessage generateAddress(EmptyMessage emptyMessage) {
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).generateAddress(emptyMessage);
    } else {
      return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).generateAddress(emptyMessage);
    }
  }

  public Transaction createWitness(Contract.WitnessCreateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).createWitness(contract);
  }

  public TransactionExtention createWitness2(Contract.WitnessCreateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).createWitness2(contract);
  }

  public Transaction updateWitness(Contract.WitnessUpdateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).updateWitness(contract);
  }

  public TransactionExtention updateWitness2(Contract.WitnessUpdateContract contract) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).updateWitness2(contract);
  }

  public boolean broadcastTransaction(Transaction signaturedTransaction) {
    int i = 10;
    GrpcAPI.Return response = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).broadcastTransaction(signaturedTransaction);
    while (response.getResult() == false && response.getCode() == response_code.SERVER_BUSY
        && i > 0) {
      i--;
      response = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).broadcastTransaction(signaturedTransaction);
      logger.info("repeat times = " + (11 - i));
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    if (response.getResult() == false) {
      logger.info("Code = " + response.getCode());
      logger.info("Message = " + response.getMessage().toStringUtf8());
    }
    return response.getResult();
  }

  public Block getBlock(long blockNum) {
    if (blockNum < 0) {
      if (blockingStubSolidity != null) {
        return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getNowBlock(EmptyMessage.newBuilder().build());
      } else {
        return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getNowBlock(EmptyMessage.newBuilder().build());
      }
    }
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getBlockByNum(builder.build());
    } else {
      return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getBlockByNum(builder.build());
    }
  }

  public long getTransactionCountByBlockNum(long blockNum) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getTransactionCountByBlockNum(builder.build()).getNum();
    } else {
      return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getTransactionCountByBlockNum(builder.build()).getNum();
    }
  }

  public BlockExtention getBlock2(long blockNum) {
    if (blockNum < 0) {
      if (blockingStubSolidity != null) {
        return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getNowBlock2(EmptyMessage.newBuilder().build());
      } else {
        return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getNowBlock2(EmptyMessage.newBuilder().build());
      }
    }
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getBlockByNum2(builder.build());
    } else {
      return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getBlockByNum2(builder.build());
    }
  }

//  public Optional<AccountList> listAccounts() {
//    AccountList accountList = blockingStubSolidity
//        .listAccounts(EmptyMessage.newBuilder().build());
//    return Optional.ofNullable(accountList);
//
//  }

  public Optional<WitnessList> listWitnesses() {
    if (blockingStubSolidity != null) {
      WitnessList witnessList = blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
          .listWitnesses(EmptyMessage.newBuilder().build());
      return Optional.ofNullable(witnessList);
    } else {
      WitnessList witnessList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).listWitnesses(EmptyMessage.newBuilder().build());
      return Optional.ofNullable(witnessList);
    }
  }

  public Optional<AssetIssueList> getAssetIssueList() {
    if (blockingStubSolidity != null) {
      AssetIssueList assetIssueList = blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
          .getAssetIssueList(EmptyMessage.newBuilder().build());
      return Optional.ofNullable(assetIssueList);
    } else {
      AssetIssueList assetIssueList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
          .getAssetIssueList(EmptyMessage.newBuilder().build());
      return Optional.ofNullable(assetIssueList);
    }
  }

  public Optional<AssetIssueList> getAssetIssueList(long offset, long limit) {
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    if (blockingStubSolidity != null) {
      AssetIssueList assetIssueList = blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
          .getPaginatedAssetIssueList(pageMessageBuilder.build());
      return Optional.ofNullable(assetIssueList);
    } else {
      AssetIssueList assetIssueList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
          .getPaginatedAssetIssueList(pageMessageBuilder.build());
      return Optional.ofNullable(assetIssueList);
    }
  }

  public Optional<ProposalList> getProposalListPaginated(long offset, long limit) {
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    ProposalList proposalList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
        .getPaginatedProposalList(pageMessageBuilder.build());
    return Optional.ofNullable(proposalList);

  }

  public Optional<ExchangeList> getExchangeListPaginated(long offset, long limit) {
    PaginatedMessage.Builder pageMessageBuilder = PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    ExchangeList exchangeList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
        .getPaginatedExchangeList(pageMessageBuilder.build());
    return Optional.ofNullable(exchangeList);

  }

  public Optional<NodeList> listNodes() {
    NodeList nodeList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).listNodes(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(nodeList);
  }

  public Optional<AssetIssueList> getAssetIssueByAccount(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    AssetIssueList assetIssueList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAssetIssueByAccount(request);
    return Optional.ofNullable(assetIssueList);
  }

  public AccountNetMessage getAccountNet(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAccountNet(request);
  }

  public AccountResourceMessage getAccountResource(byte[] address) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBS).build();
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAccountResource(request);
  }

  public Contract.AssetIssueContract getAssetIssueByName(String assetName) {
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAssetIssueByName(request);
    } else {
      return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAssetIssueByName(request);
    }
  }

  public Optional<AssetIssueList> getAssetIssueListByName(String assetName) {
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    if (blockingStubSolidity != null) {
      AssetIssueList assetIssueList = blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAssetIssueListByName(request);
      return Optional.ofNullable(assetIssueList);
    } else {
      AssetIssueList assetIssueList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAssetIssueListByName(request);
      return Optional.ofNullable(assetIssueList);
    }
  }

  public Contract.AssetIssueContract getAssetIssueById(String assetId) {
    ByteString assetIdBs = ByteString.copyFrom(assetId.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetIdBs).build();
    if (blockingStubSolidity != null) {
      return blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAssetIssueById(request);
    } else {
      return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getAssetIssueById(request);
    }
  }

  public NumberMessage getTotalTransaction() {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).totalTransaction(EmptyMessage.newBuilder().build());
  }

  public NumberMessage getNextMaintenanceTime() {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getNextMaintenanceTime(EmptyMessage.newBuilder().build());
  }

//  public Optional<AssetIssueList> getAssetIssueListByTimestamp(long time) {
//    NumberMessage.Builder timeStamp = NumberMessage.newBuilder();
//    timeStamp.setNum(time);
//    AssetIssueList assetIssueList = blockingStubSolidity
//        .getAssetIssueListByTimestamp(timeStamp.build());
//    return Optional.ofNullable(assetIssueList);
//  }

//  public Optional<TransactionList> getTransactionsByTimestamp(long start, long end, int offset,
//      int limit) {
//    TimeMessage.Builder timeMessage = TimeMessage.newBuilder();
//    timeMessage.setBeginInMilliseconds(start);
//    timeMessage.setEndInMilliseconds(end);
//    TimePaginatedMessage.Builder timePaginatedMessage = TimePaginatedMessage.newBuilder();
//    timePaginatedMessage.setTimeMessage(timeMessage);
//    timePaginatedMessage.setOffset(offset);
//    timePaginatedMessage.setLimit(limit);
//    TransactionList transactionList = blockingStubExtension
//        .getTransactionsByTimestamp(timePaginatedMessage.build());
//    return Optional.ofNullable(transactionList);
//  }

//  public NumberMessage getTransactionsByTimestampCount(long start, long end) {
//    TimeMessage.Builder timeMessage = TimeMessage.newBuilder();
//    timeMessage.setBeginInMilliseconds(start);
//    timeMessage.setEndInMilliseconds(end);
//    return blockingStubExtension.getTransactionsByTimestampCount(timeMessage.build());
//  }

  public Optional<TransactionList> getTransactionsFromThis(byte[] address, int offset, int limit) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBS).build();
    AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
    accountPaginated.setAccount(account);
    accountPaginated.setOffset(offset);
    accountPaginated.setLimit(limit);
    TransactionList transactionList = blockingStubExtension.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
        .getTransactionsFromThis(accountPaginated.build());
    return Optional.ofNullable(transactionList);
  }

  public Optional<TransactionListExtention> getTransactionsFromThis2(byte[] address, int offset,
      int limit) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBS).build();
    AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
    accountPaginated.setAccount(account);
    accountPaginated.setOffset(offset);
    accountPaginated.setLimit(limit);
    TransactionListExtention transactionList = blockingStubExtension.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
        .getTransactionsFromThis2(accountPaginated.build());
    return Optional.ofNullable(transactionList);
  }

//  public NumberMessage getTransactionsFromThisCount(byte[] address) {
//    ByteString addressBS = ByteString.copyFrom(address);
//    Account account = Account.newBuilder().setAddress(addressBS).build();
//    return blockingStubExtension.getTransactionsFromThisCount(account);
//  }

  public Optional<TransactionList> getTransactionsToThis(byte[] address, int offset, int limit) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBS).build();
    AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
    accountPaginated.setAccount(account);
    accountPaginated.setOffset(offset);
    accountPaginated.setLimit(limit);
    TransactionList transactionList = blockingStubExtension.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
        .getTransactionsToThis(accountPaginated.build());
    return Optional.ofNullable(transactionList);
  }

  public Optional<TransactionListExtention> getTransactionsToThis2(byte[] address, int offset,
      int limit) {
    ByteString addressBS = ByteString.copyFrom(address);
    Account account = Account.newBuilder().setAddress(addressBS).build();
    AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
    accountPaginated.setAccount(account);
    accountPaginated.setOffset(offset);
    accountPaginated.setLimit(limit);
    TransactionListExtention transactionList = blockingStubExtension.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS)
        .getTransactionsToThis2(accountPaginated.build());
    return Optional.ofNullable(transactionList);
  }
//  public NumberMessage getTransactionsToThisCount(byte[] address) {
//    ByteString addressBS = ByteString.copyFrom(address);
//    Account account = Account.newBuilder().setAddress(addressBS).build();
//    return blockingStubExtension.getTransactionsToThisCount(account);
//  }

  public Optional<Transaction> getTransactionById(String txID) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getTransactionById(request);
    return Optional.ofNullable(transaction);
  }

  public Optional<TransactionInfo> getTransactionInfoById(String txID) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo;
    if (blockingStubSolidity != null) {
      transactionInfo = blockingStubSolidity.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getTransactionInfoById(request);
    } else {
      transactionInfo = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getTransactionInfoById(request);
    }
    return Optional.ofNullable(transactionInfo);
  }

  public Optional<Block> getBlockById(String blockID) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(blockID));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Block block = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getBlockById(request);
    return Optional.ofNullable(block);
  }

  public Optional<BlockList> getBlockByLimitNext(long start, long end) {
    BlockLimit.Builder builder = BlockLimit.newBuilder();
    builder.setStartNum(start);
    builder.setEndNum(end);
    BlockList blockList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getBlockByLimitNext(builder.build());
    return Optional.ofNullable(blockList);
  }

  public Optional<BlockListExtention> getBlockByLimitNext2(long start, long end) {
    BlockLimit.Builder builder = BlockLimit.newBuilder();
    builder.setStartNum(start);
    builder.setEndNum(end);
    BlockListExtention blockList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getBlockByLimitNext2(builder.build());
    return Optional.ofNullable(blockList);
  }

  public Optional<BlockList> getBlockByLatestNum(long num) {
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(num).build();
    BlockList blockList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getBlockByLatestNum(numberMessage);
    return Optional.ofNullable(blockList);
  }

  public Optional<BlockListExtention> getBlockByLatestNum2(long num) {
    NumberMessage numberMessage = NumberMessage.newBuilder().setNum(num).build();
    BlockListExtention blockList = blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getBlockByLatestNum2(numberMessage);
    return Optional.ofNullable(blockList);
  }

  public TransactionExtention updateSetting(Contract.UpdateSettingContract request) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).updateSetting(request);
  }

  public TransactionExtention updateEnergyLimit(
      Contract.UpdateEnergyLimitContract request) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).updateEnergyLimit(request);
  }

  public TransactionExtention deployContract(Contract.CreateSmartContract request) {
    return blockingStubFull.deployContract(request);
  }

  public TransactionExtention triggerContract(Contract.TriggerSmartContract request) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).triggerContract(request);
  }

  public SmartContract getContract(byte[] address) {
    ByteString byteString = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).getContract(bytesMessage);
  }

  public TransactionExtention accountPermissionUpdate(
      Contract.AccountPermissionUpdateContract request) {
    return blockingStubFull.withDeadlineAfter(TronVegasApi.grpcTimeout, TimeUnit.MILLISECONDS).accountPermissionUpdate(request);
  }

}
