package statuscheck.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import statuscheck.ui.dialogs.Dialogs;

public class ImportURLs {

	public static void urlImport(Stage stage, TextArea area) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Select a text file");
		chooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

		File selected = chooser.showOpenDialog(stage);
		if (selected == null)
			return;

		try {
			String content = Files.readString(selected.toPath())
					.replace("\uFEFF", "");

			List<String> urls = Arrays.stream(content.split("\\s+"))
					.filter(s -> !s.isEmpty())
					.toList();

			area.setText(String.join("\n", urls));
		} catch (IOException e) {
			Dialogs.message("Could not read file: " + e.getMessage());
		}
	}
}