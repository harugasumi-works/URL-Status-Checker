package statuscheck.io;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.application.Platform;
import statuscheck.domain.RowItem;
import statuscheck.domain.SessionRow;
import statuscheck.ui.PopUp;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class AutoSave {
	private static final ObjectMapper mapper = JsonMapper.builder().build();
	private static final Path path = Path.of(System.getProperty("user.home"), "StatusCheck", "session.json");
	private static final Path tmpPath = path.resolveSibling("session.json.tmp");
	private static final Path backupPath = path.resolveSibling("session.json.bak");

	private static byte[] launchSnapshot;

	private static volatile boolean saveFailureReported;

	public static boolean save(List<RowItem> items) {
		try {
			Files.createDirectories(path.getParent());
			List<SessionRow> saveData = SessionRow.toSessionRow(items);
			mapper.writeValue(tmpPath.toFile(), saveData);
			moveIntoPlace(tmpPath, path);
			saveFailureReported = false;
			return true;
		} catch (IOException | RuntimeException e) {
			try {
				Files.deleteIfExists(tmpPath);
			} catch (IOException ignored) {
			}
			reportSaveFailure(e);
			return false;
		}
	}

	public static List<RowItem> load() {
		launchSnapshot = null;
		if (!Files.exists(path))
			return List.of();
		try {
			byte[] raw = Files.readAllBytes(path);
			List<SessionRow> rows = mapper.readValue(raw,
					mapper.getTypeFactory().constructCollectionType(List.class, SessionRow.class));
			validate(rows);
			List<RowItem> items = SessionRow.toRowItem(rows);
			launchSnapshot = raw;
			return items;
		} catch (IOException | RuntimeException e) {
			e.printStackTrace();
			boolean backedUp = moveAside();
			PopUp.message(backedUp
					? "Your previous session could not be read. A copy was saved to " + backupPath
					: "Your previous session could not be read.");
			return List.of();
		}
	}

	public static void noSave() {
		try {
			if (launchSnapshot != null) {
				Files.createDirectories(path.getParent());
				Files.write(tmpPath, launchSnapshot);
				moveIntoPlace(tmpPath, path);
			} else {
				Files.deleteIfExists(path);
			}
		} catch (IOException e) {
			PopUp.message(describe(e));
			e.printStackTrace();
		}
	}


	public static void discardStoredSession() {
		launchSnapshot = null;
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void validate(List<SessionRow> rows) {
		if (rows == null)
			throw new IllegalArgumentException("Session file contains no data");
		Set<String> ids = new HashSet<>();
		for (SessionRow row : rows) {
			if (row == null)
				throw new IllegalArgumentException("Session file contains an empty row");
			if (isBlank(row.id()) || isBlank(row.url()))
				throw new IllegalArgumentException("Row without id or url");
			if (!ids.add(row.id()))
				throw new IllegalArgumentException("Duplicate row id: " + row.id());
			switch (String.valueOf(row.rowType())) {
			case "Pending" -> {
			}
			case "Scanned" -> {
				if (!"Success".equals(row.outcome()) && !"Fail".equals(row.outcome()))
					throw new IllegalArgumentException("Unknown outcome: " + row.outcome());
			}
			default -> throw new IllegalArgumentException("Unknown row type: " + row.rowType());
			}
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static void moveIntoPlace(Path from, Path to) throws IOException {
		try {
			Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static boolean moveAside() {
		try {
			Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private static void reportSaveFailure(Exception e) {
		e.printStackTrace();
		if (saveFailureReported)
			return;
		saveFailureReported = true;
		String message = "Could not save session: " + describe(e);
		Platform.runLater(() -> PopUp.message(message));
	}

	private static String describe(Exception e) {
		return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
	}
}