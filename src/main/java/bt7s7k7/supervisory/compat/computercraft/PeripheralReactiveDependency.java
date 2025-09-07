package bt7s7k7.supervisory.compat.computercraft;

import static bt7s7k7.treeburst.runtime.ExpressionEvaluator.evaluateInvocation;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.sockets.SocketBasedDependency;
import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.ComputerContext;
import dan200.computercraft.shared.platform.ComponentAccess;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class PeripheralReactiveDependency extends SocketBasedDependency<IPeripheral> {
	protected final Consumer<Diagnostic> errorHandler;

	protected ComponentAccess<IPeripheral> peripheralAccess;
	protected IPeripheral cachedPeripheral = null;

	public ManagedFunction eventHandler = null;

	public final Supplier<ComputerContext> computerContextSupplier;

	public void handleEvent(String name, Object[] arguments) {
		if (this.eventHandler == null) return;

		var result = new ExpressionResult();
		result.executionLimit = 5000;

		var scope = this.owner.globalScope;
		evaluateInvocation(Primitive.VOID, Primitive.VOID, this.eventHandler, Position.INTRINSIC, List.of(Primitive.from(name), ComputerObject.toManagedValue(arguments, scope)), scope, result);
		var diagnostic = result.terminate();
		if (diagnostic != null) {
			this.errorHandler.accept(diagnostic);
		}
	}

	public PeripheralReactiveDependency(ReactivityManager owner, String name, Consumer<Diagnostic> errorHandler, Supplier<ComputerContext> computerContextSupplier) {
		super(owner, name);
		this.errorHandler = errorHandler;
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
