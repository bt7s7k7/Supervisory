package bt7s7k7.supervisory.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import bt7s7k7.supervisory.Support;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public abstract class StorageReport<THandler, TStack> {
	protected THandler handler;

	public final BlockCapabilityCache<THandler, Direction> source;

	protected final Map<Integer, TStack> slots;

	public StorageReport(BlockCapabilityCache<THandler, Direction> source, THandler handler) {
		this.source = source;
		this.handler = handler;
		this.slots = new HashMap<>();

		this.loadSlots();
	}

	protected abstract void loadSlots();

	public void ensureHandlerStillValid() {
		this.handler = this.source.getCapability();
	}

	public THandler getHandler() {
		return this.handler;
	}

	protected abstract Stream<Integer> createCommonItemStreamIfPossible(StorageReport<?, ?> other);

	protected abstract boolean compareStacks(TStack a, TStack b);

	public boolean contentChanged(StorageReport<?, ?> previous) {
		var keys = this.createCommonItemStreamIfPossible(previous);
		if (keys == null) return true;
		keys = keys.distinct();

		for (var key : Support.getIterable(keys::iterator)) {
			var oldValue = previous.slots.get(key);
			var newValue = this.slots.get(key);

			// Test if an item in slot was added or removed
			if (oldValue == null || newValue == null) return true;

			// Test if an item in lost has changed
			@SuppressWarnings("unchecked")
			var equal = this.compareStacks(newValue, (TStack) oldValue);
			if (!equal) return true;
		}

		return false;
	}

	public int count(String id) {
		var count = 0;

		for (var stack : this.getSlots().values()) {
			if (id.isEmpty() || stack.resource().id().equals(id)) {
				count += stack.count();
			}
		}

		return count;
	}

	public void find(String id, ManagedArray output, Realm realm) {
		var outputElements = output.getElementsMutable();
		for (var stack : this.getSlots().values()) {
			if (id.isEmpty() || stack.resource().id().equals(id)) {
				outputElements.add(StackReport.WRAPPER.getHandle(stack, realm));
			}
		}
	}

	protected Map<Primitive.Number, StackReport> cachedItems;

	protected abstract StackReport makeStackReport(int slot, TStack stack);

	public Map<Primitive.Number, StackReport> getSlots() {
		if (this.cachedItems != null) return this.cachedItems;
		this.cachedItems = new HashMap<>();

		for (var kv : this.slots.entrySet()) {
			this.cachedItems.put(Primitive.from(kv.getKey()), this.makeStackReport(kv.getKey(), kv.getValue()));
		}

		return this.cachedItems;
	}

	public static class Item extends StorageReport<IItemHandler, ItemStack> {
		public Item(BlockCapabilityCache<IItemHandler, Direction> source, IItemHandler handler) {
			super(source, handler);
		}

		@Override
		protected void loadSlots() {
			for (int i = 0; i < this.handler.getSlots(); i++) {
				var stack = this.handler.getStackInSlot(i);
				if (stack.isEmpty()) continue;
				this.slots.put(i, stack.copy());
			}
		}

		@Override
		protected StackReport makeStackReport(int slot, ItemStack stack) {
			return new StackReport.Item(slot, this, stack);
		}

		@Override
		protected Stream<Integer> createCommonItemStreamIfPossible(StorageReport<?, ?> other) {
			if (!(other instanceof Item other_1)) return null;
			return Stream.concat(other_1.slots.keySet().stream(), this.slots.keySet().stream());
		}

		@Override
		protected boolean compareStacks(ItemStack oldValue, ItemStack newValue) {
			return oldValue.getCount() == newValue.getCount() && ItemStack.isSameItemSameComponents(oldValue, newValue);
		}
	}

	public static class Fluid extends StorageReport<IFluidHandler, FluidStack> {
		public Fluid(BlockCapabilityCache<IFluidHandler, Direction> source, IFluidHandler handler) {
			super(source, handler);
		}

		@Override
		protected void loadSlots() {
			for (int i = 0; i < this.handler.getTanks(); i++) {
				var stack = this.handler.getFluidInTank(i);
				if (stack.isEmpty()) continue;
				this.slots.put(i, stack.copy());
			}
		}

		@Override
		protected StackReport makeStackReport(int slot, FluidStack stack) {
			return new StackReport.Fluid(slot, this, stack);
		}

		@Override
		protected Stream<Integer> createCommonItemStreamIfPossible(StorageReport<?, ?> other) {
			if (!(other instanceof Fluid other_1)) return null;
			return Stream.concat(other_1.slots.keySet().stream(), this.slots.keySet().stream());
		}

		@Override
		protected boolean compareStacks(FluidStack oldValue, FluidStack newValue) {
			return oldValue.getAmount() == newValue.getAmount() && FluidStack.isSameFluidSameComponents(oldValue, newValue);
		}
	}

	@SuppressWarnings("unchecked")
	public static final NativeHandleWrapper<StorageReport<?, ?>> WRAPPER = new NativeHandleWrapper<>("Storage.StorageReport", (Class<StorageReport<?, ?>>) (Class<?>) StorageReport.class, ctx -> ctx
			// @summary: Represents a scan of an inventory or fluid tank, containing item or fluid stacks, each represented by {@link Storage.StackReport}.
			.addMethod("count", List.of("id"), List.of(Primitive.String.class), (self, args, scope, result) -> {
				// @summary: Returns the combined amount of all stacks with the specified `id`.
				result.value = Primitive.from(self.count(args.get(0).getStringValue()));
			})
			.addMethod("find", List.of("id"), List.of(Primitive.String.class), (self, args, scope, result) -> {
				// @summary: Returns an {@link Array} of all {@link Storage.StackReport} instances with the specified `id`.
				var array = ManagedArray.empty(scope.realm.ArrayPrototype);
				self.find(args.get(0).getStringValue(), array, scope.realm);
				result.value = array;
			})
			.addMethod("getContents", Collections.emptyList(), Collections.emptyList(), (self, args, scope, result) -> {
				// @summary: Returns an {@link Array} of all {@link Storage.StackReport} instances in the storage.
				var array = ManagedArray.empty(scope.realm.ArrayPrototype);
				self.find("", array, scope.realm);
				result.value = array;
			})
			.applyDecorator((realm, self) -> {
				// Backwards compatibility
				self.declareProperty("countItems", self.getOwnProperty("count"));
				self.declareProperty("findItems", self.getOwnProperty("find"));
				self.declareProperty("getItems", self.getOwnProperty("getContents"));
			})
			.addGetter("isFluid", v -> Primitive.from(v instanceof Fluid)) // @summary: If this is instance represents a fluid tank, otherwise this instance represents an inventory.
			.addMapAccess(StorageReport::getSlots, Primitive.Number.class, StackReport.class,
					v -> v, v -> (Primitive.Number) v,
					value -> StackReport.WRAPPER.getHandle(value, ctx.realm), null));
}
