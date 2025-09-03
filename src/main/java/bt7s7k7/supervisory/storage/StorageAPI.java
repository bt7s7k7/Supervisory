package bt7s7k7.supervisory.storage;

import java.util.HashMap;
import java.util.List;

import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.support.Side;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedObject;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class StorageAPI extends LazyTable {
	public final CompositeBlockEntity owner;
	public final ReactivityManager reactivityManager;

	protected final HashMap<String, StorageReactiveDependency> connected = new HashMap<>();

	public StorageAPI(ManagedObject prototype, GlobalScope globalScope, CompositeBlockEntity entity, ReactivityManager reactivityManager) {
		super(prototype, globalScope);
		this.owner = entity;
		this.reactivityManager = reactivityManager;
	}

	@Override
	protected void initialize() {
		this.declareProperty("connect", NativeFunction.simple(this.globalScope, List.of("target"), List.of(Primitive.String.class), (args, scope, result) -> {
			var target = args.get(0).getStringValue();

			var dependency = this.connect(target);
			result.value = dependency.getHandle();
			return;
		}));
	}

	public void tick() {
		if (!this.initialized) return;

		for (var dependency : this.connected.values()) {
			this.updateDependency(dependency);
		}
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

		loadHandler: do {
			if (dependency.target != null) {
				handler = dependency.target.getCapability();
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

				// Failed to acquire capability
				break loadHandler;
			} while (false);

			// Load the newly acquired capability
			dependency.target = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, (ServerLevel) this.owner.getLevel(), position, null);
			handler = dependency.target.getCapability();
		} while (false);

		if (handler == null) {
			if (dependency.getValue() == Primitive.VOID) return;
			dependency.updateValue(Primitive.VOID);
			return;
		}

		var oldValue = dependency.getValue() == Primitive.VOID ? null : dependency.getValue().getNativeValue(StorageReport.class);
		var newValue = new StorageReport(handler);

		if (oldValue == null || newValue.itemsChanged(oldValue)) {
			dependency.updateValue(StorageReport.WRAPPER.getHandle(newValue, this.globalScope));
		}
	}
}
