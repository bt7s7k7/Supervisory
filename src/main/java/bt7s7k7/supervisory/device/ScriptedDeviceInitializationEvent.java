package bt7s7k7.supervisory.device;

import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import net.neoforged.bus.api.Event;

public class ScriptedDeviceInitializationEvent extends Event {
	public final ScriptedDeviceHost deviceHost;
	public final BlockEntityComponent.EventConnectionDelegate signalConnector;
	public final CompositeBlockEntity entity;

	public ScriptedDeviceInitializationEvent(ScriptedDeviceHost deviceHost, BlockEntityComponent.EventConnectionDelegate connect) {
		this.deviceHost = deviceHost;
		this.signalConnector = connect;
		this.entity = deviceHost.entity;
	}

	public ScriptedDevice getScriptEngine() {
		return this.deviceHost.scriptEngine;
	}
}
