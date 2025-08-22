package bt7s7k7.supervisory.configuration;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public class Payloads {
	@SubscribeEvent
	public static void register(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");

		registrar.commonToClient(
				ConfigurationRequest.TYPE,
				ConfigurationRequest.STREAM_CODEC,
				ConfigurationScreenManager::handleRequest);

		registrar.commonToServer(
				ConfigurationSubmission.TYPE,
				ConfigurationSubmission.STREAM_CODEC,
				ConfigurationManager::handleSubmission);
	}
}
