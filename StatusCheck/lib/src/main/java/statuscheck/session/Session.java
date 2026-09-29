package statuscheck.session;

import java.util.List;
import java.util.function.Predicate;

import statuscheck.domain.RowItem;
import statuscheck.domain.ScanOutput;
import statuscheck.io.AutoSave;

/**
 * Composition root for the session: wires the store to the autosave service.
 * Create one of these at startup and pass it (or its parts) to your controllers.
 */
public final class Session {

	private final SessionStore store;
	private final AutoSaveService autosave;

	public Session() {
		this(AutoSave::save);
	}

	/** Inject a fake saver in tests. */
	public Session(Predicate<List<RowItem>> saver) {
		this.store = new SessionStore(this::onStoreChanged);
		this.autosave = new AutoSaveService(this::snapshot, saver);
	}

	public SessionStore store() {
		return store;
	}

	public AutoSaveService autosave() {
		return autosave;
	}

	public ScanOutput currentOutput() {
		return SessionOutput.current(store.items());
	}

	private void onStoreChanged() {
		autosave.markDirty();
	}

	private List<RowItem> snapshot() {
		return List.copyOf(store.items());
	}
}