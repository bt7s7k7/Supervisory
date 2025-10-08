package bt7s7k7.supervisory.system;

import static bt7s7k7.treeburst.runtime.ExpressionResult.LABEL_EXCEPTION;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.MutableClassToInstanceMap;

import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.network.RemoteValueReactiveDependency;
import bt7s7k7.supervisory.script.ScriptEngine;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
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

public class ScriptedSystem extends ScriptEngine {
	public final ScriptedSystemHost host;

	public ScriptedSystem(ScriptedSystemHost host) {
		this.host = host;
	}

	@Override
	public void handleError(Diagnostic error) {
		this.host.log(Component.literal(error.format()).withStyle(ChatFormatting.RED));
	}

	protected static class StateHandle {
		public Supplier<Map<String, ManagedValue>> state;

		public StateHandle(Supplier<Map<String, ManagedValue>> state) {
			this.state = state;
		}
	}

	protected static final NativeHandleWrapper<StateHandle> STATE_HANDLE_WRAPPER = new NativeHandleWrapper<>("StateHandle", StateHandle.class, ctx -> ctx
			.addMapAccess(v -> v.state.get(), Primitive.String.class, ManagedValue.class,
					Primitive::from, ManagedValue::getStringValue,
					Function.identity(), Function.identity()));

	protected ManagedFunction createStateAccessor(GlobalScope globalScope, Function<String, ManagedValue> getter, BiConsumer<String, ManagedValue> setter) {
		return NativeFunction.simple(globalScope, List.of("name", "value?"), List.of(Primitive.String.class, ManagedValue.class), (args, scope, result) -> { // @symbol: <template>stateAccessor
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
			var elements = array.stream().map(v -> this.translateValue(globalScope, v)).toList();

			return ManagedArray.fromImmutableList(globalScope == null ? null : globalScope.ArrayPrototype, elements);
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
	public TickReactiveDependency reactivityTick;

	public final MutableClassToInstanceMap<ScriptedSystemIntegration> integrations = MutableClassToInstanceMap.create();

	@Override
	protected void initializeGlobals(GlobalScope globalScope) {
		this.host.log(Component.literal("Device restarted").withStyle(ChatFormatting.DARK_GRAY));

		this.reactivityManager = new ReactivityManager(globalScope, this);
		this.reactivityTick = TickReactiveDependency.get("tick", this.reactivityManager);

		var sys = globalScope.declareGlobal("SYS", new ManagedTable(globalScope.TablePrototype)); // @summary: Contains functions and data regarding the current system.
		sys.declareProperty("tick", this.reactivityTick.getHandle()); // @symbol: SYS.tick, @type: TickReactiveDependency, @summary: Triggers each game tick.

		for (var side : Side.values()) {
			globalScope.declareGlobal(side.name.toUpperCase(), Primitive.from(side.name));

			// @symbol: <template>side_constant, @type: String, @summary: Constant for the name of a side of the system
			// @symbol: BOTTOM, @like: <template>side_constant
			// @symbol: TOP, @like: <template>side_constant
			// @symbol: FRONT, @like: <template>side_constant
			// @symbol: BACK, @like: <template>side_constant
			// @symbol: RIGHT, @like: <template>side_constant
			// @symbol: LEFT, @like: <template>side_constant
		}

		{
			var device = this.getNetworkDevice();
			for (var key : device.getStateKeys()) {
				var result = this.importValue(device.getState(key));
				if (result == null) {
					this.host.log(Component.literal("Failed to import state value: " + key).withStyle(ChatFormatting.RED));
					continue;
				}
				device.setState(key, result);
			}
		}

		this.host.onScopeInitialization.emit(new ScriptedSystemHost.ScopeInitializationEvent(this));

		globalScope.declareGlobal("print", NativeFunction.simple(globalScope, List.of("message"), (args, scope, result) -> {
			// @summary: Prints a message to the system log. If the message is not a string it is converted using `k_dump`.
			var message = args.get(0);

			if (message instanceof Primitive.String stringMessage) {
				this.host.log(Component.literal(stringMessage.value));
				return;
			}

			this.host.log(formatValue(message, scope.globalScope));
		}));

		globalScope.declareGlobal("s", this.createStateAccessor(globalScope, key -> this.getNetworkDevice().getState(key), (key, value) -> { // @like: <template>stateAccessor
			// @summary: Allows access to the device's local state. This state is persistent after reboots.
			this.getNetworkDevice().setState(key, value);
			this.host.entity.setChanged();
		}));

		sys.declareProperty("state", new NativeHandle(STATE_HANDLE_WRAPPER.buildPrototype(globalScope), new StateHandle(() -> { // @symbol: SYS.state, @type: Map
			// @summary: Allows raw access to the device's local state.
			this.host.entity.setChanged();
			return this.getNetworkDevice().state;
		})));

		globalScope.declareGlobal("setDomain", NativeFunction.simple(globalScope, List.of("domain"), List.of(Primitive.String.class), (args, scope, result) -> {
			// @summary[[Sets the domain this device should connect to. This connection is
			// established exactly after initial code finishes executing and cannot be changed
			// after. The only way to change the domain is to reboot the device.]]
			var device = this.getNetworkDevice();
			if (device.isConnected()) {
				result.value = new Diagnostic("Cannot change the domain after already connected", Position.INTRINSIC);
				result.label = LABEL_EXCEPTION;
				return;
			}

			device.domain = args.get(0).getStringValue();
			result.value = null;
		}));

		globalScope.declareGlobal("r", this.createStateAccessor(globalScope, key -> { // @like: <template>stateAccessor
			// @summary[[Allows publishing resources onto a connected network or requesting
			// resources published by other devices.
			//
			// When writing, the system will broadcast an update message, that all devices that
			// listen to the event will react to, and save the resource in their cache. At the same
			// time the published resource will be saved in a local cache, that will be used to
			// respond to resource requests.
			//
			// When reading, the resource will be read from this system's cache or, if the resource
			// is missing, {@link void} will be returned and the system will send a resource request
			// over the network. A device that publishes this resource will hopefully respond and,
			// when an update message is received, this system's cache will be updated.]]
			var imported = this.importValue(this.getNetworkDevice().readCachedValue(key));
			if (imported == null) imported = Primitive.VOID;
			var dependency = RemoteValueReactiveDependency.get(this.reactivityManager, key, imported);
			this.getNetworkDevice().subscribe(key);
			return dependency.getHandle();
		}, (key, value) -> {
			var exported = this.exportValue(value);
			if (exported == null) return;
			this.getNetworkDevice().publishResource(key, exported);
		}));
	}

	@Override
	public void clear() {
		super.clear();

		this.reactivityManager = null;

		for (var integration : this.integrations.values()) {
			integration.teardown();
		}

		this.integrations.clear();
	}

	public NetworkDevice getNetworkDevice() {
		return this.host.networkDeviceHost.getDevice();
	}

	public void processTasks() {
		if (this.reactivityTick != null) {
			this.reactivityTick.updateValue(Primitive.from((double) this.host.entity.getLevel().getGameTime()));
		}

		for (var integration : this.integrations.values()) {
			integration.tick();
		}

		if (this.reactivityManager != null) {
			this.reactivityManager.executePendingReactions();
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
