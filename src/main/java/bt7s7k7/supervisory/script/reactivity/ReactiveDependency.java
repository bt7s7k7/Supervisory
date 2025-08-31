package bt7s7k7.supervisory.script.reactivity;

import java.util.HashSet;

import bt7s7k7.treeburst.runtime.NativeHandle;
import bt7s7k7.treeburst.support.ManagedValue;

public abstract class ReactiveDependency<T extends ManagedValue> {
	protected HashSet<ReactiveScope> subscribers = new HashSet<>();
	protected final String name;
	protected final ReactivityManager owner;

	public String getName() {
		return name;
	}

	protected T value;

	public T getValue() {
		return value;
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
			this.cachedHandle = new NativeHandle(this.owner.reactiveDependencyPrototype, this);
			this.cachedHandle.name = this.name;
		}

		return this.cachedHandle;
	}
}
