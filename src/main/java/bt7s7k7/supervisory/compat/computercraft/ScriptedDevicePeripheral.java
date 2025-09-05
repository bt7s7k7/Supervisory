package bt7s7k7.supervisory.compat.computercraft;

import org.jspecify.annotations.Nullable;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.network.chat.Component;

public class ScriptedDevicePeripheral implements IPeripheral {
	public final ScriptedDevicePeripheralHost host;

	public ScriptedDevicePeripheral(ScriptedDevicePeripheralHost host) {
		this.host = host;
	}

	@Override
	public boolean equals(@Nullable IPeripheral other) {
		return this == other || (other instanceof ScriptedDevicePeripheral other_1 && other_1.host == this.host);
	}

	@Override
	public String getType() {
		return "programmable_logic_controller";
	}

	@Override
	public void attach(IComputerAccess computer) {
		this.host.attachedComputers.add(computer);
	}

	@Override
	public void detach(IComputerAccess computer) {
		this.host.attachedComputers.remove(computer);
	}

	@LuaFunction(mainThread = true)
	public final void writeln(String text) {
		this.host.deviceHost.log(Component.literal(text));
	}

	@LuaFunction(mainThread = true)
	public final void executeCommand(String command) {
		this.host.deviceHost.executeCommand(command);
	}

	@LuaFunction(mainThread = true)
	public final void setState(String key, Object computerValue) {
		var networkDevice = this.host.deviceHost.deviceHost.tryGetDevice();
		if (networkDevice == null) return;

		var scriptEngine = this.host.deviceHost.scriptEngine;

		// If the ScriptEngine isn't loaded we don't have access to a GlobalScope and the resulting
		// ManagedValues will have null prototypes. This is not a big deal, because they will be
		// properly loaded during ScriptEngine global scope initialization, same as those that were
		// loaded from save data.
		var globalScope = scriptEngine.isEmpty() ? null : scriptEngine.getGlobalScope();

		var managedValue = ComputerObject.toManagedValue(computerValue, globalScope);
		networkDevice.setState(key, managedValue);
		this.host.deviceHost.entity.setChanged();
	}

	@LuaFunction(mainThread = true)
	public final Object getState(String key) {
		var networkDevice = this.host.deviceHost.deviceHost.tryGetDevice();
		if (networkDevice == null) return null;

		var managedValue = networkDevice.getState(key);
		return ComputerObject.fromManagedValue(managedValue);
	}
}
