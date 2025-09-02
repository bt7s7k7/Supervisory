package bt7s7k7.supervisory.network;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.ComponentSignal;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.treeburst.support.ManagedValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class NetworkDeviceHost extends BlockEntityComponent {
	public NetworkDeviceHost(CompositeBlockEntity entity) {
		super(entity);
	}

	private NetworkDevice device = null;

	public void setDevice(NetworkDevice device, boolean fresh) {
		var oldDevice = this.device;
		if (this.device != null) {
			this.device.disconnect();
		}
		this.device = device;

		this.onInitializeNetworkDevice.emit(new InitializeNetworkDeviceEvent(oldDevice, device, fresh));

		device.onUpdate = (key, value) -> this.onNetworkUpdate.emit(new NetworkUpdateEvent(key, value));

		device.connect();
		if (fresh) {
			device.sendUpdateRequests();
		}

		this.onDeviceInitialized.emit(device);
	}

	public static record InitializeNetworkDeviceEvent(NetworkDevice oldDevice, NetworkDevice device, boolean fresh) {}

	public final ComponentSignal.Emitter<InitializeNetworkDeviceEvent> onInitializeNetworkDevice = new ComponentSignal.Emitter<>();

	public final ComponentSignal.Emitter<NetworkDevice> onDeviceInitialized = new ComponentSignal.Emitter<>();

	public static record NetworkUpdateEvent(String key, ManagedValue value) {}

	public final ComponentSignal.Emitter<NetworkUpdateEvent> onNetworkUpdate = new ComponentSignal.Emitter<>();

	public NetworkDevice getDevice() {
		if (this.device == null) {
			this.device = new NetworkDevice("");
		}

		return this.device;
	}

	public NetworkDevice tryGetDevice() {
		return this.device;
	}

	public boolean hasDevice() {
		return this.device != null;
	}

	@Override
	public void teardown() {
		super.teardown();

		if (this.device != null) {
			this.device.disconnect();
		}

		this.onInitializeNetworkDevice.teardown();
		this.onNetworkUpdate.teardown();
		this.onDeviceInitialized.teardown();
	}

	@Override
	public void read(CompoundTag tag, HolderLookup.Provider registries) {
		if (tag.contains("device")) {
			NetworkDevice.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("device")).ifSuccess(device -> {
				this.setDevice(device, false);
			}).ifError(error -> {
				Supervisory.LOGGER.error("Failed to load NetworkDevice for " + this.getClass().getSimpleName() + " at " + this.entity.getBlockPos() + ": " + error.toString());
			});
		}
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries) {
		if (this.device != null && (this.device.domain != "" || !this.device.state.isEmpty())) {
			tag.put("device", NetworkDevice.CODEC.encodeStart(NbtOps.INSTANCE, this.device).getOrThrow());
		}
	}

	@Override
	public void tick() {
		if (this.device != null) {
			this.device.sendPendingUpdates();
		}
	}
}
