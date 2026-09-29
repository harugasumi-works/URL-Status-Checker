package statuscheck;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import statuscheck.concurrency.ScanOperator;
import statuscheck.domain.Fail;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;


public class ScanAllTest {
	private ScanRequest req(String url) {
		return new ScanRequest(UUID.randomUUID().toString(), url); 
	}
	
	@Test
	public void malformedURLTurnsIntoAFail() throws InterruptedException {
		var requests = List.of(
				req("asd asd asd"),
				req("ありがとう")
		);

		List<ScanResult> results =
				Collections.synchronizedList(new ArrayList<>());
		
		ScanOperator.scanAll(requests, results::add, () -> {});
		
		assertEquals(2, results.size());
		assertEquals(
				2,
				results.stream()
						.filter(result -> result.outcome() instanceof Fail)
						.count()
		);
	}
	
	@Test
	public void callbackCallsOncePerRequest() throws InterruptedException {
		AtomicInteger counter = new AtomicInteger();
		var requests = List.of(
				req("asd asd asd"),
				req("ありがとう")
		);
		
		ScanOperator.scanAll(
				requests,
				_ -> counter.incrementAndGet(),
				() -> {}
		);
		
		assertEquals(2, counter.get());
	}
	
	@Test
	public void emptyListReturnsEmptyResult() {
		List<ScanRequest> requests = List.of();
		
		assertTimeoutPreemptively(
				Duration.ofSeconds(2),
				() -> {
					List<ScanResult> results = new ArrayList<>();
					
					ScanOperator.scanAll(
							requests,
							results::add,
							() -> {}
					);
					
					assertEquals(0, results.size());
				},
				"Thread timeout"
		);
	}
	
	@Test
	public void allFailuresAreCollected() throws InterruptedException {
		var requests = List.of(
				req("asd asd asd"),
				req("ありがとう")
		);

		List<ScanResult> results =
				Collections.synchronizedList(new ArrayList<>());
		
		ScanOperator.scanAll(requests, results::add, () -> {});
		
		assertEquals(2, results.size());
		
		results.forEach(result -> {
			Fail fail = assertInstanceOf(
					Fail.class,
					result.outcome()
			);
			assertEquals(0, fail.statusCode());
		});
	}
	
	@Test
	public void consistentID() throws InterruptedException {
		var requests = List.of(
				req("asd asd asd"),
				req("ありがとう")
		);

		var originIDSet = new HashSet<>(
				requests.stream()
						.map(ScanRequest::id)
						.toList()
		);
		
		List<ScanResult> results =
				Collections.synchronizedList(new ArrayList<>());

		ScanOperator.scanAll(requests, results::add, () -> {});
		
		var executedIDSet = new HashSet<>(
				results.stream()
						.map(ScanResult::id)
						.toList()
		);
		
		assertEquals(2, results.size());
		assertEquals(originIDSet, executedIDSet);
	}
	
	@Test
	public void secondScanDoesNotContainFirstScanResults()
			throws InterruptedException {

		var requests1 = List.of(req("asd asd asd"));
		var requests2 = List.of(req("ありがとう"));
		
		List<ScanResult> results1 =
				Collections.synchronizedList(new ArrayList<>());
		List<ScanResult> results2 =
				Collections.synchronizedList(new ArrayList<>());
		
		ScanOperator.scanAll(requests1, results1::add, () -> {});
		ScanOperator.scanAll(requests2, results2::add, () -> {});
		
		assertEquals(1, results2.size());
		assertEquals(
				requests2.get(0).id(),
				results2.get(0).id()
		);
		assertNotEquals(
				requests1.get(0).id(),
				results2.get(0).id()
		);
	}
	
}