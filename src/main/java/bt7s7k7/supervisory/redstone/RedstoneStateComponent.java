package bt7s7k7.supervisory.redstone;

import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.ComponentSignal;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneStateComponent extends BlockEntityComponent {

	public RedstoneStateComponent(CompositeBlockEntity entity) {
		super(entity);
	}

	private int[] inputs = new int[6];
	private int[] outputs = new int[6];

	public static record RedstoneInputChangedEvent(Direction direction, int strength) {}

	public final ComponentSignal.Emitter<RedstoneInputChangedEvent> onRedstoneInputChanged = new ComponentSignal.Emitter<>();

	public int getInput(Direction direction) {
		return this.inputs[direction.get3DDataValue()];
	}

	public void setInput(Direction direction, int strength) {
		if (strength == this.getInput(direction)) return;
		this.inputs[direction.get3DDataValue()] = strength;
		this.onRedstoneInputChanged.emit(new RedstoneInputChangedEvent(direction, strength));
	}

	public int getOutput(Direction direction) {
		return this.outputs[direction.get3DDataValue()];
	}

	public void setOutput(Direction direction, int strength) {
		strength = Math.clamp(strength, 0, 15);

		if (strength == this.getOutput(direction)) return;
		this.outputs[direction.get3DDataValue()] = strength;

		RedstoneUtil.sendOutputSignal(this.entity.getBlockPos(), this.entity.getBlockState(), this.entity.getLevel(), direction, strength);
	}

	@Override
	public void read(CompoundTag tag, Provider registries) {
		super.read(tag, registries);

		var inputs = tag.getIntArray("redstone_input");
		if (inputs.length == 6) this.inputs = inputs;

		var outputs = tag.getIntArray("redstone_output");
		if (outputs.length == 6) this.outputs = outputs;
	}

	@Override
	public void write(CompoundTag tag, Provider registries) {
		super.write(tag, registries);

		tag.putIntArray("redstone_input", this.inputs);
		tag.putIntArray("redstone_output", this.outputs);
	}

	@Override
	public void handleNeighbourChange(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		var delta = fromPos.subtract(pos);
		var towardsNeighbour = Direction.fromDelta(delta.getX(), delta.getY(), delta.getZ());
		var signal = RedstoneUtil.getInputSignal(pos, level, towardsNeighbour);

		this.setInput(towardsNeighbour, signal);
	}

	@Override
	public void teardown() {
		super.teardown();
		this.onRedstoneInputChanged.teardown();
	}

}
