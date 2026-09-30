package statuscheck.concurrency;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Consumer;

import statuscheck.domain.Fail;
import statuscheck.domain.Outcome;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;
import statuscheck.net.LinkBuilder;
import statuscheck.util.ErrorSpecs;
import statuscheck.util.UrlCredentialSanitizer;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class ScanOperator {

	static final int MAX_CONCURRENT_REQUESTS = 50;
	private static final Semaphore permits = new Semaphore(MAX_CONCURRENT_REQUESTS);

	private static void closeQuietly(InputStream body) {
		try {
			body.close();
		} catch (IOException ignored) {
		}
	}

	private static String reasonFor(int code) {
		return switch (code) {
		case 400 -> "Bad request";
		case 401 -> "Unauthorized";
		case 403 -> "Forbidden";
		case 404 -> "Not found";
		case 405 -> "Method not allowed";
		case 408 -> "Request timeout";
		case 410 -> "Gone";
		case 429 -> "Too many requests";
		case 500 -> "Internal server error";
		case 502 -> "Bad gateway";
		case 503 -> "Service unavailable";
		case 504 -> "Gateway timeout";
		default -> code < 500 ? "Client error" : "Server error";
		};
	}

	private static Outcome scanOperator(ScanRequest req) {
		HttpRequest request;
		try {
			request = LinkBuilder.requestFactory(req.requestedURL()).build();

		} catch (Exception e) {
			return new Fail(Instant.now(), 0, ErrorSpecs.describe(e));
		}

		try {
			permits.acquire();
			try {
				Instant start = Instant.now();
				long begin = System.nanoTime();
				HttpResponse<InputStream> response = LinkBuilder.clientGet().send(request,
						BodyHandlers.ofInputStream());
				long latency = NANOSECONDS.toMillis(System.nanoTime() - begin);
				closeQuietly(response.body());
				int code = response.statusCode();

				if (code < 300) {
					return new Success(start, code, latency);
				}

				if (code < 400) {
					String location = response.headers().firstValue("Location")
							.map(UrlCredentialSanitizer::removeCredentials)
							.map(l -> " (Location: " + l + ")")
							.orElse("");

					return new Fail(start, code, "Redirect not completed" + location);
				}

				return new Fail(start, code, reasonFor(code));
			} finally {
				permits.release();
			}
		} catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			return new Fail(Instant.now(), 0, "The scan was interrupted.");
		} catch (HttpConnectTimeoutException e) {
			return new Fail(Instant.now(), 0,
					"Failed to establish TCP/TLS connection in time: " + e.getMessage());

		} catch (HttpTimeoutException _) {
			return new Fail(Instant.now(), 0, "Server took too long to respond");
		} catch (IOException e) {
			return new Fail(Instant.now(), 0,
					"The connection was disrupted: " + ErrorSpecs.describe(e));
		}

	}

	private static ScanResult scan(ScanRequest req) {
		try {
			return new ScanResult(req.id(), req, scanOperator(req));
		} catch (RuntimeException e) {
			return new ScanResult(req.id(), req,
					new Fail(Instant.now(), 0,
							"Unexpected error: " + ErrorSpecs.describe(e)));
		}
	}

	@SuppressWarnings("preview")
	public static void scanAll(List<ScanRequest> requests, Consumer<ScanResult> onResult, Runnable onTaskFailure)
			throws InterruptedException {
		List<Callable<ScanResult>> tasks = requests.stream()
				.<Callable<ScanResult>>map(req -> () -> scan(req))
				.toList();

		var joiner = new ScanJoiner(onResult, onTaskFailure);
		try (var scope = StructuredTaskScope.open(joiner)) {
			tasks.stream().forEach(scope::fork);
			scope.join();
		}
	}

}