package bt7s7k7.supervisory;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

@Mod(Supervisory.MOD_ID)
public class Supervisory {
	public static final String MOD_ID = "supervisory";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

	public static final BlockEntry<Block> MY_STAIRS = REGISTRATE.block("example_block", Block::new)
			.simpleItem()
			.blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), prov.cubeAll(Blocks.STONE_BRICKS)))
			.tag(BlockTags.STONE_BRICKS)
			.lang("Example block")
			.register();

	public Supervisory(IEventBus modEventBus, ModContainer modContainer) {
		LOGGER.info("Running supervisory! Word of the day: reticulation");

		modContainer.registerConfig(Type.COMMON, Config.SPEC);
	}
}
