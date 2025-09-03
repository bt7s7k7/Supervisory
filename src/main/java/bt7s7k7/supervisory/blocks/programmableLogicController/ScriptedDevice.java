package bt7s7k7.supervisory.blocks.programmableLogicController;

import static bt7s7k7.treeburst.runtime.ExpressionResult.LABEL_EXCEPTION;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.network.RemoteValueReactiveDependency;
import bt7s7k7.supervisory.redstone.RedstoneReactiveDependency;
import bt7s7k7.supervisory.script.ScriptEngine;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.storage.ItemReport;
import bt7s7k7.supervisory.storage.StackReport;
import bt7s7k7.supervisory.storage.StorageAPI;
import bt7s7k7.supervisory.storage.StorageReport;
import bt7s7k7.supervisory.support.Side;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.runtime.ManagedMap;
import bt7s7k7.treeburst.runtime.ManagedTable;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.NativeHandle;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ScriptedDevice extends ScriptEngine {
	public final ScriptedDeviceHost host;

	public ScriptedDevice(ScriptedDeviceHost host) {
		this.host = host;
	}

	@Override
	protected void handleError(Diagnostic error) {
		this.host.log(Component.literal(error.format()).withStyle(ChatFormatting.RED));
	}

	protected static class StateHandle {
		public Supplier<Map<String, ManagedValue>> state;

		public StateHandle(Supplier<Map<String, ManagedValue>> state) {
			this.state = state;
		}
	}

	protected static final NativeHandleWrapper<StateHandle> STATE_HANDLE_WRAPPER = new NativeHandleWrapper<>(StateHandle.class)
			.addMapAccess(v -> v.state.get(), Primitive.String.class, ManagedValue.class,
					Primitive::from, ManagedValue::getStringValue,
					Function.identity(), Function.identity());

	protected ManagedFunction createStateAccessor(GlobalScope globalScope, Function<String, ManagedValue> getter, BiConsumer<String, ManagedValue> setter) {
		return NativeFunction.simple(globalScope, List.of("name", "value?"), List.of(Primitive.String.class, ManagedValue.class), (args, scope, result) -> {
			var key = args.get(0).getStringValue();
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
	public TickReactiveDependency reactivityTick;
	public StorageAPI storage;

	@Override
	protected void initializeGlobals(GlobalScope globalScope) {
		this.host.log(Component.literal("Device restarted").withStyle(ChatFormatting.DARK_GRAY));

		this.reactivityManager = new ReactivityManager(globalScope);
		this.reactivityTick = TickReactiveDependency.get(reactivityManager);
		globalScope.declareGlobal("tick", this.reactivityTick.getHandle());

		{
			this.reactiveRedstone = new RedstoneReactiveDependency[6];
			var redstoneTable = globalScope.declareGlobal("redstone", new ManagedTable(globalScope.TablePrototype));
			var front = this.host.entity.getFront();

			for (var direction : Side.values()) {
				var absoluteDirection = direction.getDirection(front);
				var redstoneValue = this.host.redstone.getInput(absoluteDirection);
				var dependency = RedstoneReactiveDependency.get(this.reactivityManager, direction, redstoneValue);
				reactiveRedstone[direction.index] = dependency;
				redstoneTable.declareProperty(direction.name, dependency.getHandle());
				redstoneTable.declareProperty("set" + StringUtils.capitalize(direction.name), NativeFunction.simple(globalScope, List.of("strength"), List.of(Primitive.Number.class), (args, scope, result) -> {
					var strength = args.get(0).getNumberValue();
					this.host.redstone.setOutput(absoluteDirection, (int) strength);
					result.value = null;
				}));
			}
		}

		this.storage = new StorageAPI(globalScope.TablePrototype, globalScope, this.host.entity, this.reactivityManager);
		globalScope.declareGlobal("storage", this.storage);

		StorageReport.WRAPPER.ensurePrototype(globalScope);
		StackReport.WRAPPER.ensurePrototype(globalScope);
		ItemReport.WRAPPER.ensurePrototype(globalScope);

		for (var side : Side.values()) {
			globalScope.declareGlobal(side.name, Primitive.from(side.name));
		}

		{
			var device = getDevice();
			for (var key : device.getStateKeys()) {
				var result = this.importValue(device.getState(key));
				if (result == null) {
					this.host.log(Component.literal("Failed to import state value: " + key).withStyle(ChatFormatting.RED));
					continue;
				}
				device.setState(key, result);
			}
		}

		globalScope.declareGlobal("print", NativeFunction.simple(globalScope, List.of("message"), (args, scope, result) -> {
			var message = args.get(0);

			if (message instanceof Primitive.String stringMessage) {
				this.host.log(Component.literal(stringMessage.value));
				return;
			}

			this.host.log(formatValue(message, scope.globalScope));
		}));

		globalScope.declareGlobal("s", createStateAccessor(globalScope, (key) -> getDevice().getState(key), (key, value) -> {
			getDevice().setState(key, value);
			this.host.entity.setChanged();
		}));

		globalScope.declareGlobal("state", new NativeHandle(STATE_HANDLE_WRAPPER.buildPrototype(globalScope), new StateHandle(() -> {
			this.host.entity.setChanged();
			return getDevice().state;
		})));

		globalScope.declareGlobal("setDomain", NativeFunction.simple(globalScope, List.of("name"), List.of(Primitive.String.class), (args, scope, result) -> {
			var device = getDevice();
			if (device.isConnected()) {
				result.value = new Diagnostic("Cannot change the domain after already connected", Position.INTRINSIC);
				result.label = LABEL_EXCEPTION;
				return;
			}

			device.domain = args.get(0).getStringValue();
			result.value = null;
		}));

		globalScope.declareGlobal("r", createStateAccessor(globalScope, (key) -> {
			var imported = this.importValue(getDevice().readCachedValue(key));
			if (imported == null) imported = Primitive.VOID;
			var dependency = RemoteValueReactiveDependency.get(this.reactivityManager, key, imported);
			getDevice().subscribe(key);
			return dependency.getHandle();
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
		return this.host.deviceHost.getDevice();
	}

	public void processTasks() {
		if (this.reactivityTick != null) {
			this.reactivityTick.updateValue(Primitive.from((double) this.host.entity.getLevel().getGameTime()));
		}

		if (this.reactivityManager != null) {
			this.reactivityManager.executePendingReactions(this::handleError, getGlobalScope());
		}

		if (this.storage != null) {
			this.storage.tick();
		}
	}

	@Override
	public ManagedValue executeCode(String path, String code) {
		var result = super.executeCode(path, code);

		if (result != null && path.equals("command")) {
			this.host.log(formatValue(result, this.getGlobalScope()));
		}

		return result;
	}
}
