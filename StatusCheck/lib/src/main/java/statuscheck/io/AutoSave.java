package statuscheck.io;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import javafx.application.Platform;
import statuscheck.domain.RowItem;
import statuscheck.domain.SessionRow;
import statuscheck.ui.dialogs.Dialogs;
import statuscheck.util.ErrorSpecs;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class AutoSave {
	private static final ObjectMapper mapper = JsonMapper.builder().build();
	private static final Path path = Path.of(System.getProperty("user.home"), "StatusCheck", "session.json");
	private static final Path tmpPath = path.resolveSibling("session.json.tmp");

	private static final String BACKUP_PREFIX = "session-";
	private static final String BACKUP_SUFFIX = ".json.bak";
	private static final int MAX_BACKUPS = 5;
	private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private static final Object IO_LOCK = new Object();
	private static byte[] launchSnapshot; 
	private static boolean discarded; 

	private static volatile boolean saveFailureReported;

	public static boolean save(List<RowItem> items) {
		Exception failure;
		synchronized (IO_LOCK) {
			if (discarded) {
				return false; 
			}
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
				failure = e;
			}
		}
		reportSaveFailure(failure);
		return false;
	}

	public static List<RowItem> load() {
		String problem;
		synchronized (IO_LOCK) {
			discarded = false;
			launchSnapshot = null;
			if (!Files.exists(path))
				return List.of();
			try {
				byte[] raw = Files.readAllBytes(path);
				List<SessionRow> rows = mapper.readValue(raw,
						mapper.getTypeFactory().constructCollectionType(List.class, SessionRow.class));
				List<RowItem> items = SessionRow.toRowItem(rows);
				launchSnapshot = raw;
				return items;
			} catch (IOException | RuntimeException e) {
				Dialogs.message(ErrorSpecs.describe(e));
				Path backup = moveAside();
				problem = backup != null
						? "Your previous session could not be read. A copy was saved to " + backup
						: "Your previous session could not be read.";
			}
		}
		Dialogs.message(problem);
		return List.of();
	}

	public static void noSave() {
		IOException failure = null;
		synchronized (IO_LOCK) {
			discarded = true;
			try {
				if (launchSnapshot != null) {
					Files.createDirectories(path.getParent());
					Files.write(tmpPath, launchSnapshot);
					moveIntoPlace(tmpPath, path);
				} else {
					Files.deleteIfExists(path);
				}
			} catch (IOException e) {
				failure = e;
			}
		}
		if (failure != null) {
			Dialogs.message(ErrorSpecs.describe(failure));
		}
	}

	public static void discardStoredSession() {
		synchronized (IO_LOCK) {
			launchSnapshot = null;
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {
				Dialogs.message(ErrorSpecs.describe(e));
			}
		}
	}

	private static void moveIntoPlace(Path from, Path to) throws IOException {
		try {
			Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static Path moveAside() {
		String stamp = LocalDateTime.now().format(BACKUP_STAMP);
		for (int n = 0; n < 100; n++) {
			Path target = path.resolveSibling(BACKUP_PREFIX + stamp + (n == 0 ? "" : "-" + n) + BACKUP_SUFFIX);
			try {
				Files.move(path, target); 
				pruneBackups();
				return target;
			} catch (FileAlreadyExistsException e) {
			} catch (IOException e) {
				Dialogs.message(ErrorSpecs.describe(e));
				return null;
			}
		}
		return null;
	}

	private static void pruneBackups() {
		try (Stream<Path> files = Files.list(path.getParent())) {
			List<Path> backups = files.filter(p -> {
				String name = p.getFileName().toString();
				return name.startsWith(BACKUP_PREFIX) && name.endsWith(BACKUP_SUFFIX);
			}).sorted().toList();
			for (int i = 0; i < backups.size() - MAX_BACKUPS; i++) {
				Files.deleteIfExists(backups.get(i));
			}
		} catch (IOException e) {
			Dialogs.message(ErrorSpecs.describe(e));
		}
	}

	private static void reportSaveFailure(Exception e) {
		if (saveFailureReported)
			return;
		saveFailureReported = true;
		String message = "Could not save session: " + ErrorSpecs.describe(e);
		Platform.runLater(() -> Dialogs.message(message));
	}

}