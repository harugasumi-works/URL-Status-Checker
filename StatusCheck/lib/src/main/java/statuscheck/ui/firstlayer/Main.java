package statuscheck.ui.firstlayer;

import javafx.application.Application;
import javafx.stage.Stage;
import statuscheck.session.Session;
import statuscheck.ui.AppState;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {
		// start() runs on the FX thread, which is where Session and AppState must be used.
		Session session = new Session();
		AppState appState = new AppState();
		WindowInit.launch(primaryStage, session, appState);
	}

	public static void main(String[] args) {
		Application.launch(args);
	}

}