package bt7s7k7.supervisory.redstone;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.support.Side;
import bt7s7k7.treeburst.support.Primitive;

public class RedstoneReactiveDependency extends ReactiveDependency<Primitive.Number> { // @symbol: RedstoneReactiveDependency
	// @entry-symbol
	// @prototype: ReactiveDependency.prototype
	// @summary: Triggers each time the redstone input changes. The value is a {@link Number} indicating the signal strength.

	public RedstoneReactiveDependency(ReactivityManager owner, String name, Primitive.Number value) {
		super(owner, name, value);
	}

	public static RedstoneReactiveDependency get(ReactivityManager manager, Side direction, int value) {
		var key = "redstone:" + direction.name;
		var instance = manager.ensureDependency(key, RedstoneReactiveDependency.class, Primitive.from(value));
		return instance;
	}
}
