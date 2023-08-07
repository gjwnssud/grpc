package com.hzn.grpc.server.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * @author : hzn
 * @date : 2023/04/25
 * @description :
 */
public class ConvertUtil {

	public static BigDecimal toBigDecimal (String number) {
		return toBigDecimal (new BigDecimal (number), 0);
	}

	public static BigDecimal toBigDecimal (String number, int scale) {
		return toBigDecimal (new BigDecimal (number), scale);
	}

	public static BigDecimal toBigDecimal (BigDecimal number, int scale) {
		return number.divide (getDecimalPoint (scale), scale, RoundingMode.CEILING);
	}

	public static BigInteger toBigInteger (String number) {
		return toBigInteger (new BigDecimal (number), 0);
	}

	public static BigInteger toBigInteger (String number, int scale) {
		return toBigInteger (new BigDecimal (number), scale);
	}

	public static BigInteger toBigInteger (BigDecimal number) {
		return toBigInteger (number, 0);
	}

	public static BigInteger toBigInteger (BigDecimal number, int scale) {
		return number.multiply (getDecimalPoint (scale)).toBigInteger ();
	}

	public static BigDecimal getDecimalPoint (int scale) {
		return BigDecimal.TEN.pow (scale);
	}
}
