package statuscheck.util;

public class ErrorSpecs {
	
	public static String describe(Throwable t) {
		String message = t.getMessage();
		return (message == null || message.isBlank()) ? t.getClass().getSimpleName() : message;
	}
}
