package bt7s7k7.supervisory.blocks.programmableLogicController;

import java.util.Collections;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.supervisory.I18n;
import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.programmableLogicController.support.CodeEditorWidget;
import bt7s7k7.supervisory.blocks.programmableLogicController.support.LogEventRouter;
import bt7s7k7.supervisory.configuration.ConfigurationScreenManager;
import bt7s7k7.supervisory.support.GridLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProgrammableLogicControllerScreen extends Screen {
	public final ProgrammableLogicControllerBlockEntity blockEntity;
	public final ProgrammableLogicControllerBlockEntity.Configuration configuration;

	public String commandInput = "";

	protected ProgrammableLogicControllerScreen(ProgrammableLogicControllerBlockEntity blockEntity, ProgrammableLogicControllerBlockEntity.Configuration configuration) {
		super(I18n.PROGRAMMABLE_LOGIC_CONTROLLER_TITLE.toComponent());
		this.blockEntity = blockEntity;
		this.configuration = configuration;
		LogEventRouter.getInstance().onLogReceived = this::handleLogReceived;
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(guiGraphics);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected static class LogView extends FittingMultiLineTextWidget {
		public LogView(int x, int y, int width, int height, Component message, Font font) {
			super(x, y, width, height, message, font);
		}

		public boolean isScrolledUp() {
			return this.scrollAmount() < this.getMaxScrollAmount();
		}

		@Override
		public double scrollAmount() {
			return super.scrollAmount();
		}

		@Override
		protected void setScrollAmount(double scrollAmount) {
			super.setScrollAmount(scrollAmount);
		}

		// The FittingMultiLineTextWidget does not render the border or background
		// while scrolling is disabled. Therefore we force scrolling to be enabled.
		@Override
		protected boolean scrollbarVisible() {
			return true;
		}

		// If scrolling is enabled the scrollbar will be rendered. However, because
		// there is no overflow content to scroll, the renderScrollBar method will
		// cause a divide by zero. We cannot fix the renderScrollBar method, because
		// it's private, so we just don't call it when it wouldn't have been called
		// normally.
		@Override
		protected void renderDecorations(GuiGraphics guiGraphics) {
			if (super.scrollbarVisible()) {
				super.renderDecorations(guiGraphics);
			}
		}
	}

	protected LogView logView = null;
	protected GridLayout.Cell logViewPosition = null;

	public static final ResourceLocation MONOSPACE_FONT = Supervisory.resource("monocraft");
	public static final Style EDITOR_STYLE = Style.EMPTY.withFont(MONOSPACE_FONT);

	protected void rebuildLogView() {
		var targetScrollAmount = Double.POSITIVE_INFINITY;

		if (this.logView != null) {
			if (this.logView.isScrolledUp()) {
				targetScrollAmount = this.logView.scrollAmount();
			}
			this.removeWidget(this.logView);
			this.logView = null;
		}

		var logContent = Component.empty();
		for (var line : this.configuration.log) {
			logContent.append(line);
			logContent.append("\n");
		}

		logContent = logContent.withStyle(EDITOR_STYLE);

		this.logView = this.addRenderableWidget(new LogView(logViewPosition.x(), logViewPosition.y(), logViewPosition.width(), logViewPosition.height(), logContent, this.font));
		this.logView.setScrollAmount(targetScrollAmount);
	}

	@Override
	protected void init() {
		GridLayout.builder()
				.addGrowColumn()
				.addGrowColumn()
				.setGap(5)
				.addRow(Button.DEFAULT_HEIGHT).renderRow(layout -> {
					layout.cell().childLayout()
							.addRow()
							.addGrowColumn().renderColumn(layout_1 -> {
								layout_1.apply(this.addRenderableWidget(new StringWidget(this.title, this.font)).alignLeft());
							})
							.addColumn(50).renderColumn(layout_1 -> {
								layout_1.apply(this.addRenderableWidget(Button.builder(I18n.PROGRAMMABLE_LOGIC_CONTROLLER_COMPILE.toComponent(), __ -> {
									this.compile();
								}).build()));
							})
							.build();

					layout.next().cell().childLayout()
							.addRow()
							.addGrowColumn()
							.addColumn(50).renderColumn(layout_1 -> {
								layout_1.apply(this.addRenderableWidget(Button.builder(I18n.CLOSE.toComponent(), __ -> {
									this.onClose();
								}).build()));
							})
							.build();
				})
				.addGrowRow().renderRow(layout -> {
					var editBox = this.addRenderableWidget(new CodeEditorWidget(font,
							layout.cell().x(), layout.cell().y(),
							layout.cell().width(), layout.cell().height(),
							I18n.PROGRAMMABLE_LOGIC_CONTROLLER_CODE.toComponent(),
							EDITOR_STYLE));
					editBox.setValue(this.configuration.code);
					editBox.setValueListener(value -> {
						this.configuration.code = value;
					});

					layout.next();

					this.logViewPosition = layout.cell();
					rebuildLogView();
				})
				.addRow(Button.DEFAULT_HEIGHT).renderRow(layout -> {
					layout.next();

					var commandField = this.addRenderableWidget(new EditBox(font, 0, 0, Component.empty()) {
						@Override
						public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
							if (this.isActive() && this.isFocused() && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
								submitCommand();
								this.setValue("");
							}

							return super.keyPressed(keyCode, scanCode, modifiers);
						}

						@Override
						public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
							super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
							if (this.getValue().isEmpty() && !this.isFocused()) {
								guiGraphics.drawString(font, I18n.PROGRAMMABLE_LOGIC_CONTROLLER_COMMAND.toComponent(), this.getX() + 4, this.getY() + (this.height - 8) / 2, -8355712, false);
							}
						}
					});

					commandField.setResponder(value -> this.commandInput = value);
					commandField.setValue(this.commandInput);
					layout.apply(commandField);
				})
				.setOffset(5, 5)
				.setLimit(this.width - 10, this.height - 10)
				.build();
	}

	protected void compile() {
		this.configuration.log.clear();
		this.rebuildLogView();

		var configuration = new ProgrammableLogicControllerBlockEntity.Configuration("", this.configuration.code, Collections.emptyList());
		ConfigurationScreenManager.submitConfiguration(this.blockEntity.getBlockPos(), this.blockEntity, configuration);
	}

	protected void submitCommand() {
		if (this.commandInput.trim().isEmpty()) {
			return;
		}

		var configuration = new ProgrammableLogicControllerBlockEntity.Configuration(this.commandInput, "", Collections.emptyList());
		ConfigurationScreenManager.submitConfiguration(this.blockEntity.getBlockPos(), this.blockEntity, configuration);
		this.commandInput = "";
	}

	@Override
	public void onClose() {
		super.onClose();
		LogEventRouter.getInstance().onLogReceived = null;
	}

	private void handleLogReceived(Component log) {
		this.configuration.log.add(log);

		while (this.configuration.log.size() > 100) {
			this.configuration.log.removeFirst();
		}

		this.rebuildLogView();
	}
}
