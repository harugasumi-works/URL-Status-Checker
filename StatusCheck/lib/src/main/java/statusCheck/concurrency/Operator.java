package statusCheck.concurrency;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;

import statusCheck.domain.CSV;
import statusCheck.domain.ExecutionResult;
import statusCheck.domain.Fail;
import statusCheck.domain.JSON;
import statusCheck.domain.Outcome;
import statusCheck.domain.Report;
import statusCheck.domain.ScanRequest;
import statusCheck.domain.ScanResult;
import statusCheck.domain.Success;
import statusCheck.io.CSV_DTO;
import statusCheck.io.JSON_DTO;
import statusCheck.net.LinkBuilder;

public class Operator {
	
	private static List<Callable<ScanResult>> tasks = new ArrayList<>(); 
	public static List<ScanRequest> requestList = new ArrayList<>();
	public static LazyConstant<JSON> json = null;
	public static LazyConstant<CSV> csv = null;

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
		} catch (IOException | InterruptedException e) {
			return new Fail(Instant.now(), 0,"The connection was disrupted: " + e.getMessage());
		}
        
	}
	
	@SuppressWarnings("preview")
	public static boolean executeScan(Consumer<ScanResult> consumer, Runnable failSafe) {
		if (tasks.isEmpty()) return false;
		var joiner = new CustomJoin(consumer, failSafe);
		try (var scope = StructuredTaskScope.open(joiner)) {		
			tasks.stream().forEach(scope::fork) ;
			try {
				ExecutionResult results = scope.join();
				Report stat = Report.summarize(results);
				json = LazyConstant.of(() -> {return JSON_DTO.convert(stat); });
				csv = LazyConstant.of(() -> {return CSV_DTO.convert(results); });
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}


		}
		
		return true;
		
	}
	
	@SuppressWarnings("preview")
	public static ExecutionResult scanAll(List<ScanRequest> requests, Consumer<ScanResult> onResult, Runnable  onTaskFailure) throws InterruptedException {
		List<Callable<ScanResult>> tasks = requests.stream()
				.<Callable<ScanResult>>map(req -> () -> new ScanResult(req.id() ,req, scanOperator(req)))
				.toList();
		
		var joiner = new CustomJoin(onResult, onTaskFailure);
		try (var scope = StructuredTaskScope.open(joiner)) {		
			tasks.stream().forEach(scope::fork);
				return scope.join();
		}
	}
	
	public static void setUp() {
		tasks = requestList.stream()
				.<Callable<ScanResult>>map(req -> () -> new ScanResult(req.id() ,req, scanOperator(req)))
				.toList();	
	
	}
	
}
