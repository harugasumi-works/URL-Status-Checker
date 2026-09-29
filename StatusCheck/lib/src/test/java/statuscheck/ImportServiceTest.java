package statuscheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import statuscheck.service.ImportService;

public class ImportServiceTest {

	@Test
	public void hasValidUrlsAcceptsValidInput() {
		assertTrue(ImportService.hasValidUrls("example.com"));
		assertTrue(ImportService.hasValidUrls("http://example.com"));
	}

	@Test
	public void hasValidUrlsRejectsInvalidInput() {
		assertFalse(ImportService.hasValidUrls("not a url"));
	}

	@Test
	public void reportSkippedCountsInvalidAndDuplicateRows() {
		ImportService.Report report = new ImportService.Report(2, 3, 4, java.util.List.of("bad"));

		assertEquals(7, report.skipped());
	}

	@Test
	public void reportSummaryContainsSkipReasons() {
		ImportService.Report report = new ImportService.Report(2, 3, 4, java.util.List.of("bad"));

		assertEquals("Added 2, skipped 7 (4 invalid, 3 duplicate)", report.summary());
	}

	@Test
	public void reportSummaryContainsAddedOnlyWhenNothingWasSkipped() {
		ImportService.Report report = new ImportService.Report(2, 0, 0, java.util.List.of());

		assertEquals("Added 2", report.summary());
	}
}
