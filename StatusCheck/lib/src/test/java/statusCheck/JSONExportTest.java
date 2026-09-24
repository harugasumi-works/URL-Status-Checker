package statusCheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import statusCheck.domain.CountStat;
import statusCheck.domain.Fail;
import statusCheck.domain.JSON;
import statusCheck.domain.Report;
import statusCheck.domain.ScanRequest;
import statusCheck.domain.ScanResult;
import statusCheck.domain.Success;
import statusCheck.io.JSON_DTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class JSONExportTest {
	protected ObjectMapper mapper;
	protected Report report;
	protected String json;
	protected String id1, id2, id3;
	
	@BeforeEach
	public void setUp() throws Exception {
		mapper = new ObjectMapper();
		
		id1 = UUID.randomUUID().toString();
		id2 = UUID.randomUUID().toString();
		id3 = UUID.randomUUID().toString();
		
		report = new Report(
				new CountStat(3, 2, 1),
				(List<ScanResult>)List.of(new ScanResult(id1, new ScanRequest(id1, "www.google.com"), new Success((Instant)null, 0, 0)),
										  new ScanResult(id2, new ScanRequest(id2, "www.youtube.com"), new Success((Instant)null, 0, 0))),
				(List<ScanResult>)List.of(new ScanResult(id3, new ScanRequest(id3, "www.facebook.com"), new Fail((Instant)null, 404, "Client failed")))
				);
	}
	
	
	@Test
	void testJSONContent() throws Exception {
	    JSON testJSON = JSON_DTO.convert(report);
	    JsonNode node = mapper.readTree(testJSON.data());

	    assertEquals(3, node.get("stats").get("total").asInt());
	    assertEquals(2, node.get("stats").get("successCount").asInt());
	    assertEquals(1, node.get("stats").get("failureCount").asInt());
	    assertEquals("www.facebook.com", 
	        node.get("failures").get(0).get("context").get("requestedURL").asString());
	}
}
