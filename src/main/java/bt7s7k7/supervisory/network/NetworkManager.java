package bt7s7k7.supervisory.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	public static class Domain {
		public final HashSet<NetworkDevice> devices = new HashSet<>();
		public final HashMap<String, HashSet<NetworkService>> services = new HashMap<>();
	}

	private Map<String, Domain> domains = new HashMap<>();
	private ArrayList<Pair<String, Object>> pendingPackets = new ArrayList<>();

	public void connectDevice(NetworkDevice device) {
		if (device.domain.isEmpty()) {
			throw new IllegalArgumentException("Cannot connect a device to an empty domain");
		}

		var domain = this.domains.computeIfAbsent(device.domain, __ -> new Domain());
		Supervisory.LOGGER.info("Connected device to domain " + device.domain);
		domain.devices.add(device);

		for (var service : device.getServices()) {
			var services = domain.services.computeIfAbsent(service.getName(), __ -> new HashSet<>());
			services.add(service);
		}
	}

	public void disconnectDevice(NetworkDevice device) {
		if (device.domain.isEmpty()) {
			throw new IllegalArgumentException("Cannot disconnect a device from an empty domain");
		}

		var domain = this.domains.get(device.domain);
		if (domain == null) return;

		Supervisory.LOGGER.info("Disconnected device from domain " + device.domain);
		domain.devices.remove(device);

		for (var service : device.getServices()) {
			var services = domain.services.get(service.getName());
			services.remove(service);

			if (services.isEmpty()) {
				domain.services.remove(service.getName());
			}
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

			var domain = this.domains.get(domainName);
			Supervisory.LOGGER.info("Sending message to domain " + domainName + ": " + packetValue);

			if (domain == null) {
				return;
			}

			for (var device : domain.devices) {
				device.receive(packetValue);
			}
		}
	}

	public Set<NetworkService> getServices(String domainName, String serviceName) {
		var domain = this.domains.get(domainName);
		if (domain == null) return Collections.emptySet();

		var services = domain.services.get(serviceName);
		if (services == null) return Collections.emptySet();

		return Collections.unmodifiableSet(services);
	}

	public <T extends NetworkService> T findService(String domainName, Class<T> type, String serviceName) {
		return this.getServices(domainName, serviceName).stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
	}
}
