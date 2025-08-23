package bt7s7k7.supervisory;

import java.util.List;

import bt7s7k7.supervisory.support.ManagedValueCodec;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class CommonEvents {

	private static void testSerialization() {
		try {
			var array = new ManagedArray(null, List.of(
					Primitive.from(true),
					Primitive.from(1),
					Primitive.from("two")));

			var serialized = ManagedValueCodec.INSTANCE.encodeStart(NbtOps.INSTANCE, array).getOrThrow();
			Supervisory.LOGGER.info(serialized.toString());

			var deserialized = (ManagedArray) ManagedValueCodec.INSTANCE.parse(NbtOps.INSTANCE, serialized).getOrThrow();

			Supervisory.LOGGER.info("Length correct: " + (deserialized.elements.size() == 3));
			var element0 = deserialized.elements.get(0);
			Supervisory.LOGGER.info("Element 0 correct: " + (element0 instanceof Primitive.Boolean bool && bool.value == true));

			var element1 = deserialized.elements.get(1);
			Supervisory.LOGGER.info("Element 1 correct: " + (element1 instanceof Primitive.Number num && num.value == 1));

			var element2 = deserialized.elements.get(2);
			Supervisory.LOGGER.info("Element 2 correct: " + (element2 instanceof Primitive.String text && text.value == "two"));

		} catch (Exception error) {
			Supervisory.LOGGER.error("Failed serialization test", error);
		}
	}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		var commands = event.getDispatcher();
		var main = Commands.literal("supervisory");

		commands.register(main.executes((ctx) -> {
			var source = ctx.getSource();
			source.sendSuccess(() -> Component.literal("Test succeeded"), true);
			return 0;
		}));

		commands.register(main.then(Commands.literal("values").executes(ctx -> {
			testSerialization();
			return 0;
		})));
	}
}
