package bt7s7k7.supervisory;

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
}
