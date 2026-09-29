package statuscheck.ui.main;

import java.util.function.Consumer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import statuscheck.service.ExportService;
import statuscheck.service.ImportService;
import statuscheck.service.ScanService;
import statuscheck.session.Session;
import statuscheck.session.SessionStore;
import statuscheck.ui.AppState;
import statuscheck.ui.dialogs.Dialogs;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {
		Session session = new Session();
		AppState appState = new AppState();
		SessionStore store = session.store();

		Consumer<String> notifier = msg -> Platform.runLater(() -> Dialogs.message(msg));
		ScanService scan = new ScanService(store, notifier);
		appState.scanningProperty().bind(scan.scanningProperty());

		MainButtons buttons = new MainButtons(store, appState, scan,
				new ExportService(session::currentOutput, () -> primaryStage),
				new ImportService(store), notifier);

		WindowInit.launch(primaryStage, session, appState, buttons);
	}

	public static void main(String[] args) {
		Application.launch(args);
	}

}