package statuscheck.ui;

import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class PopUp {

    public static void message(String message) {
        Runnable show = () -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Notification");
            alert.setHeaderText(null); 
            alert.setContentText(message);
            
            alert.showAndWait();
        };

        // Ensure the Alert is always created on the JavaFX Application Thread
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }
    
    public static boolean confirm(String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Replace default OK/Cancel buttons with Yes and No
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        // Blocks until the user clicks a button
        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == ButtonType.YES;
    }
}