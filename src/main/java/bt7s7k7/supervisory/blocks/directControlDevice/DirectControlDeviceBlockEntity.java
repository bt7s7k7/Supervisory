package bt7s7k7.supervisory.blocks.directControlDevice;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.smartRedstoneComponent.SmartRedstoneComponentBlockEntity;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.treeburst.support.ManagedValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public abstract class DirectControlDeviceBlockEntity extends SmartRedstoneComponentBlockEntity {
	public DirectControlDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}

	private NetworkDevice device = null;

	public void setDevice(NetworkDevice device, boolean fresh) {
		if (this.device != null) {
			this.device.disconnect();
		}
		this.device = device;

		this.initializeNetworkDevice(device, fresh);

		device.onUpdate = this::handleNetworkUpdate;

		device.connect();
		if (fresh) {
			device.sendUpdateRequests();
		}
	}

	protected abstract void initializeNetworkDevice(NetworkDevice device, boolean fresh);

	protected abstract void handleNetworkUpdate(String key, ManagedValue value);

	public NetworkDevice getDevice() {
		if (this.device == null) {
			this.device = new NetworkDevice("");
		}

		return this.device;
	}

	public NetworkDevice tryGetDevice() {
		return this.device;
	}

	protected boolean hasDevice() {
		return this.device != null;
	}

	protected void teardownDevice() {
		if (this.device != null) {
			this.device.disconnect();
		}
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);

		if (tag.contains("device")) {
			NetworkDevice.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("device")).ifSuccess(device -> {
				this.setDevice(device, false);
			}).ifError(error -> {
				Supervisory.LOGGER.error("Failed to load NetworkDevice for " + this.getClass().getSimpleName() + " at " + this.worldPosition + ": " + error.toString());
			});
		}
	}

	@Override
	public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);

		if (this.device != null && (this.device.domain != "" || !this.device.state.isEmpty())) {
			tag.put("device", NetworkDevice.CODEC.encodeStart(NbtOps.INSTANCE, this.device).getOrThrow());
		}
	}

	public void tick(Level level, BlockPos pos, BlockState state) {
		if (this.device != null) {
			this.device.sendPendingUpdates();
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		this.teardownDevice();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		this.teardownDevice();
	}

	protected Direction getFront() {
		return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
	}
}
