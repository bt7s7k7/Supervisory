package bt7s7k7.supervisory.compat.computercraft;

import java.util.Collections;
import java.util.List;

import bt7s7k7.supervisory.sockets.SocketConnectionManager;
import bt7s7k7.supervisory.system.ScriptedSystem;
import bt7s7k7.supervisory.system.ScriptedSystemIntegration;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.runtime.ManagedTable;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.NativeHandle;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Primitive;
import dan200.computercraft.api.peripheral.IPeripheral;

public class InteropAPI extends LazyTable implements ScriptedSystemIntegration { // @symbol: Interop
	// @summary: Allows for interaction with ComputerCraft peripherals.
	protected final PeripheralConnectionController connectionController;
	protected final SocketConnectionManager<IPeripheral, PeripheralReactiveDependency> connectionManager;

	public InteropAPI(PeripheralConnectionController connectionController, ScriptedSystem system, GlobalScope globalScope) {
		super(globalScope.TablePrototype, globalScope);
		this.connectionController = connectionController;

		this.connectionManager = new SocketConnectionManager<>("interop", 1, connectionController.entity, system.reactivityManager, system::getNetworkDevice) {
			@Override
			protected PeripheralReactiveDependency makeHandle(String name) {
				return new PeripheralReactiveDependency(this.reactivityManager, name, system::handleError, connectionController::getComputerContext);
			}

			@Override
			protected void processDependency(PeripheralReactiveDependency dependency, IPeripheral capability) {
				var oldValue = dependency.getValueEvenIfInvalid();

				if (capability == null) {
					if (oldValue == Primitive.VOID) return;
					dependency.updateValue(Primitive.VOID);
					dependency.handleEvent("__disconnected__", new Object[0]);
					return;
				}

				if (oldValue instanceof ManagedTable table
						&& table.getOwnProperty("meta") instanceof NativeHandle handle
						&& handle.value instanceof PeripheralConnection wrapper
						&& wrapper.peripheral == capability) {
					return;
				}

				var connection = new PeripheralConnection(capability, dependency);
				dependency.updateValue(connection.buildWrappingTable(this.reactivityManager.globalScope));
				dependency.handleEvent("__connected__", new Object[0]);
				capability.attach(connection);
			}
		};
	}

	@Override
	protected void initialize() {
		this.declareProperty("isAttached", NativeFunction.simple(this.globalScope, Collections.emptyList(), (args, scope, result) -> {
			// @summary: Returns if any computers are attached to this system and use it as a peripheral.
			result.value = Primitive.from(this.connectionController.attachedComputers.hasComputers());
		}));

		this.declareProperty("emitEvent", NativeFunction.simple(this.globalScope, List.of("name", "arguments?"), List.of(Primitive.String.class, ManagedArray.class), (args, scope, result) -> {
			// @summary: Sends an event to all computers that are attached to this system.
			var name = args.get(0).getStringValue();
			Object[] eventArguments;
			if (args.size() > 1) {
				var managedArguments = args.get(1).getArrayValue().elements;
				eventArguments = new Object[managedArguments.size()];
				for (int i = 0; i < managedArguments.size(); i++) {
					var managedValue = managedArguments.get(i);
					var computerValue = ComputerObject.fromManagedValue(managedValue);
					eventArguments[i] = computerValue;
				}
			} else {
				eventArguments = null;
			}

			this.connectionController.attachedComputers.forEach(computer -> {
				computer.queueEvent(name, eventArguments);
			});

			result.value = Primitive.VOID;
		}));

		this.declareProperty("connect", NativeFunction.simple(this.globalScope, List.of("target"), List.of(Primitive.String.class), (args, scope, result) -> {
			// @summary[[Connects to a peripheral. The `target` can be a side of this system or a
			// name of a socket on the network. This function returns a {@link
			// PeripheralReactiveDependency} that will be updated when a peripheral is connected or
			// disconnected.]]
			var target = args.get(0).getStringValue();

			var dependency = this.connectionManager.connect(target);
			result.value = dependency.getHandle();
			return;
		}));

		{ // @symbol: <template>Interop.<color>
			// @type: Number
			// @summary: Constant corresponding to a ComputerCraft color code
		}

		this.declareProperty("WHITE", Primitive.from(1)); // @like: <template>Interop.<color>
		this.declareProperty("ORANGE", Primitive.from(2)); // @like: <template>Interop.<color>
		this.declareProperty("MAGENTA", Primitive.from(4)); // @like: <template>Interop.<color>
		this.declareProperty("LIGHTBLUE", Primitive.from(8)); // @like: <template>Interop.<color>
		this.declareProperty("YELLOW", Primitive.from(16)); // @like: <template>Interop.<color>
		this.declareProperty("LIME", Primitive.from(32)); // @like: <template>Interop.<color>
		this.declareProperty("PINK", Primitive.from(64)); // @like: <template>Interop.<color>
		this.declareProperty("GRAY", Primitive.from(128)); // @like: <template>Interop.<color>
		this.declareProperty("LIGHT_GRAY", Primitive.from(256)); // @like: <template>Interop.<color>
		this.declareProperty("CYAN", Primitive.from(512)); // @like: <template>Interop.<color>
		this.declareProperty("PURPLE", Primitive.from(1024)); // @like: <template>Interop.<color>
		this.declareProperty("BLUE", Primitive.from(2048)); // @like: <template>Interop.<color>
		this.declareProperty("BROWN", Primitive.from(4096)); // @like: <template>Interop.<color>
		this.declareProperty("GREEN", Primitive.from(8192)); // @like: <template>Interop.<color>
		this.declareProperty("RED", Primitive.from(16384)); // @like: <template>Interop.<color>
		this.declareProperty("BLACK", Primitive.from(32768)); // @like: <template>Interop.<color>

		this.declareProperty("setEventHandler", NativeFunction.simple(this.globalScope, List.of("peripheral", "handler"), List.of(PeripheralReactiveDependency.class, ManagedFunction.class), (args, scope, result) -> {
			// @summary[[Sets an event handler on the specified peripheral. The handler will receive
			// a `event` argument containing the {@link String} name of the event and an `arguments`
			// argument containing an {@link Array} of the arguments composing the event. Two
			// special events are emitted by this system: `__connected__`, when a peripheral is
			// connected and `__disconnected__` when a peripheral is disconnected.]]
			var dependency = args.get(0).getNativeValue(PeripheralReactiveDependency.class);
			var handler = args.get(1).getFunctionValue();

			dependency.eventHandler = handler;
			result.value = Primitive.VOID;
		}));
	}

	@Override
	public void tick() {
		if (!this.initialized) return;
		this.connectionManager.tick();
	}
}
