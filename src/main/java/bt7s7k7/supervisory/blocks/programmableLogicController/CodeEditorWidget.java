package bt7s7k7.supervisory.blocks.programmableLogicController;

import java.util.function.Function;

import bt7s7k7.supervisory.VanillaExtensionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public class CodeEditorWidget extends MultiLineEditBox {
	public final Style style;

	public CodeEditorWidget(Font font, int x, int y, int width, int height, Component placeholder, Style style) {
		super(new Font(VanillaExtensionUtil.<Function<ResourceLocation, FontSet>>getField(font, "fonts", Font.class), false) {
			@Override
			public String plainSubstrByWidth(String text, int maxWidth) {
				var formatted = FormattedText.of(text, style);
				return this.substrByWidth(formatted, maxWidth).getString();
			}

			@Override
			public int width(String text) {
				var formatted = FormattedText.of(text, style);
				return this.width(formatted);
			}
		}, x, y, width, height, placeholder, Component.empty());

		this.style = style;
	}

	@Override
	protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		var styledGraphics = new GuiGraphics(Minecraft.getInstance(), guiGraphics.bufferSource()) {
			@Override
			public int drawString(Font font, String text, int x, int y, int color) {
				return super.drawString(font, Component.literal(text).withStyle(style), x, y, color);
			}
		};

		super.renderContents(styledGraphics, mouseX, mouseY, partialTick);
	}
}
