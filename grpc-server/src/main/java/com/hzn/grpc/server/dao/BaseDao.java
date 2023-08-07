/*******************************************************************************
 *
 * Copyright 2021 edn corp. All rights reserved.
 *
 * This is a proprietary software of edn corp, and you may not use this file except in
 * compliance with license agreement with edn corp. Any redistribution or use of this
 * software, with or without modification shall be strictly prohibited without prior written
 * approval of ednh corp, and the copyright notice above does not evidence any actual or
 * intended publication of such software.
 *
 *******************************************************************************/
package com.hzn.grpc.server.dao;

import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

@Component
public class BaseDao {

	@Autowired
	@Qualifier("sqlSessionTemplate")
	private SqlSession session;

	/**
	 * return SqlSession
	 *
	 * @return
	 */
	public SqlSession getSession () {
		return session;
	}

	/**
	 * 1건 조회
	 *
	 * @param statement
	 * @return
	 */
	public <T> T selectOne (String statement) {
		return session.selectOne (statement);
	}

	/**
	 * 1건 조회
	 *
	 * @param statement
	 * @param parameter
	 * @return
	 */
	public <T> T selectOne (String statement, Object parameter) {
		return session.selectOne (statement, parameter);
	}

	/**
	 * 목록 조회
	 *
	 * @param statement
	 * @return
	 */
	public <E> List<E> selectList (String statement) {
		return session.selectList (statement);
	}

	/**
	 * 목록 조회
	 *
	 * @param statement
	 * @param parameter
	 * @return
	 */
	public <E> List<E> selectList (String statement, Object parameter) {
		return session.selectList (statement, parameter);
	}

	/**
	 * 목록 조회
	 *
	 * @param statement
	 * @param parameter
	 * @param rowBounds
	 * @return
	 */
	public <E> List<E> selectList (String statement, Object parameter, RowBounds rowBounds) {
		return session.selectList (statement, parameter, rowBounds);
	}

	/**
	 * 맵 조회
	 *
	 * @param statement
	 * @param mapKey
	 * @return
	 */
	public <K, V> Map<K, V> selectMap (String statement, String mapKey) {
		return session.selectMap (statement, mapKey);
	}

	/**
	 * 맵 조회
	 *
	 * @param statement
	 * @param parameter
	 * @param mapKey
	 * @return
	 */
	public <K, V> Map<K, V> selectMap (String statement, Object parameter, String mapKey) {
		return session.selectMap (statement, parameter, mapKey);
	}

	/**
	 * 맵 조회
	 *
	 * @param statement
	 * @param parameter
	 * @param mapKey
	 * @param rowBounds
	 * @return
	 */
	public <K, V> Map<K, V> selectMap (String statement, Object parameter, String mapKey, RowBounds rowBounds) {
		return session.selectMap (statement, parameter, mapKey, rowBounds);
	}

	@SuppressWarnings("unchecked")
	public <E> List<E> selectPagingList (String queryId, Map<String, Object> params) {

		String strPageIndex = (String) params.get ("pageIndex");
		String strPageRow = "10";
		int nPageIndex = 0;
		int nPageRow = 10;

		if (StringUtils.isEmpty (strPageIndex) == false) {
			nPageIndex = Integer.parseInt (strPageIndex) - 1;
		}

		if (StringUtils.isEmpty (strPageRow) == false) {
			nPageRow = Integer.parseInt (strPageRow);
		}

		params.put ("start", (nPageIndex * nPageRow));
		params.put ("end", 10);

		List<Object> list = session.selectList (queryId, params);

		return (List<E>) list;
	}

	@SuppressWarnings("unchecked")
	public <E> List<E> selectPagingList2 (String queryId, Map<String, Object> params) {

		String strPageIndex = (String) params.get ("pageIndex");
		String strPageRow = (String) params.get ("pageRow");
		int nPageIndex = 0;
		int nPageRow = Integer.parseInt (params.get ("pageRow").toString ());

		if (StringUtils.isEmpty (strPageIndex) == false) {
			nPageIndex = Integer.parseInt (strPageIndex) - 1;
		}

		if (StringUtils.isEmpty (strPageRow) == false) {
			nPageRow = Integer.parseInt (strPageRow);
		}

		params.put ("start", (nPageIndex * nPageRow));
		params.put ("end", nPageRow);

		List<Object> list = session.selectList (queryId, params);

		return (List<E>) list;
	}

	/**
	 * 등록
	 *
	 * @param statement
	 * @return
	 */
	public int insert (String statement) {
		return session.insert (statement);
	}

	/**
	 * 등록
	 *
	 * @param statement
	 * @param parameter
	 * @return
	 */
	public int insert (String statement, Object parameter) {
		return session.insert (statement, parameter);
	}

	/**
	 * 수정
	 *
	 * @param statement
	 * @return
	 */
	public int update (String statement) {
		return session.update (statement);
	}

	/**
	 * 수정
	 *
	 * @param statement
	 * @param parameter
	 * @return
	 */
	public int update (String statement, Object parameter) {
		return session.update (statement, parameter);
	}

	/**
	 * 삭제
	 *
	 * @param statement
	 * @return
	 */
	public int delete (String statement) {
		return session.delete (statement);
	}

	/**
	 * 삭제
	 *
	 * @param statement
	 * @param parameter
	 * @return
	 */
	public int delete (String statement, Object parameter) {
		return session.delete (statement, parameter);
	}

	/**
	 * 커밋
	 */
	public void commit () {
		session.commit ();
	}

	/**
	 * 커밋
	 *
	 * @param force
	 */
	public void commit (boolean force) {
		session.commit (force);
	}

	/**
	 * 롤백
	 */
	public void rollback () {
		session.rollback ();
	}

	/**
	 * 롤백
	 *
	 * @param force
	 */
	public void rollback (boolean force) {
		session.rollback (force);
	}

	/**
	 * 컨넥션 반환
	 *
	 * @return
	 */
	public Connection getConnection () {
		return session.getConnection ();
	}
}
