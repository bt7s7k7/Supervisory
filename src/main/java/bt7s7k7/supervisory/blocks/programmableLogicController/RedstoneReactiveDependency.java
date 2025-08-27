package bt7s7k7.supervisory.blocks.programmableLogicController;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.support.RelativeDirection;
import bt7s7k7.treeburst.support.Primitive;

public class RedstoneReactiveDependency extends ReactiveDependency<Primitive.Number> {
	public RedstoneReactiveDependency(ReactivityManager owner, String name, Primitive.Number value) {
		super(owner, name, value);
	}

	public static RedstoneReactiveDependency get(ReactivityManager manager, RelativeDirection direction, int value) {
		var key = "redstone:" + direction.name;
		var instance = manager.ensureDependency(key, RedstoneReactiveDependency.class, Primitive.from(value));
		return instance;
	}
}
