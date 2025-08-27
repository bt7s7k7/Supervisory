package bt7s7k7.supervisory.blocks.programmableLogicController;

import static bt7s7k7.treeburst.runtime.ExpressionResult.LABEL_EXCEPTION;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.script.ScriptEngine;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.support.RelativeDirection;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.runtime.ManagedMap;
import bt7s7k7.treeburst.runtime.ManagedTable;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class PlcScriptEngine extends ScriptEngine {
	protected final ProgrammableLogicControllerBlockEntity owner;

	public PlcScriptEngine(ProgrammableLogicControllerBlockEntity owner) {
		this.owner = owner;
	}

	@Override
	protected void handleError(Diagnostic error) {
		this.owner.log(Component.literal(error.format()).withStyle(ChatFormatting.RED));
	}

	protected ManagedFunction createStateAccessor(GlobalScope globalScope, Function<String, ManagedValue> getter, BiConsumer<String, ManagedValue> setter) {
		return NativeFunction.simple(globalScope, List.of("name", "value?"), List.of(Primitive.String.class, ManagedValue.class), (args, scope, result) -> {
			var key = ((Primitive.String) args.get(0)).value;
			if (args.size() == 1) {
				result.value = getter.apply(key);
				return;
			}

			var value = args.get(1);
			setter.accept(key, value);
			result.value = value;
		});
	}

	private ManagedValue translateValue(GlobalScope globalScope, ManagedValue value) {
		if (value instanceof Primitive) {
			return value;
		}

		if (value instanceof ManagedTable table) {
			var entries = table.properties.entrySet().stream().collect(Collectors.toMap(
					Map.Entry::getKey,
					kv -> this.translateValue(globalScope, kv.getValue())));

			return new ManagedTable(globalScope == null ? null : globalScope.TablePrototype, entries);
		}

		if (value instanceof ManagedArray array) {
			var elements = array.elements.stream().map(v -> this.translateValue(globalScope, v)).toList();

			return new ManagedArray(globalScope == null ? null : globalScope.ArrayPrototype, elements);
		}

		if (value instanceof ManagedMap table) {
			var entries = table.entries.entrySet().stream().collect(Collectors.toMap(
					kv -> this.translateValue(globalScope, kv.getKey()),
					kv -> this.translateValue(globalScope, kv.getValue())));

			return new ManagedMap(globalScope == null ? null : globalScope.MapPrototype, entries);
		}

		return null;
	}

	public ManagedValue importValue(ManagedValue value) {
		return this.translateValue(this.getGlobalScope(), value);
	}

	public ManagedValue exportValue(ManagedValue value) {
		return this.translateValue(null, value);
	}

	public ReactivityManager reactivityManager;
	public RedstoneReactiveDependency[] reactiveRedstone;

	@Override
	protected void initializeGlobals(GlobalScope globalScope) {
		this.owner.log(Component.literal("Device restarted").withStyle(ChatFormatting.DARK_GRAY));

		this.reactivityManager = new ReactivityManager(globalScope);

		{
			this.reactiveRedstone = new RedstoneReactiveDependency[6];
			var redstoneTable = globalScope.declareGlobal("redstone", new ManagedTable(globalScope.TablePrototype));
			var front = this.owner.getFront();

			for (var direction : RelativeDirection.values()) {
				var absoluteDirection = direction.getAbsolute(front);
				var redstoneValue = this.owner.getInput(absoluteDirection);
				var dependency = RedstoneReactiveDependency.get(this.reactivityManager, direction, redstoneValue);
				reactiveRedstone[direction.index] = dependency;
				redstoneTable.declareProperty(direction.name, dependency.makeHandle());
				redstoneTable.declareProperty("set" + StringUtils.capitalize(direction.name), NativeFunction.simple(globalScope, List.of("strength"), List.of(Primitive.Number.class), (args, scope, result) -> {
					var strength = ((Primitive.Number) args.get(0)).value;
					this.owner.setOutput(absoluteDirection, (int) strength);
					result.value = null;
				}));
			}
		}

		{
			var device = getDevice();
			for (var key : device.getStateKeys()) {
				var result = this.importValue(device.getState(key));
				if (result == null) {
					this.owner.log(Component.literal("Failed to import state value: " + key).withStyle(ChatFormatting.RED));
					continue;
				}
				device.setState(key, result);
			}
		}

		globalScope.declareGlobal("print", NativeFunction.simple(globalScope, List.of("message"), (args, scope, result) -> {
			var message = args.get(0);

			this.owner.log(formatValue(message));
		}));

		globalScope.declareGlobal("s", createStateAccessor(globalScope, (key) -> getDevice().getState(key), (key, value) -> {
			getDevice().setState(key, value);
			this.owner.setChanged();
		}));

		globalScope.declareGlobal("setDomain", NativeFunction.simple(globalScope, List.of("name"), List.of(Primitive.String.class), (args, scope, result) -> {
			var device = getDevice();
			if (device.isConnected()) {
				result.value = new Diagnostic("Cannot change the domain after already connected", Position.INTRINSIC);
				result.label = LABEL_EXCEPTION;
				return;
			}

			device.domain = ((Primitive.String) args.get(0)).value;
			result.value = null;
		}));

		globalScope.declareGlobal("r", createStateAccessor(globalScope, (key) -> {
			var imported = this.importValue(getDevice().readCachedValue(key));
			if (imported == null) imported = Primitive.VOID;
			var dependency = RemoteValueReactiveDependency.get(this.reactivityManager, key, imported);
			getDevice().subscribe(key);
			return dependency.makeHandle();
		}, (key, value) -> {
			var exported = this.exportValue(value);
			if (exported == null) return;
			getDevice().publishResource(key, exported);
		}));
	}

	@Override
	public void clear() {
		super.clear();

		this.reactivityManager = null;
		this.reactiveRedstone = null;
	}

	private NetworkDevice getDevice() {
		return this.owner.getDevice();
	}

	public void processTasks() {
		if (this.reactivityManager != null) {
			this.reactivityManager.executePendingReactions(this::handleError, getGlobalScope());
		}
	}

	@Override
	public ManagedValue executeCode(String path, String code) {
		var result = super.executeCode(path, code);

		if (result != null && path.equals("command")) {
			this.owner.log(formatValue(result));
		}

		return result;
	}
}
