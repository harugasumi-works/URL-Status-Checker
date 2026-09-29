package statuscheck.service;

import java.util.function.Predicate;
import java.util.function.Supplier;

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
	private final Predicate<ScanOutput> jsonExporter;
	private final Predicate<ScanOutput> csvExporter;

	public ExportService(Supplier<ScanOutput> source) {
		this(source, output -> ExportFile.exportJSON(output.json().get()),
				output -> ExportFile.exportCSV(output.csv().get()));
	}

	public ExportService(Supplier<ScanOutput> source, Predicate<ScanOutput> jsonExporter,
			Predicate<ScanOutput> csvExporter) {
		this.source = source;
		this.jsonExporter = jsonExporter;
		this.csvExporter = csvExporter;
	}

	public Result export(Format format) {
		try {
			ScanOutput output = source.get();
			if (output == null) {
				return new Result(Outcome.NOTHING_TO_EXPORT, null);
			}
			Predicate<ScanOutput> exporter = switch (format) {
			case JSON -> jsonExporter;
			case CSV -> csvExporter;
			};
			return new Result(exporter.test(output) ? Outcome.EXPORTED : Outcome.CANCELED, null);
		} catch (Exception e) {
			return new Result(Outcome.FAILED, ErrorSpecs.describe(e));
		}
	}
}