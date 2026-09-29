package statuscheck.session;

import java.util.List;

import statuscheck.domain.RowItem;
import statuscheck.domain.ScanOutput;
import statuscheck.domain.ScanResult;
import statuscheck.io.ReturnOutput;

public final class SessionOutput {

	private SessionOutput() {}

	public static ScanOutput current(List<RowItem> items) {
		List<ScanResult> results = items.stream().<ScanResult>mapMulti((item, consumer) -> {
			if (item instanceof RowItem.Scanned s) {
				consumer.accept(s.result());
			}
		}).toList();
		return ReturnOutput.fromResults(results);
	}
}