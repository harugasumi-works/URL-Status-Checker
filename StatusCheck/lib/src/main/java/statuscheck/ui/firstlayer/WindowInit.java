package statuscheck.ui.firstlayer;

import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Stage;
import javafx.util.Duration;
import statuscheck.domain.RowItem;
import statuscheck.io.AutoSave;
import statuscheck.ui.PopUp;
import statuscheck.ui.PopUp.CloseChoice;
import statuscheck.ui.UILogic;

public class WindowInit {

	public static void launch(Stage stage) {
		stage.setMaximized(true);
		stage.setTitle("URL Status Checker");
		stage.setScene(MainUI.createMainScene());
		stage.setOnCloseRequest(event -> {
			if (UILogic.items.isEmpty()) {
				UILogic.saveIfDirty();
				return;
			}
			CloseChoice choice = PopUp.onClose("Save this session before closing?");
			if (choice == CloseChoice.SAVE) UILogic.saveIfDirty();
			if (choice == CloseChoice.DISCARD) {
				UILogic.preventAutoSave();
				AutoSave.noSave();
			}
			if(choice == CloseChoice.CANCEL) event.consume();
		});

		List<RowItem> saved = AutoSave.load();
		if (!saved.isEmpty()) {
			if (PopUp.confirm("Restore previous session?")) {
				UILogic.restoreSession(saved);
			} else {
				AutoSave.discardStoredSession();
			}
		}
		stage.show();

		Timeline autosaveTimer = new Timeline(new KeyFrame(Duration.seconds(20), _ -> UILogic.saveIfDirty()));
		autosaveTimer.setCycleCount(Timeline.INDEFINITE);
		autosaveTimer.play();
	}

}
