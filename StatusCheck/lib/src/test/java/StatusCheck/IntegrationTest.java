package StatusCheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import StatusCheck.concurrency.Operator;
import StatusCheck.domain.ScanRequest;


@Tag("integration")
public class IntegrationTest {
	protected List<String> correctUrls = List.of("www.google.com", "www.wyz.wyz");
	protected List<String> illegalUrls = List.of("asd asd asd", "ありがとうございます。");
	protected String testJSON, testCSV;
	
	@Test
	public void testOperationCorrect() {
		Operator.requestList = correctUrls.stream()
									.map(content -> new ScanRequest(UUID.randomUUID().toString(), content))
									.toList();
		
		Operator.setUp();
		
		var ok = true;
		
		assertEquals(null, Operator.csv);
		assertEquals(null, Operator.json);
		assertEquals(ok, Operator.executeScan(_ -> {}, () -> {}));
		
		
		testJSON = Operator.json.get().data();
		testCSV = Operator.csv.get().data();
		
		assertTrue(testJSON.contains("www.google.com"));
		assertTrue(testCSV.contains("www.google.com"));
		
	}
	
	@Test
	public void testOperationIllegal() {
		Operator.requestList = illegalUrls.stream()
				.map(content -> new ScanRequest(UUID.randomUUID().toString(), content))
				.toList();
		
		Operator.setUp();
		
		var ok = true;
		
		assertEquals(null, Operator.csv);
		assertEquals(null, Operator.json);
		assertEquals(ok, Operator.executeScan(_ -> {}, () -> {}));
		
		
		testJSON = Operator.json.get().data();
		testCSV = Operator.csv.get().data();
		
		assertTrue(testJSON.contains("Illegal"));
		assertTrue(testCSV.contains("Illegal"));
		
	}
	
	@AfterEach
	public void tearDown() {
		if (!Operator.requestList.isEmpty()) { 
			Operator.requestList =  new ArrayList<>();
			Operator.setUp();
		}
		Operator.json = null;
		Operator.csv = null;
		
	}
	
}
