package bt7s7k7.supervisory.storage;

import bt7s7k7.supervisory.Support;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public class ItemReport {
	protected final ItemStack stack;

	public ItemReport(ItemStack stack) {
		this.stack = stack;
	}

	public String id() {
		var key = BuiltInRegistries.ITEM.getResourceKey(this.stack.getItem());
		if (key.isPresent()) {
			return Support.getNamespacedId(key.get());
		} else {
			return "unknown";
		}
	}

	public String code() {
		var code = this.stack.getComponents().hashCode();
		var codeString = Integer.toUnsignedString(code, Character.MAX_RADIX);
		return codeString;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;

		if (obj instanceof ItemReport other) {
			return ItemStack.isSameItemSameComponents(this.stack, other.stack);
		}

		return super.equals(obj);
	}

	public static NativeHandleWrapper<ItemReport> WRAPPER = new NativeHandleWrapper<>(ItemReport.class)
			.addName("ItemReport")
			.addGetter("id", v -> Primitive.from(v.id()))
			.addGetter("code", v -> Primitive.from(v.code()))
			.addDumpMethod((self, depth, scope, result) -> self.id());
}
