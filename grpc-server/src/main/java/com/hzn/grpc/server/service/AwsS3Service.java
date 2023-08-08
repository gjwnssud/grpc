package com.hzn.grpc.server.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.amazonaws.services.kms.model.EncryptionAlgorithmSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service("AwsS3Service")
public class AwsS3Service {
	/**
	 * AWS KMS Encrypt / Decrypt
	 **/
	@Value("${cloud.aws.kms.encrypt.keyId}")
	private String encKeyId;

	public String awsKmsEncrypt (String plaintext) {
		try {
			AWSKMS kmsClient = AWSKMSClientBuilder.standard ().withRegion (Regions.AP_NORTHEAST_2).build ();
			EncryptRequest request = new EncryptRequest ();
			request.withKeyId (encKeyId);
			request.withPlaintext (ByteBuffer.wrap (plaintext.getBytes (StandardCharsets.UTF_8)));
			request.withEncryptionAlgorithm (EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256);
			EncryptResult result = kmsClient.encrypt (request);
			ByteBuffer ciphertextBlob = result.getCiphertextBlob ();
			return new String (Base64.encodeBase64 (ciphertextBlob.array ()));
		} catch (Exception e) {
			log.error ("encrypt fail: " + e.getMessage ());
		}
		return null;
	}

	public String awsKmsDecrypt (String encriptedText) {
		try {
			AWSKMS kmsClient = AWSKMSClientBuilder.standard ().withRegion (Regions.AP_NORTHEAST_2).build ();
			DecryptRequest request = new DecryptRequest ();
			request.withCiphertextBlob (ByteBuffer.wrap (Base64.decodeBase64 (encriptedText)));
			request.withKeyId (encKeyId);
			request.withEncryptionAlgorithm (EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256);
			ByteBuffer plainText = kmsClient.decrypt (request).getPlaintext ();
			return new String (plainText.array ());
		} catch (Exception e) {
			log.error ("decrypt fail: " + e.getMessage ());
		}
		return null;
	}
}
