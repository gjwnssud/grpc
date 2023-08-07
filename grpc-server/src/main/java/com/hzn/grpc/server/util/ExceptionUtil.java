package com.hzn.grpc.server.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.util.ObjectUtils;

/**
 * @author : hzn
 * @date : 2023/03/22
 * @description :
 */
@Slf4j
public class ExceptionUtil {
	public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

	public static String generateDefaultMessage (Throwable t) {
		return ObjectUtils.isEmpty (t.getMessage ()) ? INTERNAL_SERVER_ERROR : t.getMessage ();
	}

	public static void printExceptionMessage (Throwable t) {
		printExceptionMessage (t, null, null);
	}

	public static void printExceptionMessage (Throwable t, String invokedMethodName) {
		printExceptionMessage (t, invokedMethodName, null);
	}

	public static void printExceptionMessage (Throwable t, Logger logger) {
		printExceptionMessage (t, null, logger);
	}

	public static void printExceptionMessage (Throwable t, String invokedMethodName, Logger llogger) {
		StackTraceElement[] stackTraceElements = t.getStackTrace ();
		if (!ObjectUtils.isEmpty (stackTraceElements)) {
			Logger logger = llogger == null ? log : llogger;
			StackTraceElement stackTraceElement = stackTraceElements[0];
			if (!ObjectUtils.isEmpty (invokedMethodName)) logger.error ("====================== [{}] start. ======================", invokedMethodName);
			logger.error ("ClassName : {}", stackTraceElement.getClassName ());
			logger.error ("MethodName : {}", stackTraceElement.getMethodName ());
			logger.error ("LineNumber : {}", stackTraceElement.getLineNumber ());
			logger.error ("Message : {}", t.getMessage ());
			if (!ObjectUtils.isEmpty (invokedMethodName)) logger.error ("====================== [{}] end. ======================", invokedMethodName);
		}
	}
}
