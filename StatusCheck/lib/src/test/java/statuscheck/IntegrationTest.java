package statuscheck;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import statuscheck.concurrency.Operator;
import statuscheck.domain.ScanRequest;
import statuscheck.io.ReturnOutput;


@Tag("integration")
public class IntegrationTest {
	protected List<String> correctUrls = List.of("www.google.com", "www.wyz.wyz");
	protected List<String> illegalUrls = List.of("asd asd asd", "ありがとうございます。");

	@Test
	public void testOperationCorrect() throws InterruptedException {
		var requests = correctUrls.stream()
									.map(content -> new ScanRequest(UUID.randomUUID().toString(), content))
									.toList();

		var output = ReturnOutput.output(Operator.scanAll(requests, _ -> {}, () -> {}));

		assertTrue(output.json().get().data().contains("www.google.com"));
		assertTrue(output.csv().get().data().contains("www.google.com"));
	}

	@Test
	public void testOperationIllegal() throws InterruptedException {
		var requests = illegalUrls.stream()
				.map(content -> new ScanRequest(UUID.randomUUID().toString(), content))
				.toList();

		var output = ReturnOutput.output(Operator.scanAll(requests, _ -> {}, () -> {}));

		assertTrue(output.json().get().data().contains("Illegal"));
		assertTrue(output.csv().get().data().contains("Illegal"));
	}

}