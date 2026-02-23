package bt7s7k7.supervisory.storage;

import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.sockets.SocketBasedDependency;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public abstract class StorageReactiveDependency<T> extends SocketBasedDependency<T> { // @symbol: Storage.StorageReactiveDependency
	// @prototype: ReactiveDependency.prototype
	// @summary: Triggers each time the storage contents change. The value is a {@link Storage.StorageReport}, storing the contained items.
	public BlockCapabilityCache<T, Direction> capabilityCache;

	public StorageReactiveDependency(ReactivityManager owner, String name) {
		super(owner, name);
	}

	protected abstract BlockCapability<T, Direction> getTargetCapability();

	@Override
	public void reset() {
		super.reset();
		this.capabilityCache = null;
	}

	@Override
	public T tryGetCachedCapability() {
		if (this.capabilityCache != null) {
			return this.capabilityCache.getCapability();
		}

		return null;
	}

	@Override
	public T tryAcquireCapability(ServerLevel level, BlockPos position) {
		this.capabilityCache = BlockCapabilityCache.create(this.getTargetCapability(), level, position, null);
		return this.capabilityCache.getCapability();
	}

	public static class Item extends StorageReactiveDependency<IItemHandler> {
		public Item(ReactivityManager owner, String name) {
			super(owner, name);
		}

		@Override
		protected BlockCapability<IItemHandler, Direction> getTargetCapability() {
			return Capabilities.ItemHandler.BLOCK;
		}
	}

	public static class Fluid extends StorageReactiveDependency<IFluidHandler> {
		public Fluid(ReactivityManager owner, String name) {
			super(owner, name);
		}

		@Override
		protected BlockCapability<IFluidHandler, Direction> getTargetCapability() {
			return Capabilities.FluidHandler.BLOCK;
		}
	}
}
