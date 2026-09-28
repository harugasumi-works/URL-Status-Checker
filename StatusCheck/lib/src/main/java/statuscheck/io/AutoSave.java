package statuscheck.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import statuscheck.domain.RowItem;
import statuscheck.domain.SessionRow;
import statuscheck.ui.PopUp;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class AutoSave {
	private static final ObjectMapper mapper = JsonMapper.builder().build();
	private static final Path path = Path.of(System.getProperty("user.home"), "StatusCheck", "session.json");

	

	public static boolean save(List<RowItem> items) {
		try {
			Files.createDirectories(path.getParent());
			List<SessionRow> saveData = SessionRow.toSessionRow(items);
			mapper.writeValue(path.toFile(), saveData);
			return true;
		} catch (JacksonException e) {
			PopUp.message("Could not save session: " + e.getMessage());
			return false;
		} catch (IOException e) {
			PopUp.message("Failed to create dir: " + e.getMessage());
			return false;
		}
	}

	public static List<RowItem> load() {
		if (!Files.exists(path))
			return List.of();
		try {
			List<SessionRow> retrievedData = mapper.readValue(path.toFile(),
					mapper.getTypeFactory().constructCollectionType(List.class, SessionRow.class));
			return SessionRow.toRowItem(retrievedData);
		} catch (JacksonException e) {
			PopUp.message(e.getMessage());
			return List.of();
		}
	}
	
	public static void noSave() {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			PopUp.message(e.getMessage());
			e.printStackTrace();
		}
	}
}
