package bt7s7k7.supervisory.support;

import java.util.UUID;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class DomainMonitor {
	private DomainMonitor() {};

	public static final GlobalObjectManager.InstanceHandle<DomainMonitor> HANDLE = new GlobalObjectManager.InstanceHandle<>(DomainMonitor::new, null);

	public record Subscription(UUID player, String domain) {};

	protected final Table<String, UUID, Subscription> subscriptions = HashBasedTable.create();

	public boolean subscribe(UUID player, String domain) {
		var existing = this.subscriptions.get(domain, player);
		if (existing != null) return false;

		this.subscriptions.put(domain, player, new Subscription(player, domain));
		return true;
	}

	public boolean unsubscribe(UUID player, String domain) {
		return this.subscriptions.remove(domain, player) != null;
	}

	public static void announce(String domain, String message) {
		var instance = HANDLE.tryGet();
		if (instance == null) return;
		var server = ServerLifecycleHooks.getCurrentServer();
		var players = server.getPlayerList();

		var listeners = instance.subscriptions.row(domain).keySet();
		for (var it = listeners.iterator(); it.hasNext();) {
			var uuid = it.next();

			var player = players.getPlayer(uuid);
			if (player == null) {
				// If the subscribed player has left the server, unsubscribe them
				it.remove();
				continue;
			}

			player.sendSystemMessage(Component.literal("[domain \"" + Primitive.String.escapeString(domain) + "\"] ")
					.withStyle(Style.EMPTY
							.withColor(ChatFormatting.GRAY)
							.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/supervisory monitor domain name \"" + Primitive.String.escapeString(domain) + "\" false"))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to unsubscribe"))))
					.append(Component.literal(message).withStyle(ChatFormatting.WHITE)));
		}
	}

}
