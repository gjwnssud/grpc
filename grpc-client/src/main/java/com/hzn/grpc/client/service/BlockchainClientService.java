package com.hzn.grpc.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzn.grpc.client.dto.BlockChainApiType;
import com.hzn.grpc.inf.BlockchainProto;
import com.hzn.grpc.inf.BlockchainServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : hzn
 * @fileName : BlockChainClientService
 * @date : 2022-07-08
 * @description :
 * ===========================================================
 * DATE                 AUTHOR                NOTE
 * -----------------------------------------------------------
 * 2022-07-08             hzn               최초 생성
 */
@Slf4j
@Service("BlockchainClientService")
public class BlockchainClientService {
	@GrpcClient("blockchain")
	private       BlockchainServiceGrpc.BlockchainServiceBlockingStub blockchainServiceBlockingStub;
	private final ObjectMapper                                        objectMapper      = new ObjectMapper ();
	private final DateTimeFormatter                                   dateTimeFormatter = DateTimeFormatter.ofPattern ("yyyy-MM-dd'T'HH:mm:ss");

	/**
	 * 블록체인 중계 서버 요청
	 *
	 * @param blockChainApiType API 유형
	 * @param dataParam         createdAt(요청 시간 : LocalDateTime.toString()), definitionId, name, symbol, guardian, account 등 API명 하위에 포함되는 데이터
	 * @param userParam         USER_SN(현재 사용자), USER_UUID(현재 사용자), SIGN_USER_UUID(서명자), privateKey(서명자) 데이터
	 * @return
	 */
	public BlockchainProto.ResponseDTO request (BlockChainApiType blockChainApiType, Map<String, BlockchainProto.Object> dataParam, Map<String, BlockchainProto.Object> userParam) {
		Map<String, BlockchainProto.Object> apiParam = new HashMap<> ();
		apiParam.put ("apiName", BlockchainProto.Object.newBuilder ().setString (blockChainApiType.getApiName ()).build ());
		apiParam.put ("txName", BlockchainProto.Object.newBuilder ().setString (blockChainApiType.getTxName ()).build ());
		apiParam.put ("uri", BlockchainProto.Object.newBuilder ().setString (blockChainApiType.getUri ()).build ());
		apiParam.put ("method", BlockchainProto.Object.newBuilder ().setString (blockChainApiType.getMethod ()).build ());
		apiParam.put ("contentType", BlockchainProto.Object.newBuilder ().setString (blockChainApiType.getContentType ()).build ());
		apiParam.put ("accept", BlockchainProto.Object.newBuilder ().setString (blockChainApiType.getAccept ()).build ());
		return request (apiParam, dataParam, userParam);
	}

	private BlockchainProto.ResponseDTO request (Map<String, BlockchainProto.Object> apiParam, Map<String, BlockchainProto.Object> dataParam, Map<String, BlockchainProto.Object> userParam) {
		BlockchainProto.RequestDTO requestDTO = BlockchainProto.RequestDTO.newBuilder ().putAllApiParam (apiParam).putAllDataParam (dataParam).putAllUserParam (userParam).build ();
		BlockchainProto.ResponseDTO responseDTO = null;
		try {
			if ("/tx".equals (apiParam.get ("uri").getString ()) && "POST".equals (apiParam.get ("method").getString ())) {
				responseDTO = blockchainServiceBlockingStub.tx (requestDTO);
			} else {
				responseDTO = blockchainServiceBlockingStub.request (requestDTO);
			}
		} catch (Exception e) {
			String message = "[request] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
			log.error (message);
			responseDTO = BlockchainProto.ResponseDTO.newBuilder ().setStatus (500).setErrMsg (message).build ();
		} finally {
			log.info ("====================================================");
			log.info ("Api Name : {}", apiParam.get ("apiName").getString ());
			log.info ("Tx Name : {}", apiParam.get ("txName").getString ());
			log.info ("ResponseDTO : {}", responseDTO);
			log.info ("====================================================");
		}

		return responseDTO;
	}

