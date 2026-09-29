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

		String s = raw.replace("\uFEFF", "").trim();

		String scheme = "";
		if (s.regionMatches(true, 0, "http://", 0, 7)) {
			scheme = "http://";
			s = s.substring(7);
		} else if (s.regionMatches(true, 0, "https://", 0, 8)) {
			scheme = "https://";
			s = s.substring(8);
		}

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
		if (at >= 0) {
			String userInfo = authority.substring(0, at + 1);
			String hostPort = authority.substring(at + 1);
			authority = userInfo + hostPort.toLowerCase(Locale.ROOT);
		} else {
			authority = authority.toLowerCase(Locale.ROOT);
		}

		if (rest.equals("/"))
			rest = "";

		if (scheme.isEmpty())
			scheme = "https://";

		return scheme + authority + rest;
	}

	public static boolean isValidURL(String normalized) {
		if (normalized == null || normalized.isBlank()
				|| normalized.chars().anyMatch(Character::isWhitespace)) {
			return false;
		}

		URI uri;
		try {
			uri = URI.create(normalized);
		} catch (IllegalArgumentException e) {
			return false;
		}

		String scheme = uri.getScheme();
		if (scheme == null
				|| (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
			return false;
		}

		int authorityStart = normalized.indexOf("://") + 3;

		int end = normalized.length();
		for (int i = authorityStart; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			if (c == '/' || c == '?' || c == '#') {
				end = i;
				break;
			}
		}

		String authority = normalized.substring(authorityStart, end);
		String hostPort = authority.substring(authority.lastIndexOf('@') + 1);

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
			return true;
		}

		try {
			host = IDN.toASCII(host);
		} catch (IllegalArgumentException e) {
			return false;
		}

		return host.length() <= 253 && HOST.matcher(host).matches();
	}
}