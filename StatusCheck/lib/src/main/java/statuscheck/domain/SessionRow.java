package statuscheck.domain;

import java.time.Instant;
import java.util.List;

public record SessionRow(String rowType, String outcome, String id, String url, Instant time, int code, long latency,
		String reason) {
	public static List<SessionRow> toSessionRow(List<RowItem> items) {
		return items.stream().map(item -> switch (item) {
		case RowItem.Pending p ->
			new SessionRow("Pending", "", p.request().id(), p.request().requestedURL(), null, 0, 0, "");
		case RowItem.Scanned s -> switch (s.result()) {
		case ScanResult(String id, ScanRequest context, Success outcome) -> new SessionRow("Scanned", "Success", id,
				context.requestedURL(), outcome.timeStamp(), outcome.statusCode(), outcome.latency(), "");
		case ScanResult(String id, ScanRequest context, Fail outcome) -> new SessionRow("Scanned", "Fail", id,
				context.requestedURL(), outcome.timeStamp(), outcome.statusCode(), 0, outcome.reason());
		};
		}).toList();
	}

	public static List<RowItem> toRowItem(List<SessionRow> items) {
		return items.stream().<RowItem>map(item -> {
			if (item.rowType().equals("Pending"))
				return new RowItem.Pending(new ScanRequest(item.id(), item.url()));
			else
				switch (item.outcome()) {
				case "Success":
					return new RowItem.Scanned(new ScanResult(item.id(), new ScanRequest(item.id(), item.url()),
							new Success(item.time(), item.code(), item.latency())));
				default:
					return new RowItem.Scanned(new ScanResult(item.id(), new ScanRequest(item.id(), item.url()),
							new Fail(item.time(), item.code(), item.reason())));
				}
		}).toList();
	}

}