	public BlockchainProto.RawResponseDTO request (BlockchainProto.RawRequestDTO rawRequestDTO) {
		BlockchainProto.RawResponseDTO rawResponseDTO = null;

		Map<String, Object> apiParam = null;
		try {
			apiParam = objectMapper.readValue (rawRequestDTO.getApiParam (), Map.class);
			if ("/tx".equals (apiParam.get ("uri")) && "POST".equals (apiParam.get ("method"))) {
				rawResponseDTO = blockchainServiceBlockingStub.rawTx (rawRequestDTO);
			} else {
				rawResponseDTO = blockchainServiceBlockingStub.rawRequest (rawRequestDTO);
			}
		} catch (Exception e) {
			String message = "[rawRequest] error : " + e.getStackTrace ()[0].getLineNumber () + "  line, message : " + e.getMessage ();
			log.error (message);
			rawResponseDTO = BlockchainProto.RawResponseDTO.newBuilder ().setStatus (500).setErrMsg (message).build ();
		} finally {
			log.info ("====================================================");
			assert apiParam != null;
			log.info ("Api Name : {}", apiParam.get ("apiName"));
			log.info ("Tx Name : {}", apiParam.get ("txName"));
			log.info ("ResponseDTO : {}", rawResponseDTO);
			log.info ("====================================================");
		}

		return rawResponseDTO;
	}

	/**
	 * 다중 트랜잭션 요청
	 *
	 * @param multipleRawRequestDTO
	 * @return
	 */
	public BlockchainProto.MultipleRawResponseDTO request (BlockchainProto.MultipleRawRequestDTO multipleRawRequestDTO) {
		BlockchainProto.MultipleRawResponseDTO multipleRawResponseDTO = null;
		try {
			multipleRawResponseDTO = blockchainServiceBlockingStub.multipleRawTx (multipleRawRequestDTO);
		} catch (Exception e) {
			String message = "[multipleRawTransaction] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
			log.error (message);
			multipleRawResponseDTO = BlockchainProto.MultipleRawResponseDTO.newBuilder ().setStatus (500).setErrMsg (message).build ();
		} finally {
			log.info ("====================================================");
			log.info ("Api Name : Multiple API");
			log.info ("Tx Name : Multiple Tx");
			assert multipleRawResponseDTO != null;
			log.info ("Status : {}", multipleRawResponseDTO.getStatus ());
			log.info ("Tx param : {}", multipleRawResponseDTO.getTxParamData ());
			log.info ("====================================================");
		}

		return multipleRawResponseDTO;
	}

	public BlockchainProto.HealthResponseDTO health (BlockChainApiType blockChainApiType) {
		BlockchainProto.HealthRequestDTO requestDTO = BlockchainProto.HealthRequestDTO.newBuilder ().build ();
		BlockchainProto.HealthResponseDTO responseDTO = null;
		try {
			responseDTO = blockchainServiceBlockingStub.health (requestDTO);
		} catch (Exception e) {
			String message = "[health] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
			log.error (message);
			responseDTO = BlockchainProto.HealthResponseDTO.newBuilder ().setResult ("not ok").build ();
		} finally {
			log.info ("====================================================");
			log.info ("Api Name : Health Check");
			log.info ("ResponseDTO : {}", responseDTO);
			log.info ("====================================================");
		}

		return responseDTO;
	}

	public Map<String, Object> apiTypeToMap (BlockChainApiType blockChainApiType) {
		Map<String, Object> apiParam = new HashMap<> ();
		apiParam.put ("apiName", blockChainApiType.getApiName ());
		apiParam.put ("txName", blockChainApiType.getTxName ());
		apiParam.put ("uri", blockChainApiType.getUri ());
		apiParam.put ("method", blockChainApiType.getMethod ());
		apiParam.put ("contentType", blockChainApiType.getContentType ());
		apiParam.put ("accept", blockChainApiType.getAccept ());
		return apiParam;
	}

	public String localDateTimeToString (LocalDateTime localDateTime) {
		return localDateTime.format (dateTimeFormatter).concat ("Z");
	}
}
