package com.hzn.grpc.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : hzn
 * @date : 2023/04/13
 * @description :
 */
@Configuration
public class BeanConfig {

	@Bean
	public ObjectMapper objectMapper () {
		return new ObjectMapper ();
	}
}
