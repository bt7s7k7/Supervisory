package bt7s7k7.supervisory.compat.computercraft;

import java.util.Collections;
import java.util.List;

import bt7s7k7.supervisory.device.ScriptedDeviceIntegration;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Primitive;

public class InteropAPI extends LazyTable implements ScriptedDeviceIntegration {
	protected final ScriptedDevicePeripheralHost peripheralHost;

	public InteropAPI(ScriptedDevicePeripheralHost peripheralHost, GlobalScope globalScope) {
		super(globalScope.TablePrototype, globalScope);
		this.peripheralHost = peripheralHost;
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
	}
}
