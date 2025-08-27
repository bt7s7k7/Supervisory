package bt7s7k7.supervisory.script.reactivity;

import java.util.HashSet;

import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.runtime.ManagedObject;
import bt7s7k7.treeburst.runtime.UnmanagedHandle;
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

	public class Handle extends UnmanagedHandle {
		@Override
		public boolean getProperty(String name, ExpressionResult result) {
			if (name.equals("value")) {
				result.value = ReactiveDependency.this.value;
				return true;
			}

			return super.getProperty(name, result);
		}

		public Handle(ManagedObject prototype) {
			super(prototype, ReactiveDependency.this);
			this.name = ReactiveDependency.this.name;
		}
	}

	private Handle cachedHandle;

	public Handle makeHandle() {
		if (this.cachedHandle == null) {
			this.cachedHandle = new Handle(this.owner.globalScope.TablePrototype);
		}

		return this.cachedHandle;
	}
}
