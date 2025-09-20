package bt7s7k7.supervisory.script.reactivity;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import bt7s7k7.supervisory.script.ManagedWorkDispatcher;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Position;

public class ReactivityManager {
	public boolean ready = false;
	public final GlobalScope globalScope;
	protected final ManagedWorkDispatcher workDispatcher;

	public ReactivityManager(GlobalScope globalScope, ManagedWorkDispatcher workDispatcher) {
		this.globalScope = globalScope;
		this.workDispatcher = workDispatcher;

		globalScope.declareGlobal("reactive", NativeFunction.simple(globalScope, List.of("callback"), List.of(ManagedFunction.class), (args, scope, result) -> {
			// @summary[[Creates a new {@link ReactiveScope}. The callback receives an instance of
			// {@link ReactiveScope} that you can use to set what dependencies to react to. The
			// callback will be executed once, during initialization, and the every time any of the
			// dependencies change.]]
			if (this.ready == true) {
				result.setException(new Diagnostic("Cannot create more reactive scopes after initialization", Position.INTRINSIC));
			}

			var callback = args.get(0).getFunctionValue();
			var reactiveScope = new ReactiveScope(this, globalScope, callback);
			this.queueReaction(reactiveScope);
			reactiveScope.run(scope, result);
		}));

		ReactiveDependency.WRAPPER.ensurePrototype(globalScope);
	}

	protected Stack<ReactiveScope> scopeStack = new Stack<>();
	protected HashSet<ReactiveScope> pendingScopes = new HashSet<>();

	public void pushScopeToStack(ReactiveScope scope) {
		this.scopeStack.push(scope);
	}

	public void popScopeFromStack(ReactiveScope scope) {
		if (this.scopeStack.peek() != scope) {
			throw new IllegalArgumentException("Tried to pop a reactive scope from a stack that was not the top scope");
		}
		this.scopeStack.pop();
	}

	protected HashMap<String, ReactiveDependency<?>> dependencies = new HashMap<>();

	public <TValue extends ManagedValue, TDependency extends ReactiveDependency<TValue>> TDependency ensureDependency(String name, Class<TDependency> type, TValue value) {
		var existing = this.dependencies.get(name);
		if (existing != null) return type.cast(existing);

		var ctor = type.getConstructors()[0];
		try {
			@SuppressWarnings("unchecked") // We know this cast is valid because we cast the return value of its constructor
			var instance = (TDependency) ctor.newInstance(this, name, value);
			this.dependencies.put(name, instance);
			return instance;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
			throw new RuntimeException("Cannot create an instance of " + type.getName(), exception);
		}
	}

	public <T extends ReactiveDependency<?>> T addDependency(String name, T dependency) {
		if (this.dependencies.putIfAbsent(name, dependency) != null) {
			throw new IllegalArgumentException("Duplicate addition of dependency '" + name + "'");
		}
		return dependency;
	}

	public void queueReaction(ReactiveScope scope) {
		this.pendingScopes.add(scope);
	}

	public void executePendingReactions() {
		this.ready = true;

		var pending = this.pendingScopes;
		this.pendingScopes = new HashSet<>();

		this.workDispatcher.performWork(result -> {
			for (var pendingScope : pending) {
				pendingScope.run(this.globalScope, result);

				var diagnostic = result.terminate();
				if (diagnostic != null) {
					this.workDispatcher.handleError(new Diagnostic("Failed to execute reactive scope", Position.INTRINSIC, List.of(diagnostic)));
				}
			}
		});
	}
}
