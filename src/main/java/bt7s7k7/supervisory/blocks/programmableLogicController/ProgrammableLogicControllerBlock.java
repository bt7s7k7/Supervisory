package bt7s7k7.supervisory.blocks.programmableLogicController;

import com.mojang.serialization.MapCodec;

import bt7s7k7.supervisory.blocks.AllBlockEntities;
import bt7s7k7.supervisory.composition.CompositeBlock;
import net.minecraft.world.level.block.Block;

public class ProgrammableLogicControllerBlock extends CompositeBlock {
	public static final MapCodec<ProgrammableLogicControllerBlock> CODEC = simpleCodec(ProgrammableLogicControllerBlock::new);

	public ProgrammableLogicControllerBlock(Properties properties) {
		super(AllBlockEntities.PROGRAMMABLE_LOGIC_CONTROLLER, properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}
}
