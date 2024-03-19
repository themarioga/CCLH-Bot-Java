package org.themarioga.cclh.bot.util;

import java.text.MessageFormat;

public class StringUtils {

	private StringUtils() {
		throw new UnsupportedOperationException();
	}

	public static String formatMessage(String text, Object... vars) {
		return MessageFormat.format(text, vars);
	}

}
