package statuscheck.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record SessionRow(String rowType, String outcome, String id, String url, Instant time, int code, long latency,
		String reason) {

	private static final String PENDING = "Pending";
	private static final String SCANNED = "Scanned";
	private static final String SUCCESS = "Success";
	private static final String FAIL = "Fail";

	public SessionRow {
		if (id == null || id.isBlank())
			throw new IllegalArgumentException("Session row has no id");
		if (url == null || url.isBlank())
			throw new IllegalArgumentException("Session row " + id + " has no url");
		if (rowType == null)
			throw new IllegalArgumentException("Session row " + id + " has no rowType");
		switch (rowType) {
		case PENDING -> {
		}
		case SCANNED -> {
			if (!SUCCESS.equals(outcome) && !FAIL.equals(outcome))
				throw new IllegalArgumentException("Session row " + id + " has unknown outcome: " + outcome);
			if (time == null)
				throw new IllegalArgumentException("Scanned session row " + id + " has no time");
		}
		default -> throw new IllegalArgumentException("Session row " + id + " has unknown rowType: " + rowType);
		}
	}

	public static List<SessionRow> toSessionRow(List<RowItem> items) {
		return items.stream().map(item -> switch (item) {
		case RowItem.Pending p ->
			new SessionRow(PENDING, "", p.request().id(), p.request().requestedURL(), null, 0, 0, "");
		case RowItem.Scanned s -> switch (s.result()) {
		case ScanResult(String id, ScanRequest context, Success outcome) -> new SessionRow(SCANNED, SUCCESS, id,
				context.requestedURL(), outcome.timeStamp(), outcome.statusCode(), outcome.latency(), "");
		case ScanResult(String id, ScanRequest context, Fail outcome) -> new SessionRow(SCANNED, FAIL, id,
				context.requestedURL(), outcome.timeStamp(), outcome.statusCode(), 0, outcome.reason());
		};
		}).toList();
	}

	public static List<RowItem> toRowItem(List<SessionRow> rows) {
		Objects.requireNonNull(rows, "rows");
		return rows.stream().map(row -> Objects.requireNonNull(row, "null session row").toItem()).toList();
	}

	private RowItem toItem() {
		ScanRequest request = new ScanRequest(id, url);
		return switch (rowType) {
		case PENDING -> new RowItem.Pending(request);
		case SCANNED -> {
			Outcome result = SUCCESS.equals(outcome) ? new Success(time, code, latency) : new Fail(time, code, reason);
			yield new RowItem.Scanned(new ScanResult(id, request, result));
		}
		default -> throw new IllegalStateException("Unvalidated rowType: " + rowType);
		};
	}

}