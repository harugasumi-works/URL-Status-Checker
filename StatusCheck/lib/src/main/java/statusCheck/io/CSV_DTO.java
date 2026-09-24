package statusCheck.io;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import statusCheck.domain.CSV;
import statusCheck.domain.ExecutionResult;
import statusCheck.domain.Fail;
import statusCheck.domain.ScanResult;
import statusCheck.domain.Success;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

public class CSV_DTO {
	
	public static record CsvRow(Instant timeStamp, String url, String outcome, int statusCode, long latencyMs, String reason) {
		
		@SuppressWarnings("preview")
		public static CsvRow row(ScanResult result) {
			String url = result.context().requestedURL();
			return switch (result.outcome()) {
				case Success(var ts, var code, var latency) -> new CsvRow(ts, url, "Success", code, latency, "");
				case Fail(var ts, var code, var reason) -> new CsvRow(ts, url, "Fail", code, 0, reason);
			};
		}
	}
	
	public static CSV convert(ExecutionResult report) {
		CsvSchema schema = CsvSchema.builder()
				.addColumn("timeStamp")
		        .addColumn("url")
		        .addColumn("outcome")
		        .addColumn("statusCode")
		        .addColumn("latencyMs")
		        .addColumn("reason")
		        .setUseHeader(true)
		        .build();
		
		List<CsvRow> rows = Stream.concat(report.successes().stream(), report.failures().stream())
		        .map(data -> CsvRow.row(data))
		        .toList();
		
		CsvMapper csvMapper = new CsvMapper();
		return new CSV(csvMapper.writer(schema).writeValueAsString(rows));
		
	}
	

}
