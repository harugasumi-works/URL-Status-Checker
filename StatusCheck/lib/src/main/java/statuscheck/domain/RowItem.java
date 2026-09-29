package statuscheck.domain;

public sealed interface RowItem permits RowItem.Pending, RowItem.Scanned {
    record Pending(ScanRequest request) implements RowItem {}
    record Scanned(ScanResult result) implements RowItem {}
    
    default String id() {
		return switch (this) {
		case Pending p -> p.request().id();
		case Scanned s -> s.result().context().id();
		};
	}
 
	default String url() {
		return switch (this) {
		case Pending p -> p.request().requestedURL();
		case Scanned s -> s.result().context().requestedURL();
		};
	}
}