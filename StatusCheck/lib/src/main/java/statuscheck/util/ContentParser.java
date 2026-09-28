package statuscheck.util;

import java.net.URI;
import java.util.Locale;

public class ContentParser {
	public static String normalize(String raw) {
		if (raw == null)
			return "";
		String s = raw.trim().replaceFirst("(?i)^https?://", "");
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
		if (normalized == null || normalized.isBlank()
				|| normalized.chars().anyMatch(Character::isWhitespace)) {
			return false;
		}

		try {
			URI uri = URI.create("https://" + normalized);
			return "https".equalsIgnoreCase(uri.getScheme())
					&& uri.getHost() != null
					&& !uri.getHost().isBlank();
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
