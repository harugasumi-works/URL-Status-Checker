package statusCheck.domain;

public sealed interface RowItem permits RowItem.Pending, RowItem.Scanned {
    record Pending(ScanRequest request) implements RowItem {}
    record Scanned(ScanResult result) implements RowItem {}
}