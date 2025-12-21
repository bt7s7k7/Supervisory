package bt7s7k7.supervisory.storage;

import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.world.item.ItemStack;

public class StackReport {
	public final StorageReport source;
	public final int slot;

	protected final ItemStack stack;
	protected ItemReport item;

	public StackReport(int slot, StorageReport source, ItemStack stack) {
		this.slot = slot;
		this.source = source;
		this.stack = stack;
	}

	public ItemReport item() {
		if (this.item == null) {
			this.item = new ItemReport(this.stack.copyWithCount(1));
		}

		return this.item;
	}

	public int count() {
		return this.stack.getCount();
	}

	public static NativeHandleWrapper<StackReport> WRAPPER = new NativeHandleWrapper<>("StackReport", StackReport.class, ctx -> ctx
			// @summary: Represents a single stack inside an inventory {@link StorageReport}.
			.addGetter("count", v -> Primitive.from(v.count())) // @type: Number, @summary: The number of items in this stack
			.addGetter("slot", v -> Primitive.from(v.slot)) // @type: Number, @summary: The slot number of this stack
			.addGetter("id", v -> Primitive.from(v.item().id())) // @type: String, @summary: The ID of the contained item
			.addGetter("code", v -> Primitive.from(v.item().code())) // @type: String, @summary: A unique code representing the NBT data of the item
			.addGetter("item", v -> ItemReport.WRAPPER.getHandle(v.item(), ctx.realm)) // @type: ItemReport, @summary: The item type inside this stack
			.addDumpMethod((self, depth, scope, result) -> self.count() + "*" + self.item().id()));
}
