package statuscheck.util;

public final class UrlCredentialSanitizer {

	private UrlCredentialSanitizer() {
	}

	public static String removeCredentials(String value) {
		if (value == null || value.isBlank())
			return value;

		int authorityStart = value.indexOf("://");
		authorityStart = authorityStart < 0 ? 0 : authorityStart + 3;

		int authorityEnd = value.length();
		for (int i = authorityStart; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '/' || c == '?' || c == '#') {
				authorityEnd = i;
				break;
			}
		}

		String authority = value.substring(authorityStart, authorityEnd);
		int at = authority.lastIndexOf('@');

		if (at < 0)
			return value;

		return value.substring(0, authorityStart)
				+ authority.substring(at + 1)
				+ value.substring(authorityEnd);
	}
}