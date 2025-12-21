package bt7s7k7.supervisory.storage;

import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.sockets.SocketBasedDependency;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class StorageReactiveDependency extends SocketBasedDependency<IItemHandler> {
	public BlockCapabilityCache<IItemHandler, Direction> capabilityCache;

	public StorageReactiveDependency(ReactivityManager owner, String name) {
		super(owner, name);
	}

	@Override
	public void reset() {
		super.reset();
		this.capabilityCache = null;
	}

	@Override
	public IItemHandler tryGetCachedCapability() {
		if (this.capabilityCache != null) {
			return this.capabilityCache.getCapability();
		}

		return null;
	}

	@Override
	public IItemHandler tryAcquireCapability(ServerLevel level, BlockPos position) {
		this.capabilityCache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, level, position, null);
		return this.capabilityCache.getCapability();
	}
}
