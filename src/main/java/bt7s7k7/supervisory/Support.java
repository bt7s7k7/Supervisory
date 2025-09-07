package bt7s7k7.supervisory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.loading.LoadingModList;

public final class Support {
	private Support() {}

	public static <T> Iterable<T> getIterable(Iterable<T> value) {
		return value;
	}

	public static String getNamespacedId(ResourceKey<?> key) {
		return key.location().toString();
	}

	public static void executeIfModInstalled(String id, Supplier<Runnable> callback) {
		if (LoadingModList.get().getModFileById(id) == null) return;
		callback.get().run();
	}

	public static record AnnotatedMethodInfo<T extends Annotation>(Method method, T annotation) {}

	public static <T extends Annotation> List<AnnotatedMethodInfo<T>> getMethodsAnnotatedWith(Class<?> type, Class<T> annotationType) {
		var methods = new ArrayList<AnnotatedMethodInfo<T>>();

		for (; type != Object.class; type = type.getSuperclass()) {
			for (var method : type.getDeclaredMethods()) {
				var annotation = method.getAnnotation(annotationType);
				if (annotation != null) {
					methods.add(new AnnotatedMethodInfo<>(method, annotation));
				}
			}
		}

		return methods;
	}

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
