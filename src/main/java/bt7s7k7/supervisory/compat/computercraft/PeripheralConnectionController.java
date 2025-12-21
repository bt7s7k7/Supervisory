package bt7s7k7.supervisory.compat.computercraft;

import bt7s7k7.supervisory.Support;
import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.system.ScriptedSystemHost;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.shared.computer.core.ServerContext;
import net.minecraft.server.level.ServerLevel;

public class PeripheralConnectionController extends BlockEntityComponent {
	public final ScriptedSystemHost systemHost;
	public final AttachedComputerSet attachedComputers = new AttachedComputerSet();

	protected ComputerContext computerContext;

	public ComputerContext getComputerContext() {
		if (this.computerContext != null) {
			return this.computerContext;
		}

		var serverLevel = (ServerLevel) this.entity.getLevel();
		var context = ServerContext.get(serverLevel.getServer());
		this.computerContext = Support.getField(context, "context", context.getClass());
		return this.computerContext;
	}

	public PeripheralConnectionController(CompositeBlockEntity entity, ScriptedSystemHost systemHost) {
		super(entity);
		this.systemHost = systemHost;

		this.connect(systemHost.onScopeInitialization, event -> {
			var realm = event.getRealm();
			var interop = new InteropAPI(this, event.system(), realm);

			realm.declareGlobal("Interop", interop);
			PeripheralConnection.WRAPPER.ensurePrototype(realm);

			event.system().integrations.putInstance(InteropAPI.class, interop);
		});
	}
}
