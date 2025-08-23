package bt7s7k7.supervisory.blocks.smartRedstoneComponent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class SmartRedstoneComponentBlockEntity extends BlockEntity {
	public SmartRedstoneComponentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}

	private int[] inputs = new int[6];
	private int[] outputs = new int[6];

	public int getInput(Direction direction) {
		return this.inputs[direction.get3DDataValue()];
	}

	public void setInput(Direction direction, int strength) {
		if (strength == this.getInput(direction)) return;
		this.inputs[direction.get3DDataValue()] = strength;
		this.handleRedstoneInputChange(direction, strength);
	}

	protected abstract void handleRedstoneInputChange(Direction direction, int strength);

	public int getOutput(Direction direction) {
		return this.outputs[direction.get3DDataValue()];
	}

	public void setOutput(Direction direction, int strength) {
		strength = Math.clamp(strength, 0, 15);

		if (strength == this.getOutput(direction)) return;
		this.outputs[direction.get3DDataValue()] = strength;

		RedstoneUtil.sendOutputSignal(this.worldPosition, this.getBlockState(), this.level, direction, strength);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, Provider registries) {
		super.loadAdditional(tag, registries);

		var inputs = tag.getIntArray("redstone_input");
		if (inputs.length == 6) this.inputs = inputs;

		var outputs = tag.getIntArray("redstone_output");
		if (outputs.length == 6) this.outputs = outputs;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, Provider registries) {
		super.saveAdditional(tag, registries);

		tag.putIntArray("redstone_input", this.inputs);
		tag.putIntArray("redstone_output", this.outputs);
	}

	public void handleNeighbourChange(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		var delta = fromPos.subtract(pos);
		var towardsNeighbour = Direction.fromDelta(delta.getX(), delta.getY(), delta.getZ());
		var signal = RedstoneUtil.getInputSignal(pos, level, towardsNeighbour);

		this.setInput(towardsNeighbour, signal);
	}
}
