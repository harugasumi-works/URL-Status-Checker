package statuscheck.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

import statuscheck.domain.CSV;
import statuscheck.domain.JSON;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class ExportFile {
	
	public static boolean exportJSON(JSON body) {
        return export(new ExtensionFilter("JSON Files (*.json)", "*.json"), body::data);
    }

    public static boolean exportCSV(CSV body) {
        return export(new ExtensionFilter("CSV Files (*.csv)", "*.csv"), body::data);
    }

    private static boolean export(ExtensionFilter filter, Supplier<String> content) {
    	Stage stage = new Stage();
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(filter);
        chooser.setSelectedExtensionFilter(filter);

        File targetFile = chooser.showSaveDialog(stage);

        if (targetFile == null) {
            return false;
        }

        try {
            Files.writeString(targetFile.toPath(), content.get());
            return true;
        } catch (IOException e) {
            stage.setScene(new Scene(new Group(new Text(10, 40, e.getMessage()))));
            stage.show();
            return false;
        }
    }
}