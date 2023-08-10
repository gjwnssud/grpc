package com.hzn.grpc.client.dto;

import lombok.Getter;

@Getter
public enum BlockChainApiType {
	// AccountTx
	CREATE_ACCOUNT("CreateAccount", "AccountTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	UPDATE_ACCOUNT("UpdateAccount", "AccountTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	ADD_PUBLICKEY_SUMMARIES("AddPublicKeySummaries", "AccountTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	REMOVE_PUBLICKEY_SUMMARIES("RemovePublicKeySummaries", "AccountTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	REMOVE_ACCOUNT("RemoveAccount", "AccountTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	// GroupTx
	CREATE_GROUP("CreateGroup", "GroupTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	DISBAND_GROUP("DisbandGroup", "GroupTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	ADD_ACCOUNTS("AddAccounts", "GroupTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	REMOVE_ACCOUNTS("RemoveAccounts", "GroupTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	REPLACE_COORDINATOR("ReplaceCoordinator", "GroupTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	// TokenTx
	DEFINE_TOKEN("DefineToken", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	MINT_FUNGIBLE_TOKEN("MintFungibleToken", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	MINT_NFT("MintNFT", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	BURN_NFT("BurnNFT", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	TRANSFER_FUNGIBLE_TOKEN("TransferFungibleToken", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	TRANSFER_NFT("TransferNFT", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	ENTRUST_FUNGIBLE_TOKEN("EntrustFungibleToken", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	ENTRUST_NFT("EntrustNFT", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	DISPOSE_ENTRUSTED_FUNGIBLE_TOKEN("DisposeEntrustedFungibleToken", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	DISPOSE_ENTRUSTED_NFT("DisposeEntrustedNFT", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	// RewardTx
	REGISTER_DAO("RegisterDao", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	UPDATE_DAO("UpdateDao", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	RECORD_ACTIVITY("RecordActivity", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	REGISTER_STAKING("RegisterStaking", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	REMOVE_STAKING("RemoveStaking", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	EXCUTE_STAKING_REQUEST("ExcuteStakingRequest", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	EXECUTE_REWARD("ExecuteReward", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	OFFER_REWARD("OfferReward", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	BUILD_SNAPSHOT("BuildSnapshot", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	EXECUTE_OWNERSHIP_REWARD("ExecuteOwnershipReward", "RewardTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	// HealthCheck
	HEALTH("Health", "Health", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),

	SUGGEST_FUNGIBLE_TOKEN_DEAL("SuggestFungibleTokenDeal", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	SUGGEST_SELL_DEAL("SuggestSellDeal", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	SUGGEST_BUY_DEAL("SuggestBuyDeal", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	SUGGEST_SWAP_DEAL("SuggestSwapDeal", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	ACCEPT_DEAL("AcceptDeal", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	CANCEL_SUGGESTION("CancelSuggestion", "TokenTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),

	// RandomOfferingTx
	NOTICE_TOKEN_OFFERING("NoticeTokenOffering", "RandomOfferingTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	JOIN_TOKEN_OFFERING("JoinTokenOffering", "RandomOfferingTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	INITIAL_TOKEN_OFFERING("InitialTokenOffering", "RandomOfferingTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	CLAIM_NFT("ClaimNFT", "RandomOfferingTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	VERIFIABLE_RANDOMRESULT("VerifiableRandomResult", "RandomOfferingTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	// AgendaTx
	SUGGEST_AGENDA("SuggestAgenda", "AgendaTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	VOTE_AGENDA("VoteAgenda", "AgendaTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	FINALIZE_VOTING("FinalizeVoting", "AgendaTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	SUGGEST_SIMPLE_AGENDA("SuggestSimpleAgenda", "AgendaTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),
	VOTE_SIMPLE_AGENDA("VoteSimpleAgenda", "AgendaTx", "/tx", "POST", "application/json;charset=UTF-8", "application/json;charset=UTF-8"),

	// 조회
	BALANCE("balance", "", "/balance", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	NFT_BALANCE("nft-balance", "", "/nft-balance", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	ETH("eth", "", "/eth", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	REWARD_EXPECTATION("reward-expectation", "", "/reward-expectation", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	REWARD("reward", "", "/reward", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	NFT_REWARD("nft-reward", "", "/nft-reward", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	DAO("dao", "", "/dao", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	GROUP_INFO("group", "", "/group", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	OWNERS("owners", "", "/owners", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	AGENDA("agenda", "", "/agenda", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	BLOCK("block", "", "/block", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	OFFERING("offering", "", "/offering", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	STATUS("status", "", "/status", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	TOKEN_DEF("token-def", "", "/token-def", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	TOKEN("token", "", "/token", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	TX("tx", "", "/tx", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	ACCOUNT("account", "", "/account", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	SNAPSHOT_ACCOUNT("snapshotAccount", "", "/snapshot/account", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	SNAPSHOT_TOKEN("snapshotToken", "", "/snapshot/token", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	SNAPSHOT_OWNERSHIP("snapshotOwnership", "", "/snapshot/ownership", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	REWARDED_ACCOUNT("rewardedAccount", "", "/rewarded/account", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	REWARDED_TOKEN("rewardedToken", "", "/rewarded/token", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	REWARDED_OWNERSHIP("rewardedOwnership", "", "/rewarded/ownership", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	;

	private final String apiName;
	private final String txName;
	private final String uri;
	private final String method;
	private final String contentType;
	private final String accept;

	BlockChainApiType (String apiName, String txName, String uri, String method, String contentType, String accept) {
		this.apiName = apiName;
		this.txName = txName;
		this.uri = uri;
		this.method = method;
		this.contentType = contentType;
		this.accept = accept;
	}
}
