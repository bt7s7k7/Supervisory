package bt7s7k7.supervisory.blocks.programmableLogicController;

import bt7s7k7.supervisory.storage.ItemReport;
import bt7s7k7.supervisory.storage.StackReport;
import bt7s7k7.supervisory.storage.StorageAPI;
import bt7s7k7.supervisory.storage.StorageReport;
import bt7s7k7.supervisory.system.ScriptedSystemInitializationEvent;
import bt7s7k7.supervisory.system.ScriptedSystemIntegration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public final class StorageIntegration implements ScriptedSystemIntegration {
	protected final StorageAPI storage;

	private StorageIntegration(StorageAPI storage) {
		this.storage = storage;
	}

	@Override
	public void tick() {
		this.storage.tick();
	}

	@SubscribeEvent
	public static void registerIntegration(ScriptedSystemInitializationEvent init) {
		init.signalConnector.connect(init.systemHost.onScopeInitialization, event -> {
			var globalScope = event.getGlobalScope();
			var system = event.system();

			var storage = new StorageAPI(globalScope.TablePrototype, globalScope, init.systemHost.entity, system.reactivityManager, system::getNetworkDevice);

			globalScope.declareGlobal("Storage", storage);
			StorageReport.WRAPPER.ensurePrototype(globalScope);
			StackReport.WRAPPER.ensurePrototype(globalScope);
			ItemReport.WRAPPER.ensurePrototype(globalScope);

			var integration = new StorageIntegration(storage);
			system.integrations.putInstance(StorageIntegration.class, integration);
		});
	}
}
