package statuscheck.ui;

import javafx.stage.Stage;

public class WindowInit {
			
	public static void launch(Stage stage) {
        stage.setMaximized(true);
        stage.setTitle("URL Status Checker");
        stage.setScene(UIFactory.createScene());
        stage.show();
	}

}
