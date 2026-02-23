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
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class StorageAPI extends LazyTable { // @symbol: Storage
	// @summary: Provides methods for scanning inventories and fluids tanks, getting their items and
	// fluids and transferring them around. The API for working with items and fluids is identical;
	// the only difference is using {@link Storage.connectFluid} instead of {@link Storage.connect}.
	protected final SocketConnectionManager<IItemHandler, StorageReactiveDependency<IItemHandler>> itemConnectionManager;
	protected final SocketConnectionManager<IFluidHandler, StorageReactiveDependency<IFluidHandler>> fluidConnectionManager;

	private abstract class StorageConnectionManager<T> extends SocketConnectionManager<T, StorageReactiveDependency<T>> {
		public StorageConnectionManager(String namespace, int updateInterval, CompositeBlockEntity owner, ReactivityManager reactivityManager, Supplier<NetworkDevice> networkDeviceSupplier) {
			super(namespace, updateInterval, owner, reactivityManager, networkDeviceSupplier);
		}

		protected abstract StorageReport<T, ?> makeReport(BlockCapabilityCache<T, Direction> capabilityCache, T capability);

		@Override
		protected void processDependency(StorageReactiveDependency<T> dependency, T capability) {
			if (capability == null) {
				if (dependency.getValue() == Primitive.VOID) return;
				dependency.updateValue(Primitive.VOID);
				return;
			}

			var oldValue = dependency.getValue() == Primitive.VOID ? null : dependency.getValue().getNativeValue(StorageReport.class);
			var newValue = this.makeReport(dependency.capabilityCache, capability);

			if (oldValue == null || newValue.contentChanged(oldValue)) {
				dependency.updateValue(StorageReport.WRAPPER.getHandle(newValue, StorageAPI.this.realm));
			}
		}
	}

	public StorageAPI(ManagedObject prototype, Realm realm, CompositeBlockEntity entity, ReactivityManager reactivityManager, Supplier<NetworkDevice> networkDeviceSupplier) {
		super(prototype, realm);

		this.itemConnectionManager = new StorageConnectionManager<IItemHandler>("storage", 5, entity, reactivityManager, networkDeviceSupplier) {
			@Override
			protected StorageReactiveDependency.Item makeHandle(String name) {
				return new StorageReactiveDependency.Item(this.reactivityManager, name);
			}

			@Override
			protected StorageReport<IItemHandler, ?> makeReport(BlockCapabilityCache<IItemHandler, Direction> capabilityCache, IItemHandler capability) {
				return new StorageReport.Item(capabilityCache, capability);
			}
		};

		this.fluidConnectionManager = new StorageConnectionManager<IFluidHandler>("storage", 5, entity, reactivityManager, networkDeviceSupplier) {
			@Override
			protected StorageReactiveDependency.Fluid makeHandle(String name) {
				return new StorageReactiveDependency.Fluid(this.reactivityManager, name);
			}

			@Override
			protected StorageReport<IFluidHandler, ?> makeReport(BlockCapabilityCache<IFluidHandler, Direction> capabilityCache, IFluidHandler capability) {
				return new StorageReport.Fluid(capabilityCache, capability);
			}
		};
	}

	@Override
	protected void initialize() {
		this.declareProperty("connect", NativeFunction.simple(this.realm, List.of("target"), List.of(Primitive.String.class), (args, scope, result) -> {
			// @summary[[Connects to an inventory, which will be scanned for contained items. The
			// `target` can be either a side of this system or a name of a socket on the network.
			// This function returns a {@link Storage.StorageReactiveDependency} that can watched for
			// changes using the {@link reactive} function.]]
			var target = args.get(0).getStringValue();

			var dependency = this.itemConnectionManager.connect(target);
			result.value = dependency.getHandle();
			return;
		}));

		this.declareProperty("connectFluid", NativeFunction.simple(this.realm, List.of("target"), List.of(Primitive.String.class), (args, scope, result) -> {
			// @summary[[Connects to a fluid tank, which will be scanned for contained fluids. The
			// `target` can be either a side of this system or a name of a socket on the network.
			// This function returns a {@link Storage.StorageReactiveDependency} that can watched for
			// changes using the {@link reactive} function.]]
			var target = args.get(0).getStringValue();

			var dependency = this.fluidConnectionManager.connect(target);
			result.value = dependency.getHandle();
			return;
		}));

		this.declareProperty("transfer", NativeFunction.simple(this.realm, List.of("sources", "targets", "maxCount?"), List.of(ManagedArray.class, ManagedArray.class, Primitive.Number.class), (args, scope, result) -> {
			// @summary[[Transfers all stacks in the `sources` argument into the `targets`
			// inventories/tanks. The `sources` array should contain {@link Storage.StackReport} instances
			// for all items/fluids that should be transferred or {@link Storage.StorageReport} instances
			// to specify source inventories/tanks. The `targets` array should contain {@link Storage.StorageReport}
			// instances for all inventories/tanks that should be transferred into.
			// You can specify the maximum transfer count using the `maxCount` parameter. If there
			// is not enough space to transfer items/fluids or `maxCount` is less than the available number
			// of items/fluids, only a partial transfer is performed. The amount of transferred items/fluids is
			// returned.]]
			var maxCount = args.size() > 2 ? (int) args.get(2).getNumberValue() : Integer.MAX_VALUE;

			var items = new ArrayList<StackReport>();
			var targets = new ArrayList<StorageReport.Item>();

			var fluids = new ArrayList<StackReport>();
			var fluidTargets = new ArrayList<StorageReport.Fluid>();

			var enabled = Config.ALLOW_STORAGE_TRANSFER.getAsBoolean();
			if (!enabled) {
				result.setException(new Diagnostic("Transfer function has been disabled in server config", Position.INTRINSIC));
				return;
			}

			for (var element : args.get(0).getArrayValue()) {
				if (element instanceof NativeHandle handle) {
					if (handle.value instanceof StackReport.Item report) {
						items.add(report);
						continue;
					}

					if (handle.value instanceof StorageReport.Item report) {
						items.addAll(report.getSlots().values());
						continue;
					}

					if (handle.value instanceof StackReport.Fluid report) {
						fluids.add(report);
						continue;
					}

					if (handle.value instanceof StorageReport.Fluid report) {
						fluids.addAll(report.getSlots().values());
						continue;
					}
				}

				result.setException(new Diagnostic("All source elements must be instances of StackReport or StorageReport", Position.INTRINSIC));
				return;
			}

			for (var element : args.get(1).getArrayValue()) {
				if (element instanceof NativeHandle handle) {
					if (handle.value instanceof StorageReport.Item report) {
						targets.add(report);
						continue;
					}

					if (handle.value instanceof StorageReport.Fluid report) {
						fluidTargets.add(report);
						continue;
					}

					result.setException(new Diagnostic("All target elements must be instances of StorageReport", Position.INTRINSIC));
					return;
				}
			}

			Stream.concat(
					Stream.concat(
							items.stream().map(v -> v.source),
							fluids.stream().map(v -> v.source)),
					Stream.concat(
							targets.stream(),
							fluidTargets.stream()))
					.distinct()
					.forEach(StorageReport::ensureHandlerStillValid);

			var transferCount = 0;

			for (var item : items) {
				var outputHandler = ((StorageReport.Item) item.source).getHandler();
				// This will be null if the source container was destroyed
				if (outputHandler == null) continue;

				var countToTransfer = item.count();

				if (transferCount + countToTransfer > maxCount) {
					countToTransfer = maxCount - transferCount;
				}

				var extractedStack = outputHandler.extractItem(item.slot, countToTransfer, true);
				// This may be a good place to check if the item in the slot is still the same
				// as in the report, but I don't think this will ever cause a problem.
				if (extractedStack.isEmpty()) continue;

				var extracted = 0;

				for (var target : targets) {
					var inputHandler = target.handler;
					// This will be null if the destination container was destroyed
					if (inputHandler == null) continue;

					// insertItemStacked modifies the stack to remove inserted items from it
					var prevCount = extractedStack.getCount();
					extractedStack = ItemHandlerHelper.insertItemStacked(target.handler, extractedStack, false);
					var delta = prevCount - extractedStack.getCount();
					transferCount += delta;
					extracted += delta;
					if (extractedStack.isEmpty()) break;
				}

				outputHandler.extractItem(item.slot, extracted, false);

				if (transferCount >= maxCount) {
					break;
				}
			}

			for (var fluid : fluids) {
				var outputHandler = ((StorageReport.Fluid) fluid.source).getHandler();
				// This will be null if the source container was destroyed
				if (outputHandler == null) continue;

				var countToTransfer = fluid.count();

				if (transferCount + countToTransfer > maxCount) {
					countToTransfer = maxCount - transferCount;
				}

				var extractedStack = outputHandler.drain(((StackReport.Fluid) fluid).getFluidWithAmount(countToTransfer), FluidAction.SIMULATE);
				// This may be a good place to check if the fluid in the slot is still the same
				// as in the report, but I don't think this will ever cause a problem.
				if (extractedStack.isEmpty()) continue;

				var extracted = 0;
				var extractedStackAmount = extractedStack.getAmount();

				for (var target : fluidTargets) {
					var inputHandler = target.handler;
					// This will be null if the destination container was destroyed
					if (inputHandler == null) continue;

					// insertFluidStacked modifies the stack to remove inserted fluids from it
					var delta = target.handler.fill(extractedStack.copyWithAmount(extractedStackAmount), FluidAction.EXECUTE);
					transferCount += delta;
					extracted += delta;
					extractedStackAmount -= delta;
					if (extractedStackAmount == 0) break;
				}

				outputHandler.drain(((StackReport.Fluid) fluid).getFluidWithAmount(extracted), FluidAction.EXECUTE);

				if (transferCount >= maxCount) {
					break;
				}
			}

			result.value = Primitive.from(transferCount);
		}));

		this.declareProperty("update", this.itemConnectionManager.updateTick.getHandle()); // @type: SYS.TickReactiveDependency, @summary: Triggers every 5 ticks, just after all connected inventories have been scanned.
	}

	public void tick() {
		if (!this.initialized) return;
		this.itemConnectionManager.tick();
		this.fluidConnectionManager.tick();
	}
}
