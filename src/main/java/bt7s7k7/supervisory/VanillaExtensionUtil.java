package bt7s7k7.supervisory;

public final class VanillaExtensionUtil {
	private VanillaExtensionUtil() {}

	public static <T> T getField(Object object, String fieldName, Class<?> sourceClass) {
		try {
			var field = sourceClass.getDeclaredField(fieldName);
			field.setAccessible(true);

			@SuppressWarnings("unchecked")
			var value = (T) field.get(object);

			return value;
		} catch (NoSuchFieldException | SecurityException | IllegalAccessException exception) {
			throw new RuntimeException(exception);
		}
	}
}
