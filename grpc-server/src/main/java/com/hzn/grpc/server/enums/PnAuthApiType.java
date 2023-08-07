package com.hzn.grpc.server.enums;

import lombok.Getter;

/**
 * @author : hzn
 * @date : 2023/04/13
 * @description :
 */
@Getter
public enum PnAuthApiType implements ApiType {
	MEMBERSHIP_INFO ("membershipInfo", "/pn-api/v1/me", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8"),
	MANAGER_KEY_INFO ("managerKeyInfo", "/api/v1/manage/key", "GET", "application/x-www-form-urlencoded;charset=UTF-8", "application/json;charset=UTF-8")
	;

	private final String apiName;
	private final String uri;
	private final String method;
	private final String contentType;
	private final String accept;

	PnAuthApiType (String apiName, String uri, String method, String contentType, String accept) {
		this.apiName = apiName;
		this.uri = uri;
		this.method = method;
		this.contentType = contentType;
		this.accept = accept;
	}
}
