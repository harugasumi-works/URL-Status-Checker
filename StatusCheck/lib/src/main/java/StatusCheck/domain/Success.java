package StatusCheck.domain;

import java.time.Instant;

public record Success(Instant timeStamp, int statusCode, long latency) implements Outcome{

}
