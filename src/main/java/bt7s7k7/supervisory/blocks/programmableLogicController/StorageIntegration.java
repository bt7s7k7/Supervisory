package bt7s7k7.supervisory.blocks.programmableLogicController;

import bt7s7k7.supervisory.storage.ResourceReport;
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
			var realm = event.getRealm();
			var system = event.system();

			var storage = new StorageAPI(realm.TablePrototype, realm, init.systemHost.entity, system.reactivityManager, system::getNetworkDevice);

			realm.declareGlobal("Storage", storage);
			StorageReport.WRAPPER.ensurePrototype(realm);
			StackReport.WRAPPER.ensurePrototype(realm);
			ResourceReport.WRAPPER.ensurePrototype(realm);

			var integration = new StorageIntegration(storage);
			system.integrations.putInstance(StorageIntegration.class, integration);
		});
	}
}
