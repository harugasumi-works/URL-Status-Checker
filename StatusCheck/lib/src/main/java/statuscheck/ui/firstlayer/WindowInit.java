package statuscheck.ui.firstlayer;

import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Stage;
import javafx.util.Duration;
import statuscheck.domain.RowItem;
import statuscheck.io.AutoSave;
import statuscheck.session.AutoSaveService;
import statuscheck.session.Session;
import statuscheck.ui.AppState;
import statuscheck.ui.PopUp;
import statuscheck.ui.PopUp.CloseChoice;

public class WindowInit {

	public static void launch(Stage stage, Session session, AppState appState) {
		AutoSaveService autosave = session.autosave();

		stage.setMaximized(true);
		stage.setTitle("URL Status Checker");
		stage.setScene(new MainUI(session, appState).createMainScene());
		stage.setOnCloseRequest(event -> {
			if (session.store().items().isEmpty()) {
				autosave.saveNow();
				return;
			}
			CloseChoice choice = PopUp.onClose("Save this session before closing?");
			if (choice == CloseChoice.SAVE) autosave.saveNow();
			if (choice == CloseChoice.DISCARD) {
				autosave.suppress();
				AutoSave.noSave();
			}
			if (choice == CloseChoice.CANCEL) event.consume();
		});
		// Lets the save executor finish queued work and exit once the window is really gone.
		stage.setOnHidden(_ -> autosave.shutdown());

		List<RowItem> saved = AutoSave.load();
		if (!saved.isEmpty()) {
			if (PopUp.confirm("Restore previous session?")) {
				session.store().restore(saved);
			} else {
				AutoSave.discardStoredSession();
			}
		}
		stage.show();

		Timeline autosaveTimer = new Timeline(new KeyFrame(Duration.seconds(20), _ -> autosave.saveIfDirty()));
		autosaveTimer.setCycleCount(Timeline.INDEFINITE);
		autosaveTimer.play();
	}

}