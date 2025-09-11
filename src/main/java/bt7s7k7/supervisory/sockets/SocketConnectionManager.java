package bt7s7k7.supervisory.sockets;

import java.util.HashMap;
import java.util.function.Supplier;

import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.network.NetworkManager;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.support.Side;
import bt7s7k7.supervisory.system.TickReactiveDependency;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public abstract class SocketConnectionManager<TCapability, THandle extends SocketBasedDependency<TCapability>> {
	public final TickReactiveDependency updateTick;
	public final String namespace;

	protected final CompositeBlockEntity owner;
	protected final Supplier<NetworkDevice> networkDeviceSupplier;
	protected final ReactivityManager reactivityManager;
	protected final int updateInterval;

	protected final HashMap<String, THandle> connected = new HashMap<>();
	protected int ticksUntilUpdate = 0;

	public SocketConnectionManager(String namespace, int updateInterval, CompositeBlockEntity owner, ReactivityManager reactivityManager, Supplier<NetworkDevice> networkDeviceSupplier) {
		this.namespace = namespace;
		this.updateInterval = updateInterval;
		this.owner = owner;
		this.reactivityManager = reactivityManager;
		this.networkDeviceSupplier = networkDeviceSupplier;
		this.updateTick = TickReactiveDependency.get(this.namespace + "_update", this.reactivityManager);
	}

	protected abstract THandle makeHandle(String name);

	protected abstract void processDependency(THandle dependency, TCapability capability);

	public void tick() {
		if (this.updateInterval > 1) {
			this.ticksUntilUpdate--;

			if (this.ticksUntilUpdate > 0) {
				return;
			}

			this.ticksUntilUpdate += this.updateInterval;
		}

		for (THandle dependency : this.connected.values()) {
			this.updateDependency(dependency);
		}

		this.updateTick.updateValue(Primitive.from(this.owner.getLevel().getGameTime()));

	}

	public THandle connect(String target) {
		var existing = this.connected.get(target);
		if (existing != null) return existing;

		var dependency = this.reactivityManager.addDependency("storage:" + target, this.makeHandle(target));
		this.connected.put(target, dependency);
		return dependency;
	}

	protected void updateDependency(THandle dependency) {
		var name = dependency.getName();

		TCapability handler = null;
		if (dependency.provider != null && !dependency.provider.isValid()) {
			// If the dependency used a remote StorageProvider, and that provider was destroyed,
			// clear the values and try to acquire another provider
			dependency.reset();
		}

		loadHandler: do {
			handler = dependency.tryGetCachedCapability();
			if (handler != null) break;

			BlockPos position;

			do {
				var side = Side.getByName(name);
				if (side != null) {
					var front = this.owner.getFront();
					var directionToward = side.getDirection(front);
					position = this.owner.getBlockPos().relative(directionToward);
					// Create an ephemeral provider only to provide information to the dependency
					dependency.provider = new SocketProvider(this.owner, name, directionToward);
					break;
				}

				var networkDevice = this.networkDeviceSupplier.get();
				if (networkDevice != null && !networkDevice.domain.isEmpty()) {
					var provider = NetworkManager.getInstance().findService(networkDevice.domain, SocketProvider.class, name);
					if (provider == null) break loadHandler;

					dependency.provider = provider;
					position = provider.getTarget();
					break;
				}

				// Failed to acquire capability
				break loadHandler;
			} while (false);

			// Load the newly acquired capability
			handler = dependency.tryAcquireCapability((ServerLevel) this.owner.getLevel(), position);
		} while (false);

		this.processDependency(dependency, handler);
	}
}
