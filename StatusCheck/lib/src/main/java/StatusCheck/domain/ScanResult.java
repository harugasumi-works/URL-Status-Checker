package StatusCheck.domain;

public record ScanResult(String id, ScanRequest context, Outcome outcome) {

}
