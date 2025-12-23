package bt7s7k7.supervisory.items;

import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.items.device_config_buffer.BufferContent;
import bt7s7k7.supervisory.items.device_config_buffer.DeviceConfigBufferItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber
public final class AllItems {
	private AllItems() {}

	public static final DeferredRegister.DataComponents REGISTRAR = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Supervisory.MOD_ID);

	public static final DataComponentType<Boolean> BUFFER_DOMAIN_ONLY = createComponent("buffer_domain_only", v -> v.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
	public static final DataComponentType<BufferContent> BUFFER_CONTENT = createComponent("buffer_content", v -> v.persistent(BufferContent.CODEC).networkSynchronized(BufferContent.STREAM_CODEC));

	public static final ItemEntry<DeviceConfigBufferItem> DEVICE_CONFIG_BUFFER = Supervisory.REGISTRATE.item("device_config_buffer", DeviceConfigBufferItem::new)
			.properties(p -> p.component(BUFFER_DOMAIN_ONLY, true))
			.recipe((ctx, prov) -> {
				ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ctx.getEntry())
						.requires(Items.IRON_INGOT)
						.requires(Items.COPPER_INGOT)
						.requires(Items.BLUE_DYE)
						.requires(Items.PAPER)
						.unlockedBy("has_copper", RegistrateRecipeProvider.has(Items.COPPER_INGOT))
						.save(prov, prov.safeId(ctx.getEntry()));
			})
			.register();

	private static <T> DataComponentType<T> createComponent(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
		DataComponentType<T> type = builder.apply(DataComponentType.builder()).build();
		REGISTRAR.register(name, () -> type);
		return type;
	}

	public static void register(IEventBus modBus) {
		REGISTRAR.register(modBus);
	}

	@SubscribeEvent
	private static void handleCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
			// event.accept(DEVICE_CONFIG_BUFFER);
		}
	}
}
