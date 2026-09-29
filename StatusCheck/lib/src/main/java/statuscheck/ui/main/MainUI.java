package statuscheck.ui.main;

import java.util.UUID;

import statuscheck.domain.Fail;
import statuscheck.domain.RowItem;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.Success;
import statuscheck.session.Session;
import statuscheck.session.SessionStore;
import statuscheck.ui.AppState;
import statuscheck.util.ContentParser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class MainUI {

	private final Session session;
	private final SessionStore store;
	private final AppState appState;
	private final MainButtons buttons;

	public MainUI(Session session, AppState appState, MainButtons buttons) {
		this.session = session;
		this.store = session.store();
		this.appState = appState;
		this.buttons = buttons;
	}

	public TextField input() {
		TextField field = new TextField();
		field.setPromptText("Type here...");
		field.setPrefWidth(250);

		Runnable addURL = () -> {
			String raw = field.getText();

			if (raw == null || raw.isBlank()) {
				return;
			}

			String input = ContentParser.normalize(raw);

			if (!ContentParser.isValidURL(input)) {
				appState.setNotice("Not a valid URL: " + raw.strip());
				return;
			}

			if (store.addPending(
					new ScanRequest(UUID.randomUUID().toString(), input))) {
				field.clear();
				appState.setNotice("");
			} else {
				appState.setNotice("Already in the list: " + input);
			}
		};
		
		field.setOnAction(_ -> addURL.run());
		field.textProperty().addListener((_, _, _) -> appState.setNotice(""));

		return field;
	}

	public ToolBar toolBar() {
		ToolBar toolbar = new ToolBar(buttons.scanButton(), buttons.saveButton(), new Label("Enter Text:"),
				input(), buttons.deleteAllButton(), buttons.bulkImportButton(), new Separator());

		toolbar.disableProperty().bind(appState.scanningProperty());
		return toolbar;
	}

	public static <T> BorderPane pane(ToolBar toolBar, TableView<T> table, HBox bar) {
		BorderPane root = new BorderPane();
		root.setTop(toolBar);
		root.setCenter(table);
		root.setBottom(bar);
		return root;
	}

	@SuppressWarnings("unchecked")
	public TableView<RowItem> requestTable() {
		// store.items() is read-only, so column sorting must go through a SortedList.
		SortedList<RowItem> sorted = new SortedList<>(store.items());
		TableView<RowItem> table = new TableView<>(sorted);
		sorted.comparatorProperty().bind(table.comparatorProperty());

		table.setEditable(false);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
		table.getColumns().addAll(urlColumn(), statusColumn(), codeColumn(), detailColumn());
		table.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.DELETE) {
				RowItem selectedItem = table.getSelectionModel().getSelectedItem();

				if (selectedItem != null) {
					store.remove(selectedItem);
					event.consume();
				}
			}
		});
		return table;
	}


	public static TableColumn<RowItem, String> urlColumn() {
		TableColumn<RowItem, String> col = new TableColumn<>("URL");
		col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().url()));
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

	public Label updateInfo() {
		Label label = new Label();
		label.textProperty().bind(session.autosave().statusProperty());
		return label;
	}

	public HBox bottomBar() {
		Label noticeLabel = new Label();
		noticeLabel.textProperty().bind(appState.noticeProperty());
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox box = new HBox(8, buttons.cancelButton(), noticeLabel, spacer, updateInfo());
		box.setAlignment(Pos.CENTER_LEFT);
		box.setPadding(new Insets(4, 10, 4, 10));
		return box;
	}

	public Scene createMainScene() {
		return new Scene(pane(toolBar(), requestTable(), bottomBar()), 800, 600);
	}

}