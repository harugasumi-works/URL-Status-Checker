package StatusCheck.ui;

import StatusCheck.concurrency.Operator;
import StatusCheck.domain.CSV;
import StatusCheck.domain.JSON;
import StatusCheck.domain.RowItem;
import StatusCheck.domain.ScanRequest;
import StatusCheck.domain.ScanResult;
import StatusCheck.io.ExportFile;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

public class ButtonFactory {
	
	public static Button scanButton() {
	    Button button = new Button("Scan");
	    button.setOnAction(_ -> {
	        Operator.requestList = UILogic.items.stream()
	            .<ScanRequest>mapMulti((item, consumer) -> {
	                switch (item) {
	                    case RowItem.Pending(ScanRequest request) -> consumer.accept(request);
	                    case RowItem.Scanned(ScanResult result) -> consumer.accept(result.context());
	                }
	            })
	            .toList();
	        Operator.setUp();

	        Task<Boolean> task = new Task<Boolean>() {
	            @Override protected Boolean call() {
	                return Operator.executeScan(UILogic::onScanCompleted, () -> PopUp.message("Thread failed"));  
	            }
	        };

	        task.setOnSucceeded(_ -> {
	            if (task.getValue() == false) {
	                PopUp.message("Failed to scan. Check if the list is empty");
	            } else {
	                PopUp.message("Successfully scanned");
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
        		JSON json = Operator.json.get();
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
        		CSV csv = Operator.csv.get();
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
		
		if (Operator.requestList.isEmpty()) button.disableProperty().bind(Bindings.isEmpty(UILogic.items));
		return button;
	}

}
