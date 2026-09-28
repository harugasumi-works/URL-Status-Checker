package statuscheck.ui.firstlayer;

import statuscheck.concurrency.Operator;
import statuscheck.domain.RowItem;
import statuscheck.domain.ScanOutput;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;
import statuscheck.io.ExportFile;
import statuscheck.io.ImportURLs;
import statuscheck.ui.PopUp;
import statuscheck.ui.UILogic;
import statuscheck.ui.secondlayer.ImportButtons;
import statuscheck.ui.secondlayer.ImportUI;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainButtons {

	private static Task<Void> currentScan;
	private static void notifyLater(String message) {
		Platform.runLater(() -> PopUp.message(message));
	}

	private static String describe(Exception e) {
		return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
	}

	public static String normalize(String raw) {
		if (raw == null)
			return "";
		String s = raw.trim().replaceFirst("(?i)^https?://", "");
		int end = s.length();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '/' || c == '?' || c == '#') {
				end = i;
				break;
			}
		}
		String authority = s.substring(0, end);
		String rest = s.substring(end);
		int at = authority.lastIndexOf('@');
		authority = authority.substring(0, at + 1) + authority.substring(at + 1).toLowerCase(Locale.ROOT);
		if (rest.equals("/"))
			rest = "";
		return authority + rest;
	}

	public static void cancelScan() {
		Task<Void> task = currentScan;
		if (task != null)
			task.cancel(true);
	}

	public static Button cancelButton() {
		Button button = new Button("Cancel scan");
		button.disableProperty().bind(UILogic.isScanning.not());
		button.setOnAction(_ -> cancelScan());
		return button;
	}

	private static String summarize(Set<String> scannedIds, int taskFailures) {
		int ok = 0, failed = 0, notScanned = 0;
		for (RowItem item : UILogic.items) {
			switch (item) {
			case RowItem.Pending p -> {
				if (scannedIds.contains(p.request().id()))
					notScanned++;
			}
			case RowItem.Scanned s -> {
				if (scannedIds.contains(s.result().id())) {
					if (s.result().outcome() instanceof Success)
						ok++;
					else
						failed++;
				}
			}
			}
		}
		StringBuilder sb = new StringBuilder("Scan finished: " + ok + " OK, " + failed + " failed");
		if (notScanned > 0)
			sb.append(", ").append(notScanned).append(" not scanned");
		if (taskFailures > 0)
			sb.append("\n\n").append(taskFailures)
					.append(" scan task(s) failed unexpectedly; those rows are still pending.");
		return sb.toString();
	}

	public static Button scanButton() {
		Button button = new Button("Scan");
		button.setOnAction(_ -> {
			List<ScanRequest> requests = UILogic.items.stream().<ScanRequest>mapMulti((item, consumer) -> {
				switch (item) {
				case RowItem.Pending(ScanRequest request) -> consumer.accept(request);
				case RowItem.Scanned(ScanResult result) -> consumer.accept(result.context());
				}
			}).toList();
			if (requests.isEmpty()) {
				notifyLater("Failed to scan. Check if the list is empty");
				return;
			}

			Set<String> scannedIds = requests.stream().map(ScanRequest::id).collect(Collectors.toSet());
			AtomicInteger taskFailures = new AtomicInteger();

			Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws InterruptedException {
					Operator.scanAll(requests, UILogic::onScanCompleted, taskFailures::incrementAndGet);
					return null;
				}
			};

			task.setOnSucceeded(_ -> {
				currentScan = null;
				notifyLater(summarize(scannedIds, taskFailures.get()));
			});
			task.setOnCancelled(_ -> {
				currentScan = null;
				notifyLater("Scan cancelled.");
			});
			task.setOnFailed(_ -> {
				currentScan = null;
				Throwable error = task.getException();
				notifyLater("Scan failed unexpectedly"
						+ (error != null ? ": " + (error.getMessage() != null ? error.getMessage()
								: error.getClass().getSimpleName()) : ""));
			});

			currentScan = task;
			UILogic.isScanning.bind(task.runningProperty());

			Thread thread = new Thread(task, "url-scan");
			thread.setDaemon(true);
			thread.start();
		});

		return button;
	}

	private static void runExport(Predicate<ScanOutput> exporter) {
		try {
			ScanOutput output = UILogic.currentOutput();
			if (output == null) {
				notifyLater("Nothing to export yet. Scan at least one URL first.");
				return;
			}
			boolean saved = exporter.test(output);
			notifyLater(saved ? "Successfully exported" : "Operation canceled");
		} catch (Exception e) {
			notifyLater("Operation was interrupted. Reason: " + describe(e));
		}
	}

	public static MenuItem json() {
		MenuItem item = new MenuItem("Export to JSON");
		item.setOnAction(_ -> runExport(output -> ExportFile.exportJSON(output.json().get())));
		return item;
	}

	public static MenuItem csv() {
		MenuItem item = new MenuItem("Export to CSV");
		item.setOnAction(_ -> runExport(output -> ExportFile.exportCSV(output.csv().get())));
		return item;
	}

	public static SplitMenuButton saveButton() {
		SplitMenuButton button = new SplitMenuButton("Save");

		button.setPopupSide(Side.RIGHT);
		button.getItems().addAll(json(), csv());

		button.setOnAction(_ -> {
			if (button.isShowing()) {
				button.hide();
			} else {
				button.show();
			}
		});

		button.disableProperty().bind(UILogic.hasData.not());
		return button;
	}

	public static Button deleteAllButton() {
		Button button = new Button("Clear all");
		button.setOnAction(_ -> {
			if (PopUp.confirm("Are you sure you want to clear all items?")) {
				UILogic.wipeOut();
			}
		});
		button.disableProperty().bind(UILogic.hasData.not());
		return button;
	}

	public static Button bulkImportButton() {
		Button button = new Button("Bulk Import");

		button.setOnAction(_ -> {
			Stage stage = new Stage();
			stage.initOwner(button.getScene().getWindow());
			stage.initModality(Modality.WINDOW_MODAL);

			TextArea area = ImportUI.inputArea();

			Button add = ImportButtons.addButton();
			add.setOnAction(_ -> {
				area.getText().lines().map(MainButtons::normalize).filter(s -> !s.isEmpty())
						.forEach(link -> UILogic.addPending(new ScanRequest(UUID.randomUUID().toString(), link)));
				area.clear();
			});
			add.disableProperty()
					.bind(Bindings.createBooleanBinding(() -> area.getText().trim().isEmpty(), area.textProperty()));

			Button cancel = ImportButtons.cancelButton();
			cancel.setOnAction(_ -> stage.close());

			Button importButton = ImportButtons.importFromFilesButton();
			importButton.setOnAction(_ -> {
				ImportURLs.urlImport(stage, area);
			});

			HBox box = ImportUI.buttons(add, cancel, importButton);

			stage.setScene(ImportUI.createScene(area, box));
			stage.showAndWait();
		});
		return button;
	}

}