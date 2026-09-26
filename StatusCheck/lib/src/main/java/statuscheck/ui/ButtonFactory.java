package statuscheck.ui;

import statuscheck.concurrency.Operator;
import statuscheck.domain.CSV;
import statuscheck.domain.JSON;
import statuscheck.domain.RowItem;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.io.ExportFile;

import java.util.List;

import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

public class ButtonFactory {
	
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
				PopUp.message("Failed to scan. Check if the list is empty");
				return;
			}

			Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws InterruptedException{
					Operator.scanAll(requests, UILogic::onScanCompleted, () -> PopUp.message("Thread failed"));
					return null;
				}
			};

			task.setOnSucceeded(_ -> {
					PopUp.message("Successfully scanned");

			});
			task.setOnFailed(_ -> PopUp.message("Scan failed unexpectedly"));

			new Thread(task).start();
			UILogic.isScanning.bind(task.runningProperty());
		});

		return button;
	}

	public static MenuItem json() {
	    MenuItem item = new MenuItem("Export to JSON");
	    item.setOnAction(_ -> {
	    	
	        try {
	            JSON json = UILogic.currentOutput().json().get();
	            boolean saved = ExportFile.exportJSON(json);
	            PopUp.message(saved ? "Successfully exported" : "Operation canceled");
	        } catch (Exception e) {
	            PopUp.message("Operation was interrupted. Reason: " + e.getMessage());
	        }
	    });
	    return item;
	}

	public static MenuItem csv() {
	    MenuItem item = new MenuItem("Export to CSV");
	    item.setOnAction(_ -> {
	        try {
	            CSV csv = UILogic.currentOutput().csv().get();
	            boolean saved = ExportFile.exportCSV(csv);
	            PopUp.message(saved ? "Successfully exported" : "Operation canceled");
	        } catch (Exception e) {
	            PopUp.message("Operation was interrupted. Reason: " + e.getMessage());
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
		return button;
	}

}