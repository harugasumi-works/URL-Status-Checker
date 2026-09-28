package statuscheck.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import statuscheck.domain.RowItem;
import statuscheck.domain.ScanOutput;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.io.AutoSave;
import statuscheck.io.ReturnOutput;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class UILogic {

	static final Set<String> indexRecord = ConcurrentHashMap.newKeySet();
	public static ObservableList<RowItem> items = FXCollections.observableArrayList();
	static ConcurrentHashMap<String, RowItem> itemById = new ConcurrentHashMap<>();

	public static BooleanProperty hasData = new SimpleBooleanProperty(false);
	public static final BooleanProperty isScanning = new SimpleBooleanProperty(false);
	public static final StringProperty saveStatus = new SimpleStringProperty("Autosave on");
	static volatile boolean dirty = false;
	
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static void addPending(ScanRequest content) {
		if (!indexRecord.add(content.requestedURL())) {
			return;
		}
		RowItem.Pending pendingItem = new RowItem.Pending(content);
		itemById.put(content.id(), pendingItem);
		items.add(pendingItem);
		dirty = true;
	}

	public static void onScanCompleted(ScanResult content) {
		Platform.runLater(() -> {
			RowItem newItem = new RowItem.Scanned(content);
			RowItem existingItem = itemById.put(content.id(), newItem);

			if (existingItem != null) {
				int idx = items.indexOf(existingItem);
				if (idx != -1) {
					items.set(idx, newItem);
					dirty = true;
				}
			} else {
				items.add(newItem);
				dirty = true;
			}

			if (!hasData.get()) {
				hasData.setValue(true);
			}
		});
	}

	public static void wipeOut() {
		Runnable clear = () -> {
			if (!items.isEmpty()) {
				itemById.clear();
				items.clear();
				indexRecord.clear();
				hasData.setValue(false);
				dirty = true;
			}
		};
		if (Platform.isFxApplicationThread()) {
			clear.run();
		} else {
			Platform.runLater(clear);
		}
	}

	public static void removeItem(String id) {
		if (id == null)
			return;

		Runnable removeAction = () -> {
			RowItem removedItem = itemById.remove(id);
			if (removedItem != null) {
				items.remove(removedItem);
				indexRecord.remove(getUrlFromItem(removedItem));

				hasData.setValue(items.stream().anyMatch(row -> row instanceof RowItem.Scanned));
				dirty = true;
			}
		};

		if (Platform.isFxApplicationThread()) {
			removeAction.run();
		} else {
			Platform.runLater(removeAction);
		}
	}

	public static void removeItem(RowItem item) {
		if (item != null) {
			removeItem(getIdFromItem(item));
		}
	}

	public static void saveIfDirty() {
		if (dirty) {
			if (AutoSave.save(items)) {
				dirty = false;
				saveStatus.set("Last saved: " + LocalTime.now().format(TIME_FORMAT));
			} else saveStatus.set("Autosave failed");
		}
	}

	private static String getIdFromItem(RowItem item) {
		return switch (item) {
		case RowItem.Pending p -> p.request().id();
		case RowItem.Scanned s -> s.result().context().id();
		};
	}

	private static String getUrlFromItem(RowItem item) {
		return switch (item) {
		case RowItem.Pending p -> p.request().requestedURL();
		case RowItem.Scanned s -> s.result().context().requestedURL();
		};
	}

	public static ScanOutput currentOutput() {
		List<ScanResult> results = items.stream().<ScanResult>mapMulti((item, consumer) -> {
			if (item instanceof RowItem.Scanned s) {
				consumer.accept(s.result());
			}
		}).toList();
		return ReturnOutput.fromResults(results);
	}

	public static void restoreSession(List<RowItem> restored) {
		restored.stream().forEach(item -> {
			items.add(item);
			itemById.put(getIdFromItem(item), item);
			indexRecord.add(getUrlFromItem(item));
		});
		hasData.setValue(items.stream().anyMatch(row -> row instanceof RowItem.Scanned));
	}

	public static void preventAutoSave() {
		dirty = false;
	}
}