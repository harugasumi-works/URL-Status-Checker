package statuscheck.domain;

import java.time.Instant;

public record SessionRow(String rowType, String outcome, String id, Instant time, String url, int code, long latency, String reason) {

}
