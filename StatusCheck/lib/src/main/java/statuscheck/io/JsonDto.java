package statuscheck.io;

import statuscheck.domain.JSON;
import tools.jackson.databind.*;
public class JsonDto{
	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static JSON convert(Object object) {
		return new JSON(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
	}
	

}
