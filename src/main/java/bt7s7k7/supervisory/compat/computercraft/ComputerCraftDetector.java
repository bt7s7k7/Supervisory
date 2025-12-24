package bt7s7k7.supervisory.compat.computercraft;

import bt7s7k7.supervisory.Support;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;

@EventBusSubscriber
public final class ComputerCraftDetector {
	private ComputerCraftDetector() {}

	@SubscribeEvent
	public static void detectComputerCraft(FMLConstructModEvent event) {
		Support.executeIfModInstalled("computercraft", () -> PeripheralConnectionController::initializeCompat);
	}
}
