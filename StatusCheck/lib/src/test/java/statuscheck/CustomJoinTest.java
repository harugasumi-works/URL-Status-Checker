package statuscheck;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import statuscheck.concurrency.CustomJoin;
import statuscheck.domain.ExecutionResult;
import statuscheck.domain.Fail;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;


public class CustomJoinTest {
	protected ScanRequest successReq, failReq;
	protected ScanResult successResult, failResult;
	protected Callable<ScanResult> successThread, failThread;
	protected String json;
	
	@BeforeEach
	public void setUp() throws Exception {
		successReq = new ScanRequest(UUID.randomUUID().toString(), "www.google.com");
		successThread = () -> new ScanResult(successReq.id() ,successReq, new Success((Instant)null, 0, 0));
		successResult = successThread.call();
		
		failReq = new ScanRequest(UUID.randomUUID().toString(), "www.facebook.com");
		failThread = () -> new ScanResult(failReq.id() ,failReq, new Fail((Instant)null, 404, "Client failed"));
		failResult = failThread.call();
	}
	
	
	@SuppressWarnings("preview")
	@Test
	public void singleSuccessfulTask_addedToSuccessesQueue() {
		var joiner = new CustomJoin(_ -> {}, () -> {});
		try (var scope = StructuredTaskScope.open(joiner)) {		
			scope.fork(successThread);		
			try {
				ExecutionResult executionedResult = scope.join();
				assertEquals(1, executionedResult.successes().size());
				assertEquals(0, executionedResult.failures().size());
				var actualResult = executionedResult.successes().get(0);
				assertEquals(successResult.id(), actualResult.id());
			} catch (InterruptedException e) {
				fail("Test was interrupted: " + e.getMessage());
			}
		}
	}
	
	@SuppressWarnings("preview")
	@Test
	public void singleFailTask_addedToFailQueue() {
		var joiner = new CustomJoin(_ -> {}, () -> {});
		try (var scope = StructuredTaskScope.open(joiner)) {		
			scope.fork(failThread);		
			try {
				ExecutionResult executionedResult = scope.join();
				assertEquals(0, executionedResult.successes().size());
				assertEquals(1, executionedResult.failures().size());
				var actualResult = executionedResult.failures().get(0);
				assertEquals(failResult.id(), actualResult.id());
			} catch (InterruptedException e) {
				fail("Test was interrupted: " + e.getMessage());
			}
		}
	}
	
	@SuppressWarnings("preview")
	@Test
	public void dataIsolationTest () {
		var joiner1 = new CustomJoin(_ -> {}, () -> {});
		var joiner2 = new CustomJoin(_ -> {}, () -> {});
		try (var scope = StructuredTaskScope.open(joiner1)) {		
			scope.fork(successThread);		
			try {
				ExecutionResult testResult1 = scope.join();
				assertEquals(1, testResult1.successes().size());
				assertEquals(0, testResult1.failures().size());
			} catch (InterruptedException e) {
				fail("Test was interrupted: " + e.getMessage());
			}
		}
		try (var scope = StructuredTaskScope.open(joiner2)) {		
			scope.fork(failThread);		
			try {
				ExecutionResult testResult2 = scope.join();
				assertEquals(0, testResult2.successes().size());
				assertEquals(1, testResult2.failures().size());
			} catch (InterruptedException e) {
				fail("Test was interrupted: " + e.getMessage());
			}
		}
	}
	
	@SuppressWarnings("preview")
	@Test
	public void taskLevelFailure_doesNotCrashAndTriggersFailSafe() {
		AtomicBoolean failSafeTriggered = new AtomicBoolean(false);
	    var joiner = new CustomJoin(_ -> {}, () -> failSafeTriggered.set(true));
	    
	    try (var scope = StructuredTaskScope.open(joiner)) {
	        scope.fork(() -> { throw new RuntimeException("simulated task failure"); });

	        try {
	            ExecutionResult result = scope.join();

	            assertTrue(failSafeTriggered.get());
	            assertEquals(0, result.successes().size());
	            assertEquals(0, result.failures().size());
	        } catch (InterruptedException e) {
	            fail(e.getMessage());
	        }
	    }
	}

}
