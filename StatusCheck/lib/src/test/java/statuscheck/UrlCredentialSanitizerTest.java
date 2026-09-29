package statuscheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import statuscheck.util.UrlCredentialSanitizer;

public class UrlCredentialSanitizerTest {

	@Test
	public void removesCredentialsFromHttpsUrl() {
		assertEquals(
				"https://example.com/path",
				UrlCredentialSanitizer.removeCredentials("https://user:pass@example.com/path"));
	}

	@Test
	public void removesCredentialsFromHttpUrl() {
		assertEquals(
				"http://example.com/path",
				UrlCredentialSanitizer.removeCredentials("http://user:pass@example.com/path"));
	}

	@Test
	public void preservesQueryAndFragment() {
		assertEquals(
				"https://example.com/path?q=1#top",
				UrlCredentialSanitizer.removeCredentials("https://user:pass@example.com/path?q=1#top"));
	}

	@Test
	public void urlWithoutCredentialsIsUnchanged() {
		assertEquals(
				"https://example.com/path",
				UrlCredentialSanitizer.removeCredentials("https://example.com/path"));
	}

	@Test
	public void nullAndBlankValuesAreReturnedAsIs() {
		assertEquals(null, UrlCredentialSanitizer.removeCredentials(null));
		assertEquals(" ", UrlCredentialSanitizer.removeCredentials(" "));
	}

	@Test
	public void lastAtSignSeparatesCredentialsFromHost() {
		assertEquals(
				"https://example.com/path",
				UrlCredentialSanitizer.removeCredentials("https://user@name:pass@example.com/path"));
	}
}
