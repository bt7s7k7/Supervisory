package bt7s7k7.supervisory.storage;

import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.world.item.ItemStack;

public class StackReport {
	protected final StorageReport source;
	protected final ItemStack stack;
	protected ItemReport item;

	public StackReport(StorageReport source, ItemStack stack) {
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
			.addGetter("count", v -> Primitive.from(v.count()))
			.addGetter("id", v -> Primitive.from(v.item().id()))
			.addGetter("code", v -> Primitive.from(v.item().code()))
			.addGetter("item", v -> ItemReport.WRAPPER.getHandle(v.item(), ctx.globalScope))
			.addDumpMethod((self, depth, scope, result) -> self.count() + "*" + self.item().id()));
}
