package statuscheck.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LinkBuilderTest {

	@Test
	void asciiLinksAreLeftUntouched() {
		assertEquals("example.com", LinkBuilder.toAsciiHost("example.com"));
		assertEquals("example.com/path?q=1", LinkBuilder.toAsciiHost("example.com/path?q=1"));
		assertEquals("[::1]:8080/x", LinkBuilder.toAsciiHost("[::1]:8080/x"));
	}

	@Test
	void nonAsciiHostBecomesPunycode() {
		assertEquals("xn--wgv71a119e.jp", LinkBuilder.toAsciiHost("日本語.jp"));
	}

	@Test
	void onlyTheHostIsConverted() {
		assertEquals("xn--wgv71a119e.jp/パス?q=1", LinkBuilder.toAsciiHost("日本語.jp/パス?q=1"));
		assertEquals("xn--wgv71a119e.jp:8443/x", LinkBuilder.toAsciiHost("日本語.jp:8443/x"));
		assertEquals("user@xn--wgv71a119e.jp", LinkBuilder.toAsciiHost("user@日本語.jp"));
		assertEquals("example.com/日本語", LinkBuilder.toAsciiHost("example.com/日本語"));
	}

	@Test
	void invalidNonAsciiHostThrowsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> LinkBuilder.toAsciiHost("日本語..jp"));
	}

	@Test
	void requestFactoryAcceptsInternationalizedHost() {
		assertEquals("xn--wgv71a119e.jp", LinkBuilder.requestFactory("日本語.jp").build().uri().getHost());
	}
}
