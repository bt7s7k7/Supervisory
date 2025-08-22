package bt7s7k7.supervisory.configuration;

import bt7s7k7.supervisory.Supervisory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ConfigurationManager {
	public static void requestConfiguration(ServerPlayer player, BlockPos location, Configurable<?> configurable) {
		var payload = new ConfigurationRequest(location, configurable.getPayloadData());
		PacketDistributor.sendToPlayer(player, payload);
	}

	@SuppressWarnings("unchecked") // Handling type-erased configuration objects
	public static void handleSubmission(final ConfigurationSubmission data, final IPayloadContext context) {
		var targetPosition = data.target();
		var targetBlockEntity = context.player().level().getBlockEntity(targetPosition);
		if (!(targetBlockEntity instanceof Configurable configurable)) {
			Supervisory.LOGGER.error("Received configuration submission for " + targetPosition + ", which is not configurable: " + targetBlockEntity);
			return;
		}

		var configurationCodec = configurable.getConfigurationCodec();
		var configuration = configurationCodec.parse(NbtOps.INSTANCE, data.configuration()).getOrThrow();
		configurable.setConfiguration(configuration);
	}
}
