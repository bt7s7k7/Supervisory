package bt7s7k7.supervisory.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import bt7s7k7.supervisory.Support;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class StorageReport {
	protected final IItemHandler handler;
	protected final Map<Integer, ItemStack> items;

	public StorageReport(IItemHandler handler) {
		this.handler = handler;
		var items = this.items = new HashMap<>();

		for (int i = 0; i < handler.getSlots(); i++) {
			var stack = handler.getStackInSlot(i);
			if (stack.isEmpty()) continue;
			items.put(i, stack.copy());
		}
	}

	public boolean itemsChanged(StorageReport previous) {
		var keys = Stream.concat(previous.items.keySet().stream(), this.items.keySet().stream()).distinct();

		for (var key : Support.getIterable(keys::iterator)) {
			var oldValue = previous.items.get(key);
			var newValue = this.items.get(key);

			// Test if an item in slot was added or removed
			if (oldValue == null || newValue == null) return true;

			// Test if an item in lost has changed
			var equal = ItemStack.isSameItemSameComponents(oldValue, newValue);
			if (!equal) return true;
		}

		return false;
	}

	public int countItems(String id) {
		var count = 0;

		for (var stack : this.getItems().values()) {
			if (stack.item().id().equals(id)) {
				count += stack.count();
			}
		}

		return count;
	}

	protected Map<Primitive.Number, StackReport> cachedItems;

	public Map<Primitive.Number, StackReport> getItems() {
		if (this.cachedItems != null) return this.cachedItems;
		this.cachedItems = new HashMap<>();

		for (var kv : this.items.entrySet()) {
			this.cachedItems.put(Primitive.from(kv.getKey()), new StackReport(this, kv.getValue()));
		}

		return this.cachedItems;
	}

	public static final NativeHandleWrapper<StorageReport> WRAPPER = new NativeHandleWrapper<>("StorageReport", StorageReport.class, ctx -> ctx
			.addMethod("countItems", List.of("id"), List.of(Primitive.String.class), (self, args, scope, result) -> {
				result.value = Primitive.from(self.countItems(args.get(0).getStringValue()));
			})
			.addMapAccess(StorageReport::getItems, Primitive.Number.class, StackReport.class,
					v -> v, v -> (Primitive.Number) v,
					value -> StackReport.WRAPPER.getHandle(value, ctx.globalScope), null));
}
