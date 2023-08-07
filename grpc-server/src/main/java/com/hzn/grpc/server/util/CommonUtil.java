package com.hzn.grpc.server.util;

import org.springframework.util.ObjectUtils;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CommonUtil {
	private static final CommonUtil INSTANCE = new CommonUtil ();

	public static CommonUtil getInstance () {
		return INSTANCE;
	}

	public static String setRandomCode (int len) {
		char[] charaters = {
				'A',
				'B',
				'C',
				'D',
				'E',
				'F',
				'G',
				'H',
				'I',
				'J',
				'K',
				'L',
				'M',
				'N',
				'O',
				'P',
				'Q',
				'R',
				'S',
				'T',
				'U',
				'V',
				'W',
				'X',
				'Y',
				'Z',
				'a',
				'b',
				'c',
				'd',
				'e',
				'f',
				'g',
				'h',
				'i',
				'j',
				'k',
				'l',
				'm',
				'n',
				'o',
				'p',
				'q',
				'r',
				's',
				't',
				'u',
				'v',
				'w',
				'x',
				'y',
				'z',
				'0',
				'1',
				'2',
				'3',
				'4',
				'5',
				'6',
				'7',
				'8',
				'9'
		};
		StringBuffer sb = new StringBuffer ();
		Random rn = new Random ();
		for (int i = 0; i < len; i++) {
			sb.append (charaters[rn.nextInt (charaters.length)]);
		}
		return sb.toString ();
	}

	/**
	 * 이메일 @ 앞부분 가져오기
	 **/
	public static String getEmailId (String email) {
		return email.substring (0, email.indexOf ("@"));
	}

	public static String createNumber (int num) {
		char[] charaters = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
		StringBuffer sb = new StringBuffer ();
		Random rn = new Random ();
		for (int i = 0; i < num; i++) {
			sb.append (charaters[rn.nextInt (charaters.length)]);
		}
		return sb.toString ();
	}

	public static String generatorUniqueCode (int userNo) {
		StringBuffer specialSb = new StringBuffer ("!@#$%^&*-=?~");  // 특수문자 모음, {}[] 같은 비호감문자는 뺌
		String userNoText = String.valueOf (userNo);
		String code = "";
		StringBuffer buf = new StringBuffer ();
		for (int i = 0; i < 9 - userNoText.length (); i++) {
			buf.append ((char) ((int) (Math.random () * 26) + 97));
		}
		code = buf + userNoText;
		code = code + specialSb.charAt ((int) (Math.random () * specialSb.length () - 1));
		return code;
	}

	/**
	 * 랜덤 숫자 생성
	 */
	public static String getRandomNumberGen () {
		// dupCd - 1:중복허용 2:중복제거
		int dupCd = 2;
		Random rand = new Random ();
		String numStr = ""; //난수가 저장될 변수
		for (int i = 0; i < 4; i++) { //0~9 까지 난수 생성
			String ran = Integer.toString (rand.nextInt (10));
			if (dupCd == 1) { //중복 허용시 numStr에 append
				numStr += ran;
			} else if (dupCd == 2) { //중복을 허용하지 않을시 중복된 값이 있는지 검사한다
				if (!numStr.contains (ran)) { //중복된 값이 없으면 numStr에 append
					numStr += ran;
				} else { //생성된 난수가 중복되면 루틴을 다시 실행한다
					i -= 1;
				}
			}
		}
		return numStr;
	}

	public static String convertTime (long dateM) {

		Calendar c = Calendar.getInstance ();

		long now = c.getTimeInMillis ();

		long gap = now - dateM;

		String ret = "";

		//        초       분   시
		//        1000    60  60
		gap = (long) (gap / 1000);
		long hour = gap / 3600;
		gap = gap % 3600;
		long min = gap / 60;
		long sec = gap % 60;

		Date date = new Date (dateM);

		//        if(hour > 24){
		//            ret = new SimpleDateFormat("yy.MM.dd").format(date);
		//        }
		//        else if(hour > 0){
		//            ret = hour+"시간 전";
		//        }
		//        else if(min > 0){
		//            ret = min+"분 전";
		//        }
		//        else if(sec > 0){
		//            ret = sec+"초 전";
		//        }
		//        else{
		ret = new SimpleDateFormat ("yy.MM.dd").format (date);
		//        }
		return ret;

	}

	public static String convertTime (LocalDateTime data) {

		long dateM = ZonedDateTime.of (data, ZoneId.systemDefault ()).toInstant ().toEpochMilli ();

		Calendar c = Calendar.getInstance ();

		long now = c.getTimeInMillis ();

		long gap = now - dateM;

		String ret = "";

		//        초       분   시
		//        1000    60  60
		gap = (long) (gap / 1000);
		long hour = gap / 3600;
		gap = gap % 3600;
		long min = gap / 60;
		long sec = gap % 60;

		Date date = new Date (dateM);

		//        if(hour > 24){
		//            ret = new SimpleDateFormat("yy.MM.dd").format(date);
		//        }
		//        else if(hour > 0){
		//            ret = hour+"시간 전";
		//        }
		//        else if(min > 0){
		//            ret = min+"분 전";
		//        }
		//        else if(sec > 0){
		//            ret = sec+"초 전";
		//        }
		//        else{
		ret = new SimpleDateFormat ("yy.MM.dd").format (date);
		//        }
		return ret;

	}

	public static LocalDateTime getCurrentLocalDateTime (String zone) {
		return LocalDateTime.ofInstant (Instant.ofEpochMilli (System.currentTimeMillis ()), ZoneId.of (zone));
	}

	public static int getAmericanAge (String birthDate) {
		LocalDate now = LocalDate.now ();
		LocalDate parsedBirthDate = LocalDate.parse (birthDate, DateTimeFormatter.ofPattern ("yyyy-MM-dd"));

		int americanAge = now.minusYears (parsedBirthDate.getYear ()).getYear (); // (1)

		// (2)
		// 생일이 지났는지 여부를 판단하기 위해 (1)을 입력받은 생년월일의 연도에 더한다.
		// 연도가 같아짐으로 생년월일만 판단할 수 있다!
		if (parsedBirthDate.plusYears (americanAge).isAfter (now)) {
			americanAge = americanAge - 1;
		}

		return americanAge;
	}

	public static String SHA256 (String text) {
		try {
			MessageDigest md = MessageDigest.getInstance ("SHA-256");
			md.update (text.getBytes ());

			return bytesToHex (md.digest ());

		} catch (Exception e) {
			e.printStackTrace ();
		}

		return "";
	}

	private static String bytesToHex (byte[] bytes) {
		StringBuilder builder = new StringBuilder ();
		for (byte b : bytes) {
			builder.append (String.format ("%02x", b));
		}
		return builder.toString ();
	}

	/**
	 * offset, limit 파라미터 Integer 변환
	 *
	 * @param param
	 */
	public static void convertStringToInteger (Map<String, Object> param) {
		String offset = (String) param.get ("offset");
		String limit = (String) param.get ("limit");
		if (!ObjectUtils.isEmpty (offset)) {
			param.put ("offset", Integer.valueOf (offset));
		}
		if (!ObjectUtils.isEmpty (limit)) {
			param.put ("limit", Integer.valueOf (limit));
		}
	}

	/**
	 * 해시태그 보정
	 *
	 * @param map
	 */
	public static void correctionHashTag (Map<String, Object> map) {
		List<String> modifiedHashTagList = new ArrayList<String> ();
		String hashTags = (String) map.get ("COLCT_HASHTAG");
		if (!ObjectUtils.isEmpty (hashTags)) {
			List<String> hashTagList = Arrays.asList (hashTags.split (","));
			hashTagList.stream ().forEach (tag -> {
				tag = tag.trim ();
				if (tag.charAt (0) != '#') {
					tag = "#" + tag;
				}

				modifiedHashTagList.add (tag);
			});

			String[] modifiedHashTagArray = modifiedHashTagList.toArray (new String[modifiedHashTagList.size ()]);
			map.put ("COLCT_HASHTAG", String.join (",", modifiedHashTagArray));
		}
	}

	/**
	 * 가상자산 거래 번호 생성
	 *
	 * @param tradeTyCode
	 * @param localDateTime
	 * @param userSn
	 * @return
	 */
	public static String createCxTradeNo (String tradeTyCode, LocalDateTime localDateTime, String userSn) {
		StringBuilder sb = new StringBuilder ();
		sb.append (tradeTyCode).append (localDateTime.format (DateTimeFormatter.ofPattern ("yyyyMMddHHmmss"))).append ("-");
		Random random = new Random ();
		sb.append (random.nextInt (10000)).append (userSn);
		return sb.toString ();
	}

	public static String padding (String input, int scale) {
		if (input == null) return null;

		String[] temp = input.split ("\\.");
		StringBuilder sb = new StringBuilder (temp[0]);
		if (temp.length > 1) {
			String decimal = temp[1];
			int padCount = scale - decimal.length ();
			if (padCount > 0) {
				sb.append (".").append (decimal);
				for (int i = 0; i < padCount; i++) {
					sb.append (0);
				}
			} else if (padCount < 0) {
				char[] chars = decimal.toCharArray ();
				for (int i = 0; i < scale; i++) {
					sb.append (chars[i]);
				}
			} else {
				sb.append (".").append (decimal);
			}
		} else {
			for (int i = 0; i < scale; i++) {
				sb.append (0);
			}
		}

		String[] temp2 = sb.toString ().split ("\\.");
		if (temp2.length > 1) {
			if (temp2[0].equals ("0")) {
				sb.setLength (0);
				sb.append (temp2[1]);
			}
		}

		return sb.toString ();
	}

	public static String unPadding (String input) {
		if (input == null) return null;

		String[] temp = input.split ("\\.");
		StringBuilder sb = new StringBuilder (temp[0]);
		if (temp.length > 1) {
			String decimal = temp[1].replaceAll ("0", "");
			if (!ObjectUtils.isEmpty (decimal)) sb.append (".").append (decimal);
		}

		return sb.toString ();
	}
}
