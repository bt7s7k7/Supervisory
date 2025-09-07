package bt7s7k7.supervisory.compat.computercraft;

import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.sockets.SocketBasedDependency;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.platform.ComponentAccess;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class PeripheralReactiveDependency extends SocketBasedDependency<IPeripheral> {
	protected ComponentAccess<IPeripheral> peripheralAccess;
	protected IPeripheral cachedPeripheral = null;

	public PeripheralReactiveDependency(ReactivityManager owner, String name) {
		super(owner, name);
	}

	@Override
	public ManagedValue getValue() {
		// Make sure a stale wrapper is not returned
		if (this.cachedPeripheral == null) return Primitive.VOID;
		return super.getValue();
	}

	public ManagedValue getValueEvenIfInvalid() {
		return super.getValue();
	}

	@Override
	public IPeripheral tryGetCachedCapability() {
		if (this.cachedPeripheral == null && this.peripheralAccess != null) {
			this.cachedPeripheral = this.peripheralAccess.get(this.provider.getDirection());
		}
		return this.cachedPeripheral;
	}

	@Override
	public IPeripheral tryAcquireCapability(ServerLevel level, BlockPos position) {
		this.peripheralAccess = PlatformHelper.get().createPeripheralAccess(this.provider.entity, direction -> {
			// This is an invalidation callback
			if (!direction.equals(this.provider.getDirection())) return;
			this.cachedPeripheral = null;
		});

		return this.cachedPeripheral = this.peripheralAccess.get(this.provider.getDirection());
	}
}
