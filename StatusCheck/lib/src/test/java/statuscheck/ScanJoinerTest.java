package statuscheck;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import statuscheck.concurrency.ScanJoiner;
import statuscheck.domain.Fail;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;


public class ScanJoinerTest {
	protected ScanRequest successReq, failReq;
	protected ScanResult successResult, failResult;
	protected Callable<ScanResult> successThread, failThread;
	
	@BeforeEach
	public void setUp() throws Exception {
		successReq = new ScanRequest(UUID.randomUUID().toString(), "www.google.com");
		successThread = () -> new ScanResult(
				successReq.id(),
				successReq,
				new Success((Instant) null, 0, 0)
		);
		successResult = successThread.call();
		
		failReq = new ScanRequest(UUID.randomUUID().toString(), "www.facebook.com");
		failThread = () -> new ScanResult(
				failReq.id(),
				failReq,
				new Fail((Instant) null, 404, "Client failed")
		);
		failResult = failThread.call();
	}
	
	
	@SuppressWarnings("preview")
	@Test
	public void singleSuccessfulTask_isPassedToCallback() {
		List<ScanResult> results = new ArrayList<>();
		var joiner = new ScanJoiner(results::add, () -> {});
		
		try (var scope = StructuredTaskScope.open(joiner)) {		
			scope.fork(successThread);
			
			try {
				scope.join();
				
				assertEquals(1, results.size());
				assertEquals(successResult.id(), results.get(0).id());
			} catch (InterruptedException e) {
				fail("Test was interrupted: " + e.getMessage());
			}
		}
	}
	
	@SuppressWarnings("preview")
	@Test
	public void singleFailTask_isPassedToCallback() {
		List<ScanResult> results = new ArrayList<>();
		var joiner = new ScanJoiner(results::add, () -> {});
		
		try (var scope = StructuredTaskScope.open(joiner)) {		
			scope.fork(failThread);
			
			try {
				scope.join();
				
				assertEquals(1, results.size());
				assertEquals(failResult.id(), results.get(0).id());
			} catch (InterruptedException e) {
				fail("Test was interrupted: " + e.getMessage());
			}
		}
	}
	
	@SuppressWarnings("preview")
	@Test
	public void dataIsolationTest() {
		List<ScanResult> results1 = new ArrayList<>();
		List<ScanResult> results2 = new ArrayList<>();
		
		var joiner1 = new ScanJoiner(results1::add, () -> {});
		var joiner2 = new ScanJoiner(results2::add, () -> {});
		
		try (var scope = StructuredTaskScope.open(joiner1)) {		
			scope.fork(successThread);
			
			try {
				scope.join();
			} catch (InterruptedException e) {
				fail("Test was interrupted: " + e.getMessage());
			}
		}
		
		try (var scope = StructuredTaskScope.open(joiner2)) {		
			scope.fork(failThread);
			
			try {
				scope.join();
			} catch (InterruptedException e) {
				fail("Test was interrupted: " + e.getMessage());
			}
		}
		
		assertEquals(1, results1.size());
		assertEquals(successResult.id(), results1.get(0).id());
		
		assertEquals(1, results2.size());
		assertEquals(failResult.id(), results2.get(0).id());
	}
	
	@SuppressWarnings("preview")
	@Test
	public void taskLevelFailure_doesNotCrashAndTriggersFailSafe() {
		AtomicBoolean failSafeTriggered = new AtomicBoolean(false);
		List<ScanResult> results = new ArrayList<>();
		
	    var joiner = new ScanJoiner(
	    		results::add,
	    		() -> failSafeTriggered.set(true)
	    );
	    
	    try (var scope = StructuredTaskScope.open(joiner)) {
	        scope.fork(() -> {
	        	throw new RuntimeException("simulated task failure");
	        });

	        try {
	            scope.join();
	            
	            assertTrue(failSafeTriggered.get());
	            assertEquals(0, results.size());
	        } catch (InterruptedException e) {
	            fail(e.getMessage());
	        }
	    }
	}

}