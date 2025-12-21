package bt7s7k7.supervisory.script.reactivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.runtime.ManagedTable;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.runtime.Scope;
import bt7s7k7.treeburst.support.Primitive;

public class ReactiveScope {
	protected final ReactivityManager owner;
	public final ArrayList<ReactiveDependency<?>> dependencies = new ArrayList<>();
	public final ManagedFunction callback;

	protected final ManagedTable context;
	protected boolean missingDependency = false;

	public ReactiveScope(ReactivityManager owner, Realm realm, ManagedFunction callback) {
		this.callback = callback;
		this.owner = owner;

		this.context = new ManagedTable(realm.TablePrototype, Map.of( // @symbol: ReactiveScope, @entry-symbol
				// @summary: Allows for adding dependencies to the scope of a {@link reactive} function.
				"use", NativeFunction.simple(realm, List.of("dependency"), List.of(ReactiveDependency.class), (args, scope, result) -> { // @symbol: ReactiveScope.use
					// @summary: Adds a dependency to this scope. This scope's callback will be executed each time the dependency changes. Returns the value of the dependency.
					var dependency = args.get(0).getNativeValue(ReactiveDependency.class);

					if (!this.dependencies.contains(dependency)) {
						this.dependencies.add(dependency);
					}

					if (!dependency.isReady()) {
						this.missingDependency = true;
					}

					result.value = dependency.getValue();

					return;
				}),
				"awaitReady", NativeFunction.simple(realm, Collections.emptyList(), (args, scope, result) -> { // @symbol: ReactiveScope.awaitReady
					// @summary: If any of the dependencies of this scope don't yet have a value, aborts the execution of this function. The callback will be called again, when the values are acquired.
					result.value = Primitive.VOID;

					if (this.missingDependency) {
						result.label = ExpressionResult.LABEL_RETURN;
					}
				})));
	}

	public void run(Scope scope, ExpressionResult result) {
		for (var dependency : this.dependencies) {
			dependency.unsubscribe(this);
		}

		this.dependencies.clear();
		this.missingDependency = false;

		// Leave the error handling to the caller of this method
		this.callback.invoke(List.of(this.context), scope, result);

		for (var dependency : this.dependencies) {
			dependency.subscribe(this);
		}
	}
}
