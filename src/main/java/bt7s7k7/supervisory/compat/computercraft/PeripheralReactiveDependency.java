package bt7s7k7.supervisory.compat.computercraft;

import static bt7s7k7.treeburst.runtime.EvaluationUtil.evaluateInvocation;

import java.util.List;
import java.util.function.Supplier;

import bt7s7k7.supervisory.script.ManagedWorkDispatcher;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.sockets.SocketBasedDependency;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.shared.platform.ComponentAccess;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class PeripheralReactiveDependency extends SocketBasedDependency<IPeripheral> { // @symbol: Interop.PeripheralReactiveDependency
	// @prototype: ReactiveDependency.prototype
	// @native-alias: PeripheralReactiveDependency
	// @summary: Allows for connecting to a ComputerCraft peripheral. The value is a {@link Interop.PeripheralConnection} that represents a connected peripheral.
	protected final ManagedWorkDispatcher workDispatcher;

	protected ComponentAccess<IPeripheral> peripheralAccess;
	protected IPeripheral cachedPeripheral = null;

	public ManagedFunction eventHandler = null;
	public Runnable teardownCallback = null;

	public final Supplier<ComputerContext> computerContextSupplier;

	public void handleEvent(String name, Object[] arguments) {
		if (this.eventHandler == null) return;

		this.workDispatcher.performWork(result -> {
			var realm = this.owner.realm;
			var managedArguments = List.of(Primitive.from(name), ComputerObject.toManagedValue(arguments, realm));
			evaluateInvocation(Primitive.VOID, Primitive.VOID, this.eventHandler, Position.INTRINSIC, managedArguments, realm.globalScope, result);
		});
	}

	public PeripheralReactiveDependency(ReactivityManager owner, String name, ManagedWorkDispatcher workDispatcher, Supplier<ComputerContext> computerContextSupplier) {
		super(owner, name);
		this.workDispatcher = workDispatcher;
		this.computerContextSupplier = computerContextSupplier;
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
