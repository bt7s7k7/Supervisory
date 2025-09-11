package bt7s7k7.supervisory.system;

import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import net.neoforged.bus.api.Event;

public class ScriptedSystemInitializationEvent extends Event {
    public final ScriptedSystemHost systemHost;
    public final BlockEntityComponent.EventConnectionDelegate signalConnector;
    public final CompositeBlockEntity entity;

    public ScriptedSystemInitializationEvent(ScriptedSystemHost systemHost, BlockEntityComponent.EventConnectionDelegate signalConnector) {
        this.systemHost = systemHost;
        this.signalConnector = signalConnector;
        this.entity = systemHost.entity;
    }

    public ScriptedSystem getScriptEngine() {
        return this.systemHost.system;
    }
}
