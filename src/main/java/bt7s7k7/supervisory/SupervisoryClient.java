package bt7s7k7.supervisory;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Supervisory.MOD_ID, dist = Dist.CLIENT)
// @EventBusSubscriber(modid = Supervisory.MOD_ID, value = Dist.CLIENT)
public class SupervisoryClient {
	public SupervisoryClient(ModContainer container) {
		Supervisory.LOGGER.info("We are here");
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
	}
}
