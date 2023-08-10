package com.hzn.grpc.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzn.grpc.server.dto.HttpResponse;
import com.hzn.grpc.server.enums.ApiType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author : hzn
 * @date : 2023/04/13
 * @description :
 */
@Component
@RequiredArgsConstructor
public class HttpUtil {
	private final ObjectMapper objectMapper;

	public HttpResponse<?> send (String domain, ApiType apiType, Object param) {
		return send (domain, apiType, param, null);
	}

	public HttpResponse<?> send (String domain, ApiType apiType, Object param, Map<String, Object> headers) {
		boolean isGet = "GET".equals (apiType.getMethod ());
		HttpURLConnection conn = null;
		try {
			conn = getUrlConnection (domain, apiType, param, headers);
			if (!isGet) {
				try (OutputStream os = conn.getOutputStream ()) {
					byte[] input = objectMapper.writeValueAsString (param).getBytes (StandardCharsets.UTF_8);
					os.write (input, 0, input.length);
				}
			}

			StringBuilder sb = new StringBuilder ();
			int HttpResult = conn.getResponseCode ();
			String line;
			if (HttpResult == HttpURLConnection.HTTP_OK) {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getInputStream (), StandardCharsets.UTF_8))) {
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				}
			} else {
				try (BufferedReader br = new BufferedReader (new InputStreamReader (conn.getErrorStream (), StandardCharsets.UTF_8))) {
					while ((line = br.readLine ()) != null) {
						sb.append (line);
					}
				}
			}
			return new HttpResponse<> (HttpResult, sb.toString ());
		} catch (Exception e) {
			ExceptionUtil.printExceptionMessage (e, "send");
			return new HttpResponse<> (HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage ());
		} finally {
			assert conn != null;
			conn.disconnect ();
		}
	}

	private HttpURLConnection getUrlConnection (String domain, ApiType apiType, Object param, Map<String, Object> headers) throws IOException {
		String uri = apiType.getUri ();
		String method = apiType.getMethod ();
		StringBuilder sb = new StringBuilder (domain);

		if ("GET".equals (method)) {
			sb.append (uri);
			setQueryString (sb, (Map) param);
		} else {
			sb.append (uri);
		}

		URL reqUrl = new URL (sb.toString ());
		HttpURLConnection connection = (HttpURLConnection) reqUrl.openConnection ();
		connection.setRequestMethod (method);
		connection.setRequestProperty ("Content-Type", apiType.getContentType ());
		connection.setRequestProperty ("Accept", apiType.getAccept ());
		if (headers != null) {
			for (String key : headers.keySet ()) {
				connection.setRequestProperty (key, String.valueOf (headers.get (key)));
			}
		}
		if ("POST".equals (method)) {
			connection.setDoOutput (true);
		}
		return connection;
	}

	private void setQueryString (StringBuilder sb, Map<String, Object> param) throws JsonProcessingException {
		boolean isFirst = true;
		for (String key : param.keySet ()) {
			Object value = param.get (key);

			if (ObjectUtils.isEmpty (value)) continue;

			if (isFirst) {
				sb.append ("?");
				isFirst = false;
			} else {
				sb.append ("&");
			}

			sb.append (key).append ("=").append (value);
		}
	}
}


