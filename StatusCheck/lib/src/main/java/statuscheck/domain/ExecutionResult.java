package statuscheck.domain;

import java.util.List;

public record ExecutionResult(List<ScanResult> successes, List<ScanResult> failures) {}