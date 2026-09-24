package statusCheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import statusCheck.domain.CSV;
import statusCheck.domain.ExecutionResult;
import statusCheck.domain.Fail;
import statusCheck.domain.ScanRequest;
import statusCheck.domain.ScanResult;
import statusCheck.domain.Success;
import statusCheck.io.CSV_DTO;
import statusCheck.io.CSV_DTO.CsvRow;
import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;

public class CSVExportTest {
	protected String id1, id2, id3;
	protected List<ScanResult> success, fail;
	protected ExecutionResult result;
	
	
	@BeforeEach
	public void setUp() throws Exception {
		
		id1 = UUID.randomUUID().toString();
		id2 = UUID.randomUUID().toString();
		id3 = UUID.randomUUID().toString();
		
		success = List.of(new ScanResult(id1, new ScanRequest(id1, "www.google.com"), new Success((Instant)null, 0, 0)),
						  new ScanResult(id2, new ScanRequest(id2, "www.youtube.com"), new Success((Instant)null, 0, 0)));
		
		fail = List.of(new ScanResult(id3, new ScanRequest(id3, "www.facebook.com"), new Fail((Instant)null, 404, "Client failed")));
		
		result = new ExecutionResult(success, fail);
		
	}
	
	@Test
	public void testSuccessCsvRowInit() {
		CsvRow row = CsvRow.row(success.get(0));
		
		assertEquals(null, row.timeStamp());
		assertEquals("www.google.com", row.url());
		assertEquals("Success", row.outcome());
		assertEquals(0, row.statusCode());
		assertEquals(0, row.latencyMs());
		assertEquals("", row.reason());
	}
	
	@Test
	public void testFailCsvRowInit() {
		CsvRow row = CsvRow.row(fail.get(0));
		
		assertEquals(null, row.timeStamp());
		assertEquals("www.facebook.com", row.url());
		assertEquals("Fail", row.outcome());
		assertEquals(404, row.statusCode());
		assertEquals(0, row.latencyMs());
		assertEquals("Client failed", row.reason());
	}
	
	@Test
	public void testCSVConvert() {
		CSV csv = CSV_DTO.convert(result);
;
		CsvMapper csvMapper = new CsvMapper();
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		MappingIterator<Map<String, String>> it = csvMapper.readerForMapOf(String.class)
		    .with(schema)
		    .readValues(csv.data());
		List<Map<String, String>> rows = it.readAll();

		assertEquals(3, rows.size());
		assertEquals("Fail", rows.get(2).get("outcome"));
		assertEquals("www.facebook.com", rows.get(2).get("url"));
	}
	
	

}
