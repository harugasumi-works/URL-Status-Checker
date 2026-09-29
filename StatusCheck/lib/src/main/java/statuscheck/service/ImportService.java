package statuscheck.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import statuscheck.domain.ScanRequest;
import statuscheck.session.SessionStore;
import statuscheck.util.FxThread;
import statuscheck.util.ImportCheck;

public final class ImportService {

	public record Report(int added, int duplicates, int invalid, int http, List<String> rejectedLines) {

		public int skipped() {
			return invalid + http + duplicates;
		}

		public String summary() {
			List<String> reasons = new ArrayList<>();
			if (invalid > 0)
				reasons.add(invalid + " invalid");
			if (http > 0)
				reasons.add(http + " http:// (only HTTPS is checked)");
			if (duplicates > 0)
				reasons.add(duplicates + " duplicate");
			if (reasons.isEmpty())
				return "Added " + added;
			return "Added " + added + ", skipped " + skipped() + " (" + String.join(", ", reasons) + ")";
		}
	}

	private final SessionStore store;

	public ImportService(SessionStore store) {
		this.store = store;
	}

	/** Cheap check, e.g. for enabling an "Add" button. */
	public static boolean hasValidUrls(String text) {
		return !ImportCheck.parseImport(text).valid().isEmpty();
	}

	/** FX thread only. Returns empty when the text contains no valid URL. */
	public Optional<Report> addFrom(String text) {
		FxThread.require();
		ImportCheck parsed = ImportCheck.parseImport(text);
		if (parsed.valid().isEmpty()) {
			return Optional.empty();
		}
		int added = 0, duplicates = 0;
		for (String link : parsed.valid()) {
			if (store.addPending(new ScanRequest(UUID.randomUUID().toString(), link))) {
				added++;
			} else {
				duplicates++;
			}
		}
		return Optional.of(new Report(added, duplicates, parsed.invalid(), parsed.http(), parsed.rejectedLines()));
	}
}