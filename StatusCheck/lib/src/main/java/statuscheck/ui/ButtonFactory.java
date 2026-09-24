package statuscheck.ui;

import statuscheck.concurrency.Operator;
import statuscheck.domain.CSV;
import statuscheck.domain.JSON;
import statuscheck.domain.RowItem;
import statuscheck.domain.ScanOutput;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.io.ExportFile;
import statuscheck.io.ReturnOutput;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

public class ButtonFactory {
	
	static List<ScanRequest> requests = List.of();

	public static Button scanButton() {
		Button button = new Button("Scan");
		button.setOnAction(_ -> {
			requests = UILogic.items.stream().<ScanRequest>mapMulti((item, consumer) -> {
				switch (item) {
					case RowItem.Pending(ScanRequest request) -> consumer.accept(request);
					case RowItem.Scanned(ScanResult result) -> consumer.accept(result.context());
				}
			}).toList();
			if (requests.isEmpty()) {
				PopUp.message("Failed to scan. Check if the list is empty");
				return;
			}

			Task<ScanOutput> task = new Task<ScanOutput>() {
				@Override
				protected ScanOutput call() throws InterruptedException{
					return ReturnOutput.output(Operator.scanAll(requests, UILogic::onScanCompleted, () -> PopUp.message("Thread failed")));
				}
			};

			task.setOnSucceeded(_ -> {
				try {
					UILogic.lastScan.setValue(task.get());
					PopUp.message("Successfully scanned");
				} catch (InterruptedException e) {
					PopUp.message(e.getMessage());
				} catch (ExecutionException e) {
					PopUp.message(e.getMessage());
				}
			});
			task.setOnFailed(_ -> PopUp.message("Scan failed unexpectedly"));

			new Thread(task).start();
			button.disableProperty().bind(task.runningProperty());
		});

		return button;
	}

	public static MenuItem json() {
		MenuItem item = new MenuItem("Export to JSON");
		item.setOnAction(_ -> {
			try {
				JSON json = UILogic.lastScan.get().json().get();
				ExportFile.exportJSON(json);
				PopUp.message("Successfully exported");
			} catch (Exception _) {
				PopUp.message("Operation was interrupted");
			}
		});
		return item;
	}

	public static MenuItem csv() {
		MenuItem item = new MenuItem("Export to CSV");
		item.setOnAction(_ -> {
			try {
				CSV csv = UILogic.lastScan.get().csv().get();
				ExportFile.exportCSV(csv);
				PopUp.message("Successfully exported");
			} catch (Exception _) {
				PopUp.message("Operation was interrupted");
			}
		});
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

		if (requests.isEmpty())
			button.disableProperty().bind(UILogic.lastScan.isNull());
		return button;
	}

}
