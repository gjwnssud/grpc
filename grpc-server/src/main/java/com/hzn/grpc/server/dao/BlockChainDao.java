package com.hzn.grpc.server.dao;

import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * @author : hzn
 * @fileName : BlockChainDao
 * @date : 2022-07-07
 * @description :
 * ===========================================================
 * DATE                 AUTHOR                NOTE
 * -----------------------------------------------------------
 * 2022-07-07             hzn               최초 생성
 */
@Repository("BlockChainDao")
public class BlockChainDao extends BaseDao {
	private String mapper = "blockChainMapper.";

	public int insertTxHashLog (Map<String, Object> param) {
		return insert (mapper.concat ("insertTxHashLog"), param);
	}

	public String selectMngrPrivateKey (String BLC_MNGR_ID) {
		return selectOne (mapper.concat ("selectMngrPrivateKey"), BLC_MNGR_ID);
	}

	public String selectGroupId () {
		return selectOne (mapper.concat ("selectGroupId"));
	}

	public String selectTxHash (String TXHASH_ID) {
		return selectOne (mapper.concat ("selectTxHash"), TXHASH_ID);
	}

	/**
	 * 개인키 part1 호출
	 *
	 * @param blcMngrId
	 * @return
	 */
	public Map<String, Object> selectBlcMngrBass (String blcMngrId) {
		return selectOne (mapper.concat ("selectBlcMngrBass"), blcMngrId);
	}
}
