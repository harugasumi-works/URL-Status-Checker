package statuscheck.session;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import statuscheck.domain.RowItem;
import statuscheck.util.FxThread;

public final class AutoSaveService {

	private static final System.Logger LOG = System.getLogger(AutoSaveService.class.getName());
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final long SAVE_NOW_TIMEOUT_SECONDS = 10;

	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "session-autosave");
		thread.setDaemon(true);
		return thread;
	});

	private final Supplier<List<RowItem>> snapshotSource;
	private final Predicate<List<RowItem>> saver;
	private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper("Autosave on");

	private long version;
	private long savedVersion;
	private long reportedVersion;
	private int inFlight;

	public AutoSaveService(Supplier<List<RowItem>> snapshotSource, Predicate<List<RowItem>> saver) {
		this.snapshotSource = snapshotSource;
		this.saver = saver;
	}

	public ReadOnlyStringProperty statusProperty() {
		return status.getReadOnlyProperty();
	}

	public void markDirty() {
		FxThread.require();
		version++;
	}

	public void suppress() {
		FxThread.run(() -> savedVersion = version);
	}

	public boolean isDirty() {
		FxThread.require();
		return version > savedVersion;
	}

	public void saveIfDirty() {
		FxThread.run(() -> {
			if (!isDirty() || inFlight > 0) {
				return;
			}
			List<RowItem> snapshot = snapshotSource.get();
			long snapshotVersion = version;
			inFlight++;
			try {
				executor.execute(() -> {
					boolean ok = runSaver(snapshot);
					Platform.runLater(() -> {
						inFlight--;
						applyResult(ok, snapshotVersion);
						if (ok && isDirty()) {
							saveIfDirty();
						}
					});
				});
			} catch (RejectedExecutionException e) { 
				inFlight--;
			}
		});
	}

	public void saveNow() {
		FxThread.require();
		if (!isDirty()) {
			return;
		}
		List<RowItem> snapshot = snapshotSource.get();
		long snapshotVersion = version;
		Future<Boolean> result;
		try {
			result = executor.submit(() -> runSaver(snapshot));
		} catch (RejectedExecutionException e) {
			status.set("Autosave failed");
			return;
		}
		try {
			applyResult(result.get(SAVE_NOW_TIMEOUT_SECONDS, TimeUnit.SECONDS), snapshotVersion);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			status.set("Autosave interrupted");
		} catch (ExecutionException | TimeoutException e) {
			status.set("Autosave failed");
		}
	}

	public void shutdown() {
		executor.shutdown();
	}

	private boolean runSaver(List<RowItem> snapshot) {
		try {
			return saver.test(snapshot);
		} catch (RuntimeException e) {
			LOG.log(System.Logger.Level.ERROR, "Autosave threw", e);
			return false;
		}
	}

	private void applyResult(boolean ok, long snapshotVersion) {
		if (snapshotVersion < reportedVersion) {
			return;
		}
		reportedVersion = snapshotVersion;
		if (ok) {
			savedVersion = Math.max(savedVersion, snapshotVersion);
			status.set("Last saved: " + LocalTime.now().format(TIME_FORMAT));
		} else {
			status.set("Autosave failed");
		}
	}
}