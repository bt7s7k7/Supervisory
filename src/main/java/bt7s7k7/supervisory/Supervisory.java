package bt7s7k7.supervisory;

import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.Registrate;

import bt7s7k7.supervisory.blocks.AllBlockEntities;
import bt7s7k7.supervisory.blocks.AllBlocks;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Supervisory.MOD_ID)
@EventBusSubscriber
public class Supervisory {
	public static final String MOD_ID = "supervisory";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

	public Supervisory(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(Type.COMMON, Config.SPEC);

		AllBlocks.register();
		AllBlockEntities.register();
		I18n.register();
	}

	public static ResourceLocation resource(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		var commands = event.getDispatcher();

		var main = Commands.literal("supervisory");

		commands.register(main.executes(ctx -> {
			var source = ctx.getSource();
			source.sendSuccess(() -> Component.literal("Supervisory running"), true);
			return 0;
		}));

		commands.register(main.then(Commands.literal("components").then(Commands.argument("block", BlockPosArgument.blockPos()).executes(ctx -> {
			var position = BlockPosArgument.getLoadedBlockPos(ctx, "block");
			var source = ctx.getSource();
			var level = source.getLevel();
			var be = level.getBlockEntity(position);

			if (be != null && be instanceof CompositeBlockEntity entity) {
				source.sendSuccess(() -> Component.literal("Components: " + entity.getComponents().stream().map(v -> v.getClass().getSimpleName()).collect(Collectors.joining(", "))), true);
				return 1;
			}

			source.sendFailure(Component.literal("Selected block can not have components"));
			return 0;
		}))));
	}
}
