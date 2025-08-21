package bt7s7k7.supervisory;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class CommonEvents {

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		Supervisory.LOGGER.info("Registering commands");

		var commands = event.getDispatcher();
		commands.register(Commands.literal("supervisory").executes((ctx) -> {
			var source = ctx.getSource();
			source.sendSuccess(
					() -> Component.literal("Supervisory is ready. Test value " + Config.TEST_NUMBER.getAsInt()), true);
			return 0;
		}));
	}
}
