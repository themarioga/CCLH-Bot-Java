package org.themarioga.cclh.bot.util;

import java.text.MessageFormat;

public class StringUtils {

	private StringUtils() {
		throw new UnsupportedOperationException();
	}

	public static boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		try {
			Long.parseLong(strNum);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public static String formatMessage(String text, Object... vars) {
		return MessageFormat.format(text, vars);
	}

	public static String booleanToSpanish(boolean bool) {
		return bool ? "Si" : "No";
	}

}
