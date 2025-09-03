package bt7s7k7.supervisory.script;

import java.util.function.Function;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.supervisory.VanillaExtensionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.StringSplitter.WidthProvider;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class CodeEditorWidget extends MultiLineEditBox {
	public static final int LINE_NUMBER_WIDTH = 20;
	public static final Style LINE_NUMBER_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withFont(Minecraft.DEFAULT_FONT);
	public static final Style SELECTED_LINE_NUMBER_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY).withFont(Minecraft.DEFAULT_FONT);

	public final Font defaultFont;
	public final Style style;
	public final MultilineTextField field;

	public CodeEditorWidget(Font font, int x, int y, int width, int height, Component placeholder, Style style) {
		super(new Font(VanillaExtensionUtil.<Function<ResourceLocation, FontSet>>getField(font, "fonts", Font.class), false) {
			public final StringSplitter defaultSplitter = VanillaExtensionUtil.<StringSplitter>getField(this, "splitter", Font.class);
			public final StringSplitter styledSplitter = new StringSplitter(VanillaExtensionUtil.<WidthProvider>getField(this.defaultSplitter, "widthProvider", StringSplitter.class)) {
				@Override
				public void splitLines(String content, int maxWidth, Style p_style, boolean withNewLines, LinePosConsumer linePos) {
					super.splitLines(content, maxWidth, style, withNewLines, linePos);
				}
			};

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

			@Override
			public StringSplitter getSplitter() {
				return this.styledSplitter;
			}
		}, x, y, width - LINE_NUMBER_WIDTH, height, placeholder, Component.empty());

		this.defaultFont = font;
		this.style = style;
		this.field = VanillaExtensionUtil.<MultilineTextField>getField(this, "textField", MultiLineEditBox.class);

		this.setWidth(width);
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
	protected void renderBackground(GuiGraphics guiGraphics) {
		super.renderBackground(guiGraphics);
		guiGraphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + LINE_NUMBER_WIDTH, this.getY() + this.getHeight() - 1, 0xFF151515);
	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		var currentLine = this.field.getLineAtCursor();

		var styledGraphics = new GuiGraphics(Minecraft.getInstance(), guiGraphics.bufferSource()) {
			@Override
			public void fill(RenderType renderType, int minX, int minY, int maxX, int maxY, int color) {
				super.fill(renderType, minX + LINE_NUMBER_WIDTH, minY, maxX + LINE_NUMBER_WIDTH, maxY, color);
			}

			@Override
			public int drawString(Font font, String text, int x, int y, int color) {
				if (x == CodeEditorWidget.this.getX() + 4) {
					var lineIndex = (y - CodeEditorWidget.this.getY()) / 9;
					var lineString = Integer.toString(lineIndex + 1);
					var lineStringWidth = CodeEditorWidget.this.defaultFont.width(lineString);

					var lineNumberStyle = LINE_NUMBER_STYLE;
					if (lineIndex == currentLine) {
						lineNumberStyle = SELECTED_LINE_NUMBER_STYLE;
					}

					super.drawString(font, FormattedCharSequence.forward(lineString, lineNumberStyle), x + LINE_NUMBER_WIDTH - lineStringWidth - 5, y, color);
					return super.drawString(font, Component.literal(text).withStyle(CodeEditorWidget.this.style), x + LINE_NUMBER_WIDTH, y, color);
				}

				return super.drawString(font, Component.literal(text).withStyle(CodeEditorWidget.this.style), x, y, color);
			}
		};

		super.renderWidget(styledGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.renderContents(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (mouseX < this.getX() + this.getWidth()) mouseX -= LINE_NUMBER_WIDTH;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (mouseX < this.getX() + this.getWidth()) {
			mouseX -= LINE_NUMBER_WIDTH;
			dragX -= LINE_NUMBER_WIDTH;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}
}
