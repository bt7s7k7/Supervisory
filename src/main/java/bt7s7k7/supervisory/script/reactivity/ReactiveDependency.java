package bt7s7k7.supervisory.script.reactivity;

import java.util.HashSet;

import bt7s7k7.treeburst.runtime.NativeHandle;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.ManagedValue;

public abstract class ReactiveDependency<T extends ManagedValue> {
	protected HashSet<ReactiveScope> subscribers = new HashSet<>();
	protected final String name;
	protected final ReactivityManager owner;

	public String getName() {
		return this.name;
	}

	protected T value;

	public boolean isReady() {
		return true;
	}

	public T getValue() {
		return this.value;
	}

	public ReactiveDependency(ReactivityManager owner, String name, T value) {
		this.owner = owner;
		this.name = name;
		this.value = value;
	}

	public void subscribe(ReactiveScope scope) {
		var success = this.subscribers.add(scope);
		if (!success) {
			throw new IllegalArgumentException("Duplicate reactive scope subscription to dependency " + this.name);
		}
	}

	public void unsubscribe(ReactiveScope scope) {
		var wasSubscribed = this.subscribers.remove(scope);
		if (!wasSubscribed) {
			throw new IllegalArgumentException("Tried to unsubscribe reactive scope that was not subscribed from dependency " + this.name);
		}
	}

	public void updateValue(T value) {
		this.value = value;

		for (var subscriber : this.subscribers) {
			this.owner.queueReaction(subscriber);
		}
	}

	private NativeHandle cachedHandle;

	public NativeHandle getHandle() {
		if (this.cachedHandle == null) {
			this.cachedHandle = WRAPPER.getHandle(this, this.owner.globalScope);
			this.cachedHandle.name = this.name;
		}

		return this.cachedHandle;
	}

	@SuppressWarnings("rawtypes")
	protected static final NativeHandleWrapper<ReactiveDependency> WRAPPER = new NativeHandleWrapper<>("ReactiveDependency", ReactiveDependency.class, ctx -> ctx
			// @summary: Represents a value that can change and the change can be observed using the {@link reactive} function.
			.addGetter("value", ReactiveDependency::getValue) // @type: any, @summary: The value of the dependency. Will be {@link void} if the dependency does not have a value.
			.addDumpMethod((self, depth, scope, result) -> {
				var value = scope.globalScope.tryInspect(self.value, depth, result);
				if (value == null) return null;
				return self.name + "[" + value + "]";
			}));
}
