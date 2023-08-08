package com.hzn.grpc.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzn.grpc.inf.BlockchainProto;
import com.hzn.grpc.inf.BlockchainServiceGrpc;
import com.hzn.grpc.server.dao.BlockChainDao;
import com.hzn.grpc.server.dto.KeyPair;
import com.hzn.grpc.server.exception.GrpcServerException;
import com.hzn.grpc.server.util.CommonUtil;
import com.hzn.grpc.server.util.ConvertUtil;
import com.hzn.grpc.server.util.UidUtil;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.web3j.crypto.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.hzn.grpc.inf.BlockchainProto.Object.ValueCase.AMOUNT;

/**
 * @author : hzn
 * @fileName : BlockChainService
 * @date : 2022-07-07
 * @description :
 * ===========================================================
 * DATE                 AUTHOR                NOTE
 * -----------------------------------------------------------
 * 2022-07-07             hzn               최초 생성
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class BlockChainService extends BlockchainServiceGrpc.BlockchainServiceImplBase {
	private final BlockChainDao blockChainDao;
	private final AwsS3Service  awsS3Service;
	private final PnAuthService pnAuthService;
	@Value("${default.blockchain.api.url}")
	private       String        BLC_API_URL;
	private final int           responseBodyLimit = 21800;

	private final DateTimeFormatter formatter    = DateTimeFormatter.ofPattern ("yyyy-MM-dd'T'HH:mm:ss");
	private final ObjectMapper      objectMapper;
	private final int               NETWORKID    = 1000;
	private final long              decimalPoint = 1000000000000000000L;
	private final String            amountPrefix = "amount:";

	@Override
	public void health (BlockchainProto.HealthRequestDTO request, StreamObserver<BlockchainProto.HealthResponseDTO> responseObserver) {
		BlockchainProto.HealthResponseDTO responseDTO = BlockchainProto.HealthResponseDTO.newBuilder ().setResult ("ok").build ();
		responseObserver.onNext (responseDTO);
		responseObserver.onCompleted ();
	}

	@Override
	public void tx (BlockchainProto.RequestDTO request, StreamObserver<BlockchainProto.ResponseDTO> responseObserver) {
		BlockchainProto.ResponseDTO responseDTO = null;
		Map<String, BlockchainProto.Object> apiParam = new HashMap<> (request.getApiParamMap ());
		Map<String, BlockchainProto.Object> dataParam = new HashMap<> (correctAmountForProtoMap (request.getDataParamMap ())); // amount 보정
		Map<String, BlockchainProto.Object> userParam = new HashMap<> (request.getUserParamMap ());

		try {
			responseDTO = getTxHash (apiParam, dataParam, userParam);
			if (responseDTO.getStatus () == 200) {
				responseDTO = getSignedTxHash (responseDTO, apiParam, userParam);
			}
		} catch (Exception e) {
			String message = "[tx] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
			log.error (message);
			responseDTO = BlockchainProto.ResponseDTO.newBuilder ().setStatus (500).setErrMsg (message).build ();
		} finally {
			responseObserver.onNext (responseDTO);
			responseObserver.onCompleted ();
		}
	}

	@Override
	public void request (BlockchainProto.RequestDTO request, StreamObserver<BlockchainProto.ResponseDTO> responseObserver) {
		BlockchainProto.ResponseDTO responseDTO = null;

		try {
			Map<String, BlockchainProto.Object> apiParam = new HashMap<> (request.getApiParamMap ());
			Map<String, BlockchainProto.Object> dataParam = ObjectUtils.isEmpty (request.getDataParamMap ()) ? new HashMap<> () : new HashMap<> (request.getDataParamMap ());
			Map<String, BlockchainProto.Object> userParam = ObjectUtils.isEmpty (request.getUserParamMap ()) ? new HashMap<> () : new HashMap<> (request.getUserParamMap ());
			String id = ObjectUtils.isEmpty (dataParam.get ("pathVariable")) ? "" : "/" + dataParam.get ("pathVariable").getString ();
			apiParam.put ("uri", BlockchainProto.Object.newBuilder ().setString (apiParam.get ("uri").getString () + id).build ());
			dataParam.remove ("pathVariable");
			responseDTO = request (apiParam, dataParam, userParam);

			String apiName = apiParam.get ("apiName").getString ();
			switch (apiName) {
				case "balance":
					responseDTO = responseDTO.toBuilder ().setBalanceDTO (toBalanceDTO (responseDTO)).build ();
					break;
				case "nft-balance":
					responseDTO = responseDTO.toBuilder ().setNftBalanceDTO (toNftBalanceDTO (responseDTO)).build ();
					break;
			}
		} catch (Exception e) {
			String message = "[request] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
			log.error (message);
			responseDTO = BlockchainProto.ResponseDTO.newBuilder ().setStatus (500).setErrMsg (message).build ();
		} finally {
			responseObserver.onNext (responseDTO);
			responseObserver.onCompleted ();
		}
	}

	@Override
	public void multipleRawTx (BlockchainProto.MultipleRawRequestDTO multipleRawRequestDTO, StreamObserver<BlockchainProto.MultipleRawResponseDTO> responseObserver) {
		BlockchainProto.MultipleRawResponseDTO multipleRawResponseDTO = null;
		try {
			multipleRawResponseDTO = getTxHash (multipleRawRequestDTO);
			if (multipleRawResponseDTO.getStatus () == 200) {
				multipleRawResponseDTO = getSignedTxHash (multipleRawResponseDTO, multipleRawRequestDTO);
			}
		} catch (Exception e) {
			String message = "[multipleRawTx] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
			log.error (message);
			multipleRawResponseDTO = BlockchainProto.MultipleRawResponseDTO.newBuilder ().setStatus (500).setErrMsg (message).build ();
		} finally {
			responseObserver.onNext (multipleRawResponseDTO);
			responseObserver.onCompleted ();
		}
	}

	private BlockchainProto.ResponseDTO getTxHash (Map<String, BlockchainProto.Object> apiParam, Map<String, BlockchainProto.Object> dataParam, Map<String, BlockchainProto.Object> userParam) throws JsonProcessingException {
		String txName = apiParam.get ("txName").getString ();
		String apiName = apiParam.get ("apiName").getString ();
		String createdAt = LocalDateTime.parse (dataParam.get ("createdAt").getString ()).format (formatter) + "Z";
		dataParam.put ("createdAt", BlockchainProto.Object.newBuilder ().setString (createdAt).build ());
		apiParam.put ("uri", BlockchainProto.Object.newBuilder ().setString ("/txhash").build ());

		List<BlockchainProto.Map> request = new ArrayList<> ();
		BlockchainProto.Map.Builder requestFormBuilder = BlockchainProto.Map.newBuilder ();
		BlockchainProto.Map.Builder txFormBuilder = BlockchainProto.Map.newBuilder ();
		BlockchainProto.Map.Builder apiFormBuilder = BlockchainProto.Map.newBuilder ();
		apiFormBuilder.putData ("networkId", BlockchainProto.Object.newBuilder ().setLong (NETWORKID).build ());
		apiFormBuilder.putAllData (dataParam);
		txFormBuilder.putData (apiName, BlockchainProto.Object.newBuilder ().setMap (apiFormBuilder.build ()).build ());
		requestFormBuilder.putData (txName, BlockchainProto.Object.newBuilder ().setMap (txFormBuilder.build ()).build ());
		request.add (requestFormBuilder.build ());

		BlockchainProto.ResponseDTO responseDTO = transaction (apiParam, request);
		BlockchainProto.TxDTO txDTO = responseDTO.getTxDTO ();

		Map<String, Object> txParam = new HashMap<> ();
		txParam.put ("TX_KND_NM", txName);
		txParam.put ("TX_NM", apiName);
		txParam.put ("NTWRK_ID", NETWORKID);
		txParam.put ("ACNT_ID", ObjectUtils.isEmpty (userParam.get ("USER_UUID")) ? "" : userParam.get ("USER_UUID").getString ());
		txParam.put ("GUARDIAN_ACNT_ID", ObjectUtils.isEmpty (dataParam.get ("guardian")) ? "" : dataParam.get ("guardian").getString ());
		txParam.put ("TXHASH_ID", txDTO.getTxHash ());
		txParam.put ("REQUST_URL", BLC_API_URL + "/txhash");
		txParam.put ("REQUST_HTTP_METHOD", "POST");
		txParam.put ("REQUST_PARAMTR", objectMapper.writeValueAsString (protoMapListToMapList (request)));
		txParam.put ("HTTP_RESULT_CODE", responseDTO.getStatus ());
		txParam.put ("SIGN_TXHASH", txDTO.getTxHash ());
		txParam.put ("USER_SN", ObjectUtils.isEmpty (userParam.get ("USER_SN")) ? null : userParam.get ("USER_SN").getString ());
		txParam.put ("RSPNS_RESULT", responseDTO.getResponseBody ());

		log.info ("==================== txHash log ====================");
		log.info ("Api Name : {}", apiParam.get ("apiName").getString ());
		log.info ("Tx Name : {}", apiParam.get ("txName").getString ());
		log.info ("response : " + responseDTO);
		log.info ("txLogData : " + txParam);
		log.info ("====================================================");

		blockChainDao.insertTxHashLog (txParam);

		txDTO = txDTO.toBuilder ().putAllTxHashData (mapToProtoMap (txParam)).build ();
		return responseDTO.toBuilder ().setTxDTO (txDTO).build ();
	}

	private BlockchainProto.ResponseDTO getSignedTxHash (BlockchainProto.ResponseDTO txHash, Map<String, BlockchainProto.Object> apiParam, Map<String, BlockchainProto.Object> userParam) throws JsonProcessingException {
		return getSignedTxHash (txHash, apiParam, userParam, true);
	}

	private BlockchainProto.ResponseDTO getSignedTxHash (BlockchainProto.ResponseDTO txHash, Map<String, BlockchainProto.Object> apiParam, Map<String, BlockchainProto.Object> userParam, boolean save) throws JsonProcessingException {
		String signUserUuid = userParam.get ("SIGN_USER_UUID").getString ();
		String uri = "/tx";
		apiParam.put ("uri", BlockchainProto.Object.newBuilder ().setString (uri).build ());

		List<Map<String, Object>> requestArray = getSignedRequestParameter (txHash, userParam);

		Map<String, Object> txLogData = protoMapToMap (txHash.getTxDTO ().getTxHashDataMap ());
		txLogData.put ("REQUST_URL", BLC_API_URL + uri);
		txLogData.put ("REQUST_PARAMTR", objectMapper.writeValueAsString (requestArray));
		txLogData.put ("ACNT_ID", signUserUuid);

		BlockchainProto.ResponseDTO response = BlockchainProto.ResponseDTO.getDefaultInstance ();
		if (save) {
			response = transaction (apiParam, mapListToProtoMapList (requestArray));
			txLogData.put ("HTTP_RESULT_CODE", response.getStatus ());
			txLogData.put ("RSPNS_RESULT", response.getResponseBody ());
		}
		log.info ("================== signed tx log ==================");
		log.info ("Api Name : {}", apiParam.get ("apiName").getString ());
		log.info ("Tx Name : {}", apiParam.get ("txName").getString ());
		log.info ("response : " + response);
		log.info ("txLogData : " + txLogData);
		log.info ("===================================================");

		blockChainDao.insertTxHashLog (txLogData);

		BlockchainProto.TxDTO data = response.getTxDTO ().toBuilder ().putAllTxHashData (mapToProtoMap (txLogData)).build ();
		response = response.toBuilder ().setTxDTO (data.toBuilder ().setTxHashSn (txLogData.get ("TXHASH_SN").toString ()).build ()).build ();

		return response;
	}

	private BlockchainProto.ResponseDTO transaction (Map<String, BlockchainProto.Object> apiParam, List<BlockchainProto.Map> param) {
		BlockchainProto.ResponseDTO.Builder responseDTOBuilder = BlockchainProto.ResponseDTO.newBuilder ();
		BlockchainProto.TxDTO.Builder txDTOBuilder = BlockchainProto.TxDTO.newBuilder ();
		txDTOBuilder.addAllParam (param);

		boolean isGet = "GET".equals (apiParam.get ("method").toString ().toUpperCase ());
		HttpURLConnection conn = null;
		try {
			conn = getUrlConnection (apiParam, param);

			if (!isGet) {
				try (OutputStream os = conn.getOutputStream ()) {
					byte[] input = objectMapper.writeValueAsString (protoMapListToMapList (param)).getBytes (StandardCharsets.UTF_8);
					os.write (input, 0, input.length);
				} catch (Exception e) {
					String message = "[transaction] writeValueAsString error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
			}

			StringBuilder sb = new StringBuilder ();
			int HttpResult = conn.getResponseCode ();
			if (HttpResult == HttpURLConnection.HTTP_OK) {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getInputStream (), StandardCharsets.UTF_8))) {
					String line = null;
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				} catch (Exception e) {
					String message = "[transaction] inputStream error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
				Object obj = objectMapper.readValue (sb.toString (), Object.class);
				if (obj instanceof List) txDTOBuilder.setTxHash ((String) ((List) obj).get (0));
				else if (obj instanceof Map) txDTOBuilder.setTxHash ((String) ((Map) obj).get ("response"));
			} else {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getErrorStream (), StandardCharsets.UTF_8))) {
					String line = null;
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				} catch (Exception e) {
					String message = "[transaction] errorStream error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
				responseDTOBuilder.setErrMsg (sb.toString ());
			}

			responseDTOBuilder.setStatus (HttpResult).setResponseBody (sb.toString ());
			conn.disconnect ();
		} catch (Exception e) {
			String message = ObjectUtils.isEmpty (e.getMessage ()) ? "[transaction] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage () : e.getMessage ();
			log.error (message);
			responseDTOBuilder.setStatus (500).setErrMsg (message);
		}

		return responseDTOBuilder.setTxDTO (txDTOBuilder.build ()).build ();
	}

	private BlockchainProto.ResponseDTO request (Map<String, BlockchainProto.Object> apiParam, Map<String, BlockchainProto.Object> dataParam, Map<String, BlockchainProto.Object> userParam) {
		BlockchainProto.ResponseDTO.Builder responseBuilder = BlockchainProto.ResponseDTO.newBuilder ();
		BlockchainProto.ResponseDTO responseDTO = null;
		boolean isGet = "GET".equals (apiParam.get ("method").getString ().toUpperCase ());
		HttpURLConnection conn;
		try {
			conn = getUrlConnection (apiParam, dataParam);

			if (!isGet) {
				try (OutputStream os = conn.getOutputStream ()) {
					byte[] input = objectMapper.writeValueAsString (protoMapToMap (dataParam)).getBytes (StandardCharsets.UTF_8);
					os.write (input, 0, input.length);
				} catch (Exception e) {
					String message = "[request] writeValueAsString error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
			}

			StringBuilder sb = new StringBuilder ();
			int HttpResult = conn.getResponseCode ();
			if (HttpResult == HttpURLConnection.HTTP_OK) {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getInputStream (), StandardCharsets.UTF_8))) {
					String line = null;
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				} catch (Exception e) {
					String message = "[request] InputStream error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
			} else {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getErrorStream (), StandardCharsets.UTF_8))) {
					String line = null;
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				} catch (Exception e) {
					String message = "[request] ErrorStream error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
				responseBuilder.setErrMsg (sb.toString ());
			}
			responseBuilder.setStatus (HttpResult);
			responseBuilder.setResponseBody (sb.toString ());
			conn.disconnect ();

			responseDTO = responseBuilder.build ();
		} catch (Exception e) {
			String message = ObjectUtils.isEmpty (e.getMessage ()) ? "[request] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage () : e.getMessage ();
			log.error (message);
			responseDTO = responseBuilder.setStatus (500).setErrMsg (message).build ();
		} finally {
			log.info ("=================== request log ===================");
			log.info ("Api Name : {}", apiParam.get ("apiName").getString ());
			log.info ("Tx Name : {}", apiParam.get ("txName").getString ());
			log.info ("response : " + responseDTO);
			log.info ("===================================================");

			responseDTO = addLog (responseDTO, apiParam, userParam);
		}

		return responseDTO;
	}

	private BlockchainProto.MultipleRawResponseDTO getTxHash (BlockchainProto.MultipleRawRequestDTO multipleRawRequestDTO) throws JsonProcessingException {
		BlockchainProto.MultipleRawResponseDTO.Builder multipleRawResponseBuilder = BlockchainProto.MultipleRawResponseDTO.newBuilder ();
		multipleRawResponseBuilder.setStatus (200);

		for (BlockchainProto.RawRequestDTO rawRequestDTO : multipleRawRequestDTO.getRawRequestDTOList ()) {
			Map<String, Object> apiParam = objectMapper.readValue (rawRequestDTO.getApiParam (), Map.class);
			apiParam.put ("uri", "/txhash");
			String apiName = apiParam.get ("apiName").toString ();
			String txName = apiParam.get ("txName").toString ();

			Map<String, Object> dataParam = objectMapper.readValue (rawRequestDTO.getDataParam (), Map.class);
			String createdAt = LocalDateTime.parse (dataParam.get ("createdAt").toString ()).format (formatter) + "Z";
			dataParam.put ("createdAt", createdAt);
			correctAmountValue (dataParam); // amount 보정
			Map<String, Object> userParam = objectMapper.readValue (rawRequestDTO.getUserParam (), Map.class);

			List<Map<String, Object>> request = new ArrayList<> ();
			Map<String, Object> requestForm = new HashMap<> ();
			Map<String, Object> txForm = new HashMap<> ();
			Map<String, Object> apiForm = new HashMap<> ();
			apiForm.put ("networkId", NETWORKID);
			apiForm.putAll (dataParam);
			txForm.put (apiName, apiForm);
			requestForm.put (txName, txForm);
			request.add (requestForm);

			BlockchainProto.RawResponseDTO rawResponseDTO = rawTransaction (apiParam, request);
			Map<String, Object> txParam = new HashMap<> ();
			txParam.put ("TX_KND_NM", txName);
			txParam.put ("TX_NM", apiName);
			txParam.put ("NTWRK_ID", NETWORKID);
			txParam.put ("ACNT_ID", ObjectUtils.isEmpty (userParam.get ("USER_UUID")) ? "" : userParam.get ("USER_UUID"));
			txParam.put ("GUARDIAN_ACNT_ID", ObjectUtils.isEmpty (dataParam.get ("guardian")) ? "" : dataParam.get ("guardian"));
			txParam.put ("TXHASH_ID", rawResponseDTO.getTxHash ());
			txParam.put ("REQUST_URL", BLC_API_URL + "/txhash");
			txParam.put ("REQUST_HTTP_METHOD", "POST");
			txParam.put ("REQUST_PARAMTR", objectMapper.writeValueAsString (request));
			txParam.put ("HTTP_RESULT_CODE", rawResponseDTO.getStatus ());
			txParam.put ("SIGN_TXHASH", rawResponseDTO.getTxHash ());
			txParam.put ("USER_SN", ObjectUtils.isEmpty (userParam.get ("USER_SN")) ? null : userParam.get ("USER_SN"));
			txParam.put ("RSPNS_RESULT", rawResponseDTO.getResponseBody ());

			log.info ("==================== txHash log ====================");
			log.info ("Api Name : {}", apiName);
			log.info ("Tx Name : {}", txName);
			log.info ("response : " + rawResponseDTO);
			log.info ("txLogData : " + txParam);
			log.info ("====================================================");

			blockChainDao.insertTxHashLog (txParam);

			rawResponseDTO = rawResponseDTO.toBuilder ().setTxHashLogData (objectMapper.writeValueAsString (txParam)).build ();
			if (rawResponseDTO.getStatus () != 200) multipleRawResponseBuilder.setStatus (rawResponseDTO.getStatus ());
			multipleRawResponseBuilder.addRawResponseDTO (rawResponseDTO);
		}

		return multipleRawResponseBuilder.build ();
	}

	private BlockchainProto.MultipleRawResponseDTO getSignedTxHash (BlockchainProto.MultipleRawResponseDTO signedRawResponseDTO, BlockchainProto.MultipleRawRequestDTO multipleRawRequestDTO) throws JsonProcessingException {
		BlockchainProto.MultipleRawResponseDTO.Builder multipleRawResponseDTOBuilder = BlockchainProto.MultipleRawResponseDTO.newBuilder ();

		List<BlockchainProto.RawResponseDTO> modifiedRawResponseDTOList = new ArrayList<> ();
		List<BlockchainProto.RawResponseDTO> rawResponseDTOList = signedRawResponseDTO.getRawResponseDTOList ();
		List<BlockchainProto.RawRequestDTO> rawRequestDTOList = multipleRawRequestDTO.getRawRequestDTOList ();
		List<Map<String, Object>> multipleRequestArray = new ArrayList<> ();
		for (int i = 0, n = rawResponseDTOList.size (); i < n; i++) { // 트랜잭션 별 사인 진행
			BlockchainProto.RawResponseDTO rawResponseDTO = rawResponseDTOList.get (i);
			BlockchainProto.RawRequestDTO rawRequestDTO = rawRequestDTOList.get (i);
			Map<String, Object> apiParam = objectMapper.readValue (rawRequestDTO.getApiParam (), Map.class);
			Map<String, Object> userParam = objectMapper.readValue (rawRequestDTO.getUserParam (), Map.class);

			String signUserUuid = userParam.get ("SIGN_USER_UUID").toString ();
			String uri = "/tx";
			apiParam.put ("uri", uri);

			List<Map<String, Object>> requestArray = getSignedRawRequestParameter (rawResponseDTO, userParam);

			Map<String, Object> txHashLogData = objectMapper.readValue (rawResponseDTO.getTxHashLogData (), Map.class);
			txHashLogData.put ("REQUST_URL", BLC_API_URL + uri);
			txHashLogData.put ("REQUST_PARAMTR", objectMapper.writeValueAsString (requestArray));
			txHashLogData.put ("ACNT_ID", signUserUuid);

			multipleRequestArray.add (requestArray.get (0));
			modifiedRawResponseDTOList.add (rawResponseDTO.toBuilder ().setTxHashLogData (objectMapper.writeValueAsString (txHashLogData)).build ());
		}

		// multiple tx 전송
		Map<String, Object> multipleApiParam = new HashMap<> ();
		multipleApiParam.put ("txName", "MultipleTx");
		multipleApiParam.put ("apiName", "Multiple API");
		multipleApiParam.put ("uri", "/tx");
		multipleApiParam.put ("method", "POST");
		multipleApiParam.put ("contentType", "application/json;charset=UTF-8");
		multipleApiParam.put ("accept", "application/json;charset=UTF-8");
		BlockchainProto.RawResponseDTO responseDTO = rawTransaction (multipleApiParam, multipleRequestArray);
		String responseBody = responseDTO.getResponseBody ();
		multipleRawResponseDTOBuilder.setStatus (responseDTO.getStatus ());
		multipleRawResponseDTOBuilder.setResponseBody (responseBody);
		multipleRawResponseDTOBuilder.setTxParamData (objectMapper.writeValueAsString (multipleRequestArray));

		log.info ("================== multiple tx log ==================");
		log.info ("Api Name : {}", multipleApiParam.get ("apiName"));
		log.info ("Tx Name : {}", multipleApiParam.get ("txName"));
		log.info ("RequestBody : {}", multipleRawResponseDTOBuilder.getTxParamData ());
		log.info ("ResponseBody : {}", responseBody);
		log.info ("===================================================");

		List<String> txHashList = new ArrayList<> ();
		if (responseDTO.getStatus () == 200) {
			txHashList.addAll (objectMapper.readValue (responseBody, List.class));
		} else {
			multipleRawResponseDTOBuilder.setErrMsg (responseDTO.getErrMsg ());
		}

		// txHashLog 등록
		Map<String, Object> apiParam = null;
		for (int i = 0, n = modifiedRawResponseDTOList.size (); i < n; i++) {
			BlockchainProto.RawResponseDTO rawResponseDTO = modifiedRawResponseDTOList.get (i);
			BlockchainProto.RawRequestDTO rawRequestDTO = rawRequestDTOList.get (i);
			Map<String, Object> txHashLogData = objectMapper.readValue (rawResponseDTO.getTxHashLogData (), Map.class);
			String response = responseBody;
			if (responseDTO.getStatus () == 200) response = "[" + txHashList.get (i) + "]";
			txHashLogData.put ("HTTP_RESULT_CODE", responseDTO.getStatus ());
			txHashLogData.put ("RSPNS_RESULT", response);

			apiParam = objectMapper.readValue (rawRequestDTO.getApiParam (), Map.class);
			log.info ("================== signed tx log ==================");
			log.info ("Api Name : {}", apiParam.get ("apiName"));
			log.info ("Tx Name : {}", apiParam.get ("txName"));
			log.info ("response : " + response);
			log.info ("txLogData : " + txHashLogData);
			log.info ("===================================================");

			blockChainDao.insertTxHashLog (txHashLogData);

			rawResponseDTO = rawResponseDTO.toBuilder ().setTxHashLogData (objectMapper.writeValueAsString (txHashLogData)).setTxHash (txHashLogData.get ("SIGN_TXHASH").toString ())
					.setTxHashSn (Long.parseLong (String.valueOf (txHashLogData.get ("TXHASH_SN")))).build ();
			multipleRawResponseDTOBuilder.addRawResponseDTO (rawResponseDTO);
		}

		return multipleRawResponseDTOBuilder.build ();
	}

	private BlockchainProto.RawResponseDTO rawTransaction (Map<String, Object> apiParam, Object param) throws JsonProcessingException {
		BlockchainProto.RawResponseDTO.Builder rawResponseBuilder = BlockchainProto.RawResponseDTO.newBuilder ();
		rawResponseBuilder.setTxParamData (objectMapper.writeValueAsString (param));

		boolean isGet = "GET".equalsIgnoreCase (apiParam.get ("method").toString ());
		HttpURLConnection conn = null;
		try {
			conn = getRawUrlConnection (apiParam, param);

			if (!isGet) {
				try (OutputStream os = conn.getOutputStream ()) {
					byte[] input = objectMapper.writeValueAsString (param).getBytes (StandardCharsets.UTF_8);
					os.write (input, 0, input.length);
				} catch (Exception e) {
					String message = "[rawTransaction] writeValueAsString error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
			}

			StringBuilder sb = new StringBuilder ();
			int HttpResult = conn.getResponseCode ();
			if (HttpResult == HttpURLConnection.HTTP_OK) {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getInputStream (), StandardCharsets.UTF_8))) {
					String line = null;
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				} catch (Exception e) {
					String message = "[rawTransaction] InputStream error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
				Object object = objectMapper.readValue (sb.toString (), Object.class);
				if (object instanceof List) {
					List list = (List) object;
					if (!ObjectUtils.isEmpty (list) && list.size () < 2) {
						rawResponseBuilder.setTxHash (String.valueOf (list.get (0)));
					}
				}
			} else {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getErrorStream (), StandardCharsets.UTF_8))) {
					String line = null;
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				} catch (Exception e) {
					String message = "[rawTransaction] ErrorStream error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
				rawResponseBuilder.setErrMsg (sb.toString ());
			}

			rawResponseBuilder.setStatus (HttpResult).setResponseBody (sb.toString ());
		} catch (Exception e) {
			String message = ObjectUtils.isEmpty (e.getMessage ()) ? "[rawTransaction] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage () : e.getMessage ();
			log.error (message);
			rawResponseBuilder.setStatus (500).setErrMsg (message);
		} finally {
			conn.disconnect ();
		}

		return rawResponseBuilder.build ();
	}

	@Override
	public void rawTx (BlockchainProto.RawRequestDTO request, StreamObserver<BlockchainProto.RawResponseDTO> responseObserver) {
		BlockchainProto.RawResponseDTO rawResponseDTO = null;

		try {
			Map<String, Object> apiParam = new HashMap<> (objectMapper.readValue (request.getApiParam (), Map.class));
			Map<String, Object> dataParam = new HashMap<> (objectMapper.readValue (request.getDataParam (), Map.class));
			correctAmountValue (dataParam); // amount 보정
			Map<String, Object> userParam = new HashMap<> (objectMapper.readValue (request.getUserParam (), Map.class));

			rawResponseDTO = getRawTxHash (apiParam, dataParam, userParam);
			if (rawResponseDTO.getStatus () == 200) {
				rawResponseDTO = getRawSignedTxHash (rawResponseDTO, apiParam, userParam);
			}
		} catch (Exception e) {
			String message = "[rawTx] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
			log.error (message);
			rawResponseDTO = BlockchainProto.RawResponseDTO.newBuilder ().setStatus (500).setErrMsg (message).build ();
		} finally {
			responseObserver.onNext (rawResponseDTO);
			responseObserver.onCompleted ();
		}
	}

	@Override
	public void rawRequest (BlockchainProto.RawRequestDTO request, StreamObserver<BlockchainProto.RawResponseDTO> responseObserver) {
		BlockchainProto.RawResponseDTO rawResponseDTO = null;

		try {
			Map<String, Object> apiParam = new HashMap<> (objectMapper.readValue (request.getApiParam (), Map.class));
			Map<String, Object> dataParam = ObjectUtils.isEmpty (request.getDataParam ()) ? new HashMap<> () : new HashMap<> (objectMapper.readValue (request.getDataParam (), Map.class));
			Map<String, Object> userParam = ObjectUtils.isEmpty (request.getUserParam ()) ? new HashMap<> () : new HashMap<> (objectMapper.readValue (request.getUserParam (), Map.class));
			String id = ObjectUtils.isEmpty (dataParam.get ("pathVariable")) ? "" : "/" + dataParam.get ("pathVariable").toString ();
			apiParam.put ("uri", BlockchainProto.Object.newBuilder ().setString (apiParam.get ("uri").toString () + id).build ());
			dataParam.remove ("pathVariable");
			rawResponseDTO = rawRequest (apiParam, dataParam, userParam);
		} catch (Exception e) {
			String message = "[rawRequest] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
			log.error (message);
			rawResponseDTO = BlockchainProto.RawResponseDTO.newBuilder ().setStatus (500).setErrMsg (message).build ();
		} finally {
			responseObserver.onNext (rawResponseDTO);
			responseObserver.onCompleted ();
		}
	}

	private BlockchainProto.RawResponseDTO getRawTxHash (Map<String, Object> apiParam, Map<String, Object> dataParam, Map<String, Object> userParam) throws JsonProcessingException {
		String txName = apiParam.get ("txName").toString ();
		String apiName = apiParam.get ("apiName").toString ();
		String createdAt = LocalDateTime.parse (dataParam.get ("createdAt").toString ()).format (formatter) + "Z";
		dataParam.put ("createdAt", createdAt);
		apiParam.put ("uri", "/txhash");

		List<Map<String, Object>> request = new ArrayList<> ();
		Map<String, Object> requestForm = new HashMap<> ();
		Map<String, Object> txForm = new HashMap<> ();
		Map<String, Object> apiForm = new HashMap<> ();
		apiForm.put ("networkId", NETWORKID);
		apiForm.putAll (dataParam);
		txForm.put (apiName, apiForm);
		requestForm.put (txName, txForm);
		request.add (requestForm);

		BlockchainProto.RawResponseDTO rawResponseDTO = rawTransaction (apiParam, request);

		Map<String, Object> txParam = new HashMap<> ();
		txParam.put ("TX_KND_NM", txName);
		txParam.put ("TX_NM", apiName);
		txParam.put ("NTWRK_ID", NETWORKID);
		txParam.put ("ACNT_ID", ObjectUtils.isEmpty (userParam.get ("USER_UUID")) ? "" : userParam.get ("USER_UUID").toString ());
		txParam.put ("GUARDIAN_ACNT_ID", ObjectUtils.isEmpty (dataParam.get ("guardian")) ? "" : dataParam.get ("guardian").toString ());
		txParam.put ("TXHASH_ID", rawResponseDTO.getTxHash ());
		txParam.put ("REQUST_URL", BLC_API_URL + "/txhash");
		txParam.put ("REQUST_HTTP_METHOD", "POST");
		txParam.put ("REQUST_PARAMTR", objectMapper.writeValueAsString (request));
		txParam.put ("HTTP_RESULT_CODE", rawResponseDTO.getStatus ());
		txParam.put ("SIGN_TXHASH", rawResponseDTO.getTxHash ());
		txParam.put ("USER_SN", ObjectUtils.isEmpty (userParam.get ("USER_SN")) ? null : userParam.get ("USER_SN").toString ());
		txParam.put ("RSPNS_RESULT", rawResponseDTO.getResponseBody ());

		log.info ("==================== txHash log ====================");
		log.info ("Api Name : {}", apiParam.get ("apiName").toString ());
		log.info ("Tx Name : {}", apiParam.get ("txName").toString ());
		log.info ("response : " + rawResponseDTO);
		log.info ("txLogData : " + txParam);
		log.info ("====================================================");

		blockChainDao.insertTxHashLog (txParam);
		return rawResponseDTO.toBuilder ().setTxHashLogData (objectMapper.writeValueAsString (txParam)).build ();
	}

	private BlockchainProto.RawResponseDTO getRawSignedTxHash (BlockchainProto.RawResponseDTO txHash, Map<String, Object> apiParam, Map<String, Object> userParam) throws JsonProcessingException {
		return getRawSignedTxHash (txHash, apiParam, userParam, true);
	}

	private BlockchainProto.RawResponseDTO getRawSignedTxHash (BlockchainProto.RawResponseDTO txHash, Map<String, Object> apiParam, Map<String, Object> userParam, boolean save) throws JsonProcessingException {
		String signUserUuid = userParam.get ("SIGN_USER_UUID").toString ();
		String uri = "/tx";
		apiParam.put ("uri", uri);

		List<Map<String, Object>> requestArray = getSignedRawRequestParameter (txHash, userParam);

		Map<String, Object> txLogData = objectMapper.readValue (txHash.getTxHashLogData (), Map.class);
		txLogData.put ("REQUST_URL", BLC_API_URL + uri);
		txLogData.put ("REQUST_PARAMTR", objectMapper.writeValueAsString (requestArray));
		txLogData.put ("ACNT_ID", signUserUuid);

		BlockchainProto.RawResponseDTO rawResponse = BlockchainProto.RawResponseDTO.getDefaultInstance ();
		if (save) {
			rawResponse = rawTransaction (apiParam, requestArray);
			txLogData.put ("HTTP_RESULT_CODE", rawResponse.getStatus ());
			txLogData.put ("RSPNS_RESULT", rawResponse.getResponseBody ());
		}
		log.info ("================== signed tx log ==================");
		log.info ("Api Name : {}", apiParam.get ("apiName").toString ());
		log.info ("Tx Name : {}", apiParam.get ("txName").toString ());
		log.info ("response : " + rawResponse);
		log.info ("txLogData : " + txLogData);
		log.info ("===================================================");

		blockChainDao.insertTxHashLog (txLogData);

		return rawResponse.toBuilder ().setTxHashLogData (objectMapper.writeValueAsString (txLogData)).setTxHashSn (Long.valueOf (txLogData.get ("TXHASH_SN").toString ())).build ();
	}

	private BlockchainProto.RawResponseDTO rawRequest (Map<String, Object> apiParam, Map<String, Object> dataParam, Map<String, Object> userParam) {
		BlockchainProto.RawResponseDTO.Builder rawResponseBuilder = BlockchainProto.RawResponseDTO.newBuilder ();
		BlockchainProto.RawResponseDTO rawResponseDTO = null;
		boolean isGet = "GET".equalsIgnoreCase (apiParam.get ("method").toString ());
		HttpURLConnection conn;
		try {
			conn = getRawUrlConnection (apiParam, dataParam);

			if (!isGet) {
				try (OutputStream os = conn.getOutputStream ()) {
					byte[] input = objectMapper.writeValueAsString (dataParam).getBytes (StandardCharsets.UTF_8);
					os.write (input, 0, input.length);
				} catch (Exception e) {
					String message = "[rawRequest] writeValueAsString error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
			}

			StringBuilder sb = new StringBuilder ();
			int HttpResult = conn.getResponseCode ();
			if (HttpResult == HttpURLConnection.HTTP_OK) {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getInputStream (), StandardCharsets.UTF_8))) {
					String line;
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				} catch (Exception e) {
					String message = "[rawRequest] InputStream error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
			} else {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getErrorStream (), StandardCharsets.UTF_8))) {
					String line = null;
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				} catch (Exception e) {
					String message = "[rawRequest] ErrorStream error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage ();
					throw new GrpcServerException (message);
				}
				rawResponseBuilder.setErrMsg (sb.toString ());
			}
			rawResponseBuilder.setStatus (HttpResult);
			rawResponseBuilder.setResponseBody (sb.toString ());
			conn.disconnect ();

			rawResponseDTO = rawResponseBuilder.build ();
		} catch (Exception e) {
			String message = ObjectUtils.isEmpty (e.getMessage ()) ? "[rawRequest] error : " + e.getStackTrace ()[0].getLineNumber () + " line, message : " + e.getMessage () : e.getMessage ();
			log.error (message);
			rawResponseDTO = rawResponseBuilder.setStatus (500).setErrMsg (message).build ();
		} finally {
			log.info ("=================== request log ===================");
			log.info ("Api Name : {}", apiParam.get ("apiName").toString ());
			log.info ("Tx Name : {}", apiParam.get ("txName").toString ());
			log.info ("response : " + rawResponseDTO);
			log.info ("===================================================");

			rawResponseDTO = addRawLog (rawResponseDTO, apiParam, userParam);
		}

		return rawResponseDTO;
	}

	private ECKeyPair getECKeyPair (String privateKey) {
		BigInteger privateKeyInBT = new BigInteger (awsS3Service.awsKmsDecrypt (privateKey), 16);
		//		BigInteger privateKeyInBT = new BigInteger ("93385cf4046c44307c73b4b378fe62631a200a25d1f452b4593e354e22b1c5d9", 16);
		ECKeyPair aPair = ECKeyPair.create (privateKeyInBT);
		return aPair;
	}

	private BlockchainProto.ResponseDTO addLog (BlockchainProto.ResponseDTO responseDTO, Map<String, BlockchainProto.Object> apiParam, Map<String, BlockchainProto.Object> userParam) {
		String responseBody = responseDTO.getResponseBody ();
		if (responseBody.getBytes (StandardCharsets.UTF_8).length > responseBodyLimit) { // MySQL TEXT 타입 용량 제한
			responseBody = responseBody.substring (0, responseBodyLimit);
		}

		String apiName = apiParam.get ("apiName").getString ();
		Map<String, Object> txLogData = new HashMap<> ();
		txLogData.put ("TX_KND_NM", apiName);
		txLogData.put ("TX_NM", apiName);
		txLogData.put ("NTWRK_ID", NETWORKID);
		txLogData.put ("ACNT_ID", ObjectUtils.isEmpty (userParam.get ("USER_UUID")) ? null : userParam.get ("USER_UUID").getString ());
		txLogData.put ("REQUST_HTTP_METHOD", apiParam.get ("method").getString ());
		txLogData.put ("USER_SN", ObjectUtils.isEmpty (userParam.get ("USER_SN")) ? null : userParam.get ("USER_SN").getString ());
		txLogData.put ("REQUST_URL", apiParam.get ("uri").getString ());
		txLogData.put ("HTTP_RESULT_CODE", responseDTO.getStatus ());
		txLogData.put ("RSPNS_RESULT", responseBody);
		blockChainDao.insertTxHashLog (txLogData);
		// TXHASH_SN 저장
		return responseDTO.toBuilder ().setTxDTO (responseDTO.getTxDTO ().toBuilder ().setTxHashSn (String.valueOf (txLogData.get ("TXHASH_SN"))).build ()).build ();
	}

	private BlockchainProto.RawResponseDTO addRawLog (BlockchainProto.RawResponseDTO rawResponseDTO, Map<String, Object> apiParam, Map<String, Object> userParam) {
		String responseBody = rawResponseDTO.getResponseBody ();
		if (responseBody.getBytes (StandardCharsets.UTF_8).length > responseBodyLimit) { // MySQL TEXT 타입 용량 제한
			responseBody = responseBody.substring (0, responseBodyLimit);
		}

		String apiName = apiParam.get ("apiName").toString ();
		Map<String, Object> txLogData = new HashMap<> ();
		txLogData.put ("TX_KND_NM", apiName);
		txLogData.put ("TX_NM", apiName);
		txLogData.put ("NTWRK_ID", NETWORKID);
		txLogData.put ("ACNT_ID", ObjectUtils.isEmpty (userParam.get ("USER_UUID")) ? null : userParam.get ("USER_UUID").toString ());
		txLogData.put ("REQUST_HTTP_METHOD", apiParam.get ("method").toString ());
		txLogData.put ("USER_SN", ObjectUtils.isEmpty (userParam.get ("USER_SN")) ? null : userParam.get ("USER_SN").toString ());
		txLogData.put ("REQUST_URL", apiParam.get ("uri").toString ());
		txLogData.put ("HTTP_RESULT_CODE", rawResponseDTO.getStatus ());
		txLogData.put ("RSPNS_RESULT", responseBody);
		blockChainDao.insertTxHashLog (txLogData);
		// TXHASH_SN 저장
		return rawResponseDTO.toBuilder ().setTxHashSn (Long.parseLong ((txLogData.get ("TXHASH_SN")).toString ())).build ();
	}

	private HttpURLConnection getUrlConnection (Map<String, BlockchainProto.Object> apiParam, Object param) throws IOException {
		StringBuilder sb = new StringBuilder (BLC_API_URL + apiParam.get ("uri").getString ());
		if ("GET".equals (apiParam.get ("method").getString ().toUpperCase ())) {
			boolean isFirst = true;
			if (param instanceof List) {
				List<BlockchainProto.Map> paramList = (List) param;
				paramList.stream ().forEach (p -> {
					setQueryString (p.getDataMap (), sb, isFirst);
				});
			} else if (param instanceof Map) {
				Map<String, BlockchainProto.Object> paramMap = (Map) param;
				setQueryString (paramMap, sb, isFirst);
			}
		}

		URL reqUrl = new URL (sb.toString ());
		HttpURLConnection connection = (HttpURLConnection) reqUrl.openConnection ();
		connection.setRequestMethod (apiParam.get ("method").getString ());
		connection.setRequestProperty ("Content-Type", apiParam.get ("contentType").getString ());
		connection.setRequestProperty ("Accept", apiParam.get ("accept").getString ());
		if ("POST".equals (apiParam.get ("method").getString ().toUpperCase ())) {
			connection.setDoOutput (true);
		}
		return connection;
	}

	private HttpURLConnection getRawUrlConnection (Map<String, Object> apiParam, Object param) throws IOException {
		StringBuilder sb = new StringBuilder (BLC_API_URL + apiParam.get ("uri").toString ());
		if ("GET".equalsIgnoreCase (apiParam.get ("method").toString ())) {
			boolean isFirst = true;
			if (param instanceof List) {
				List<Map<String, Object>> paramList = (List) param;
				paramList.stream ().forEach (p -> {
					setQueryString2 (p, sb, isFirst);
				});
			} else if (param instanceof Map) {
				Map<String, Object> paramMap = (Map) param;
				setQueryString2 (paramMap, sb, isFirst);
			}
		}

		URL reqUrl = new URL (sb.toString ());
		HttpURLConnection connection = (HttpURLConnection) reqUrl.openConnection ();
		connection.setRequestMethod (apiParam.get ("method").toString ());
		connection.setRequestProperty ("Content-Type", apiParam.get ("contentType").toString ());
		connection.setRequestProperty ("Accept", apiParam.get ("accept").toString ());
		if ("POST".equalsIgnoreCase (apiParam.get ("method").toString ())) {
			connection.setDoOutput (true);
		}
		return connection;
	}

	private void setQueryString (Map<String, BlockchainProto.Object> param, StringBuilder sb, boolean isFirst) {
		for (String key : param.keySet ()) {
			if (isFirst) {
				sb.append ("?");
				isFirst = false;
			} else {
				sb.append ("&");
			}
			BlockchainProto.Object object = param.get (key);
			String value = null;
			if (object.getValueCase ().getNumber () == AMOUNT.getNumber ()) {
				//				value = CommonUtil.padding (object.getAmount (), 18).replace (".", "");
				value = ConvertUtil.toBigInteger (object.getAmount (), 18).toString ();
			} else {
				value = object.getString ();
			}
			sb.append (key).append ("=").append (value);
		}
	}

	private void setQueryString2 (Map<String, Object> param, StringBuilder sb, boolean isFirst) {
		for (String key : param.keySet ()) {
			if (isFirst) {
				sb.append ("?");
				isFirst = false;
			} else {
				sb.append ("&");
			}
			Object obj = param.get (key);
			String value = null;
			if (obj instanceof Number) {
				//				value = CommonUtil.padding (String.valueOf (obj), 18).replace (".", "");
				value = ConvertUtil.toBigInteger (String.valueOf (obj), 18).toString ();
			} else {
				value = obj.toString ();
			}
			sb.append (key).append ("=").append (value);
		}
	}

	private void correctAmountValue (Object obj) {
		if (obj instanceof Map) {
			Map<String, Object> param = (Map) obj;
			for (String key : param.keySet ()) {
				Object obj2 = param.get (key);
				if (obj2 instanceof Map || obj2 instanceof List) {
					correctAmountValue (obj2);
				} else if (obj2 instanceof String) {
					String value = (String) obj2;
					if (value.startsWith (amountPrefix)) {
						//						value = CommonUtil.padding (value.replace (amountPrefix, ""), 18).replace (".", "");
						value = ConvertUtil.toBigInteger (value.replace (amountPrefix, ""), 18).toString ();
						param.put (key, value);
					}
				}
			}
		} else if (obj instanceof List) {
			List<Object> param = (List) obj;
			param.stream ().forEach (obj2 -> {
				if (obj2 instanceof Map || obj2 instanceof List) {
					correctAmountValue (obj2);
				} else if (obj2 instanceof String) {
					String value = (String) obj2;
					if (value.startsWith (amountPrefix)) {
						//						value = CommonUtil.padding (value.replace (amountPrefix, ""), 18).replace (".", "");
						value = ConvertUtil.toBigInteger (value.replace (amountPrefix, ""), 18).toString ();
						param.add (value);
					}
				}
			});
		}
	}

	public BlockchainProto.BalanceDTO toBalanceDTO (BlockchainProto.ResponseDTO responseDTO) throws JsonProcessingException {
		return toBalanceDTO (responseDTO, "LM");
	}

	public BlockchainProto.BalanceDTO toBalanceDTO (BlockchainProto.ResponseDTO responseDTO, String type) throws JsonProcessingException {
		BlockchainProto.BalanceDTO.Builder builder = BlockchainProto.BalanceDTO.newBuilder ();
		if (responseDTO.getStatus () == 200) {
			Map<String, Object> map = objectMapper.readValue (responseDTO.getResponseBody (), Map.class);
			Map<String, Object> typeMap = (Map) map.get (type);
			if (typeMap != null) {
				//				builder.setTotalAmount (CommonUtil.unPadding (new BigDecimal (String.valueOf (typeMap.get ("totalAmount"))).divide (BigDecimal.valueOf (decimalPoint), 18, RoundingMode.CEILING).toPlainString ()));
				builder.setTotalAmount (ConvertUtil.toBigDecimal (String.valueOf (typeMap.get ("totalAmount")), 18).toPlainString ());
				Map<String, Object> data = (Map) typeMap.get ("unused");
				Set<String> bal = data.keySet ();
				builder.setBalanceList (BlockchainProto.StringList.newBuilder ().addAllData (new ArrayList<> (bal))).build ();
			}
		} else {
			builder.setTotalAmount ("0");
		}
		return builder.build ();
	}

	public BlockchainProto.NftBalanceDTO toNftBalanceDTO (BlockchainProto.ResponseDTO responseDTO) throws JsonProcessingException {
		return toNftBalanceDTO (responseDTO, "LM");
	}

	public BlockchainProto.NftBalanceDTO toNftBalanceDTO (BlockchainProto.ResponseDTO responseDTO, String type) throws JsonProcessingException {
		BlockchainProto.NftBalanceDTO.Builder builder = BlockchainProto.NftBalanceDTO.newBuilder ();
		if (responseDTO.getStatus () == 200) {
			Map<String, Object> tokenIdMap = objectMapper.readValue (responseDTO.getResponseBody (), Map.class);
			Iterator<String> toKenIdItr = tokenIdMap.keySet ().iterator ();
			while (toKenIdItr.hasNext ()) {
				String key = toKenIdItr.next ();
				Map<String, Object> infoMap = (Map) tokenIdMap.get (key);
				if (infoMap != null) {
					builder.putNftBalanceInfo (key, BlockchainProto.Object.newBuilder ().setString ((String) infoMap.get ("txHash")).build ());
				}
			}
		}
		return builder.build ();
	}

	private List<Map<String, Object>> protoMapListToMapList (List<BlockchainProto.Map> sourceParams) {
		List<Map<String, Object>> targetParams = new ArrayList<> ();
		sourceParams.stream ().forEach (m -> {
			Map<String, Object> targetMap = protoMapToMap (m.getDataMap ());
			targetParams.add (targetMap);
		});
		return targetParams;
	}

	private Map<String, Object> protoMapToMap (Map<String, BlockchainProto.Object> sourceParam) {
		Map<String, Object> targetParam = new HashMap<> ();
		Iterator<String> itr = sourceParam.keySet ().iterator ();
		while (itr.hasNext ()) {
			String key = (String) itr.next ();
			BlockchainProto.Object value = sourceParam.get (key);
			switch (value.getValueCase ()) {
				case BOOL:
					targetParam.put (key, value.getBool ());
					break;
				case LIST:
					targetParam.put (key, protoObjectListToObjectList (value.getList ().getDataList ()));
					break;
				case MAP:
					targetParam.put (key, protoMapToMap (value.getMap ().getDataMap ()));
					break;
				case DOUBLE:
					targetParam.put (key, value.getDouble ());
					break;
				case LONG:
					targetParam.put (key, value.getLong ());
					break;
				case STRING:
					targetParam.put (key, value.getString ());
					break;
				case AMOUNT:
					targetParam.put (key, value.getAmount ());
					break;
				case INT:
					targetParam.put (key, value.getInt ());
					break;
				case STRINGLIST:
					targetParam.put (key, new ArrayList<> (value.getStringList ().getDataList ()));
					break;
				case NULL:
				default:
					break;
			}
		}

		return targetParam;
	}

	private List<Object> protoObjectListToObjectList (List<BlockchainProto.Object> sourceList) {
		List<Object> targetList = new ArrayList<> ();

		sourceList.stream ().forEach (value -> {
			switch (value.getValueCase ()) {
				case BOOL:
					targetList.add (value.getBool ());
					break;
				case LIST:
					targetList.add (protoObjectListToObjectList (value.getList ().getDataList ()));
					break;
				case MAP:
					targetList.add (protoMapToMap (value.getMap ().getDataMap ()));
					break;
				case DOUBLE:
					targetList.add (value.getDouble ());
					break;
				case LONG:
					targetList.add (value.getLong ());
					break;
				case STRING:
					targetList.add (value.getString ());
					break;
				case AMOUNT:
					targetList.add (value.getAmount ());
					break;
				case INT:
					targetList.add (value.getInt ());
					break;
				case STRINGLIST:
					targetList.add (new ArrayList<> (value.getStringList ().getDataList ()));
					break;
				case NULL:
				default:
					break;
			}
		});

		return targetList;
	}

	private List<BlockchainProto.Object> objectListToProtoObjectList (List<Object> sourceList) {
		List<BlockchainProto.Object> targetList = new ArrayList<> ();

		sourceList.stream ().forEach (value -> {
			if (value instanceof Boolean) {
				targetList.add (BlockchainProto.Object.newBuilder ().setBool ((Boolean) value).build ());
			} else if (value instanceof List) {
				targetList.add (BlockchainProto.Object.newBuilder ().setList (BlockchainProto.List.newBuilder ().addAllData (objectListToProtoObjectList ((List) value))).build ());
			} else if (value instanceof Double) {
				targetList.add (BlockchainProto.Object.newBuilder ().setDouble ((Double) value).build ());
			} else if (value instanceof Long) {
				targetList.add (BlockchainProto.Object.newBuilder ().setLong ((Long) value).build ());
			} else if (value instanceof String) {
				targetList.add (BlockchainProto.Object.newBuilder ().setString ((String) value).build ());
			} else if (value instanceof Map) {
				targetList.add (BlockchainProto.Object.newBuilder ().setMap (BlockchainProto.Map.newBuilder ().putAllData (mapToProtoMap ((Map) value))).build ());
			} else if (value instanceof Byte) {
				targetList.add (BlockchainProto.Object.newBuilder ().setLong (((Byte) value).longValue ()).build ());
			} else if (value instanceof BigInteger) {
				targetList.add (BlockchainProto.Object.newBuilder ().setAmount (((BigInteger) value).toString ()).build ());
			} else if (value instanceof Integer) {
				targetList.add (BlockchainProto.Object.newBuilder ().setInt ((Integer) value).build ());
			}
		});

		return targetList;
	}

	private List<BlockchainProto.Map> mapListToProtoMapList (List<Map<String, Object>> sourceParams) {
		List<BlockchainProto.Map> targetParams = new ArrayList<> ();
		sourceParams.stream ().forEach (m -> {
			BlockchainProto.Map map = BlockchainProto.Map.newBuilder ().putAllData (mapToProtoMap (m)).build ();
			targetParams.add (map);
		});
		return targetParams;
	}

	private Map<String, BlockchainProto.Object> mapToProtoMap (Map<String, Object> sourceParam) {
		Iterator<String> itr = sourceParam.keySet ().iterator ();
		Map<String, BlockchainProto.Object> targetParam = new HashMap<> ();
		while (itr.hasNext ()) {
			String key = itr.next ();
			Object value = "TXHASH_SN".equals (key) ? Integer.valueOf (sourceParam.get (key).toString ()) : sourceParam.get (key);
			if (value instanceof Boolean) {
				targetParam.put (key, BlockchainProto.Object.newBuilder ().setBool ((Boolean) value).build ());
			} else if (value instanceof List) {
				targetParam.put (key, BlockchainProto.Object.newBuilder ().setList (BlockchainProto.List.newBuilder ().addAllData (objectListToProtoObjectList ((List) value)).build ()).build ());
			} else if (value instanceof Double) {
				targetParam.put (key, BlockchainProto.Object.newBuilder ().setDouble ((Double) value).build ());
			} else if (value instanceof Long) {
				targetParam.put (key, BlockchainProto.Object.newBuilder ().setLong ((Long) value).build ());
			} else if (value instanceof String) {
				targetParam.put (key, BlockchainProto.Object.newBuilder ().setString ((String) value).build ());
			} else if (value instanceof Map) {
				targetParam.put (key, BlockchainProto.Object.newBuilder ().setMap (BlockchainProto.Map.newBuilder ().putAllData (mapToProtoMap ((Map) value))).build ());
			} else if (value instanceof Byte) {
				targetParam.put (key, BlockchainProto.Object.newBuilder ().setLong (((Byte) value).longValue ()).build ());
			} else if (value instanceof BigInteger) {
				targetParam.put (key, BlockchainProto.Object.newBuilder ().setAmount (value.toString ()).build ());
			} else if (value instanceof Integer) {
				targetParam.put (key, BlockchainProto.Object.newBuilder ().setInt ((Integer) value).build ());
			}
		}

		return targetParam;
	}

	public KeyPair getKeyPair () {
		String salt = UidUtil.getID ("") + CommonUtil.setRandomCode (36) + UidUtil.getID ("");
		try {
			SecureRandom seed = new SecureRandom ();
			seed.setSeed (salt.getBytes ());
			ECKeyPair ecKeyPair = Keys.createEcKeyPair (seed);
			BigInteger privateKeyInDec = ecKeyPair.getPrivateKey ();
			BigInteger publicKeyInDec = ecKeyPair.getPublicKey ();
			WalletFile aWallet = Wallet.createLight (salt, ecKeyPair);

			KeyPair response = new KeyPair ();
			response.setPrivateKey (awsS3Service.awsKmsEncrypt (privateKeyInDec.toString (16)));
			response.setPublicKey (publicKeyInDec.toString (16));
			response.setAddress (aWallet.getAddress ());

			return response;

		} catch (Exception e) {
			e.printStackTrace ();
		}

		return new KeyPair ();
	}

	private Map<String, BlockchainProto.Object> correctAmountForProtoMap (Map<String, BlockchainProto.Object> param) {
		Map<String, BlockchainProto.Object> resultMap = new HashMap<> ();
		if (param != null) {
			for (String key : param.keySet ()) {
				BlockchainProto.Object value = param.get (key);
				switch (value.getValueCase ()) {
					case AMOUNT ->
						// DOT(.) 제거 후 저장
							resultMap.put (key, BlockchainProto.Object.newBuilder ().setAmount (ConvertUtil.toBigInteger (value.getAmount (), 18).toString ()).build ());
					case MAP -> resultMap.put (key, BlockchainProto.Object.newBuilder ().setMap (BlockchainProto.Map.newBuilder ().putAllData (correctAmountForProtoMap (value.getMap ().getDataMap ()))).build ());
					case LIST -> resultMap.put (key, BlockchainProto.Object.newBuilder ().setList (BlockchainProto.List.newBuilder ().addAllData (correctAmountForProtoList (value.getList ().getDataList ())).build ()).build ());
					default -> resultMap.put (key, value);
				}
			}
		}

		return resultMap;
	}

	private List<BlockchainProto.Object> correctAmountForProtoList (List<BlockchainProto.Object> param) {
		List<BlockchainProto.Object> resultList = new ArrayList<> ();
		if (param != null) {
			param.forEach (p -> {
				switch (p.getValueCase ()) {
					case AMOUNT ->
						// DOT(.) 제거 후 저장
							resultList.add (BlockchainProto.Object.newBuilder ().setAmount (ConvertUtil.toBigInteger (p.getAmount (), 18).toString ()).build ());
					case MAP -> resultList.add (BlockchainProto.Object.newBuilder ().setMap (BlockchainProto.Map.newBuilder ().putAllData (correctAmountForProtoMap (p.getMap ().getDataMap ()))).build ());
					case LIST -> resultList.add (BlockchainProto.Object.newBuilder ().setList (BlockchainProto.List.newBuilder ().addAllData (correctAmountForProtoList (p.getList ().getDataList ())).build ()).build ());
					default -> resultList.add (p);
				}
			});
		}

		return resultList;
	}

	private String getPlaynommPrivateKey () throws JsonProcessingException {
		Map<String, Object> managerInfo = blockChainDao.selectBlcMngrBass ("playnomm");
		String privateKey1 = awsS3Service.awsKmsDecrypt ((String) managerInfo.get ("BLC_MNGR_PRIVKY"));
		String privateKey2 = awsS3Service.awsKmsDecrypt (pnAuthService.getBlcMngrPrivky ((Integer) managerInfo.get ("BLC_MNGR_BASS_SN")));
		return awsS3Service.awsKmsEncrypt (privateKey1.concat (privateKey2));
	}

	private List<Map<String, Object>> getSignedRequestParameter (BlockchainProto.ResponseDTO responseDTO, Map<String, BlockchainProto.Object> userParam) throws JsonProcessingException {
		String signUserUuid = userParam.get ("SIGN_USER_UUID").getString ();
		String privateKey = "playnomm".equals (signUserUuid) ? getPlaynommPrivateKey () : userParam.get ("privateKey").getString ();
		ECKeyPair keyPair = getECKeyPair (privateKey);
		BigInteger txHashHex = new BigInteger (responseDTO.getTxDTO ().getTxHash (), 16);
		byte[] hexMessageHex = txHashHex.toByteArray ();

		// BigInteger parsing 오류 처리
		if (hexMessageHex.length == 33) {
			byte[] test = new byte[32];
			for (int i = 1; i < hexMessageHex.length; i++) {
				test[i - 1] = hexMessageHex[i];
			}
			hexMessageHex = test;
		}

		Sign.SignatureData signMessageHex = Sign.signMessage (hexMessageHex, keyPair, false);

		byte[] signVHex = signMessageHex.getV ();
		byte[] signRHex = signMessageHex.getR ();
		byte[] signSHex = signMessageHex.getS ();

		Map<String, Object> sig = new HashMap<> ();
		Map<String, Object> sigRequest = new HashMap<> ();

		sigRequest.put ("v", signVHex[0]);
		sigRequest.put ("r", new BigInteger (1, signRHex).toString (16));
		sigRequest.put ("s", new BigInteger (1, signSHex).toString (16));

		sig.put ("sig", sigRequest);
		sig.put ("account", signUserUuid);

		List<Map<String, Object>> requestArray = new ArrayList<> ();
		Map<String, Object> request = new HashMap<> ();

		request.put ("sig", sig);
		request.put ("value", protoMapListToMapList (responseDTO.getTxDTO ().getParamList ()).get (0));

		requestArray.add (request);
		return requestArray;
	}

	private List<Map<String, Object>> getSignedRawRequestParameter (BlockchainProto.RawResponseDTO rawResponseDTO, Map<String, Object> userParam) throws JsonProcessingException {
		String signUserUuid = userParam.get ("SIGN_USER_UUID").toString ();
		String privateKey = "playnomm".equals (signUserUuid) ? getPlaynommPrivateKey () : (String) userParam.get ("privateKey");
		ECKeyPair keyPair = getECKeyPair (privateKey);
		BigInteger txHashHex = new BigInteger (rawResponseDTO.getTxHash (), 16);
		byte[] hexMessageHex = txHashHex.toByteArray ();

		// BigInteger parsing 오류 처리
		if (hexMessageHex.length == 33) {
			byte[] test = new byte[32];
			for (int j = 1; j < hexMessageHex.length; j++) {
				test[j - 1] = hexMessageHex[j];
			}
			hexMessageHex = test;
		}

		Sign.SignatureData signMessageHex = Sign.signMessage (hexMessageHex, keyPair, false);
		byte[] signVHex = signMessageHex.getV ();
		byte[] signRHex = signMessageHex.getR ();
		byte[] signSHex = signMessageHex.getS ();

		Map<String, Object> sig = new HashMap<> ();
		Map<String, Object> sigRequest = new HashMap<> ();

		sigRequest.put ("v", signVHex[0]);
		sigRequest.put ("r", new BigInteger (1, signRHex).toString (16));
		sigRequest.put ("s", new BigInteger (1, signSHex).toString (16));

		sig.put ("sig", sigRequest);
		sig.put ("account", signUserUuid);

		List<Map<String, Object>> requestArray = new ArrayList<> ();
		Map<String, Object> request = new HashMap<> ();
		request.put ("sig", sig);
		request.put ("value", objectMapper.readValue (rawResponseDTO.getTxParamData (), List.class).get (0));
		requestArray.add (request);

		return requestArray;
	}
}
