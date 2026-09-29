package statuscheck.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import statuscheck.concurrency.ScanOperator;
import statuscheck.io.ReturnOutput;

public class ScanOutputTest {
	protected ScanRequest req1, req2;
	protected List<ScanResult> successResults, failResults;
	protected Outcome success, fail;

	private ScanRequest req(String url) {
		return new ScanRequest(UUID.randomUUID().toString(), url);
	}

	@BeforeEach
	public void setUp() throws Exception {
		req1 = new ScanRequest(
				UUID.randomUUID().toString(),
				"www.google.com"
		);
		req2 = new ScanRequest(
				UUID.randomUUID().toString(),
				"www.notalink.invalid"
		);

		success = new Success((Instant) null, 0, 0);
		fail = new Fail((Instant) null, 404, "Client failed");

		successResults = List.of(
				new ScanResult(req1.id(), req1, success)
		);

		failResults = List.of(
				new ScanResult(req2.id(), req2, fail)
		);
	}

	@Test
	public void outputReflectsBothSuccessAndFailure() {
		var result = new ExecutionResult(
				successResults,
				failResults
		);

		var output = ReturnOutput.output(result);

		assertEquals(2, output.report().stats().total());
		assertEquals(1, output.report().stats().failureCount());
		assertEquals(1, output.report().stats().successCount());

		assertTrue(output.json().get().data().contains("google"));
		assertTrue(output.csv().get().data().contains("google"));
		assertTrue(output.json().get().data().contains("notalink"));
		assertTrue(output.csv().get().data().contains("notalink"));
	}

	@Test
	public void fromResultsReturnsNullForEmptyResults() {
		assertEquals(null, ReturnOutput.fromResults(List.of()));
	}

	@Test
	public void fromResultsSeparatesSuccessAndFailure() {
		var result = ReturnOutput.fromResults(List.of(successResults.get(0), failResults.get(0)));

		assertEquals(2, result.report().stats().total());
		assertEquals(1, result.report().stats().successCount());
		assertEquals(1, result.report().stats().failureCount());
	}

	@Test
	public void repeatedGetReturnsSameInstance() {
		var result = new ExecutionResult(
				successResults,
				failResults
		);

		var output = ReturnOutput.output(result);

		var firstJSON = output.json().get();
		var secondJSON = output.json().get();
		assertSame(firstJSON, secondJSON);

		var firstCSV = output.csv().get();
		var secondCSV = output.csv().get();
		assertSame(firstCSV, secondCSV);
	}

	@Test
	public void secondOutputDoesNotContainFirstScan()
			throws InterruptedException {

		var requests1 = List.of(req("first scan"));
		var requests2 = List.of(req("second scan"));

		List<ScanResult> results1 =
				Collections.synchronizedList(new ArrayList<>());
		List<ScanResult> results2 =
				Collections.synchronizedList(new ArrayList<>());

		ScanOperator.scanAll(requests1, results1::add, () -> {});
		ScanOperator.scanAll(requests2, results2::add, () -> {});

		var result = new ExecutionResult(
				results2.stream()
						.filter(item -> item.outcome() instanceof Success)
						.toList(),
				results2.stream()
						.filter(item -> item.outcome() instanceof Fail)
						.toList()
		);

		var output = ReturnOutput.output(result);

		assertEquals(1, output.report().stats().total());

		assertFalse(output.json().get().data().contains("first"));
		assertFalse(output.csv().get().data().contains("first"));

		assertTrue(output.json().get().data().contains("second"));
		assertTrue(output.csv().get().data().contains("second"));
	}
}
