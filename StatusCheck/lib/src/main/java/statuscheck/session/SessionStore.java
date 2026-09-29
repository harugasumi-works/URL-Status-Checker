package statuscheck.session;

import java.util.ArrayList;
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
	private final Map<String, Integer> indexById = new HashMap<>();

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
		if (knownUrls.contains(request.requestedURL())) {
			return false;
		}
		if (indexById.containsKey(request.id())) {
			throw new IllegalArgumentException("Duplicate request id: " + request.id());
		}
		knownUrls.add(request.requestedURL());
		indexById.put(request.id(), items.size());
		items.add(new RowItem.Pending(request));
		onChanged.run();
		return true;
	}

	public void completeScan(ScanResult result) {
		FxThread.run(() -> {
			Integer idx = indexById.get(result.context().id());
			if (idx == null) {
				return;
			}
			if (!(items.get(idx) instanceof RowItem.Scanned)) {
				scannedCount++;
			}
			items.set(idx, new RowItem.Scanned(result));
			refreshHasData();
			onChanged.run();
		});
	}

	public void remove(String id) {
		if (id == null) {
			return;
		}
		FxThread.run(() -> {
			Integer idx = indexById.remove(id);
			if (idx == null) {
				return;
			}
			RowItem removed = items.remove(idx.intValue());
			if (removed instanceof RowItem.Scanned) {
				scannedCount--;
			}
			knownUrls.remove(removed.url());
			reindexFrom(idx);
			refreshHasData();
			onChanged.run();
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
			items.clear();
			indexById.clear();
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
		List<RowItem> toAdd = new ArrayList<>(restored.size());
		for (RowItem item : restored) {
			if (indexById.containsKey(item.id())) {
				continue;
			}
			indexById.put(item.id(), items.size() + toAdd.size());
			knownUrls.add(item.url());
			toAdd.add(item);
			if (item instanceof RowItem.Scanned) {
				scannedCount++;
			}
		}
		items.addAll(toAdd);
		refreshHasData();
	}

	private void reindexFrom(int from) {
		for (int i = from; i < items.size(); i++) {
			indexById.put(items.get(i).id(), i);
		}
	}

	private void refreshHasData() {
		hasData.set(scannedCount > 0);
	}
}