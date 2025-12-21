package bt7s7k7.supervisory.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mojang.datafixers.util.Pair;

import bt7s7k7.supervisory.support.DomainMonitor;
import bt7s7k7.supervisory.support.GlobalObjectManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber
public class NetworkManager {
	protected NetworkManager() {}

	public static final GlobalObjectManager.InstanceHandle<NetworkManager> HANDLE = new GlobalObjectManager.InstanceHandle<>(NetworkManager::new, null);

	@SubscribeEvent
	private static void tick(ServerTickEvent.Post event) {
		var instance = HANDLE.tryGet();
		if (instance == null) return;
		instance.sendPendingPackets();
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
		DomainMonitor.announce(device.domain, "Device connected");
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

		DomainMonitor.announce(device.domain, "Device disconnected");
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
			DomainMonitor.announce(domainName, packetValue.toString());

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
