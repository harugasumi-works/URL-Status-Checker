package statuscheck.service;

import java.io.UncheckedIOException;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import javafx.stage.Window;
import statuscheck.domain.ScanOutput;
import statuscheck.io.ExportFile;
import statuscheck.util.ErrorSpecs;

public final class ExportService {

	public enum Format { JSON, CSV }

	public enum Outcome { NOTHING_TO_EXPORT, EXPORTED, CANCELED, FAILED }

	public record Result(Outcome outcome, String reason) {
		public String message() {
			return switch (outcome) {
			case NOTHING_TO_EXPORT -> "Nothing to export yet. Scan at least one URL first.";
			case EXPORTED -> "Successfully exported";
			case CANCELED -> "Operation canceled";
			case FAILED -> "Operation was interrupted. Reason: " + reason;
			};
		}
	}

	private final Supplier<ScanOutput> source;
	private final Supplier<Window> owner;
	private final BiPredicate<Window, ScanOutput> jsonExporter;
	private final BiPredicate<Window, ScanOutput> csvExporter;

	public ExportService(Supplier<ScanOutput> source, Supplier<Window> owner) {
		this(source, owner, (window, output) -> ExportFile.exportJSON(window, output.json().get()),
				(window, output) -> ExportFile.exportCSV(window, output.csv().get()));
	}

	public ExportService(Supplier<ScanOutput> source, Supplier<Window> owner,
			BiPredicate<Window, ScanOutput> jsonExporter, BiPredicate<Window, ScanOutput> csvExporter) {
		this.source = source;
		this.owner = owner;
		this.jsonExporter = jsonExporter;
		this.csvExporter = csvExporter;
	}

	public Result export(Format format) {
		try {
			ScanOutput output = source.get();
			if (output == null) {
				return new Result(Outcome.NOTHING_TO_EXPORT, null);
			}
			BiPredicate<Window, ScanOutput> exporter = switch (format) {
			case JSON -> jsonExporter;
			case CSV -> csvExporter;
			};
			return new Result(exporter.test(owner.get(), output) ? Outcome.EXPORTED : Outcome.CANCELED, null);
		} catch (Exception e) {
			Exception cause = e instanceof UncheckedIOException u && u.getCause() != null ? u.getCause() : e;
			return new Result(Outcome.FAILED, ErrorSpecs.describe(cause));
		}
	}
}