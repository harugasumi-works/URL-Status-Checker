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

	public record Report(int added, int duplicates, int invalid, List<String> rejectedLines) {

		public int skipped() {
			return invalid + duplicates;
		}

		public String summary() {
			List<String> reasons = new ArrayList<>();

			if (invalid > 0)
				reasons.add(invalid + " invalid");

			if (duplicates > 0)
				reasons.add(duplicates + " duplicate");

			if (reasons.isEmpty())
				return "Added " + added;

			return "Added " + added + ", skipped " + skipped()
					+ " (" + String.join(", ", reasons) + ")";
		}
	}

	private final SessionStore store;

	public ImportService(SessionStore store) {
		this.store = store;
	}

	public static boolean hasValidUrls(String text) {
		return !ImportCheck.parseImport(text).valid().isEmpty();
	}

	public Optional<Report> addFrom(String text) {
		FxThread.require();

		ImportCheck parsed = ImportCheck.parseImport(text);

		if (parsed.valid().isEmpty()) {
			return Optional.empty();
		}

		int added = 0;
		int duplicates = 0;

		for (String link : parsed.valid()) {
			if (store.addPending(
					new ScanRequest(UUID.randomUUID().toString(), link))) {
				added++;
			} else {
				duplicates++;
			}
		}

		return Optional.of(new Report(
				added,
				duplicates,
				parsed.invalid(),
				parsed.rejectedLines()));
	}
}