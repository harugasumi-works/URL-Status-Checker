package statuscheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import statuscheck.domain.JSON;
import statuscheck.domain.CSV;
import statuscheck.domain.ScanOutput;
import statuscheck.service.ExportService;

public class ExportServiceTest {

	private ScanOutput output() {
		return new ScanOutput(
				null,
				LazyConstant.of(() -> new JSON("json")),
				LazyConstant.of(() -> new CSV("csv")));
	}

	@Test
	public void noOutputReturnsNothingToExport() {
		ExportService service = new ExportService(() -> null, () -> null);

		ExportService.Result result = service.export(ExportService.Format.JSON);

		assertEquals(ExportService.Outcome.NOTHING_TO_EXPORT, result.outcome());
	}

	@Test
	public void jsonExporterIsSelected() {
		AtomicInteger jsonCalls = new AtomicInteger();
		AtomicInteger csvCalls = new AtomicInteger();
		ExportService service = new ExportService(
				this::output,
				() -> null,
				(window, value) -> {
					jsonCalls.incrementAndGet();
					return true;
				},
				(window, value) -> {
					csvCalls.incrementAndGet();
					return true;
				});

		ExportService.Result result = service.export(ExportService.Format.JSON);

		assertEquals(ExportService.Outcome.EXPORTED, result.outcome());
		assertEquals(1, jsonCalls.get());
		assertEquals(0, csvCalls.get());
	}

	@Test
	public void csvExporterIsSelected() {
		AtomicInteger jsonCalls = new AtomicInteger();
		AtomicInteger csvCalls = new AtomicInteger();
		ExportService service = new ExportService(
				this::output,
				() -> null,
				(window, value) -> {
					jsonCalls.incrementAndGet();
					return true;
				},
				(window, value) -> {
					csvCalls.incrementAndGet();
					return true;
				});

		ExportService.Result result = service.export(ExportService.Format.CSV);

		assertEquals(ExportService.Outcome.EXPORTED, result.outcome());
		assertEquals(0, jsonCalls.get());
		assertEquals(1, csvCalls.get());
	}

	@Test
	public void falseExporterMeansCanceled() {
		ExportService service = new ExportService(
				this::output,
				() -> null,
				(window, value) -> false,
				(window, value) -> false);

		ExportService.Result result = service.export(ExportService.Format.JSON);

		assertEquals(ExportService.Outcome.CANCELED, result.outcome());
	}

	@Test
	public void exporterExceptionReturnsFailed() {
		ExportService service = new ExportService(
				this::output,
				() -> null,
				(window, value) -> {
					throw new IllegalStateException("export failed");
				},
				(window, value) -> true);

		ExportService.Result result = service.export(ExportService.Format.JSON);

		assertEquals(ExportService.Outcome.FAILED, result.outcome());
		assertEquals("export failed", result.reason());
	}
}
