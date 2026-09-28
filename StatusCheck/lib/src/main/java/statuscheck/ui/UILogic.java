package statuscheck.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private static final Set<String> removedIds = ConcurrentHashMap.newKeySet();
	public static ObservableList<RowItem> items = FXCollections.observableArrayList();
	static ConcurrentHashMap<String, RowItem> itemById = new ConcurrentHashMap<>();

	public static BooleanProperty hasData = new SimpleBooleanProperty(false);
	public static final BooleanProperty isScanning = new SimpleBooleanProperty(false);
	public static final StringProperty saveStatus = new SimpleStringProperty("Autosave on");
	static volatile boolean dirty = false;
	private static long changeVersion = 0;
	private static boolean saveInProgress = false;
	private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "session-autosave");
		thread.setDaemon(true);
		return thread;
	});
	
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static void addPending(ScanRequest content) {
		if (!indexRecord.add(content.requestedURL())) {
			return;
		}
		removedIds.remove(content.id());
		RowItem.Pending pendingItem = new RowItem.Pending(content);
		itemById.put(content.id(), pendingItem);
		items.add(pendingItem);
		markDirty();
	}

	public static void onScanCompleted(ScanResult content) {
		Platform.runLater(() -> {
			if (removedIds.contains(content.id())) {
				return;
			}
			RowItem newItem = new RowItem.Scanned(content);
			RowItem existingItem = itemById.put(content.id(), newItem);

			if (existingItem != null) {
				int idx = items.indexOf(existingItem);
				if (idx != -1) {
					items.set(idx, newItem);
					markDirty();
				}
			} else {
				items.add(newItem);
				markDirty();
			}

			if (!hasData.get()) {
				hasData.setValue(true);
			}
		});
	}

	public static void wipeOut() {
		Runnable clear = () -> {
			if (!items.isEmpty()) {
				for (RowItem item : items) {
					removedIds.add(getIdFromItem(item));
				}
				itemById.clear();
				items.clear();
				indexRecord.clear();
				hasData.setValue(false);
				markDirty();
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
			removedIds.add(id);
			RowItem removedItem = itemById.remove(id);
			if (removedItem != null) {
				items.remove(removedItem);
				indexRecord.remove(getUrlFromItem(removedItem));

				hasData.setValue(items.stream().anyMatch(row -> row instanceof RowItem.Scanned));
				markDirty();
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
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(UILogic::saveIfDirty);
			return;
		}
		if (!dirty || saveInProgress) return;
		List<RowItem> snapshot = List.copyOf(items);
		long snapshotVersion = changeVersion;
		saveInProgress = true;
		SAVE_EXECUTOR.execute(() -> {
			boolean saved = AutoSave.save(snapshot);
			Platform.runLater(() -> {
				saveInProgress = false;
				if (saved) {
					if (changeVersion == snapshotVersion) dirty = false;
					saveStatus.set("Last saved: " + LocalTime.now().format(TIME_FORMAT));
				} else {
					saveStatus.set("Autosave failed");
				}
				if (dirty) saveIfDirty();
			});
		});
	}

	public static void saveNow() {
		if (!Platform.isFxApplicationThread()) {
			throw new IllegalStateException("saveNow must run on the JavaFX application thread");
		}
		if (!dirty) return;
		try {
			SAVE_EXECUTOR.submit(() -> {}).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			saveStatus.set("Autosave interrupted");
			return;
		} catch (ExecutionException e) {
			saveStatus.set("Autosave failed");
			return;
		}
		List<RowItem> snapshot = List.copyOf(items);
		long snapshotVersion = changeVersion;
		if (AutoSave.save(snapshot)) {
			if (changeVersion == snapshotVersion) dirty = false;
			saveStatus.set("Last saved: " + LocalTime.now().format(TIME_FORMAT));
		} else {
			saveStatus.set("Autosave failed");
		}
	}

	private static void markDirty() {
		changeVersion++;
		dirty = true;
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