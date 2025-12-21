package bt7s7k7.supervisory.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import bt7s7k7.supervisory.Supervisory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class GlobalObjectManager {
	private GlobalObjectManager() {};

	public static class InstanceHandle<T> {
		public final Supplier<T> factory;
		public final Consumer<T> cleanup;

		public InstanceHandle(Supplier<T> factory, Consumer<T> cleanup) {
			this.factory = factory;
			this.cleanup = cleanup;
		}

		protected T instance = null;
		protected boolean managed = false;

		protected void teardown() {
			if (this.instance == null) return;
			if (this.cleanup != null) this.cleanup.accept(this.instance);
			this.instance = null;
		}

		public T get() {
			if (this.instance == null) {
				if (!this.managed) {
					handles.add(this);
					this.managed = true;
				}

				this.instance = this.factory.get();
			}

			return this.instance;
		}

		public T tryGet() {
			return this.instance;
		}
	};

	protected static List<InstanceHandle<?>> handles = new ArrayList<>();

	@SubscribeEvent
	private static void teardown(ServerStoppedEvent event) {
		Supervisory.LOGGER.info("Tearing down global objects");

		for (var handle : handles) {
			handle.teardown();
		}
	}
}
