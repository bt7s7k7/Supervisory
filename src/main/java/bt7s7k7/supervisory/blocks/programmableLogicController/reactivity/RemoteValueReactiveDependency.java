package bt7s7k7.supervisory.blocks.programmableLogicController.reactivity;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.treeburst.support.ManagedValue;

public class RemoteValueReactiveDependency extends ReactiveDependency<ManagedValue> {

	public RemoteValueReactiveDependency(ReactivityManager owner, String name, ManagedValue value) {
		super(owner, name, value);
	}

	public static RemoteValueReactiveDependency get(ReactivityManager manager, String resourceKey, ManagedValue value) {
		var key = "remote:" + resourceKey;
		var instance = manager.ensureDependency(key, RemoteValueReactiveDependency.class, value);
		return instance;
	}
}
