package statusCheck.ui;

import java.util.UUID;

import statusCheck.domain.Fail;
import statusCheck.domain.RowItem;
import statusCheck.domain.ScanRequest;
import statusCheck.domain.Success;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;

public class UIFactory {

	
	public static TextField input() {
		TextField field = new TextField();
		field.setPromptText("Type here...");
        field.setPrefWidth(250);
        
        Runnable addURL = () -> {
            String input = field.getText().trim();
            if (!input.isEmpty()) {
            	UILogic.addPending(new ScanRequest(UUID.randomUUID().toString(), input));
                field.clear();
            }
        };
        
        field.setOnAction(_ -> addURL.run());
        
		return field;
	}
	
	public static ToolBar toolBar() {
		return new ToolBar(
        		ButtonFactory.scanButton(), 
        		ButtonFactory.saveButton(),
        		new Label("Enter Text:"),
        		UIFactory.input(),
        		new Separator());  
	}
	
	
	
	public static <T> BorderPane pane(ToolBar bar, TableView<T> table) {
		BorderPane root = new BorderPane();
        root.setTop(bar);
        root.setCenter(table);
        return root;
	}
	

	@SuppressWarnings("unchecked")
	public static TableView<RowItem> requestTable() {
		TableView<RowItem> table = new TableView<>(UILogic.items);
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().addAll(urlColumn(), statusColumn(), codeColumn(), detailColumn());
        return table;
		
	}

	
	public static TableColumn<RowItem, String> urlColumn() {
	    TableColumn<RowItem, String> col = new TableColumn<>("URL");
	    col.setCellValueFactory(data -> new SimpleStringProperty(switch (data.getValue()) {
	        case RowItem.Pending p -> p.request().requestedURL();
	        case RowItem.Scanned s -> s.result().context().requestedURL();
	    }));
	    return col;
	}

	public static TableColumn<RowItem, String> statusColumn() {
	    TableColumn<RowItem, String> col = new TableColumn<>("Status");
	    col.setCellValueFactory(data -> new SimpleStringProperty(switch (data.getValue()) {
	        case RowItem.Pending _ -> "Pending";
	        case RowItem.Scanned s -> switch (s.result().outcome()) {
	            case Success _ -> "Success";
	            case Fail _ -> "Fail";
	        };
	    }));
	    return col;
	}

	@SuppressWarnings("preview")
	public static TableColumn<RowItem, String> codeColumn() {
	    TableColumn<RowItem, String> col = new TableColumn<>("Code");
	    col.setCellValueFactory(data -> new SimpleStringProperty(switch (data.getValue()) {
	        case RowItem.Pending _ -> "—";
	        case RowItem.Scanned s -> switch (s.result().outcome()) {
	            case Success(_, var code, _) -> String.valueOf(code);
	            case Fail(_, var code, _) -> String.valueOf(code);
	        };
	    }));
	    return col;
	}

	@SuppressWarnings("preview")
	public static TableColumn<RowItem, String> detailColumn() {
	    TableColumn<RowItem, String> col = new TableColumn<>("Detail");
	    col.setCellValueFactory(data -> new SimpleStringProperty(switch (data.getValue()) {
	        case RowItem.Pending _ -> "";
	        case RowItem.Scanned s -> switch (s.result().outcome()) {
	            case Success(var _, _, var latency) -> latency + " ms";
	            case Fail(var _, _, var msg) -> msg;
	        };
	    }));
	    return col;
	}
	
	
	
	public static Scene createScene() {
		return new Scene(pane(toolBar(), requestTable()), 800, 600);
	}

}
