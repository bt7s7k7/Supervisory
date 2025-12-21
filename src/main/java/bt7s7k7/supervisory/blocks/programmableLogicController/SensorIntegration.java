package bt7s7k7.supervisory.blocks.programmableLogicController;

import bt7s7k7.supervisory.sensor.SensorAPI;
import bt7s7k7.supervisory.sensor.SensorReading;
import bt7s7k7.supervisory.system.ScriptedSystemInitializationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public final class SensorIntegration {
	private SensorIntegration() {}

	@SubscribeEvent
	public static void registerIntegration(ScriptedSystemInitializationEvent init) {
		init.signalConnector.connect(init.systemHost.onScopeInitialization, event -> {
			var realm = event.getRealm();
			var system = event.system();

			var sensorApi = new SensorAPI(realm.TablePrototype, realm, init.systemHost.entity, system.reactivityManager, system::getNetworkDevice);

			realm.declareGlobal("Sensor", sensorApi);
			SensorReading.WRAPPER.ensurePrototype(realm);

			system.integrations.putInstance(SensorAPI.class, sensorApi);
		});
	}
}
