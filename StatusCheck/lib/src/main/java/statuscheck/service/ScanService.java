package statuscheck.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Task;
import statuscheck.concurrency.ScanOperator;
import statuscheck.domain.RowItem;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.session.SessionStore;
import statuscheck.util.FxThread;

public final class ScanService {

	private final SessionStore store;
	private final Consumer<String> notifier;
	private final ReadOnlyBooleanWrapper scanning = new ReadOnlyBooleanWrapper(false);
	private Task<Void> current;

	public ScanService(SessionStore store, Consumer<String> notifier) {
		this.store = store;
		this.notifier = notifier;
	}

	public ReadOnlyBooleanProperty scanningProperty() {
		return scanning.getReadOnlyProperty();
	}

	public void start() {
		FxThread.require();
		if (current != null) {
			return; 
		}
		List<ScanRequest> requests = requestsFrom(store.items());
		if (requests.isEmpty()) {
			notifier.accept("Failed to scan. Check if the list is empty");
			return;
		}

		Set<String> scannedIds = requests.stream().map(ScanRequest::id).collect(Collectors.toSet());
		AtomicInteger taskFailures = new AtomicInteger();

		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws InterruptedException {
				ScanOperator.scanAll(requests, store::completeScan, taskFailures::incrementAndGet);
				return null;
			}
		};

		task.setOnSucceeded(_ -> {
			finish();
			notifier.accept(ScanSummary.format(store.items(), scannedIds, taskFailures.get()));
		});
		task.setOnCancelled(_ -> {
			finish();
			notifier.accept("Scan cancelled.");
		});
		task.setOnFailed(_ -> {
			finish();
			Throwable error = task.getException();
			notifier.accept("Scan failed unexpectedly" + (error != null ? ": " + describe(error) : ""));
		});

		current = task;
		scanning.set(true);

		Thread thread = new Thread(task, "url-scan");
		thread.setDaemon(true);
		thread.start();
	}

	public void cancel() {
		FxThread.require();
		Task<Void> task = current;
		if (task != null) {
			task.cancel(true);
		}
	}

	public static List<ScanRequest> requestsFrom(List<RowItem> items) {
		return items.stream().<ScanRequest>mapMulti((item, consumer) -> {
			switch (item) {
			case RowItem.Pending(ScanRequest request) -> consumer.accept(request);
			case RowItem.Scanned(ScanResult result) -> consumer.accept(result.context());
			}
		}).toList();
	}

	private void finish() {
		current = null;
		scanning.set(false);
	}

	private static String describe(Throwable error) {
		return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
	}
}