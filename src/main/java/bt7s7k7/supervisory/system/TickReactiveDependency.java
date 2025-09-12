package bt7s7k7.supervisory.system;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.treeburst.support.Primitive;

public class TickReactiveDependency extends ReactiveDependency<Primitive.Number> { // @symbol: TickReactiveDependency
	// @entry-symbol
	// @prototype:ReactiveDependency.prototype
	// @summary: Triggers on a set interval of ticks. The value is a {@link Number} corresponding to the current game tick of the world.

	public TickReactiveDependency(ReactivityManager owner, String name, Primitive.Number value) {
		super(owner, name, value);
	}

	@Override
	public boolean isReady() {
		return !this.value.equals(Primitive.ZERO);
	}

	public static TickReactiveDependency get(String name, ReactivityManager manager) {
		var instance = manager.ensureDependency(name, TickReactiveDependency.class, Primitive.ZERO);
		return instance;
	}
}
