package bt7s7k7.supervisory.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.sockets.SocketConnectionManager;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.ManagedObject;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.NativeHandle;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class StorageAPI extends LazyTable {
	protected final SocketConnectionManager<IItemHandler, StorageReactiveDependency> connectionManager;

	public StorageAPI(ManagedObject prototype, GlobalScope globalScope, CompositeBlockEntity entity, ReactivityManager reactivityManager, Supplier<NetworkDevice> deviceGetter) {
		super(prototype, globalScope);

		this.connectionManager = new SocketConnectionManager<IItemHandler, StorageReactiveDependency>("storage", 5, entity, reactivityManager, deviceGetter) {
			@Override
			protected void processDependency(StorageReactiveDependency dependency, IItemHandler capability) {
				if (capability == null) {
					if (dependency.getValue() == Primitive.VOID) return;
					dependency.updateValue(Primitive.VOID);
					return;
				}

				var oldValue = dependency.getValue() == Primitive.VOID ? null : dependency.getValue().getNativeValue(StorageReport.class);
				var newValue = new StorageReport(dependency.capabilityCache, capability);

				if (oldValue == null || newValue.itemsChanged(oldValue)) {
					dependency.updateValue(StorageReport.WRAPPER.getHandle(newValue, StorageAPI.this.globalScope));
				}
			}

			@Override
			protected StorageReactiveDependency makeHandle(String name) {
				return new StorageReactiveDependency(this.reactivityManager, name);
			}
		};
	}

	@Override
	protected void initialize() {
		this.declareProperty("connect", NativeFunction.simple(this.globalScope, List.of("target"), List.of(Primitive.String.class), (args, scope, result) -> {
			var target = args.get(0).getStringValue();

			var dependency = this.connectionManager.connect(target);
			result.value = dependency.getHandle();
			return;
		}));

		this.declareProperty("transfer", NativeFunction.simple(this.globalScope, List.of("items", "targets"), List.of(ManagedArray.class, ManagedArray.class), (args, scope, result) -> {
			var items = new ArrayList<StackReport>();
			var targets = new ArrayList<StorageReport>();

			for (var element : args.get(0).getArrayValue().elements) {
				if (!(element instanceof NativeHandle handle) || !(handle.value instanceof StackReport report)) {
					result.setException(new Diagnostic("All item elements must be instances of StackReport", Position.INTRINSIC));
					return;
				}

				items.add(report);
			}

			for (var element : args.get(1).getArrayValue().elements) {
				if (!(element instanceof NativeHandle handle) || !(handle.value instanceof StorageReport report)) {
					result.setException(new Diagnostic("All target elements must be instances of StorageReport", Position.INTRINSIC));
					return;
				}

				targets.add(report);
			}

			Stream.concat(
					items.stream().map(v -> v.source),
					targets.stream())
					.distinct()
					.forEach(StorageReport::ensureHandlerStillValid);

			var transferCount = 0;

			for (var item : items) {
				var outputHandler = item.source.getHandler();
				// This will be null if the source container was destroyed
				if (outputHandler == null) continue;

				var originalStack = outputHandler.getStackInSlot(item.slot);
				var stack = originalStack.copy();
				// This may be a good place to check if the item in the slot is still the same
				// as in the report, but I don't think this will ever cause a problem.
				if (stack.isEmpty()) continue;

				for (var target : targets) {
					var inputHandler = target.handler;
					// This will be null if the destination container was destroyed
					if (inputHandler == null) continue;

					// insertItemStacked modifies the stack to remove inserted items from it
					var prevCount = stack.getCount();
					stack = ItemHandlerHelper.insertItemStacked(target.handler, stack, false);
					transferCount += prevCount - stack.getCount();
					if (stack.isEmpty()) break;
				}

				originalStack.setCount(stack.getCount());
			}

			result.value = Primitive.from(transferCount);
		}));

		this.declareProperty("update", this.connectionManager.updateTick.getHandle());
	}

	public void tick() {
		if (!this.initialized) return;
		this.connectionManager.tick();
	}
}
