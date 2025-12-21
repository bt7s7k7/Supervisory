package bt7s7k7.supervisory.blocks;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.programmableLogicController.ProgrammableLogicControllerBlock;
import bt7s7k7.supervisory.blocks.remoteTerminalUnit.RemoteTerminalUnitBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber
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
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item()
			.recipe((ctx, prov) -> {
				prov.singleItem(DataIngredient.items(AllBlocks.PROGRAMMABLE_LOGIC_CONTROLLER.asItem()), RecipeCategory.REDSTONE, ctx::getEntry, 1, 1);

				ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.getEntry())
						.define('I', Items.IRON_INGOT)
						.define('R', Items.REDSTONE)
						.define('C', Items.COPPER_BLOCK)
						.define('S', Items.STONE)
						.pattern("III")
						.pattern("RCR")
						.pattern("SSS")
						.unlockedBy("has_copper", RegistrateRecipeProvider.has(Items.COPPER_INGOT))
						.save(prov, prov.safeId(ctx.getEntry()).withSuffix("_full"));
			})
			.build()
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
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.item()
			.recipe((ctx, prov) -> {
				prov.singleItem(DataIngredient.items(REMOTE_TERMINAL_UNIT.asItem()), RecipeCategory.REDSTONE, ctx::getEntry, 1, 1);
			})
			.build()
			.register();

	public static void register() {
		// Load this class
	}

	@SubscribeEvent
	private static void handleCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
			event.accept(PROGRAMMABLE_LOGIC_CONTROLLER);
			event.accept(REMOTE_TERMINAL_UNIT);
		}
	}
}
