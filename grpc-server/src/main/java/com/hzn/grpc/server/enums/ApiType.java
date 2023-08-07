package com.hzn.grpc.server.enums;

/**
 * @author : hzn
 * @date : 2023/04/13
 * @description :
 */
public interface ApiType {
	String getApiName ();

	String getUri ();

	String getMethod ();

	String getContentType ();

	String getAccept ();
}
