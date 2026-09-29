package statuscheck.io;

import statuscheck.domain.JSON;
import statuscheck.util.UrlCredentialSanitizer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class JsonDto {

	private static final ObjectMapper mapper = new ObjectMapper();

	public static JSON convert(Object object) {
		JsonNode node = mapper.valueToTree(object);
		JsonNode sanitized = sanitizeNode(node);

		return new JSON(
				mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sanitized));
	}

	private static JsonNode sanitizeNode(JsonNode node) {
		if (node == null)
			return null;

		if (node.isObject()) {
			ObjectNode sanitized = mapper.createObjectNode();

			for (var entry : node.properties()) {
				String fieldName = entry.getKey();
				JsonNode value = entry.getValue();

				if (value != null && value.isString() && isUrlField(fieldName)) {
					sanitized.put(
							fieldName,
							UrlCredentialSanitizer.removeCredentials(value.asString()));
				} else {
					sanitized.set(fieldName, sanitizeNode(value));
				}
			}

			return sanitized;
		}

		if (node.isArray()) {
			ArrayNode sanitized = mapper.createArrayNode();

			for (JsonNode child : node) {
				sanitized.add(sanitizeNode(child));
			}

			return sanitized;
		}

		return node;
	}

	private static boolean isUrlField(String fieldName) {
		return "requestedURL".equals(fieldName) || "url".equals(fieldName);
	}

}