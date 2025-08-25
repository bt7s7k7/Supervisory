package bt7s7k7.supervisory.blocks.programmableLogicController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Consumer;

import bt7s7k7.supervisory.Supervisory;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber
public final class LogEventRouter {
	private LogEventRouter() {};

	private static LogEventRouter instance = null;

	public static LogEventRouter getInstance() {
		return instance;
	}

	private HashMap<String, HashSet<UUID>> listenersLookup = new HashMap<>();
	private HashMap<UUID, String> playerSubscriptionLookup = new HashMap<>();

	public Consumer<Component> onLogReceived = null;

	@SubscribeEvent
	private static void initialize(ServerStartedEvent event) {
		Supervisory.LOGGER.info("Initializing log event router");
		instance = new LogEventRouter();
	}

	@SubscribeEvent
	private static void teardown(ServerStoppedEvent event) {
		Supervisory.LOGGER.info("Tearing down log event router");
		instance = null;
	}

	@SubscribeEvent
	private static void registerPayload(final RegisterPayloadHandlersEvent event) {
		var registrar = event.registrar("1");

		registrar.commonToClient(LogEvent.TYPE, LogEvent.STREAM_CODEC, (data, context) -> {
			var instance = getInstance();
			if (instance.onLogReceived != null) {
				instance.onLogReceived.accept(data.event);
			}
		});
	}

	private static String getSubscriptionToken(Level level, BlockPos position) {
		return level.dimension().registry().toString() + "," + position.toString();
	}

	public void subscribePlayer(ServerPlayer player, Level level, BlockPos position) {
		// Make sure player can only be subscribed to one log source
		var existingSubscription = this.playerSubscriptionLookup.get(player.getUUID());
		if (existingSubscription != null) {
			var existingSet = this.listenersLookup.get(existingSubscription);
			if (existingSet != null) {
				existingSet.remove(player.getUUID());
				if (existingSet.isEmpty()) {
					this.listenersLookup.remove(existingSubscription);
				}
			}
		}

		var token = getSubscriptionToken(level, position);
		var subscriptionSet = this.listenersLookup.computeIfAbsent(token, __ -> new HashSet<>());
		subscriptionSet.add(player.getUUID());
	}

	public static record LogEvent(Component event) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<LogEvent> TYPE = new CustomPacketPayload.Type<>(Supervisory.resource("log_event"));

		public static final StreamCodec<ByteBuf, LogEvent> STREAM_CODEC = StreamCodec.composite(
				ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC,
				LogEvent::event,
				LogEvent::new);

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public void sendLogEvent(Level level, BlockPos position, Component event) {
		var token = getSubscriptionToken(level, position);
		var listeners = this.listenersLookup.get(token);
		if (listeners == null) return;

		var players = level.getServer().getPlayerList();

		for (var it = listeners.iterator(); it.hasNext();) {
			var listenerId = it.next();

			var player = players.getPlayer(listenerId);
			if (player == null) {
				// If the subscribed player has left the server, unsubscribe them
				it.remove();
				continue;
			}

			PacketDistributor.sendToPlayer(player, new LogEvent(event));
		}

		if (listeners.isEmpty()) {
			this.listenersLookup.remove(token);
		}
	}
}
