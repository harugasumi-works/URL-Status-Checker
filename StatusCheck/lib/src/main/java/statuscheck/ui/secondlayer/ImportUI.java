package statuscheck.ui.secondlayer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class ImportUI {

	public static BorderPane pane(TextArea area, HBox box) {
		Label label = new Label("Please write one link in each line");
		BorderPane root = new BorderPane();
		root.setTop(label);
		root.setCenter(area);
		root.setBottom(box);
		return root;
	}

	public static TextArea inputArea() {
		TextArea area = new TextArea();
		area.setPromptText("""
				e.g:
				google.com
				www.google.com/webhp
				""");
		return area;
	}

	public static HBox buttons(Button add, Button cancel, Button importButton) {
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox box = new HBox(add, cancel, spacer, importButton);

		box.setPadding(new Insets(10));
		return box;
	}

	public static Scene createScene(TextArea area, HBox box) {
		return new Scene(pane(area, box), 500, 300);
	}
}
