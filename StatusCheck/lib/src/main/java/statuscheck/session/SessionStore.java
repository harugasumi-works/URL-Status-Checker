package statuscheck.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import statuscheck.domain.RowItem;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.util.FxThread;

public final class SessionStore {

	private final Set<String> knownUrls = new HashSet<>();
	private final Set<String> removedIds = new HashSet<>();
	private final Map<String, RowItem> itemById = new HashMap<>();

	private final ObservableList<RowItem> items = FXCollections.observableArrayList();
	private final ObservableList<RowItem> itemsView = FXCollections.unmodifiableObservableList(items);
	private final ReadOnlyBooleanWrapper hasData = new ReadOnlyBooleanWrapper(false);
	private int scannedCount;

	private final Runnable onChanged;

	public SessionStore(Runnable onChanged) {
		this.onChanged = onChanged;
	}

	public ObservableList<RowItem> items() {
		return itemsView;
	}

	public ReadOnlyBooleanProperty hasDataProperty() {
		return hasData.getReadOnlyProperty();
	}

	public boolean addPending(ScanRequest request) {
		FxThread.require();
		if (!knownUrls.add(request.requestedURL())) {
			return false;
		}
		removedIds.remove(request.id());
		RowItem pending = new RowItem.Pending(request);
		itemById.put(request.id(), pending);
		items.add(pending);
		onChanged.run();
		return true;
	}

	public void completeScan(ScanResult result) {
		FxThread.run(() -> {
			String id = result.context().id();
			if (removedIds.contains(id)) {
				return; 
			}
			RowItem scanned = new RowItem.Scanned(result);
			RowItem previous = itemById.put(id, scanned);
			if (!(previous instanceof RowItem.Scanned)) {
				scannedCount++; 
			}
			int idx = previous == null ? -1 : items.indexOf(previous);
			if (idx >= 0) {
				items.set(idx, scanned);
			} else {
				items.add(scanned);
			}
			knownUrls.add(scanned.url());
			refreshHasData();
			onChanged.run();
		});
	}

	public void remove(String id) {
		if (id == null) {
			return;
		}
		FxThread.run(() -> {
			removedIds.add(id);
			RowItem removed = itemById.remove(id);
			if (removed != null) {
				if (removed instanceof RowItem.Scanned) {
					scannedCount--;
				}
				items.remove(removed);
				knownUrls.remove(removed.url());
				refreshHasData();
				onChanged.run();
			}
		});
	}

	public void remove(RowItem item) {
		if (item != null) {
			remove(item.id());
		}
	}

	public void clear() {
		FxThread.run(() -> {
			boolean hadItems = !items.isEmpty();
			for (RowItem item : items) {
				removedIds.add(item.id());
			}
			items.clear();
			itemById.clear();
			knownUrls.clear();
			scannedCount = 0;
			refreshHasData();
			if (hadItems) {
				onChanged.run();
			}
		});
	}

	public void restore(List<RowItem> restored) {
		FxThread.require();
		for (RowItem item : restored) {
			removedIds.remove(item.id());
			itemById.put(item.id(), item);
			knownUrls.add(item.url());
			items.add(item);
			if (item instanceof RowItem.Scanned) {
				scannedCount++;
			}
		}
		refreshHasData();
	}

	private void refreshHasData() {
		hasData.set(scannedCount > 0);
	}
}