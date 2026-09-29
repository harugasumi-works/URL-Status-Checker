package statuscheck.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LinkBuilderTest {

	@Test
	void asciiLinksAreLeftUntouched() {
		assertEquals("https://example.com", LinkBuilder.toAsciiHost("https://example.com"));
		assertEquals("http://example.com/path?q=1", LinkBuilder.toAsciiHost("http://example.com/path?q=1"));
		assertEquals("https://[::1]:8080/x", LinkBuilder.toAsciiHost("https://[::1]:8080/x"));
	}

	@Test
	void nonAsciiHostBecomesPunycode() {
		assertEquals("https://xn--wgv71a119e.jp", LinkBuilder.toAsciiHost("https://日本語.jp"));
	}

	@Test
	void onlyTheHostIsConverted() {
		assertEquals("https://xn--wgv71a119e.jp/パス?q=1", LinkBuilder.toAsciiHost("https://日本語.jp/パス?q=1"));
		assertEquals("http://xn--wgv71a119e.jp:8443/x", LinkBuilder.toAsciiHost("http://日本語.jp:8443/x"));
		assertEquals("https://user@xn--wgv71a119e.jp", LinkBuilder.toAsciiHost("https://user@日本語.jp"));
		assertEquals("https://example.com/日本語", LinkBuilder.toAsciiHost("https://example.com/日本語"));
	}

	@Test
	void linksWithoutSchemeAreLeftUntouched() {
		assertEquals("example.com/path?q=1", LinkBuilder.toAsciiHost("example.com/path?q=1"));
		assertEquals("日本語.jp/path", LinkBuilder.toAsciiHost("日本語.jp/path"));
	}

	@Test
	void invalidNonAsciiHostThrowsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> LinkBuilder.toAsciiHost("https://日本語..jp"));
	}

	@Test
	void requestFactoryAddsHttpsByDefault() {
		assertEquals("https", LinkBuilder.requestFactory("example.com").build().uri().getScheme());
		assertEquals("example.com", LinkBuilder.requestFactory("example.com").build().uri().getHost());
	}

	@Test
	void requestFactoryPreservesHttpScheme() {
		assertEquals("http", LinkBuilder.requestFactory("http://example.com").build().uri().getScheme());
		assertEquals("example.com", LinkBuilder.requestFactory("http://example.com").build().uri().getHost());
	}

	@Test
	void requestFactoryConvertsInternationalizedHost() {
		assertEquals("xn--wgv71a119e.jp", LinkBuilder.requestFactory("日本語.jp").build().uri().getHost());
		assertEquals("http", LinkBuilder.requestFactory("http://日本語.jp").build().uri().getScheme());
	}
}
