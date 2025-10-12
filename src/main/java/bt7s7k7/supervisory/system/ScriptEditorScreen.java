package bt7s7k7.supervisory.system;

import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import bt7s7k7.supervisory.I18n;
import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.configuration.ConfigurationScreenManager;
import bt7s7k7.supervisory.script.CodeEditorWidget;
import bt7s7k7.supervisory.support.GridLayout;
import bt7s7k7.supervisory.support.LogEventRouter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScriptEditorScreen extends Screen {
	public final ScriptedSystemHost host;
	public final ScriptedSystemHost.Configuration configuration;

	public String commandInput = "";
	public int historyIndex;
	public boolean dirty = false;
	public int selectedModule = 0;

	public ScriptEditorScreen(ScriptedSystemHost blockEntity, ScriptedSystemHost.Configuration configuration) {
		super(I18n.PROGRAMMABLE_LOGIC_CONTROLLER_TITLE.toComponent());
		this.host = blockEntity;
		this.configuration = configuration;
		this.historyIndex = blockEntity.commandHistory.size();
		LogEventRouter.getInstance().onLogReceived = this::handleLogReceived;

		// Ensure at least one module to display
		if (configuration.modules.isEmpty()) {
			configuration.modules.add("");
		}
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

		this.logView = this.addRenderableWidget(new LogView(this.logViewPosition.x(), this.logViewPosition.y(), this.logViewPosition.width(), this.logViewPosition.height(), logContent, this.font));
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
								layout_1.apply(this.addRenderableWidget(Button.builder(I18n.COMPILE.toComponent(), __ -> {
									this.compile();
								}).build()));
							})
							.build();

					layout.next().cell().childLayout()
							.setGap(5)
							.addRow()
							.addColumn(35).renderColumn(layout_1 -> {
								layout_1.apply(this.addRenderableWidget(new StringWidget(I18n.DOMAIN.toComponent(), this.font)));
							})
							.addGrowColumn().renderColumn(layout_1 -> {
								var domainBox = this.addRenderableWidget(new EditBox(this.font, 0, 0, I18n.DOMAIN.toComponent()));
								layout_1.apply(domainBox);
								domainBox.setValue(this.configuration.domain);
								domainBox.setResponder(value -> {
									this.configuration.domain = value;
									this.dirty = true;
								});
							})
							.addColumn(50).renderColumn(layout_1 -> {
								layout_1.apply(this.addRenderableWidget(Button.builder(I18n.CLOSE.toComponent(), __ -> {
									this.closeOrConfirmLostChanges();
								}).build()));
							})
							.build();
				})
				.addGrowRow().renderRow(layout -> {
					var editBox = this.addRenderableWidget(new CodeEditorWidget(this.font,
							layout.cell().x(), layout.cell().y(),
							layout.cell().width(), layout.cell().height(),
							I18n.ENTER_CODE.toComponent(),
							EDITOR_STYLE));
					editBox.setValue(this.configuration.modules.get(this.selectedModule));
					editBox.setValueListener(value -> {
						this.configuration.modules.set(this.selectedModule, value);
						this.dirty = true;
					});

					editBox.field.seekCursor(Whence.ABSOLUTE, 0);
					layout.next();

					this.logViewPosition = layout.cell();
					this.rebuildLogView();
				})
				.addRow(Button.DEFAULT_HEIGHT).renderRow(layout -> {
					layout.cell().childLayout()
							.setGap(5)
							.addRow()
							.addGrowColumn()
							.addColumn(25).renderColumn(layout_1 -> {
								var button = this.addRenderableWidget(Button.builder(Component.literal("←"), __ -> {
									if (this.selectedModule > 0) {
										if (Screen.hasShiftDown()) {
											var tmp = this.configuration.modules.get(this.selectedModule - 1);
											this.configuration.modules.set(this.selectedModule - 1, this.configuration.modules.get(this.selectedModule));
											this.configuration.modules.set(this.selectedModule, tmp);
											this.dirty = true;
										}

										this.selectedModule--;
										this.rebuildWidgets();
									}
								}).tooltip(Tooltip.create(I18n.TOOLTIP_PREV_MODULE.toComponent())).build());
								button.active = this.selectedModule > 0;

								layout_1.apply(button);
							})
							.addColumn(25).renderColumn(layout_1 -> {
								var label = (this.selectedModule + 1) + "/" + this.configuration.modules.size();
								layout_1.apply(this.addRenderableWidget(new StringWidget(Component.literal(label), this.font)));
							})
							.addColumn(25).renderColumn(layout_1 -> {
								var button = this.addRenderableWidget(Button.builder(Component.literal("→"), __ -> {
									if (this.selectedModule < this.configuration.modules.size() - 1) {
										if (Screen.hasShiftDown()) {
											var tmp = this.configuration.modules.get(this.selectedModule + 1);
											this.configuration.modules.set(this.selectedModule + 1, this.configuration.modules.get(this.selectedModule));
											this.configuration.modules.set(this.selectedModule, tmp);
											this.dirty = true;
										}

										this.selectedModule++;
										this.rebuildWidgets();
									}
								}).tooltip(Tooltip.create(I18n.TOOLTIP_NEXT_MODULE.toComponent())).build());
								button.active = this.selectedModule < this.configuration.modules.size() - 1;

								layout_1.apply(button);
							})
							.addColumn(25).renderColumn(layout_1 -> {
								var button = this.addRenderableWidget(Button.builder(Component.literal("+/-"), __ -> {
									if (Screen.hasShiftDown()) {
										this.configuration.modules.remove(this.selectedModule);

										if (this.configuration.modules.isEmpty()) {
											this.configuration.modules.add("");
										} else {
											if (this.selectedModule != 0) {
												this.selectedModule--;
											}
										}
									} else {
										this.selectedModule = this.configuration.modules.size();
										this.configuration.modules.add("");
									}

									this.dirty = true;
									this.rebuildWidgets();
								}).tooltip(Tooltip.create(I18n.TOOLTIP_ADD_MODULE.toComponent())).build());

								layout_1.apply(button);
							})
							.addGrowColumn()
							.build();

					layout.next();

					var commandField = this.addRenderableWidget(new EditBox(this.font, 0, 0, Component.empty()) {
						@Override
						public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
							if (this.isActive() && this.isFocused()) {
								if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
									ScriptEditorScreen.this.submitCommand();
									this.setValue("");
									return true;
								}

								if (keyCode == InputConstants.KEY_UP) {
									if (ScriptEditorScreen.this.historyIndex - 1 >= 0) {
										ScriptEditorScreen.this.historyIndex--;
										ScriptEditorScreen.this.commandInput = ScriptEditorScreen.this.host.commandHistory.get(ScriptEditorScreen.this.historyIndex);
										this.setValue(ScriptEditorScreen.this.commandInput);
									}

									return true;
								}

								if (keyCode == InputConstants.KEY_DOWN) {
									if (ScriptEditorScreen.this.historyIndex + 1 <= ScriptEditorScreen.this.host.commandHistory.size()) {
										ScriptEditorScreen.this.historyIndex++;
										ScriptEditorScreen.this.commandInput = ScriptEditorScreen.this.historyIndex >= ScriptEditorScreen.this.host.commandHistory.size()
												? ""
												: ScriptEditorScreen.this.host.commandHistory.get(ScriptEditorScreen.this.historyIndex);

										this.setValue(ScriptEditorScreen.this.commandInput);
									}

									return true;
								}
							}

							return super.keyPressed(keyCode, scanCode, modifiers);
						}

						@Override
						public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
							super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
							if (this.getValue().isEmpty() && !this.isFocused()) {
								guiGraphics.drawString(ScriptEditorScreen.this.font, I18n.ENTER_COMMAND.toComponent(), this.getX() + 4, this.getY() + (this.height - 8) / 2, -8355712, false);
							}
						}
					});

					commandField.setResponder(value -> this.commandInput = value);
					commandField.setValue(this.commandInput);
					commandField.setMaxLength(2048);
					layout.apply(commandField);
				})
				.setOffset(5, 5)
				.setLimit(this.width - 10, this.height - 10)
				.build();
	}

	protected void compile() {
		this.configuration.log.clear();
		this.rebuildLogView();

		var update = new ScriptedSystemHost.ConfigurationUpdate();
		update.modules = Optional.of(this.configuration.modules);
		update.domain = Optional.of(this.configuration.domain);
		ConfigurationScreenManager.submitConfiguration(this.host.entity.getBlockPos(), this.host, update);

		this.dirty = false;
	}

	protected void submitCommand() {
		if (this.commandInput.trim().isEmpty()) {
			return;
		}

		if (this.host.commandHistory.isEmpty() || !this.host.commandHistory.getLast().equals(this.commandInput)) {
			this.host.commandHistory.add(this.commandInput);
		}

		this.historyIndex = this.host.commandHistory.size();

		var update = new ScriptedSystemHost.ConfigurationUpdate();
		update.command = Optional.of(this.commandInput);
		ConfigurationScreenManager.submitConfiguration(this.host.entity.getBlockPos(), this.host, update);

		this.commandInput = "";
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == InputConstants.KEY_ESCAPE) {
			this.closeOrConfirmLostChanges();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	public void closeOrConfirmLostChanges() {
		if (!this.dirty) {
			this.onClose();
			return;
		}

		Minecraft.getInstance().pushGuiLayer(new Screen(I18n.CLOSE_CONFIRM.toComponent()) {
			@Override
			protected void init() {
				GridLayout.builder()
						.addColumn(200)
						.setGap(5)
						.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
							layout.apply(this.addRenderableWidget(new StringWidget(I18n.CLOSE_CONFIRM.toComponent(), this.font)));
						})
						.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
							layout.apply(this.addRenderableWidget(new StringWidget(I18n.CLOSE_CONFIRM_DESC.toComponent(), this.font)));
						})
						.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
							layout.cell().childLayout()
									.addRow()
									.setGap(5)
									.addGrowColumn()
									.addColumn(50).renderColumn(layout_1 -> {
										layout_1.apply(this.addRenderableWidget(Button.builder(I18n.CONFIRM.toComponent(), __ -> {
											ScriptEditorScreen.this.onClose();
											this.onClose();
										}).build()));
									})
									.addColumn(50).renderColumn(layout_1 -> {
										layout_1.apply(this.addRenderableWidget(Button.builder(I18n.CANCEL.toComponent(), __ -> {
											this.onClose();
										}).build()));
									})
									.build();
						})
						.centerAround(this.width / 2, this.height / 2)
						.build();
			}

			@Override
			public boolean isPauseScreen() {
				return false;
			}
		});
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
