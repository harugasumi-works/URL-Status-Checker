package statuscheck;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import statuscheck.concurrency.Operator;
import statuscheck.domain.ExecutionResult;
import statuscheck.domain.Fail;
import statuscheck.domain.ScanRequest;


public class ScanAllTest {
	private ScanRequest req(String url) {
		return new ScanRequest(UUID.randomUUID().toString(), url); 
	}
	
	@Test
	public void malformedURLTurnsIntoAFail() throws InterruptedException {	
		var requests = List.of(req("asd asd asd"), req("ありがとう"));
		var result = Operator.scanAll(requests, _ -> {}, () -> {});
		
		assertEquals(0, result.successes().size());
		assertEquals(2, result.failures().size());
	}
	
	@Test
	public void callbackCallsOncePerRequest() throws InterruptedException {	
		AtomicInteger counter = new AtomicInteger();
		var requests = List.of(req("asd asd asd"), req("ありがとう"));
		Operator.scanAll(requests, _ -> counter.incrementAndGet(), () -> {});
		
		assertEquals(2, counter.get());
	}
	
	@Test
	public void emptyListReturnsEmptyResult() throws InterruptedException {
		List<ScanRequest> requests = List.of();
		ExecutionResult result = assertTimeoutPreemptively(Duration.ofSeconds(2), 
				() -> {
					return Operator.scanAll(requests, _ -> {}, () -> {});
				},
				"Thread timeout"
				);
		
		assertEquals(0, result.successes().size());
		assertEquals(0, result.failures().size());
	}
	
	@Test
	public void allFailuresAreCollected() throws InterruptedException {
		var requests = List.of(req("asd asd asd"), req("ありがとう"));
		var result = Operator.scanAll(requests, _ -> {}, () -> {});
		
		assertEquals(2, result.failures().size());
		
		List<Fail> failList = List.of(assertInstanceOf(Fail.class, result.failures().get(0).outcome()),
				                      assertInstanceOf(Fail.class, result.failures().get(1).outcome()));
		failList.forEach(fail -> assertEquals(0, fail.statusCode()));
	}
	
	@Test
	public void consistentID() throws InterruptedException {	
		var requests = List.of(req("asd asd asd"), req("ありがとう"));
		var originIDSet = new HashSet<>(
				requests.stream()
					.map(req -> req.id())
					.toList()
				);
									
		var result = Operator.scanAll(requests, _ -> {}, () -> {});
		var executedIDSet = new HashSet<>(
				result.failures().stream()
					.map(item -> item.id())
					.toList()
				);
		
		
		assertEquals(2, result.failures().size());
		assertEquals(originIDSet, executedIDSet);
	}
	
	@Test
	public void secondScanDoesNotContainFirstScanResults() throws InterruptedException {
		var requests_1 = List.of(req("asd asd asd"));
		var requests_2 = List.of(req("ありがとう"));
		
		Operator.scanAll(requests_1, _ -> {}, () -> {});
		var result = Operator.scanAll(requests_2, _ -> {}, () -> {});
		
		assertEquals(1, result.failures().size());
		assertEquals(0, result.successes().size());
		
		assertNotEquals(requests_1.get(0).id(), result.failures().get(0).id());
		assertEquals(requests_2.get(0).id(), result.failures().get(0).id());
	}
	
}
