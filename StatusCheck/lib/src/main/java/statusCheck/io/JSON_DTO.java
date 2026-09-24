package statusCheck.io;

import statusCheck.domain.JSON;
import tools.jackson.databind.*;
public class JSON_DTO{
	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static JSON convert(Object object) {
		return new JSON(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
	}
	

}
