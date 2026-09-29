package statuscheck.io;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.function.Supplier;

import statuscheck.domain.CSV;
import statuscheck.domain.JSON;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

public class ExportFile {

	public static boolean exportJSON(Window owner, JSON body) {
		return export(owner, new ExtensionFilter("JSON Files (*.json)", "*.json"), body::data);
	}

	public static boolean exportCSV(Window owner, CSV body) {
		return export(owner, new ExtensionFilter("CSV Files (*.csv)", "*.csv"), body::data);
	}

	private static boolean export(Window owner, ExtensionFilter filter, Supplier<String> content) {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().add(filter);
		chooser.setSelectedExtensionFilter(filter);

		File targetFile = chooser.showSaveDialog(owner);

		if (targetFile == null) {
			return false;
		}

		try {
			Files.writeString(targetFile.toPath(), content.get());
			return true;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}