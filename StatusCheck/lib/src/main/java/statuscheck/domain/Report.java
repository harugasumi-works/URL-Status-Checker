package statuscheck.domain;

import java.util.List;

public record Report(CountStat stats, List<ScanResult> successes, List<ScanResult> failures) {

public static Report summarize(ExecutionResult result) {
    int success = result.successes().size();
    int failure = result.failures().size();
    CountStat stats = new CountStat(success + failure, success, failure);
    return new Report(stats, result.successes(), result.failures());
}
}