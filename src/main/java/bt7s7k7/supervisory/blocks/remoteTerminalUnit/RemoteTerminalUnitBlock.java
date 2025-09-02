package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import com.mojang.serialization.MapCodec;

import bt7s7k7.supervisory.blocks.AllBlockEntities;
import bt7s7k7.supervisory.composition.CompositeBlock;
import net.minecraft.world.level.block.Block;

public class RemoteTerminalUnitBlock extends CompositeBlock {
	public static final MapCodec<RemoteTerminalUnitBlock> CODEC = simpleCodec(RemoteTerminalUnitBlock::new);

	public RemoteTerminalUnitBlock(Properties properties) {
		super(AllBlockEntities.REMOTE_TERMINAL_UNIT, properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}
}
