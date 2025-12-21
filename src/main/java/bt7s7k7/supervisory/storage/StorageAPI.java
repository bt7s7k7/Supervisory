package bt7s7k7.supervisory.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import bt7s7k7.supervisory.Config;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.sockets.SocketConnectionManager;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.ManagedObject;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.NativeHandle;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class StorageAPI extends LazyTable { // @symbol: Storage
	// @summary: Provides methods for scanning inventories, getting their items and transferring items between inventories.
	protected final SocketConnectionManager<IItemHandler, StorageReactiveDependency> connectionManager;

	public StorageAPI(ManagedObject prototype, Realm realm, CompositeBlockEntity entity, ReactivityManager reactivityManager, Supplier<NetworkDevice> networkDeviceSupplier) {
		super(prototype, realm);

		this.connectionManager = new SocketConnectionManager<IItemHandler, StorageReactiveDependency>("storage", 5, entity, reactivityManager, networkDeviceSupplier) {
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
					dependency.updateValue(StorageReport.WRAPPER.getHandle(newValue, StorageAPI.this.realm));
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
		this.declareProperty("connect", NativeFunction.simple(this.realm, List.of("target"), List.of(Primitive.String.class), (args, scope, result) -> {
			// @summary[[Connects to an inventory, that will be scanned for contained items. The
			// `target` can be either a side of this system or a name of a socket on the network.
			// This function returns a {@link StorageReactiveDependency} that can watched for
			// changes using the {@link reactive} function.]]
			var target = args.get(0).getStringValue();

			var dependency = this.connectionManager.connect(target);
			result.value = dependency.getHandle();
			return;
		}));

		this.declareProperty("transfer", NativeFunction.simple(this.realm, List.of("items", "targets"), List.of(ManagedArray.class, ManagedArray.class), (args, scope, result) -> {
			// @summary[[Transfers all item stacks in the `items` argument into the `targets`
			// inventories. The `items` array should contain {@link StackReport} instances for all
			// items that should be transferred. The `targets` array should contain {@link
			// StorageReport} instances for all inventories that should be transferred into. If
			// there is not enough space to transfer items, only a partial transfer is performed.
			// The amount of transferred items is returned.]]
			var items = new ArrayList<StackReport>();
			var targets = new ArrayList<StorageReport>();

			var enabled = Config.ALLOW_STORAGE_TRANSFER.getAsBoolean();
			if (!enabled) {
				result.setException(new Diagnostic("Transfer function has been disabled in server config", Position.INTRINSIC));
				return;
			}

			for (var element : args.get(0).getArrayValue()) {
				if (!(element instanceof NativeHandle handle) || !(handle.value instanceof StackReport report)) {
					result.setException(new Diagnostic("All item elements must be instances of StackReport", Position.INTRINSIC));
					return;
				}

				items.add(report);
			}

			for (var element : args.get(1).getArrayValue()) {
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

		this.declareProperty("update", this.connectionManager.updateTick.getHandle()); // @type: TickReactiveDependency, @summary: Triggers every 5 ticks, just after all connected inventories have been scanned.
	}

	public void tick() {
		if (!this.initialized) return;
		this.connectionManager.tick();
	}
}
