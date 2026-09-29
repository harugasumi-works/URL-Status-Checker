package statuscheck.util;

import java.net.IDN;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public class ContentParser {

	private static final Pattern HOST = Pattern.compile(
			"[A-Za-z0-9_](?:[A-Za-z0-9_-]{0,61}[A-Za-z0-9_])?(?:\\.[A-Za-z0-9_](?:[A-Za-z0-9_-]{0,61}[A-Za-z0-9_])?)*\\.?");

	public static String normalize(String raw) {
		if (raw == null)
			return "";
		String s = raw.replace("\uFEFF", "").trim().replaceFirst("(?i)^https?://", "");
		int end = s.length();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '/' || c == '?' || c == '#') {
				end = i;
				break;
			}
		}
		String authority = s.substring(0, end);
		String rest = s.substring(end);
		int at = authority.lastIndexOf('@');
		authority = authority.substring(0, at + 1) + authority.substring(at + 1).toLowerCase(Locale.ROOT);
		if (rest.equals("/"))
			rest = "";
		return authority + rest;
	}

	public static boolean isValidURL(String normalized) {
		if (normalized == null || normalized.isBlank() || normalized.chars().anyMatch(Character::isWhitespace)) {
			return false;
		}

		try {
			URI.create("https://" + normalized);
		} catch (IllegalArgumentException e) {
			return false;
		}
		int end = normalized.length();
		for (int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			if (c == '/' || c == '?' || c == '#') {
				end = i;
				break;
			}
		}
		String hostPort = normalized.substring(0, end);
		hostPort = hostPort.substring(hostPort.lastIndexOf('@') + 1);
		String host = hostPort;
		int colon = hostPort.lastIndexOf(':');
		if (colon >= 0 && !hostPort.endsWith("]")) {
			host = hostPort.substring(0, colon);
			String port = hostPort.substring(colon + 1);
			if (!port.isEmpty() && !port.chars().allMatch(Character::isDigit)) {
				return false;
			}
		}
		if (host.isEmpty()) {
			return false;
		}
		if (host.startsWith("[") && host.endsWith("]")) {
			return true; // IPv6 literal, syntax already checked by URI
		}
		try {
			host = IDN.toASCII(host);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return host.length() <= 253 && HOST.matcher(host).matches();
	}

	public static boolean isPlainHttp(String raw) {
		if (raw == null)
			return false;
		return raw.replace("\uFEFF", "").stripLeading().regionMatches(true, 0, "http://", 0, 7);
	}

}
