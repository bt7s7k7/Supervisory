package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RemoteTerminalUnitBlockEntity extends BlockEntity {
	public RemoteTerminalUnitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}

	public String domain = "";
	public String name = "";

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.domain = tag.getString("domain");
		this.name = tag.getString("name");
	}

	@Override
	public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putString("domain", this.domain);
		tag.putString("name", this.name);
	}

	public void tick(Level level, BlockPos pos, BlockState state) {
	}
}
