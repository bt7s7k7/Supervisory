package bt7s7k7.supervisory;

import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import com.tterrag.registrate.Registrate;

import bt7s7k7.supervisory.blocks.AllBlockEntities;
import bt7s7k7.supervisory.blocks.AllBlocks;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.support.DomainMonitor;
import bt7s7k7.supervisory.support.LogEventRouter;
import bt7s7k7.supervisory.support.LogEventRouter.Subscription;
import bt7s7k7.supervisory.system.ScriptedSystemHost;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.GlobalPos;
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

	private static Supervisory instance;
	public final IEventBus bus;

	public static Supervisory getInstance() {
		return instance;
	}

	public Supervisory(IEventBus modEventBus, ModContainer modContainer) {
		this.bus = modEventBus;
		instance = this;

		modContainer.registerConfig(Type.SERVER, Config.SERVER_SPEC);

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

		var monitorBlock = Commands.literal("block").then(Commands.argument("block", BlockPosArgument.blockPos()).then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> {
			var source = ctx.getSource();
			var blockPos = BlockPosArgument.getLoadedBlockPos(ctx, "block");
			var level = source.getLevel();
			var position = new GlobalPos(level.dimension(), blockPos);
			var enable = BoolArgumentType.getBool(ctx, "enable");
			var be = level.getBlockEntity(blockPos);
			var player = source.getPlayer();
			var router = LogEventRouter.CHAT.get();

			if (enable) {
				if (be == null
						|| !(be instanceof CompositeBlockEntity entity)
						|| !(entity.getComponent(ScriptedSystemHost.class).orElse(null) instanceof ScriptedSystemHost)) {
					source.sendFailure(Component.literal("Selected block is not a valid target for monitoring"));
					return 0;
				}

				var success = router.subscribe(new Subscription(player.getUUID(), position));
				if (!success) {
					source.sendFailure(Component.literal("There is already a subscription for this block"));
					return 0;
				}

				source.sendSuccess(() -> Component.literal("Subscribed"), true);
				return 1;
			} else {
				var success = router.unsubscribe(player.getUUID(), position);

				if (!success) {
					source.sendFailure(Component.literal("There was no subscription for this block"));
					return 0;
				}

				source.sendSuccess(() -> Component.literal("Unsubscribed"), true);
				return 1;
			}
		})));

		var monitorDomain = Commands.literal("domain")
				.then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.string()).then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> {
					var source = ctx.getSource();
					var player = source.getPlayer();
					var enable = BoolArgumentType.getBool(ctx, "enable");
					var name = StringArgumentType.getString(ctx, "name");
					var monitor = DomainMonitor.HANDLE.get();

					if (enable) {
						var success = monitor.subscribe(player.getUUID(), name);
						if (!success) {
							source.sendFailure(Component.literal("There is already a subscription for this domain"));
							return 0;
						}

						source.sendSuccess(() -> Component.literal("Subscribed"), true);
						return 1;
					} else {
						var success = monitor.unsubscribe(player.getUUID(), name);

						if (!success) {
							source.sendFailure(Component.literal("There was no subscription for this domain"));
							return 0;
						}

						source.sendSuccess(() -> Component.literal("Unsubscribed"), true);
						return 1;
					}
				}))))
				.then(Commands.literal("controller").then(Commands.argument("block", BlockPosArgument.blockPos()).then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> {
					var source = ctx.getSource();
					var blockPos = BlockPosArgument.getLoadedBlockPos(ctx, "block");
					var level = source.getLevel();
					var enable = BoolArgumentType.getBool(ctx, "enable");
					var be = level.getBlockEntity(blockPos);
					var player = source.getPlayer();
					var monitor = DomainMonitor.HANDLE.get();

					if (be == null
							|| !(be instanceof CompositeBlockEntity entity)
							|| !(entity.getComponent(ScriptedSystemHost.class).orElse(null) instanceof ScriptedSystemHost)) {
						source.sendFailure(Component.literal("Selected block is not a valid target for monitoring"));
						return 0;
					}

					var name = level.dimension().location().toString() + " " + blockPos.toShortString();
					if (enable) {
						var success = monitor.subscribe(player.getUUID(), name);
						if (!success) {
							source.sendFailure(Component.literal("There is already a subscription for this domain"));
							return 0;
						}

						source.sendSuccess(() -> Component.literal("Subscribed"), true);
						return 1;
					} else {
						var success = monitor.unsubscribe(player.getUUID(), name);

						if (!success) {
							source.sendFailure(Component.literal("There was no subscription for this domain"));
							return 0;
						}

						source.sendSuccess(() -> Component.literal("Unsubscribed"), true);
						return 1;
					}
				}))));

		var monitorClear = Commands.literal("clear").executes(ctx -> {
			var source = ctx.getSource();
			LogEventRouter.CHAT.get().clear(source.getPlayer().getUUID());
			return 1;
		});

		commands.register(main.then(Commands.literal("monitor").requires(CommandSourceStack::isPlayer)
				.then(monitorBlock)
				.then(monitorDomain)
				.then(monitorClear)));
	}
}
