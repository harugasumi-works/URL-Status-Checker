package statuscheck.util;

import java.util.ArrayList;
import java.util.List;

public record ImportCheck(List<String> valid, List<String> rejectedLines, int invalid) {

	public static ImportCheck parseImport(String text) {
		List<String> valid = new ArrayList<>();
		List<String> rejected = new ArrayList<>();
		int invalid = 0;

		for (String line : text.lines().toList()) {
			if (line.isBlank())
				continue;

			String normalized = ContentParser.normalize(line);

			if (ContentParser.isValidURL(normalized)) {
				valid.add(normalized);
			} else {
				invalid++;
				rejected.add(line);
			}
		}

		return new ImportCheck(valid, rejected, invalid);
	}
}