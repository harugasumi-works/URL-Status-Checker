package statuscheck.ui;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class PopUp {

    private static Boolean isToolkitInitialized = false;

    public static void message(String message) {
        // Ensure FX Toolkit is initialized if no Application class started it
        ensureToolkitInitialized();

        Runnable show = () -> {
            Stage stage = new Stage();
            
            // Note: Modality.WINDOW_MODAL requires an owner to restrict. 
            // Modality.APPLICATION_MODAL blocks all FX windows if no owner is found.
            stage.initModality(Modality.APPLICATION_MODAL);

            Window activeWindow = Window.getWindows().stream()
                    .filter(Window::isFocused)
                    .findFirst()
                    .orElse(null);

            if (activeWindow != null) {
                stage.initOwner(activeWindow);
            }

            stage.setScene(new Scene(new Group(new Text(10, 40, message)), 200, 100));
            stage.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    private static synchronized void ensureToolkitInitialized() {
        if (!isToolkitInitialized) {
            try {
                // Starts the toolkit without creating a primary Stage
                Platform.startup(() -> {}); 
                Platform.setImplicitExit(false); // Prevents app shutdown when this popup closes
                isToolkitInitialized = true;
            } catch (IllegalStateException e) {
                // Toolkit was already started elsewhere
                isToolkitInitialized = true;
            }
        }
    }
}