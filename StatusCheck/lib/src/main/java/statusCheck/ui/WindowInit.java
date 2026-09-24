package statusCheck.ui;

import javafx.stage.Stage;

public class WindowInit {
			
	public static void launch(Stage stage) {
        stage.setMaximized(true);
        stage.setTitle("Application");
        stage.setScene(UIFactory.createScene());
        stage.show();
	}

}
