package bt7s7k7.supervisory.storage;

import java.util.Collections;
import java.util.List;

import bt7s7k7.supervisory.support.ManagedValueCodec;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public abstract class ResourceReport {
	public abstract String id();

	protected abstract DataComponentPatch getPatch();

	@Override
	public abstract boolean equals(Object obj);

	public String code() {
		var data = this.getPatch();
		var nbt = DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
		return Integer.toUnsignedString(nbt.toString().hashCode(), Character.MAX_RADIX);
	}

	public void getComponents(ManagedArray result) {
		var data = this.getPatch();
		var resultElements = result.getElementsMutable();

		for (var kv : data.entrySet()) {
			var type = kv.getKey();
			var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKeyOrNull(type);
			if (key == null) continue;
			resultElements.add(Primitive.from(key.toString()));
		}
	}

	public ManagedValue getComponent(String name, Realm realm) {
		var data = this.getPatch();

		@SuppressWarnings("unchecked")
		var type = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(name));
		if (type == null) return Primitive.VOID;

		var component = data.get(type);
		if (component == null || component.isEmpty()) return Primitive.VOID;

		var nbt = type.codec().encodeStart(NbtOps.INSTANCE, component.get());
		return ManagedValueCodec.importNbtData(nbt.getOrThrow(), realm, Primitive.VOID);
	}

	public static class Item extends ResourceReport {
		protected final ItemStack stack;

		protected Item(ItemStack stack) {
			this.stack = stack;
		}

		@Override
		public String id() {
			var key = BuiltInRegistries.ITEM.getKeyOrNull(this.stack.getItem());
			return key != null ? key.toString() : "unknown";
		}

		@Override
		protected DataComponentPatch getPatch() {
			return this.stack.getComponentsPatch();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;

			return obj instanceof Item other && ItemStack.isSameItemSameComponents(this.stack, other.stack);
		}
	}

	public static class Fluid extends ResourceReport {
		protected final FluidStack stack;

		protected Fluid(FluidStack stack) {
			this.stack = stack;
		}

		@Override
		public String id() {
			var key = BuiltInRegistries.FLUID.getKeyOrNull(this.stack.getFluid());
			return key != null ? key.toString() : "unknown";
		}

		@Override
		protected DataComponentPatch getPatch() {
			return this.stack.getComponentsPatch();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;

			return obj instanceof Fluid other && FluidStack.isSameFluidSameComponents(this.stack, other.stack);
		}
	}

	public static NativeHandleWrapper<ResourceReport> WRAPPER = new NativeHandleWrapper<>("Storage.ResourceReport", ResourceReport.class, ctx -> ctx
			// @summary: Represents an item or fluid type inside a {@link Storage.StackReport}.
			.addGetter("id", v -> Primitive.from(v.id())) // @type: String, @summary: The ID of the resource.
			.addGetter("code", v -> Primitive.from(v.code())) // @type: String, @summary: A unique code representing the NBT data of the resource.
			.addMethod("getComponents", Collections.emptyList(), Collections.emptyList(), (self, args, scope, result) -> {
				// @summary: Returns an {@link Array} of the names of all component types on this resource.
				var array = ManagedArray.empty(scope.realm.ArrayPrototype);
				self.getComponents(array);
				result.value = array;
			})
			.addMethod("getComponent", List.of("name"), List.of(Primitive.String.class), (self, args, scope, result) -> {
				// @summary: Returns the data of the specified component name. If this resource does not have the component, returns {@link void}.
				var name = args.get(0).getStringValue();
				result.value = self.getComponent(name, scope.realm);
			})
			.addGetter("isFluid", v -> Primitive.from(v instanceof Fluid)) // @summary: If this is instance represents a fluid, otherwise this instance represents an item.
			.addDumpMethod((self, depth, scope, result) -> self.id()));
}
