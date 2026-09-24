package statusCheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import statusCheck.domain.CountStat;
import statusCheck.domain.ExecutionResult;
import statusCheck.domain.Fail;
import statusCheck.domain.Outcome;
import statusCheck.domain.Report;
import statusCheck.domain.ScanRequest;
import statusCheck.domain.ScanResult;
import statusCheck.domain.Success;

public class ReportTest {

	protected ScanRequest req1, req2, req3, req4;
	protected List<ScanResult> successResults, failResults;
	protected Outcome success, fail;
	
	@BeforeEach
	public void setUp() throws Exception {
		req1 = new ScanRequest(UUID.randomUUID().toString(), "www.google.com");
		req2 = new ScanRequest(UUID.randomUUID().toString(), "www.facebook.com");
		req3 = new ScanRequest(UUID.randomUUID().toString(), "www.youtube.com");
		req4 = new ScanRequest(UUID.randomUUID().toString(), "www.reddit.com");
		
		success = new Success((Instant)null, 0, 0);
		fail = new Fail((Instant)null, 404, "Client failed");
		
		successResults = List.of(
				new ScanResult(req1.id(), req1, success),
				new ScanResult(req3.id(), req3, success),
				new ScanResult(req4.id(), req4, success));
		
		failResults = List.of(new ScanResult(req2.id(), req2, fail));
		
	}
	
	@Test
	public void testSuccessAndFailureReport() {
		ExecutionResult result = new ExecutionResult(successResults, failResults);
		Report report = Report.summarize(result);
		
		assertEquals(new CountStat(4, 3, 1), report.stats());
		assertEquals(result.successes(), report.successes());
		assertEquals(result.failures(), report.failures());
	}
	
	@Test
	public void testAllSuccess() {
		ExecutionResult result = new ExecutionResult(successResults, List.of());
		Report report = Report.summarize(result);
		
		assertEquals(new CountStat(3, 3, 0), report.stats());
		assertEquals(result.successes(), report.successes());
		assertEquals(0, report.failures().size());
	}
	
	@Test
	public void testAllFailure() {
		ExecutionResult result = new ExecutionResult(List.of(), failResults);
		Report report = Report.summarize(result);
		
		assertEquals(new CountStat(1, 0, 1), report.stats());
		assertEquals(0, report.successes().size());
		assertEquals(result.failures(), report.failures());
	}
	
	@Test
	public void testEmptyReport() {
		ExecutionResult result = new ExecutionResult(List.of(), List.of());
		Report report = Report.summarize(result);
		
		assertEquals(new CountStat(0, 0, 0), report.stats());
		assertEquals(0, report.successes().size());
		assertEquals(0, report.failures().size());
	}
	
}
