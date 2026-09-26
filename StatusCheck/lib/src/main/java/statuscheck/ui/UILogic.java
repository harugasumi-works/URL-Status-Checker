package statuscheck.ui;


import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import statuscheck.domain.RowItem;
import statuscheck.domain.ScanOutput;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.io.ReturnOutput;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class UILogic {

	public static ObservableList<RowItem> items = FXCollections.observableArrayList();
	static ConcurrentHashMap<String, Integer> indexById = new ConcurrentHashMap<>();
	public static BooleanProperty hasData = new SimpleBooleanProperty(false);
	public static final BooleanProperty isScanning = new SimpleBooleanProperty(false);

	public static void addPending(ScanRequest content) {
	    items.add(new RowItem.Pending(content));
	    indexById.put(content.id(), items.size() - 1);
	}

	public static void onScanCompleted(ScanResult content) {
	    Integer idx = indexById.get(content.id());
	    Platform.runLater(() -> {
	    	if (idx != null) {
	    		items.set(idx, new RowItem.Scanned(content));
	    		if (hasData.get() == false)
	    			hasData.setValue(true);
	    	} else {
	    		items.add(new RowItem.Scanned(content));
	    		indexById.put(content.id(), items.size() - 1);
	    		if (hasData.get() == false)
	    			hasData.setValue(true);
	    	}
	    });
	}
	
	public static void wipeOut() {
		Runnable clear = () -> {
	        if (!items.isEmpty()) {
	            indexById.clear();
	            items.clear();
	            hasData.setValue(false);
	        }
	    };
	    if (Platform.isFxApplicationThread()) {
	        clear.run();
	    } else {
	        Platform.runLater(clear);
	    }
	}
	
	public static void removeItem(RowItem item) {
	    if (item != null) {
	        String id = switch (item) {
	            case RowItem.Pending p -> p.request().id();
	            case RowItem.Scanned s -> s.result().context().id();
	        };
	        indexById.remove(id);
	        items.remove(item);
	        hasData.setValue(items.stream().anyMatch(row -> row instanceof RowItem.Scanned));
	    } 
	    
	}
	
	public static ScanOutput currentOutput() {
	    List<ScanResult> results = items.stream().<ScanResult>mapMulti((item, consumer) -> {
	        if (item instanceof RowItem.Scanned s) {
	            consumer.accept(s.result());
	        }
	    }).toList();
	    return ReturnOutput.fromResults(results);
	}
	
	
}
