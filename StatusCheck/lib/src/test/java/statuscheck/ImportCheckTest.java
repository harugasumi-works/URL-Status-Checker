package statuscheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import statuscheck.util.ImportCheck;

public class ImportCheckTest {

	@Test
	public void parsesValidAndInvalidLines() {
		ImportCheck result = ImportCheck.parseImport(
				"example.com\nhttps://google.com\ninvalid url\n");

		assertEquals(2, result.valid().size());
		assertEquals("https://example.com", result.valid().get(0));
		assertEquals("https://google.com", result.valid().get(1));
		assertEquals(1, result.invalid());
		assertEquals("invalid url", result.rejectedLines().get(0));
	}

	@Test
	public void blankLinesAreIgnored() {
		ImportCheck result = ImportCheck.parseImport("\nexample.com\n\n");

		assertEquals(1, result.valid().size());
		assertEquals(0, result.invalid());
		assertEquals(0, result.rejectedLines().size());
	}

	@Test
	public void httpUrlIsAccepted() {
		ImportCheck result = ImportCheck.parseImport("http://example.com");

		assertEquals(1, result.valid().size());
		assertEquals("http://example.com", result.valid().get(0));
		assertEquals(0, result.invalid());
	}

	@Test
	public void invalidLinesKeepOriginalInput() {
		ImportCheck result = ImportCheck.parseImport(" https://example.com/path with space ");

		assertEquals(0, result.valid().size());
		assertEquals(1, result.invalid());
		assertEquals(" https://example.com/path with space ", result.rejectedLines().get(0));
	}
}
