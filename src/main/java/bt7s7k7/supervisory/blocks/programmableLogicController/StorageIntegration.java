package bt7s7k7.supervisory.blocks.programmableLogicController;

import bt7s7k7.supervisory.device.ScriptedDeviceInitializationEvent;
import bt7s7k7.supervisory.device.ScriptedDeviceIntegration;
import bt7s7k7.supervisory.storage.ItemReport;
import bt7s7k7.supervisory.storage.StackReport;
import bt7s7k7.supervisory.storage.StorageAPI;
import bt7s7k7.supervisory.storage.StorageReport;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public final class StorageIntegration implements ScriptedDeviceIntegration {
	protected final StorageAPI storage;

	private StorageIntegration(StorageAPI storage) {
		this.storage = storage;
	}

	@Override
	public void tick() {
		this.storage.tick();
	}

	@SubscribeEvent
	public static void registerIntegration(ScriptedDeviceInitializationEvent init) {
		init.signalConnector.connect(init.deviceHost.onScopeInitialization, event -> {
			var globalScope = event.getGlobalScope();
			var device = event.device();

			var storage = new StorageAPI(globalScope.TablePrototype, globalScope, init.deviceHost.entity, device.reactivityManager, device::getDevice);

			globalScope.declareGlobal("Storage", storage);
			StorageReport.WRAPPER.ensurePrototype(globalScope);
			StackReport.WRAPPER.ensurePrototype(globalScope);
			ItemReport.WRAPPER.ensurePrototype(globalScope);

			var integration = new StorageIntegration(storage);
			device.integrations.putInstance(StorageIntegration.class, integration);
		});
	}
}
