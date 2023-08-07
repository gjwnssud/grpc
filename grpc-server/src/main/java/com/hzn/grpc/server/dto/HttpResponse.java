package com.hzn.grpc.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * @author : hzn
 * @date : 2023/04/13
 * @description :
 */

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpResponse<T> {
	private int code;
	private T   data;

	public HttpResponse (int code) {
		this (code, null);
	}

	public HttpResponse (int code, T data) {
		this.code = code;
		this.data = data;
	}
}
