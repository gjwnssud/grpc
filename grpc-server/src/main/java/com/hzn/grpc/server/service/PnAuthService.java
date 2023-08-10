package com.hzn.grpc.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzn.grpc.server.dto.HttpResponse;
import com.hzn.grpc.server.enums.PnAuthApiType;
import com.hzn.grpc.server.exception.GrpcServerException;
import com.hzn.grpc.server.util.HttpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author : hzn
 * @date : 2023/04/13
 * @description :
 */
@Service
@RequiredArgsConstructor
public class PnAuthService {

	@Value("${playnomm.auth.api.url}")
	private       String       pnAuthDomain;
	private final HttpUtil     httpUtil;
	private final ObjectMapper objectMapper;

	/**
	 * 개인키 part2 호출
	 *
	 * @param blcMngrBassSn
	 * @return
	 */
	public String getBlcMngrPrivky (Integer blcMngrBassSn) throws JsonProcessingException {
		HttpResponse<?> response = httpUtil.send (pnAuthDomain, PnAuthApiType.MANAGER_KEY_INFO, Map.of ("sn", blcMngrBassSn));
		if (response.getCode () != 200) throw new GrpcServerException ("privateKey call failed.");
		Map<?, ?> dataMap = objectMapper.readValue ((String) response.getData (), Map.class);
		return (String) ((Map<?, ?>) dataMap.get ("data")).get ("key");
	}
}
