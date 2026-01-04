package bt7s7k7.supervisory.compat.computercraft;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.treeburst.runtime.EvaluationUtil;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.runtime.ManagedTable;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.WorkMonitor;
import dan200.computercraft.core.methods.PeripheralMethod;

public class PeripheralConnection implements IComputerAccess {
	private final static class DummyContext implements ILuaContext {
		@Override
		public long issueMainThreadTask(LuaTask arg0) throws LuaException {
			return 0;
		}

		@Override
		public MethodResult executeMainThreadTask(LuaTask task) throws LuaException {
			return MethodResult.of(task.execute());
		}
	}

	private static final ILuaContext DUMMY_CONTEXT = new DummyContext();

	public final IPeripheral peripheral;
	public final PeripheralReactiveDependency source;
	public final Map<String, PeripheralMethod> methods;

	public PeripheralConnection(IPeripheral peripheral, PeripheralReactiveDependency source) {
		this.peripheral = peripheral;
		this.source = source;
		this.methods = this.source.computerContextSupplier.get().peripheralMethods().getSelfMethods(peripheral);
	}

	public boolean isValid() {
		return this.peripheral == this.source.cachedPeripheral;
	}

	public void teardown() {
		this.peripheral.detach(this);
	}

	public IPeripheral getPeripheralIfValid() {
		if (this.isValid()) return this.peripheral;
		return null;
	}

	public ManagedTable buildWrappingTable(Realm realm) {
		var table = new ManagedTable(realm.TablePrototype);

		var prototype = WRAPPER.ensurePrototype(realm);
		// Force initialize the lazy table
		prototype.getOwnProperty("");
		var handle = WRAPPER.getHandle(this, realm);
		for (var kv : prototype.properties.entrySet()) {
			if (kv.getValue() instanceof ManagedFunction function) {
				table.declareProperty(kv.getKey(), new NativeFunction(realm.FunctionPrototype, Collections.emptyList(), (args, scope, result) -> {
					EvaluationUtil.evaluateInvocation(handle, handle, function, Position.INTRINSIC, args, scope, result);
				}));
			}
		}

		table.declareProperty("__handle__", handle);

		for (var kv : this.methods.entrySet()) {
			var name = kv.getKey();
			var method = kv.getValue();

			table.declareProperty(name, new NativeFunction(realm.FunctionPrototype, List.of("???"), (args, scope, result) -> {
				var computerArguments = new ObjectArguments(args.stream().map(ComputerObject::fromManagedValue).toList());
				try {
					var computerResult = method.apply(this.peripheral, DUMMY_CONTEXT, this, computerArguments);

					var callback = computerResult.getCallback();
					if (callback != null) {
						// We expect the callback to be a TaskCallback instance. To get the result
						// of this class, it expects a 3 element array where the second element is
						// the task ID (always 0 due to our DummyContext) and the third element is
						// "isOk" which it is. This should return a MethodResult with a result.
						computerResult = callback.resume(new Object[] { null, 0, true });
						if (computerResult.getCallback() != null) throw new IllegalStateException("Received MethodResult with a callback after unwrapping");
					}

					result.value = ComputerObject.toManagedValue(computerResult.getResult(), scope.realm);
				} catch (LuaException exception) {
					result.setException(new Diagnostic(exception.getMessage(), Position.INTRINSIC));
				}
			}));
		}
		return table;
	}

	public static NativeHandleWrapper<PeripheralConnection> WRAPPER = new NativeHandleWrapper<>("Interop.PeripheralConnection", PeripheralConnection.class, ctx -> ctx
			// @summary[[Represents a connected peripheral. Each connection is a unique object, with
			// dynamically generated methods for every method of the peripheral.]]
			.addGetter("__valid__", v -> Primitive.from(v.isValid())) // @type: Boolean, @summary: If the peripheral this connection represents is still connected to the system.
			.addGetter("__type__", v -> Primitive.from(v.peripheral.getType()))); // @type: String, @summary: The type of the connected peripheral.

	@Override
	public String getAttachmentName() {
		return this.source.getName();
	}

	@Override
	public @Nullable IPeripheral getAvailablePeripheral(String arg0) {
		if (this.source.getName().equals(arg0)) return this.peripheral;
		return null;
	}

	@Override
	public Map<String, IPeripheral> getAvailablePeripherals() {
		return Map.of(this.source.getName(), this.peripheral);
	}

	@Override
	public int getID() {
		Supervisory.LOGGER.warn("Something used the IComputerAccess.getID of a PeripheralConnection object. This is not supported and returns an invalid value.");
		return 0;
	}

	@Override
	public WorkMonitor getMainThreadMonitor() {
		// Unlink CC computers, our script execution does not have time limits
		return new WorkMonitor() {
			@Override
			public boolean canWork() {
				return true;
			}

			@Override
			public boolean shouldWork() {
				return true;
			}

			@Override
			public void trackWork(long arg0, TimeUnit arg1) {}
		};
	}

	@Override
	public @Nullable String mount(String arg0, Mount arg1, String arg2) {
		return "";
	}

	@Override
	public @Nullable String mountWritable(String arg0, WritableMount arg1, String arg2) {
		return "";
	}

	@Override
	public void queueEvent(String arg0, @Nullable Object... arg1) {
		if (!this.isValid()) {
			Supervisory.LOGGER.warn("Received event to a non-valid connection");
			this.peripheral.detach(this);
			return;
		}

		this.source.handleEvent(arg0, arg1);
	}

	@Override
	public void unmount(@Nullable String arg0) {}
}
