package bt7s7k7.supervisory.storage;

import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public abstract class StackReport {
	public final StorageReport<?, ?> source;
	public final int slot;

	protected ResourceReport report;

	protected StackReport(int slot, StorageReport<?, ?> source) {
		this.slot = slot;
		this.source = source;
	}

	public abstract ResourceReport resource();

	public abstract int count();

	public static class Item extends StackReport {

		protected Item(int slot, StorageReport<?, ?> source, ItemStack stack) {
			super(slot, source);
			this.stack = stack;
		}

		protected final ItemStack stack;

		@Override
		public ResourceReport resource() {
			if (this.report == null) {
				this.report = new ResourceReport.Item(this.stack.copyWithCount(1));
			}

			return this.report;
		}

		@Override
		public int count() {
			return this.stack.getCount();
		}
	}

	public static class Fluid extends StackReport {

		protected Fluid(int slot, StorageReport<?, ?> source, FluidStack stack) {
			super(slot, source);
			this.stack = stack;
		}

		protected final FluidStack stack;

		public FluidStack getFluidWithAmount(int amount) {
			return this.stack.copyWithAmount(amount);
		}

		@Override
		public ResourceReport resource() {
			if (this.report == null) {
				this.report = new ResourceReport.Fluid(this.stack.copyWithAmount(1));
			}

			return this.report;
		}

		@Override
		public int count() {
			return this.stack.getAmount();
		}
	}

	public static NativeHandleWrapper<StackReport> WRAPPER = new NativeHandleWrapper<>("Storage.StackReport", StackReport.class, ctx -> ctx
			// @summary: Represents a single stack inside an inventory or fluid tank, see {@link Storage.StorageReport}.
			.addGetter("count", v -> Primitive.from(v.count())) // @type: Number, @summary: The number of items or amount of fluid in this stack
			.addGetter("slot", v -> Primitive.from(v.slot)) // @type: Number, @summary: The slot number of this stack
			.addGetter("id", v -> Primitive.from(v.resource().id())) // @type: String, @summary: The ID of the contained resource
			.addGetter("code", v -> Primitive.from(v.resource().code())) // @type: String, @summary: A unique code representing the NBT data of the resource
			.addGetter("type", v -> ResourceReport.WRAPPER.getHandle(v.resource(), ctx.realm)) // @type: Storage.ResourceReport, @summary: The resource type inside this stack
			.addGetter("isFluid", v -> Primitive.from(v instanceof Fluid)) // @summary: If this is instance represents a fluid stack, otherwise this instance represents an item stack.
			.addDumpMethod((self, depth, scope, result) -> self.count() + "*" + self.resource().id()));
}
