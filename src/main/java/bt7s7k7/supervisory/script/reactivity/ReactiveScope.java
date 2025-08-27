package bt7s7k7.supervisory.script.reactivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.runtime.ManagedTable;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.Scope;
import bt7s7k7.treeburst.runtime.UnmanagedHandle;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;

public class ReactiveScope {
	protected final ReactivityManager owner;
	public final ArrayList<ReactiveDependency<?>> dependencies = new ArrayList<>();
	public final ManagedFunction callback;

	protected final ManagedTable context;

	public ReactiveScope(ReactivityManager owner, GlobalScope globalScope, ManagedFunction callback) {
		this.callback = callback;
		this.owner = owner;

		this.context = new ManagedTable(globalScope.TablePrototype, Map.of(
				"use", NativeFunction.simple(globalScope, List.of("dependency"), List.of(UnmanagedHandle.class), (args, scope, result) -> {
					if (!(((UnmanagedHandle) args.get(0)).value instanceof ReactiveDependency<?> dependency)) {
						result.value = new Diagnostic("Invalid dependency object", Position.INTRINSIC);
						return;
					}

					if (!this.dependencies.contains(dependency)) {
						this.dependencies.add(dependency);
					}

					result.value = dependency.getValue();
					return;
				}),
				"awaitReady", NativeFunction.simple(globalScope, Collections.emptyList(), (args, scope, result) -> {
					result.value = Primitive.VOID;

					if (!this.owner.ready) {
						result.label = ExpressionResult.LABEL_RETURN;
					}
				})));
	}

	public void run(Scope scope, ExpressionResult result) {
		for (var dependency : this.dependencies) {
			dependency.unsubscribe(this);
		}

		this.dependencies.clear();

		// Leave the error handling to the caller of this method
		callback.invoke(List.of(this.context), scope, result);

		for (var dependency : this.dependencies) {
			dependency.subscribe(this);
		}
	}
}
