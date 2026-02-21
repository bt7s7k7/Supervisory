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
import net.neoforged.neoforge.items.IItemHandler;

public class StorageReport {
	protected IItemHandler handler;

	public final BlockCapabilityCache<IItemHandler, Direction> source;

	protected final Map<Integer, ItemStack> items;

	public StorageReport(BlockCapabilityCache<IItemHandler, Direction> source, IItemHandler handler) {
		this.source = source;
		this.handler = handler;
		var items = this.items = new HashMap<>();

		for (int i = 0; i < handler.getSlots(); i++) {
			var stack = handler.getStackInSlot(i);
			if (stack.isEmpty()) continue;
			items.put(i, stack.copy());
		}
	}

	public void ensureHandlerStillValid() {
		this.handler = this.source.getCapability();
	}

	public IItemHandler getHandler() {
		return this.handler;
	}

	public boolean itemsChanged(StorageReport previous) {
		var keys = Stream.concat(previous.items.keySet().stream(), this.items.keySet().stream()).distinct();

		for (var key : Support.getIterable(keys::iterator)) {
			var oldValue = previous.items.get(key);
			var newValue = this.items.get(key);

			// Test if an item in slot was added or removed
			if (oldValue == null || newValue == null) return true;

			// Test if an item in lost has changed
			var equal = oldValue.getCount() == newValue.getCount() && ItemStack.isSameItemSameComponents(oldValue, newValue);
			if (!equal) return true;
		}

		return false;
	}

	public int countItems(String id) {
		var count = 0;

		for (var stack : this.getItems().values()) {
			if (id.isEmpty() || stack.item().id().equals(id)) {
				count += stack.count();
			}
		}

		return count;
	}

	public void findItems(String id, ManagedArray output, Realm realm) {
		var outputElements = output.getElementsMutable();
		for (var stack : this.getItems().values()) {
			if (id.isEmpty() || stack.item().id().equals(id)) {
				outputElements.add(StackReport.WRAPPER.getHandle(stack, realm));
			}
		}
	}

	protected Map<Primitive.Number, StackReport> cachedItems;

	public Map<Primitive.Number, StackReport> getItems() {
		if (this.cachedItems != null) return this.cachedItems;
		this.cachedItems = new HashMap<>();

		for (var kv : this.items.entrySet()) {
			this.cachedItems.put(Primitive.from(kv.getKey()), new StackReport(kv.getKey(), this, kv.getValue()));
		}

		return this.cachedItems;
	}

	public static final NativeHandleWrapper<StorageReport> WRAPPER = new NativeHandleWrapper<>("Storage.StorageReport", StorageReport.class, ctx -> ctx
			// @summary: Represents a scan of an inventory, containing stacks of items, each represented by {@link Storage.StackReport}.
			.addMethod("countItems", List.of("id"), List.of(Primitive.String.class), (self, args, scope, result) -> {
				// @summary: Returns the count of all numbers with the specified id.
				result.value = Primitive.from(self.countItems(args.get(0).getStringValue()));
			})
			.addMethod("findItems", List.of("id"), List.of(Primitive.String.class), (self, args, scope, result) -> {
				// @summary: Returns an {@link Array} of all {@link Storage.StackReport} instances with the specified `id`.
				var array = ManagedArray.empty(scope.realm.ArrayPrototype);
				self.findItems(args.get(0).getStringValue(), array, scope.realm);
				result.value = array;
			})
			.addMethod("getItems", Collections.emptyList(), Collections.emptyList(), (self, args, scope, result) -> {
				// @summary: Returns an {@link Array} of all {@link Storage.StackReport} instances in the storage.
				var array = ManagedArray.empty(scope.realm.ArrayPrototype);
				self.findItems("", array, scope.realm);
				result.value = array;
			})
			.addMapAccess(StorageReport::getItems, Primitive.Number.class, StackReport.class,
					v -> v, v -> (Primitive.Number) v,
					value -> StackReport.WRAPPER.getHandle(value, ctx.realm), null));
}
