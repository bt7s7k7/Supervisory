package bt7s7k7.supervisory;

import net.minecraft.resources.ResourceKey;

public final class Support {
	private Support() {}

	public static <T> Iterable<T> getIterable(Iterable<T> value) {
		return value;
	}

	public static String getNamespacedId(ResourceKey<?> key) {
		return key.location().toString();
	}
}
