package statuscheck.service;

import java.util.List;
import java.util.Set;

import statuscheck.domain.RowItem;
import statuscheck.domain.Success;

public final class ScanSummary {

	private ScanSummary() {}

	public static String format(List<RowItem> items, Set<String> scannedIds, int taskFailures) {
		int ok = 0, failed = 0, notScanned = 0;
		for (RowItem item : items) {
			if (!scannedIds.contains(item.id()))
				continue;
			switch (item) {
			case RowItem.Pending _ -> notScanned++;
			case RowItem.Scanned s -> {
				if (s.result().outcome() instanceof Success)
					ok++;
				else
					failed++;
			}
			}
		}
		StringBuilder sb = new StringBuilder("Scan finished: " + ok + " OK, " + failed + " failed");
		if (notScanned > 0)
			sb.append(", ").append(notScanned).append(" not scanned");
		if (taskFailures > 0)
			sb.append("\n\n").append(taskFailures)
					.append(" scan task(s) failed unexpectedly; those rows are still pending.");
		return sb.toString();
	}
}