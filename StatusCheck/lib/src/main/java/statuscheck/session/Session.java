package statuscheck.session;

import java.util.List;
import java.util.function.Predicate;

import statuscheck.domain.RowItem;
import statuscheck.domain.ScanOutput;
import statuscheck.io.AutoSave;

public final class Session {

	private final SessionStore store;
	private final AutoSaveService autosave;

	public Session() {
		this(AutoSave::save);
	}

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