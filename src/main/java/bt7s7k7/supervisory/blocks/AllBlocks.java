package bt7s7k7.supervisory.blocks;

import com.tterrag.registrate.util.entry.BlockEntry;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.remoteTerminalUnit.RemoteTerminalUnitBlock;
import net.minecraft.world.level.block.Blocks;

public class AllBlocks {
	public static final BlockEntry<RemoteTerminalUnitBlock> REMOTE_TERMINAL_UNIT = Supervisory.REGISTRATE.block("remote_terminal_unit", RemoteTerminalUnitBlock::new)
			.initialProperties(() -> Blocks.STONE)
			.simpleItem()
			.blockstate((ctx, prov) -> prov.horizontalBlock(ctx.getEntry(),
					prov.models().orientableWithBottom(
							ctx.getName(),
							prov.modLoc("block/" + ctx.getName() + "_side"),
							prov.modLoc("block/" + ctx.getName() + "_front"),
							prov.mcLoc("minecraft:block/furnace_top"),
							prov.mcLoc("minecraft:block/beehive_end"))))
			.register();

	public static void register() {
		// Load this class
	}
}
