package statuscheck.concurrency;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;

import statuscheck.domain.ExecutionResult;
import statuscheck.domain.Fail;
import statuscheck.domain.Outcome;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;
import statuscheck.net.LinkBuilder;

public class Operator {

	private static Outcome scanOperator(ScanRequest req) {
		HttpRequest request;
		try {
			request = LinkBuilder.requestFactory(req.requestedURL()).build();
        
		} catch (Exception e) {
			return new Fail(Instant.now(), 0, e.getMessage());
		}

		try {
			Instant start = Instant.now();
			HttpResponse<String> response = LinkBuilder.clientGet().send(request, HttpResponse.BodyHandlers.ofString());
			Instant end = Instant.now();
			int code = response.statusCode();
			return switch(code) {
			case int c when (c < 400) -> new Success(start, code, Duration.between(start, end).toMillis());
			case int c when (c >= 400 && c < 500) -> new Fail(start, code,"Client failed to make a request.");
			default -> new Fail(start, code,"Server failed.");
			};
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return new Fail(Instant.now(), 0, "The scan was interrupted.");
		} catch (IOException e) {
			return new Fail(Instant.now(), 0,"The connection was disrupted: " + e.getMessage());
		}
        
	}
	
	private static ScanResult scan(ScanRequest req) {
		try {
			return new ScanResult(req.id(), req, scanOperator(req));
		} catch (RuntimeException e) {
			return new ScanResult(req.id(), req, new Fail(Instant.now(), 0, "Unexpected error: " + e));
		}
	}
	
	@SuppressWarnings("preview")
	public static ExecutionResult scanAll(List<ScanRequest> requests, Consumer<ScanResult> onResult, Runnable  onTaskFailure) throws InterruptedException {
		List<Callable<ScanResult>> tasks = requests.stream()
				.<Callable<ScanResult>>map(req -> () -> scan(req))
				.toList();
		
		var joiner = new CustomJoin(onResult, onTaskFailure);
		try (var scope = StructuredTaskScope.open(joiner)) {		
			tasks.stream().forEach(scope::fork);
				return scope.join();
		}
	}
	
	
}