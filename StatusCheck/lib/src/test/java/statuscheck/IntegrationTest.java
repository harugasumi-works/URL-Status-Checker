package statuscheck;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import statuscheck.concurrency.ScanOperator;
import statuscheck.domain.ExecutionResult;
import statuscheck.domain.Fail;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;
import statuscheck.io.ReturnOutput;


@Tag("integration")
public class IntegrationTest {
	protected List<String> correctUrls = List.of("www.google.com", "www.wyz.wyz");
	protected List<String> illegalUrls = List.of("asd asd asd", "ありがとうございます。");

	@Test
	public void testOperationCorrect() throws InterruptedException {
		var requests = correctUrls.stream()
				.map(content -> new ScanRequest(
						UUID.randomUUID().toString(),
						content
				))
				.toList();

		List<ScanResult> results =
				Collections.synchronizedList(new ArrayList<>());

		ScanOperator.scanAll(requests, results::add, () -> {});

		var executionResult = new ExecutionResult(
				results.stream()
						.filter(result -> result.outcome() instanceof Success)
						.toList(),
				results.stream()
						.filter(result -> result.outcome() instanceof Fail)
						.toList()
		);

		var output = ReturnOutput.output(executionResult);

		assertTrue(output.json().get().data().contains("www.google.com"));
		assertTrue(output.csv().get().data().contains("www.google.com"));
	}

	@Test
	public void testOperationIllegal() throws InterruptedException {
		var requests = illegalUrls.stream()
				.map(content -> new ScanRequest(
						UUID.randomUUID().toString(),
						content
				))
				.toList();

		List<ScanResult> results =
				Collections.synchronizedList(new ArrayList<>());

		ScanOperator.scanAll(requests, results::add, () -> {});

		var executionResult = new ExecutionResult(
				results.stream()
						.filter(result -> result.outcome() instanceof Success)
						.toList(),
				results.stream()
						.filter(result -> result.outcome() instanceof Fail)
						.toList()
		);

		var output = ReturnOutput.output(executionResult);

		assertTrue(output.json().get().data().contains("Illegal"));
		assertTrue(output.csv().get().data().contains("Illegal"));
	}

}