package bt7s7k7.supervisory.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.mojang.datafixers.util.Pair;

import bt7s7k7.supervisory.Supervisory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber
public class NetworkManager {
	protected NetworkManager() {}

	protected static NetworkManager instance = null;

	public static NetworkManager getInstance() {
		return instance;
	}

	@SubscribeEvent
	private static void tick(ServerTickEvent.Post event) {
		if (instance != null) {
			instance.sendPendingPackets();
		}
	}

	@SubscribeEvent
	private static void initializeNetwork(ServerStartedEvent event) {
		Supervisory.LOGGER.info("Initializing network manager");
		instance = new NetworkManager();
	}

	@SubscribeEvent
	// Using ServerStoppedEvent instead of ServerStoppingEvent because loaded NetworkDevices use
	// disconnectDevice on unload which is called after ServerStoppingEvent
	private static void uninitializeNetwork(ServerStoppedEvent event) {
		Supervisory.LOGGER.info("Unloading network manager");
		instance = null;
	}

	private Map<String, HashSet<NetworkDevice>> devices = new HashMap<>();
	private ArrayList<Pair<String, Object>> pendingPackets = new ArrayList<>();

	public void connectDevice(NetworkDevice device) {
		if (device.domain.isEmpty()) {
			throw new IllegalArgumentException("Cannot connect a device to an empty domain");
		}

		var domain = this.devices.computeIfAbsent(device.domain, __ -> new HashSet<>());
		Supervisory.LOGGER.info("Connected device to domain " + device.domain);
		domain.add(device);
	}

	public void disconnectDevice(NetworkDevice device) {
		if (device.domain.isEmpty()) {
			throw new IllegalArgumentException("Cannot disconnect a device from an empty domain");
		}

		var domain = this.devices.get(device.domain);
		if (domain != null) {
			Supervisory.LOGGER.info("Disconnected device to domain " + device.domain);
			domain.remove(device);
		}
	}

	public void send(String domainName, Object packetValue) {
		this.pendingPackets.add(Pair.of(domainName, packetValue));
	}

	private void sendPendingPackets() {
		var pendingPackets = this.pendingPackets;
		this.pendingPackets = new ArrayList<>();
		for (var packet : pendingPackets) {
			var domainName = packet.getFirst();
			var packetValue = packet.getSecond();

			var domain = this.devices.get(domainName);
			Supervisory.LOGGER.info("Sending message to domain " + domainName + ": " + packetValue);

			if (domain == null) {
				return;
			}

			for (var device : domain) {
				device.receive(packetValue);
			}
		}
	}
}
