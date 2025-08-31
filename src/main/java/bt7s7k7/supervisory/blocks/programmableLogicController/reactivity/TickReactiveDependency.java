package bt7s7k7.supervisory.blocks.programmableLogicController.reactivity;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.treeburst.support.Primitive;

public class TickReactiveDependency extends ReactiveDependency<Primitive.Number> {
	public TickReactiveDependency(ReactivityManager owner, String name, Primitive.Number value) {
		super(owner, name, value);
	}

	public static TickReactiveDependency get(ReactivityManager manager, double value) {
		var instance = manager.ensureDependency("tick", TickReactiveDependency.class, Primitive.from(value));
		return instance;
	}
}
