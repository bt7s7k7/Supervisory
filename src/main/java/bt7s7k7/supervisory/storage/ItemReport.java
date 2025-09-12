package bt7s7k7.supervisory.storage;

import java.util.Collections;
import java.util.List;

import bt7s7k7.supervisory.support.ManagedValueCodec;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ItemReport {
	protected final ItemStack stack;

	public ItemReport(ItemStack stack) {
		this.stack = stack;
	}

	public String id() {
		var key = BuiltInRegistries.ITEM.getKeyOrNull(this.stack.getItem());
		if (key != null) {
			return key.toString();
		} else {
			return "unknown";
		}
	}

	public String code() {
		var data = this.stack.getComponentsPatch();
		var nbt = DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
		return Integer.toUnsignedString(nbt.toString().hashCode(), Character.MAX_RADIX);
	}

	public void getComponents(ManagedArray result) {
		var data = this.stack.getComponentsPatch();

		for (var kv : data.entrySet()) {
			var type = kv.getKey();
			var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKeyOrNull(type);
			if (key == null) continue;
			var stringKey = key.toString();
			result.elements.add(Primitive.from(stringKey));
		}
	}

	public ManagedValue getComponent(String name, GlobalScope globalScope) {
		var data = this.stack.getComponentsPatch();

		@SuppressWarnings("unchecked")
		var type = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(name));
		if (type == null) return Primitive.VOID;

		var component = data.get(type);
		if (component == null || component.isEmpty()) return Primitive.VOID;

		var nbt = type.codec().encodeStart(NbtOps.INSTANCE, component.get());
		return ManagedValueCodec.importNbtData(nbt.getOrThrow(), globalScope, Primitive.VOID);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;

		if (obj instanceof ItemReport other) {
			return ItemStack.isSameItemSameComponents(this.stack, other.stack);
		}

		return super.equals(obj);
	}

	public static NativeHandleWrapper<ItemReport> WRAPPER = new NativeHandleWrapper<>("ItemReport", ItemReport.class, ctx -> ctx
			// @summary: Represents an item inside a {@link StackReport}.
			.addGetter("id", v -> Primitive.from(v.id())) // @type: String, @summary: The ID of the item.
			.addGetter("code", v -> Primitive.from(v.code())) // @type: String, @summary: A unique code representing the NBT data of the item.
			.addMethod("getComponents", Collections.emptyList(), Collections.emptyList(), (self, args, scope, result) -> {
				// @summary: Returns an {@link Array} of the names of all component types on this item.
				var array = new ManagedArray(scope.globalScope.ArrayPrototype);
				self.getComponents(array);
				result.value = array;
			})
			.addMethod("getComponent", List.of("name"), List.of(Primitive.String.class), (self, args, scope, result) -> {
				// @summary: Returns the data of the specified component name. If this item does not have the component, returns {@link void}.
				var name = args.get(0).getStringValue();
				result.value = self.getComponent(name, scope.globalScope);
			})
			.addDumpMethod((self, depth, scope, result) -> self.id()));
}
