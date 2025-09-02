package bt7s7k7.supervisory.composition;

import java.util.ArrayList;
import java.util.function.Consumer;

public class ComponentSignal<T> {
	protected Consumer<T> handler;

	public ComponentSignal(Consumer<T> handler) {
		this.handler = handler;
	}

	public void invoke(T event) {
		if (this.handler == null) return;
		this.handler.accept(event);
	}

	public void invalidate() {
		this.handler = null;
	}

	public boolean isInvalid() {
		return this.handler == null;
	}

	public static class Emitter<T> {
		protected final ArrayList<ComponentSignal<T>> signals = new ArrayList<>();

		public void emit(T event) {
			this.signals.removeIf(ComponentSignal::isInvalid);
			for (var signal : this.signals) {
				signal.invoke(event);
			}
		}

		public void connect(ComponentSignal<T> signal) {
			this.signals.add(signal);
		}

		public void teardown() {

		}
	}
}
