package statuscheck.ui.firstlayer;

import java.util.Optional;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import statuscheck.io.ImportURLs;
import statuscheck.service.ExportService;
import statuscheck.service.ImportService;
import statuscheck.service.ScanService;
import statuscheck.session.SessionStore;
import statuscheck.ui.AppState;
import statuscheck.ui.PopUp;
import statuscheck.ui.secondlayer.ImportButtons;
import statuscheck.ui.secondlayer.ImportUI;

/** Builds the buttons and wires them to services. Contains no scan, export or import logic. */
public class MainButtons {

	private final SessionStore store;
	private final AppState appState;
	private final ScanService scan;
	private final ExportService export;
	private final ImportService importer;
	private final Consumer<String> notifier;

	public MainButtons(SessionStore store, AppState appState, ScanService scan, ExportService export,
			ImportService importer, Consumer<String> notifier) {
		this.store = store;
		this.appState = appState;
		this.scan = scan;
		this.export = export;
		this.importer = importer;
		this.notifier = notifier;
	}

	public void cancelScan() {
		scan.cancel();
	}

	public Button cancelButton() {
		Button button = new Button("Cancel scan");
		button.disableProperty().bind(appState.scanningProperty().not());
		button.setOnAction(_ -> scan.cancel());
		return button;
	}

	public Button scanButton() {
		Button button = new Button("Scan");
		button.setOnAction(_ -> scan.start());
		return button;
	}

	private MenuItem exportItem(String label, ExportService.Format format) {
		MenuItem item = new MenuItem(label);
		item.setOnAction(_ -> notifier.accept(export.export(format).message()));
		return item;
	}

	public MenuItem json() {
		return exportItem("Export to JSON", ExportService.Format.JSON);
	}

	public MenuItem csv() {
		return exportItem("Export to CSV", ExportService.Format.CSV);
	}

	public SplitMenuButton saveButton() {
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
		button.disableProperty().bind(store.hasDataProperty().not());
		return button;
	}

	public Button deleteAllButton() {
		Button button = new Button("Clear all");
		button.setOnAction(_ -> {
			if (PopUp.confirm("Are you sure you want to clear all items?")) {
				store.clear();
			}
		});
		button.disableProperty().bind(Bindings.isEmpty(store.items()));
		return button;
	}

	public Button bulkImportButton() {
		Button button = new Button("Bulk Import");

		button.setOnAction(_ -> {
			Stage stage = new Stage();
			stage.initOwner(button.getScene().getWindow());
			stage.initModality(Modality.WINDOW_MODAL);

			TextArea area = ImportUI.inputArea();

			Button add = ImportButtons.addButton();
			add.setOnAction(_ -> {
				Optional<ImportService.Report> result = importer.addFrom(area.getText());
				if (result.isEmpty()) {
					notifier.accept("Enter at least one URL before adding.");
					return;
				}
				ImportService.Report report = result.get();
				// Keep only the rejected lines in the box so the user can fix them.
				area.setText(String.join("\n", report.rejectedLines()));
				appState.setNotice(report.summary());
				if (report.skipped() > 0) {
					notifier.accept(report.summary()
							+ (report.rejectedLines().isEmpty() ? "" : "\n\nRejected lines were left in the box."));
				}
			});
			add.disableProperty().bind(Bindings.createBooleanBinding(
					() -> !ImportService.hasValidUrls(area.getText()), area.textProperty()));

			Button cancel = ImportButtons.cancelButton();
			cancel.setOnAction(_ -> stage.close());

			Button importButton = ImportButtons.importFromFilesButton();
			importButton.setOnAction(_ -> ImportURLs.urlImport(stage, area));

			HBox box = ImportUI.buttons(add, cancel, importButton);

			stage.setScene(ImportUI.createScene(area, box));
			stage.showAndWait();
		});

		return button;
	}

}