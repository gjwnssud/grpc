package com.hzn.grpc.server.exception;

/**
 * @author : hzn
 * @fileName : GrpcServerException
 * @date : 2022/09/19
 * @description :
 * ===========================================================
 * DATE                 AUTHOR                NOTE
 * -----------------------------------------------------------
 * 2022/09/19            hzn               최초 생성
 */
public class GrpcServerException extends RuntimeException {
	public GrpcServerException (String message) {
		super (message);
	}
}
