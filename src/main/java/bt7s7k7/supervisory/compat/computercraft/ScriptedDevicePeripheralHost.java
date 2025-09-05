package bt7s7k7.supervisory.compat.computercraft;

import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.device.ScriptedDeviceHost;
import dan200.computercraft.api.peripheral.AttachedComputerSet;

public class ScriptedDevicePeripheralHost extends BlockEntityComponent {
	public final ScriptedDeviceHost deviceHost;
	public final AttachedComputerSet attachedComputers = new AttachedComputerSet();

	public ScriptedDevicePeripheralHost(CompositeBlockEntity entity, ScriptedDeviceHost device) {
		super(entity);
		this.deviceHost = device;

		this.connect(device.onScopeInitialization, event -> {
			var globalScope = event.getGlobalScope();
			var interop = new InteropAPI(this, globalScope);
			globalScope.declareGlobal("Interop", interop);
		});
	}
}
