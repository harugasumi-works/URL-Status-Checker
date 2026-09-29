package statuscheck.ui.importdialog;

import javafx.geometry.Pos;
import javafx.scene.control.Button;

public class ImportButtons {
	public static Button addButton() {
		Button button = new Button("Add");
		return button;
	}

	public static Button cancelButton() {
		Button button = new Button("Cancel");
		return button;
	}

	public static Button importFromFilesButton() {
		Button button = new Button("Import");
		button.setAlignment(Pos.BOTTOM_RIGHT);
		return button;
	}
}
