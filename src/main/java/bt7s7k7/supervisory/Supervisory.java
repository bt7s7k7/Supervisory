package bt7s7k7.supervisory;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.Registrate;

import bt7s7k7.supervisory.blocks.AllBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

@Mod(Supervisory.MOD_ID)
public class Supervisory {
	public static final String MOD_ID = "supervisory";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

	public Supervisory(IEventBus modEventBus, ModContainer modContainer) {
		LOGGER.info("Running supervisory! Word of the day: reticulation");

		modContainer.registerConfig(Type.COMMON, Config.SPEC);

		AllBlocks.register();
	}
}
