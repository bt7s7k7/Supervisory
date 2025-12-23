package bt7s7k7.supervisory.items;

import java.util.List;

import bt7s7k7.supervisory.I18n;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public interface ItemWithHint {
	MutableComponent getHint();

	default void appendHint(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		if (tooltipFlag.hasShiftDown()) {
			tooltipComponents.add(this.getHint().withStyle(ChatFormatting.GRAY));
		} else {
			tooltipComponents.add(I18n.TOOLTIP_HOLD_SHIFT.toComponent().withStyle(ChatFormatting.GRAY));
		}
	}
}
