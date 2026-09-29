package statuscheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import statuscheck.util.ContentParser;

public class ContentParserTest {

	@Test
	public void nullInputNormalizesToEmpty() {
		assertEquals("", ContentParser.normalize(null));
	}

	@Test
	public void missingSchemeDefaultsToHttps() {
		assertEquals("https://example.com/path", ContentParser.normalize(" example.com/path "));
	}

	@Test
	public void httpSchemeIsPreserved() {
		assertEquals("http://example.com/path", ContentParser.normalize("HTTP://Example.COM/path"));
	}

	@Test
	public void httpsSchemeIsPreserved() {
		assertEquals("https://example.com/path", ContentParser.normalize("HTTPS://Example.COM/path"));
	}

	@Test
	public void bomIsRemoved() {
		assertEquals("https://example.com", ContentParser.normalize("\uFEFFexample.com"));
	}

	@Test
	public void rootPathIsRemoved() {
		assertEquals("https://example.com", ContentParser.normalize("example.com/"));
	}

	@Test
	public void hostIsLowercasedWithoutChangingPathQueryOrFragment() {
		assertEquals(
				"https://example.com/Path?q=1#Fragment",
				ContentParser.normalize("Example.COM/Path?q=1#Fragment"));
	}

	@Test
	public void userInfoIsPreservedWhileHostIsLowercased() {
		assertEquals(
				"https://User:Pass@example.com/path",
				ContentParser.normalize("https://User:Pass@Example.COM/path"));
	}

	@Test
	public void httpAndHttpsUrlsAreValid() {
		assertTrue(ContentParser.isValidURL("http://example.com"));
		assertTrue(ContentParser.isValidURL("https://example.com"));
	}

	@Test
	public void internationalizedAndUnderscoredHostsAreValid() {
		assertTrue(ContentParser.isValidURL("https://日本語.jp"));
		assertTrue(ContentParser.isValidURL("https://_service.example.com"));
	}

	@Test
	public void ipv6AndPortAreValid() {
		assertTrue(ContentParser.isValidURL("http://[::1]:8080/path"));
	}

	@Test
	public void whitespaceAndUnsupportedSchemesAreInvalid() {
		assertFalse(ContentParser.isValidURL("https://example.com/path with space"));
		assertFalse(ContentParser.isValidURL("ftp://example.com"));
	}

	@Test
	public void missingHostIsInvalid() {
		assertFalse(ContentParser.isValidURL("https://"));
	}

	@Test
	public void nonNumericPortIsInvalid() {
		assertFalse(ContentParser.isValidURL("https://example.com:abc"));
	}

	@Test
	public void malformedHostIsInvalid() {
		assertFalse(ContentParser.isValidURL("https://example..com"));
		assertFalse(ContentParser.isValidURL("https://exa mple.com"));
	}
}
