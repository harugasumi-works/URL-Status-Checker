package StatusCheck.ui;


import java.util.concurrent.ConcurrentHashMap;

import StatusCheck.domain.RowItem;
import StatusCheck.domain.ScanRequest;
import StatusCheck.domain.ScanResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class UILogic {

	public static ObservableList<RowItem> items = FXCollections.observableArrayList();
	static ConcurrentHashMap<String, Integer> indexById = new ConcurrentHashMap<>();

	public static void addPending(ScanRequest content) {
	    items.add(new RowItem.Pending(content));
	    indexById.put(content.id(), items.size() - 1);
	}

	public static void onScanCompleted(ScanResult content) {
	    Integer idx = indexById.get(content.id());
	    Platform.runLater(() -> {
	    	if (idx != null) {
	    		items.set(idx, new RowItem.Scanned(content));
	    		// indexById entry stays valid since replacing in place doesn't shift indices
	    	} else {
	    		items.add(new RowItem.Scanned(content));
	    		indexById.put(content.id(), items.size() - 1);
	    	}
	    });
	}
	
	
}
