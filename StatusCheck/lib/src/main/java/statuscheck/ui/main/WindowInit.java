package statuscheck.ui.main;

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
import statuscheck.ui.dialogs.Dialogs;
import statuscheck.ui.dialogs.Dialogs.CloseChoice;

public class WindowInit {

	public static void launch(Stage stage, Session session, AppState appState, MainButtons buttons) {
		AutoSaveService autosave = session.autosave();

		stage.setMaximized(true);
		stage.setTitle("URL Status Checker");
		stage.setScene(new MainUI(session, appState, buttons).createMainScene());
		stage.setOnCloseRequest(event -> {
			if (session.store().items().isEmpty()) {
				autosave.saveNow();
				return;
			}
			CloseChoice choice = Dialogs.onClose("Save this session before closing?");
			if (choice == CloseChoice.SAVE) autosave.saveNow();
			if (choice == CloseChoice.DISCARD) {
				autosave.suppress();
				AutoSave.noSave();
			}
			if (choice == CloseChoice.CANCEL) event.consume();
		});
		stage.setOnHidden(_ -> autosave.shutdown());

		List<RowItem> saved = AutoSave.load();
		if (!saved.isEmpty()) {
			if (Dialogs.confirm("Restore previous session?")) {
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