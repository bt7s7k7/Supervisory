package bt7s7k7.supervisory.configuration;

import bt7s7k7.supervisory.Supervisory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ConfigurationScreenManager {
	public static <T> void submitConfiguration(BlockPos location, Configurable<T> configurable, T configuration) {
		var payload = new ConfigurationSubmission(location, configurable.getPayloadData(configuration));
		PacketDistributor.sendToServer(payload);
	}

	@SuppressWarnings("unchecked") // Handling type-erased configuration objects
	public static void handleRequest(final ConfigurationRequest data, final IPayloadContext context) {
		var targetPosition = data.target();
		var targetBlockEntity = Minecraft.getInstance().player.level().getBlockEntity(targetPosition);
		if (!(targetBlockEntity instanceof Configurable configurable)) {
			Supervisory.LOGGER.error("Received configuration request for " + targetPosition + ", which is not configurable: " + targetBlockEntity);
			return;
		}

		var configurationCodec = configurable.getConfigurationCodec();
		var configuration = configurationCodec.parse(NbtOps.INSTANCE, data.configuration()).getOrThrow();
		configurable.openConfigurationScreen(configuration);
	}
}
