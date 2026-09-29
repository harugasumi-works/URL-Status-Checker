package statuscheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import statuscheck.domain.CountStat;
import statuscheck.domain.Fail;
import statuscheck.domain.JSON;
import statuscheck.domain.Report;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;
import statuscheck.io.JsonDto;
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
				(List<ScanResult>) List.of(new ScanResult(id1, new ScanRequest(id1, "www.google.com"),
						new Success((Instant) null, 0, 0)),
						new ScanResult(id2, new ScanRequest(id2, "www.youtube.com"),
								new Success((Instant) null, 0, 0))),
				(List<ScanResult>) List.of(new ScanResult(id3, new ScanRequest(id3, "www.facebook.com"),
						new Fail((Instant) null, 404, "Client failed"))));
	}

	@Test
	void testJSONContent() throws Exception {
		JSON testJSON = JsonDto.convert(report);
		JsonNode node = mapper.readTree(testJSON.data());

		assertEquals(3, node.get("stats").get("total").asInt());
		assertEquals(2, node.get("stats").get("successCount").asInt());
		assertEquals(1, node.get("stats").get("failureCount").asInt());
		assertEquals("www.facebook.com",
				node.get("failures").get(0).get("context").get("requestedURL").asString());
	}

	@Test
	void credentialsAreRemovedFromGenericUrlField() throws Exception {
		JSON testJSON = JsonDto.convert(java.util.Map.of(
				"url", "https://user:pass@example.com/path"));
		JsonNode node = mapper.readTree(testJSON.data());

		assertEquals("https://example.com/path", node.get("url").asString());
	}

	@Test
	void credentialsAreRemovedFromRequestedURL() throws Exception {
		String id = UUID.randomUUID().toString();
		Report secureReport = new Report(
				new CountStat(1, 1, 0),
				List.of(new ScanResult(
						id,
						new ScanRequest(id, "https://user:pass@example.com/path"),
						new Success((Instant) null, 200, 10))),
				List.of());

		JSON testJSON = JsonDto.convert(secureReport);
		JsonNode node = mapper.readTree(testJSON.data());
		String url = node.get("successes").get(0).get("context").get("requestedURL").asString();

		assertEquals("https://example.com/path", url);
		assertFalse(testJSON.data().contains("user:pass@"));
	}
}
