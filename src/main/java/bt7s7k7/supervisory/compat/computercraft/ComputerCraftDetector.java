package bt7s7k7.supervisory.compat.computercraft;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.Support;
import bt7s7k7.supervisory.blocks.AllBlockEntities;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.system.ScriptedSystemInitializationEvent;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber
public final class ComputerCraftDetector {
	private ComputerCraftDetector() {}

	@SubscribeEvent
	public static void detectComputerCraft(FMLConstructModEvent event) {
		Support.executeIfModInstalled("computercraft", () -> ComputerCraftDetector::initializeCompat);
	}

	private static void initializeCompat() {
		Supervisory.LOGGER.info("Initializing CC: Tweaked compatibility");
		var bus = Supervisory.getInstance().bus;

		bus.addListener((RegisterCapabilitiesEvent event) -> {
			event.registerBlockEntity(PeripheralCapability.get(), AllBlockEntities.PROGRAMMABLE_LOGIC_CONTROLLER.get(), (blockEntity, context) -> {
				if (!(blockEntity instanceof CompositeBlockEntity composite)) return null;

				var host = composite.getComponent(PeripheralConnectionController.class);
				if (!host.isPresent()) return null;

				return new ScriptedSystemPeripheral(host.get());
			});
		});

		NeoForge.EVENT_BUS.addListener((ScriptedSystemInitializationEvent init) -> {
			init.entity.addComponent(new PeripheralConnectionController(init.entity, init.systemHost));
		});
	}
}
