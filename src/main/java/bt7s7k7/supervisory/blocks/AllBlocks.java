package bt7s7k7.supervisory.blocks;

import com.tterrag.registrate.util.entry.BlockEntry;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.programmableLogicController.ProgrammableLogicControllerBlock;
import bt7s7k7.supervisory.blocks.remoteTerminalUnit.RemoteTerminalUnitBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class AllBlocks {
	private static final TagKey<Block> WRENCH_PICKUP = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create", "wrench_pickup"));

	public static final BlockEntry<RemoteTerminalUnitBlock> REMOTE_TERMINAL_UNIT = Supervisory.REGISTRATE.block("remote_terminal_unit", RemoteTerminalUnitBlock::new)
			.initialProperties(() -> Blocks.DROPPER)
			.simpleItem()
			.blockstate((ctx, prov) -> prov.horizontalBlock(ctx.getEntry(),
					prov.models().orientableWithBottom(
							ctx.getName(),
							prov.modLoc("block/" + ctx.getName() + "_side"),
							prov.modLoc("block/" + ctx.getName() + "_front"),
							prov.mcLoc("minecraft:block/furnace_top"),
							prov.mcLoc("minecraft:block/smithing_table_top"))))
			.tag(WRENCH_PICKUP)
			.register();

	public static final BlockEntry<ProgrammableLogicControllerBlock> PROGRAMMABLE_LOGIC_CONTROLLER = Supervisory.REGISTRATE.block("programmable_logic_controller", ProgrammableLogicControllerBlock::new)
			.initialProperties(() -> Blocks.DROPPER)
			.simpleItem()
			.blockstate((ctx, prov) -> prov.horizontalBlock(ctx.getEntry(),
					prov.models().orientableWithBottom(
							ctx.getName(),
							prov.modLoc("block/" + ctx.getName() + "_side"),
							prov.modLoc("block/" + ctx.getName() + "_front"),
							prov.mcLoc("minecraft:block/furnace_top"),
							prov.mcLoc("minecraft:block/beehive_end"))))
			.tag(WRENCH_PICKUP)
			.register();

	public static void register() {
		// Load this class
	}
}
