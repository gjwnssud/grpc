package com.hzn.grpc.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * @author : hzn
 * @date : 2023/07/14
 * @description :
 */
@SpringBootApplication
public class GrpcServerApplication {
	public static void main (String[] args) {
		TimeZone.setDefault (TimeZone.getTimeZone ("UTC"));
		SpringApplication.run (GrpcServerApplication.class, args);
	}
}
