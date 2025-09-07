package bt7s7k7.supervisory.compat.computercraft;

import java.util.Collections;
import java.util.List;

import bt7s7k7.supervisory.device.ScriptedDevice;
import bt7s7k7.supervisory.device.ScriptedDeviceIntegration;
import bt7s7k7.supervisory.sockets.SocketConnectionManager;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.ManagedTable;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.NativeHandle;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Primitive;
import dan200.computercraft.api.peripheral.IPeripheral;

public class InteropAPI extends LazyTable implements ScriptedDeviceIntegration {
	protected final ScriptedDevicePeripheralHost peripheralHost;
	protected final SocketConnectionManager<IPeripheral, PeripheralReactiveDependency> connectionManager;

	public InteropAPI(ScriptedDevicePeripheralHost peripheralHost, ScriptedDevice scriptedDevice, GlobalScope globalScope) {
		super(globalScope.TablePrototype, globalScope);
		this.peripheralHost = peripheralHost;

		this.connectionManager = new SocketConnectionManager<>("interop", 1, peripheralHost.entity, scriptedDevice.reactivityManager, scriptedDevice::getDevice) {
			@Override
			protected PeripheralReactiveDependency makeHandle(String name) {
				return new PeripheralReactiveDependency(this.reactivityManager, name);
			}

			@Override
			protected void processDependency(PeripheralReactiveDependency dependency, IPeripheral capability) {
				var oldValue = dependency.getValueEvenIfInvalid();

				if (capability == null) {
					if (oldValue == Primitive.VOID) return;
					dependency.updateValue(Primitive.VOID);
					return;
				}

				if (oldValue instanceof ManagedTable table
						&& table.getOwnProperty("meta") instanceof NativeHandle handle
						&& handle.value instanceof PeripheralConnection wrapper
						&& wrapper.peripheral == capability) {
					return;
				}

				dependency.updateValue(new PeripheralConnection(capability, dependency).buildWrappingTable(this.reactivityManager.globalScope));
			}
		};
	}

	@Override
	protected void initialize() {
		this.declareProperty("isAttached", NativeFunction.simple(this.globalScope, Collections.emptyList(), (args, scope, result) -> {
			result.value = Primitive.from(this.peripheralHost.attachedComputers.hasComputers());
		}));

		this.declareProperty("emitEvent", NativeFunction.simple(this.globalScope, List.of("name", "arguments?"), List.of(Primitive.String.class, ManagedArray.class), (args, scope, result) -> {
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

			this.peripheralHost.attachedComputers.forEach(computer -> {
				computer.queueEvent(name, eventArguments);
			});

			result.value = Primitive.VOID;
		}));

		this.declareProperty("connect", NativeFunction.simple(this.globalScope, List.of("target"), List.of(Primitive.String.class), (args, scope, result) -> {
			var target = args.get(0).getStringValue();

			var dependency = this.connectionManager.connect(target);
			result.value = dependency.getHandle();
			return;
		}));

		this.declareProperty("WHITE", Primitive.from(1));
		this.declareProperty("ORANGE", Primitive.from(2));
		this.declareProperty("MAGENTA", Primitive.from(4));
		this.declareProperty("LIGHTBLUE", Primitive.from(8));
		this.declareProperty("YELLOW", Primitive.from(16));
		this.declareProperty("LIME", Primitive.from(32));
		this.declareProperty("PINK", Primitive.from(64));
		this.declareProperty("GRAY", Primitive.from(128));
		this.declareProperty("LIGHT_GRAY", Primitive.from(256));
		this.declareProperty("CYAN", Primitive.from(512));
		this.declareProperty("PURPLE", Primitive.from(1024));
		this.declareProperty("BLUE", Primitive.from(2048));
		this.declareProperty("BROWN", Primitive.from(4096));
		this.declareProperty("GREEN", Primitive.from(8192));
		this.declareProperty("RED", Primitive.from(16384));
		this.declareProperty("BLACK", Primitive.from(32768));
	}

	@Override
	public void tick() {
		if (!this.initialized) return;
		this.connectionManager.tick();
	}
}
