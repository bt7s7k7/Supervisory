package bt7s7k7.supervisory.blocks.programmableLogicController.reactivity;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.treeburst.support.Primitive;

public class TickReactiveDependency extends ReactiveDependency<Primitive.Number> {
	public TickReactiveDependency(ReactivityManager owner, String name, Primitive.Number value) {
		super(owner, name, value);
	}

	@Override
	public boolean isReady() {
		return !this.value.equals(Primitive.ZERO);
	}

	public static TickReactiveDependency get(ReactivityManager manager) {
		var instance = manager.ensureDependency("tick", TickReactiveDependency.class, Primitive.ZERO);
		return instance;
	}
}
