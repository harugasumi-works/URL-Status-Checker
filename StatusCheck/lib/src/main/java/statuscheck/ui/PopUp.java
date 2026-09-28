package statuscheck.ui;

import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
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
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == ButtonType.YES;
    }
    
    public enum CloseChoice { SAVE, DISCARD, CANCEL }
    
    public static CloseChoice onClose(String message) {
    	Alert alert = new Alert(AlertType.CONFIRMATION);
    	ButtonType save = new ButtonType("Save", ButtonData.YES);
    	ButtonType dontSave = new ButtonType("Don't Save", ButtonData.NO);
    	ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(save, dontSave, cancel);

        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isEmpty()) return CloseChoice.CANCEL;
        if (result.get() == save) return CloseChoice.SAVE;
        if (result.get() == dontSave) return CloseChoice.DISCARD;
        return CloseChoice.CANCEL;
    }
}