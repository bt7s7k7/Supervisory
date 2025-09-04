package bt7s7k7.supervisory.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import bt7s7k7.supervisory.blocks.programmableLogicController.TickReactiveDependency;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.network.NetworkManager;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.support.Side;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.ManagedObject;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.NativeHandle;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class StorageAPI extends LazyTable {
	public final CompositeBlockEntity owner;
	public final ReactivityManager reactivityManager;

	protected final HashMap<String, StorageReactiveDependency> connected = new HashMap<>();
	protected final Supplier<NetworkDevice> deviceGetter;

	protected int ticksUntilUpdate = 0;
	protected final TickReactiveDependency updateTick;

	public StorageAPI(ManagedObject prototype, GlobalScope globalScope, CompositeBlockEntity entity, ReactivityManager reactivityManager, Supplier<NetworkDevice> deviceGetter) {
		super(prototype, globalScope);
		this.owner = entity;
		this.reactivityManager = reactivityManager;
		this.deviceGetter = deviceGetter;
		this.updateTick = TickReactiveDependency.get("storage:update", reactivityManager);
	}

	@Override
	protected void initialize() {
		this.declareProperty("connect", NativeFunction.simple(this.globalScope, List.of("target"), List.of(Primitive.String.class), (args, scope, result) -> {
			var target = args.get(0).getStringValue();

			var dependency = this.connect(target);
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

		this.declareProperty("update", this.updateTick.getHandle());
	}

	public void tick() {
		if (!this.initialized) return;

		if (this.ticksUntilUpdate > 0) {
			this.ticksUntilUpdate--;
			return;
		}

		for (var dependency : this.connected.values()) {
			this.updateDependency(dependency);
		}

		this.updateTick.updateValue(Primitive.from(this.owner.getLevel().getGameTime()));

		this.ticksUntilUpdate += 4;
	}

	public StorageReactiveDependency connect(String target) {
		var existing = this.connected.get(target);
		if (existing != null) return existing;

		var dependency = this.reactivityManager.addDependency("storage:" + target, new StorageReactiveDependency(this.reactivityManager, target));
		this.connected.put(target, dependency);
		return dependency;
	}

	protected void updateDependency(StorageReactiveDependency dependency) {
		var name = dependency.getName();

		IItemHandler handler = null;
		if (dependency.provider != null && !dependency.provider.isValid()) {
			// If the dependency used a remote StorageProvider, and that provider was destroyed,
			// clear the values and try to acquire another provider
			dependency.provider = null;
			dependency.capabilityCache = null;
		}

		loadHandler: do {
			if (dependency.capabilityCache != null) {
				handler = dependency.capabilityCache.getCapability();
				break;
			}

			BlockPos position;

			do {
				var side = Side.getByName(name);
				if (side != null) {
					var front = this.owner.getFront();
					var directionToward = side.getDirection(front);
					position = this.owner.getBlockPos().relative(directionToward);
					break;
				}

				var networkDevice = this.deviceGetter.get();
				if (networkDevice != null && !networkDevice.domain.isEmpty()) {
					var provider = NetworkManager.getInstance().findService(networkDevice.domain, StorageProvider.class, name);
					if (provider == null) break loadHandler;

					dependency.provider = provider;
					position = provider.getTarget();
					break;
				}

				// Failed to acquire capability
				break loadHandler;
			} while (false);

			// Load the newly acquired capability
			dependency.capabilityCache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, (ServerLevel) this.owner.getLevel(), position, null);
			handler = dependency.capabilityCache.getCapability();
		} while (false);

		if (handler == null) {
			if (dependency.getValue() == Primitive.VOID) return;
			dependency.updateValue(Primitive.VOID);
			return;
		}

		var oldValue = dependency.getValue() == Primitive.VOID ? null : dependency.getValue().getNativeValue(StorageReport.class);
		var newValue = new StorageReport(dependency.capabilityCache, handler);

		if (oldValue == null || newValue.itemsChanged(oldValue)) {
			dependency.updateValue(StorageReport.WRAPPER.getHandle(newValue, this.globalScope));
		}
	}
}
