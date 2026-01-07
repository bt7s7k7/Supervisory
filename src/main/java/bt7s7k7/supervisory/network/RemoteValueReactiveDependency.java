package bt7s7k7.supervisory.network;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;

public class RemoteValueReactiveDependency extends ReactiveDependency<ManagedValue> { // @symbol: SYS.RemoteValueReactiveDependency
	// @prototype: ReactiveDependency.prototype
	// @summary: Triggers every time a published resource on the network changes. Returned when reading a value using {@link r}.

	public RemoteValueReactiveDependency(ReactivityManager owner, String name, ManagedValue value) {
		super(owner, name, value);
	}

	@Override
	public boolean isReady() {
		return this.value != Primitive.VOID;
	}

	public static RemoteValueReactiveDependency get(ReactivityManager manager, String resourceKey, ManagedValue value) {
		var key = "remote:" + resourceKey;
		var instance = manager.ensureDependency(key, RemoteValueReactiveDependency.class, value);
		return instance;
	}
}
