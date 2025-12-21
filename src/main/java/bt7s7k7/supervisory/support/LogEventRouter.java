package bt7s7k7.supervisory.support;

import java.util.UUID;
import java.util.function.Consumer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import bt7s7k7.supervisory.Supervisory;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber
public abstract sealed class LogEventRouter {
	public static final class GuiLogRouter extends LogEventRouter {
		public Consumer<Component> onLogReceived = null;

		@Override
		protected void handleEvent(Level level, BlockPos position, ServerPlayer player, Component event) {
			PacketDistributor.sendToPlayer(player, new LogEvent(event));
		}

		@Override
		public boolean subscribe(Subscription subscription) {
			// Make sure player can only have one GUI log subscription
			var existingSubscriptions = this.playerSubscriptionLookup.row(subscription.player);
			for (var existingSubscription : existingSubscriptions.values()) {
				// Unsubscribe only the GUI log part of the subscription
				this.unsubscribe(existingSubscription);
			}

			return super.subscribe(subscription);
		}
	}

	public static final class ChatRouter extends LogEventRouter {
		@Override
		protected void handleEvent(Level level, BlockPos position, ServerPlayer player, Component event) {
			player.sendSystemMessage(Component.literal("[" + position.toShortString() + "] ")
					.withStyle(Style.EMPTY
							.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/supervisory monitor block " + position.getX() + " " + position.getY() + " " + position.getZ() + " false"))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to unsubscribe")))
							.withColor(ChatFormatting.DARK_GRAY))
					.append(Component.empty().append(event).withStyle(ChatFormatting.WHITE)));
		}
	}

	public static final GlobalObjectManager.InstanceHandle<GuiLogRouter> GUI = new GlobalObjectManager.InstanceHandle<>(GuiLogRouter::new, null);
	public static final GlobalObjectManager.InstanceHandle<ChatRouter> CHAT = new GlobalObjectManager.InstanceHandle<>(ChatRouter::new, null);

	public static class Subscription {
		public final UUID player;
		public final GlobalPos target;

		public Subscription(UUID player, GlobalPos target) {
			this.player = player;
			this.target = target;
		}
	};

	protected Table<GlobalPos, UUID, Subscription> listenersLookup = HashBasedTable.create();
	protected Table<UUID, GlobalPos, Subscription> playerSubscriptionLookup = HashBasedTable.create();

	@SubscribeEvent
	protected static void registerPayload(final RegisterPayloadHandlersEvent event) {
		var registrar = event.registrar("1");

		registrar.commonToClient(LogEvent.TYPE, LogEvent.STREAM_CODEC, (data, context) -> {
			var instance = GUI.get();
			if (instance.onLogReceived != null) {
				instance.onLogReceived.accept(data.event);
			}
		});
	}

	public boolean unsubscribe(Subscription subscription) {
		return this.unsubscribe(subscription.player, subscription.target);
	}

	public boolean unsubscribe(UUID player, GlobalPos target) {
		this.playerSubscriptionLookup.remove(player, target);
		return this.listenersLookup.remove(target, player) != null;
	}

	public void clear(UUID player) {
		for (var it = this.playerSubscriptionLookup.row(player).keySet().iterator(); it.hasNext();) {
			var target = it.next();
			this.listenersLookup.remove(target, player);
			it.remove();
		}
	}

	public boolean subscribe(UUID player, GlobalPos target) {
		return this.subscribe(new Subscription(player, target));
	}

	public boolean subscribe(Subscription subscription) {
		this.playerSubscriptionLookup.put(subscription.player, subscription.target, subscription);
		this.listenersLookup.put(subscription.target, subscription.player, subscription);
		return true;
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

	protected void handleEvent(Level level, BlockPos position, Component event) {
		var listeners = this.listenersLookup.row(new GlobalPos(level.dimension(), position)).values();
		if (listeners == null) return;

		var players = level.getServer().getPlayerList();

		for (var it = listeners.iterator(); it.hasNext();) {
			var subscription = it.next();

			var player = players.getPlayer(subscription.player);
			if (player == null) {
				// If the subscribed player has left the server, unsubscribe them
				it.remove();
				continue;
			}

			this.handleEvent(level, position, player, event);
		}
	}

	protected abstract void handleEvent(Level level, BlockPos position, ServerPlayer player, Component event);

	public static void sendLogEvent(Level level, BlockPos position, Component event) {
		var gui = GUI.tryGet();
		if (gui != null) gui.handleEvent(level, position, event);

		var chat = CHAT.tryGet();
		if (chat != null) chat.handleEvent(level, position, event);
	}
}
