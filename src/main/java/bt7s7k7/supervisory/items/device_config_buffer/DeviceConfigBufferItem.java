package bt7s7k7.supervisory.items.device_config_buffer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import bt7s7k7.supervisory.I18n;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.items.AllItems;
import bt7s7k7.supervisory.items.ItemWithHint;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;

@EventBusSubscriber
public class DeviceConfigBufferItem extends Item implements ItemWithHint {
	public DeviceConfigBufferItem(Properties properties) {
		super(properties);
	}

	@Override
	public MutableComponent getHint() {
		return I18n.DEVICE_CONFIG_BUFFER_DESC.toComponent(
				Component.keybind("key.use"),
				Component.keybind("key.use"),
				Component.keybind("key.use"),
				Component.keybind("key.attack"));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
		var stack = player.getItemInHand(usedHand);
		var content = stack.get(AllItems.BUFFER_CONTENT);

		if (player.isShiftKeyDown() && content != null) {
			stack.remove(AllItems.BUFFER_CONTENT);
			return InteractionResultHolder.success(stack);
		}

		return super.use(level, player, usedHand);
	}

	@SubscribeEvent
	private static void preventBlocksHandlingRight(PlayerInteractEvent.RightClickBlock event) {
		// Block#useItemOn has priority in the interaction pipeline, make sure it is not invoked
		if (event.getItemStack().getItem() instanceof DeviceConfigBufferItem) {
			event.setUseBlock(TriState.FALSE);
		}
	}

	@SubscribeEvent
	private static void preventBlocksHandlingLeft(PlayerInteractEvent.LeftClickBlock event) {
		// Block#attack has priority in the interaction pipeline, make sure it is not invoked.
		// Additionally, in creative mode, if this event were to continue, the target block would
		// just be broken. Therefore we have to handle everything here.
		var stack = event.getItemStack();
		if (!(stack.getItem() instanceof DeviceConfigBufferItem)) return;

		var level = event.getLevel();
		var player = event.getEntity();
		if (event.getAction() == Action.START) {
			var domainOnly = Optional.ofNullable(stack.get(AllItems.BUFFER_DOMAIN_ONLY)).orElse(false).booleanValue();
			domainOnly = !domainOnly;
			stack.set(AllItems.BUFFER_DOMAIN_ONLY, domainOnly);
			player.setItemInHand(event.getHand(), stack);

			if (!level.isClientSide) {
				((ServerPlayer) player).sendSystemMessage(Component.empty()
						.append(I18n.TOOLTIP_DOMAIN_ONLY.toComponent())
						.append(Component.literal(": "))
						.append(Component.empty().append((domainOnly ? I18n.TOOLTIP_TRUE : I18n.TOOLTIP_FALSE).toComponent()).withStyle(ChatFormatting.GOLD)), true);
			}
		}

		event.setCanceled(true);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		var level = context.getLevel();
		var position = context.getClickedPos();
		var stack = context.getItemInHand();

		var player = context.getPlayer();
		if (player == null) return InteractionResult.PASS;

		var content = stack.get(AllItems.BUFFER_CONTENT);

		do {
			if (player.isShiftKeyDown() && content != null) {
				stack.remove(AllItems.BUFFER_CONTENT);
				break;
			}

			var be = level.getBlockEntity(position);
			if (!(be instanceof CompositeBlockEntity entity)) return InteractionResult.PASS;

			var subject = entity.getComponent(ConfigBufferSubject.class).orElse(null);
			if (subject == null) return InteractionResult.PASS;

			if (content == null) {
				if (!player.isShiftKeyDown()) return InteractionResult.PASS;
				var newContent = subject.getConfigBuffer();
				stack.set(AllItems.BUFFER_CONTENT, newContent);
				break;
			}

			var domainOnly = Optional.ofNullable(stack.get(AllItems.BUFFER_DOMAIN_ONLY)).orElse(false).booleanValue();

			var type = subject.getDeviceType();
			if (!domainOnly && !Objects.equals(type, content.source())) {
				if (!level.isClientSide) {
					((ServerPlayer) player).sendSystemMessage(I18n.INCOMPATIBLE_DEVICE.toComponent(), true);
				}

				break;
			}

			if (domainOnly) {
				subject.applyDomain(content.domain());
			} else {
				subject.applyConfig(content);
			}

			level.playSound(player, position, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS);
		} while (false);

		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		var domainOnly = stack.get(AllItems.BUFFER_DOMAIN_ONLY);
		if (domainOnly != null) {
			tooltipComponents.add(Component.empty()
					.append(I18n.TOOLTIP_DOMAIN_ONLY.toComponent())
					.append(Component.literal(": "))
					.append(Component.empty().append((domainOnly.booleanValue() ? I18n.TOOLTIP_TRUE : I18n.TOOLTIP_FALSE).toComponent()).withStyle(ChatFormatting.GOLD)));
		}

		var content = stack.get(AllItems.BUFFER_CONTENT);
		if (content != null) {
			tooltipComponents.add(Component.empty()
					.append(I18n.DOMAIN.toComponent())
					.append(Component.literal(": "))
					.append(Component.literal("\"" + Primitive.String.escapeString(content.domain()) + "\"").withStyle(ChatFormatting.GOLD)));

			tooltipComponents.add(Component.empty()
					.append(I18n.TOOLTIP_DEVICE_TYPE.toComponent())
					.append(Component.literal(": "))
					.append(Component.translatable(content.source().toLanguageKey("block")).withStyle(ChatFormatting.GOLD)));
		}

		this.appendHint(stack, context, tooltipComponents, tooltipFlag);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(AllItems.BUFFER_CONTENT);
	}
}
