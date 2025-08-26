package bt7s7k7.supervisory.blocks.programmableLogicController;

import java.util.function.Function;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.supervisory.VanillaExtensionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public class CodeEditorWidget extends MultiLineEditBox {
	public final Style style;
	public final MultilineTextField field;

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
		this.field = VanillaExtensionUtil.<MultilineTextField>getField(this, "textField", MultiLineEditBox.class);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == InputConstants.KEY_TAB) {
			this.field.insertText("    ");
			return true;
		}

		if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
			var cursor = this.field.cursor();
			var content = this.field.value();

			if (cursor > 0) {
				var lastNonSpace = cursor;

				while (cursor > 0) {
					cursor--;

					if (content.charAt(cursor) == '\n') {
						cursor++;
						break;
					}

					if (content.charAt(cursor) != ' ') {
						lastNonSpace = cursor;
					}
				}

				var indent = lastNonSpace - cursor;
				if (indent < 0) indent = 0;
				this.field.insertText("\n" + " ".repeat(indent));
				return true;
			}
		}

		if (Screen.isPaste(keyCode)) {
			var clipboardText = Minecraft.getInstance().keyboardHandler.getClipboard();
			this.field.insertText(clipboardText.replaceAll("\\t", "    "));
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		var styledGraphics = new GuiGraphics(Minecraft.getInstance(), guiGraphics.bufferSource()) {
			@Override
			public int drawString(Font font, String text, int x, int y, int color) {
				return super.drawString(font, Component.literal(text).withStyle(style), x, y, color);
			}
		};

		super.renderWidget(styledGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderContents(guiGraphics, mouseX, mouseY, partialTick);
	}
}
