package bt7s7k7.supervisory.blocks;

import java.util.List;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;

import bt7s7k7.supervisory.I18n;
import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.programmableLogicController.ProgrammableLogicControllerBlock;
import bt7s7k7.supervisory.blocks.remoteTerminalUnit.RemoteTerminalUnitBlock;
import bt7s7k7.supervisory.items.ItemWithHint;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber
public class AllBlocks {
	private static final TagKey<Block> WRENCH_PICKUP = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create", "wrench_pickup"));

	private static abstract class BlockItemWithHint extends BlockItem implements ItemWithHint {
		public BlockItemWithHint(Block block, Properties properties) {
			super(block, properties);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
			this.appendHint(stack, context, tooltipComponents, tooltipFlag);
			super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
		}
	}

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
			.item((b, p) -> new BlockItemWithHint(b, p) {
				@Override
				public MutableComponent getHint() {
					return I18n.REMOTE_TERMINAL_UNIT_DESC.toComponent();
				};
			})
			.recipe((ctx, prov) -> {
				ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.getEntry())
						.define('I', Items.IRON_INGOT)
						.define('R', Items.REDSTONE)
						.define('C', Items.COPPER_INGOT)
						.define('S', Items.STONE)
						.pattern("ICI")
						.pattern("RCR")
						.pattern("SSS")
						.unlockedBy("has_copper", RegistrateRecipeProvider.has(Items.COPPER_INGOT))
						.save(prov);
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
			.item((b, p) -> new BlockItemWithHint(b, p) {
				@Override
				public MutableComponent getHint() {
					return I18n.PROGRAMMABLE_LOGIC_CONTROLLER_DESC.toComponent();
				};
			})
			.recipe((ctx, prov) -> {
				ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ctx.getEntry())
						.define('I', Items.IRON_INGOT)
						.define('R', Items.REDSTONE)
						.define('C', Items.COPPER_BLOCK)
						.define('S', Items.STONE)
						.pattern("III")
						.pattern("RCR")
						.pattern("SSS")
						.unlockedBy("has_copper", RegistrateRecipeProvider.has(Items.COPPER_INGOT))
						.save(prov);
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
