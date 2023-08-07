package com.hzn.grpc.server.dto;

import lombok.Data;

@Data
public class NommKeyPair {
	private String privateKey;
	private String publicKey;
	private String address;
}
