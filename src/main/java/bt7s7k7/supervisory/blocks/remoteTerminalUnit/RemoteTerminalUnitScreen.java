package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import java.util.function.Consumer;

import bt7s7k7.supervisory.I18n;
import bt7s7k7.supervisory.blocks.remoteTerminalUnit.IOManager.SideConfiguration;
import bt7s7k7.supervisory.configuration.ConfigurationScreenManager;
import bt7s7k7.supervisory.support.GridLayout;
import bt7s7k7.supervisory.support.Side;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RemoteTerminalUnitScreen extends Screen {
	public final IOManager host;
	public final IOManager.Configuration configuration;

	public Side selectedSide = Side.FRONT;
	public SideConfiguration sideConfiguration;

	protected RemoteTerminalUnitScreen(IOManager blockEntity, IOManager.Configuration configuration) {
		super(I18n.REMOTE_TERMINAL_UNIT_TITLE.toComponent());

		this.host = blockEntity;
		this.configuration = configuration;

		this.sideConfiguration = configuration.sides[this.selectedSide.index];
		if (this.sideConfiguration == null) {
			configuration.sides[this.selectedSide.index] = this.sideConfiguration = new SideConfiguration();
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

	private EditBox addStringField(GridLayout layout, Component label, String value, Consumer<String> responder) {
		layout.apply(this.addRenderableWidget(new StringWidget(label, this.font)).alignLeft());
		var inputField = this.addRenderableWidget(new EditBox(this.font, 0, 0, label));
		layout.apply(inputField);
		inputField.setValue(value);
		inputField.setResponder(responder);
		inputField.setMaxLength(2048);
		return inputField;
	}

	@Override
	protected void init() {
		super.init();

		GridLayout.builder()
				.addColumn(50)
				.addColumn(Button.BIG_WIDTH)
				.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
					layout.colspan().apply(this.addRenderableWidget(new StringWidget(this.title, this.font)));
				})
				.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
					this.addStringField(layout, I18n.DOMAIN.toComponent(), this.configuration.domain, v -> this.configuration.domain = v);
				})
				.addRow(Button.DEFAULT_HEIGHT * 3).render(layout -> {
					layout.apply(this.addRenderableWidget(new StringWidget(I18n.SIDE.toComponent(), this.font)).alignLeft());

					layout.cell().childLayout()
							.setGap(5)
							.addGrowColumn()
							.addColumns(50, 3)
							.addGrowColumn()
							.addGrowRow()
							.addGrowRow()
							.addGrowRow()
							.render(layout_1 -> {
								for (var side : new Side[] {
										null, null, Side.TOP, Side.BACK, null,
										null, Side.RIGHT, Side.FRONT, Side.LEFT, null,
										null, null, Side.BOTTOM, null, null
								}) {
									if (side == null) {
										layout_1.next();
										continue;
									}

									var button = this.addRenderableWidget(Button.builder(I18n.SIDES.get(side).toComponent(), e -> {
										this.selectedSide = side;
										this.sideConfiguration = this.configuration.sides[this.selectedSide.index];
										if (this.sideConfiguration == null) {
											this.configuration.sides[this.selectedSide.index] = this.sideConfiguration = new SideConfiguration();
										}

										this.rebuildWidgets();
									}).build());
									layout_1.apply(button);

									button.active = this.selectedSide != side;
								}
							})
							.build();
				})
				.addRow(Button.DEFAULT_HEIGHT).renderRow(layout -> {
					var field = this.addStringField(layout, I18n.NAME.toComponent(), this.sideConfiguration.name, v -> this.sideConfiguration.name = v);

					field.setFormatter((value, __) -> FormattedCharSequence.forward(value, Style.EMPTY.withColor(switch (this.sideConfiguration.type) {
						case IOManager.SideType.INPUT -> ChatFormatting.AQUA;
						case IOManager.SideType.OUTPUT -> ChatFormatting.GOLD;
						case IOManager.SideType.SOCKET -> ChatFormatting.GREEN;
						default -> ChatFormatting.WHITE;
					})));
				})
				.addRow(Button.DEFAULT_HEIGHT).renderRow(layout -> {
					layout.apply(this.addRenderableWidget(new StringWidget(I18n.TYPE.toComponent(), this.font)).alignLeft());

					var builder = layout.cell().childLayout()
							.setGap(5)
							.addGrowRow();

					for (var type : IOManager.SideType.values()) {
						builder.addGrowColumn().renderColumn(layout_1 -> {
							var button = this.addRenderableWidget(Button.builder(I18n.SIDE_TYPES.get(type).toComponent(), e -> {
								this.sideConfiguration.type = type;
								this.rebuildWidgets();
							}).build());

							layout_1.apply(button);

							button.active = this.sideConfiguration.type != type;
						});
					}

					builder.build();
				})
				.addRow(5)
				.addRow(Button.DEFAULT_HEIGHT).renderRow(layout -> {
					layout.colspan(2).cell().childLayout()
							.addGrowColumn()
							.addColumn(50)
							.addColumn(50)
							.setGap(5)
							.addRow().render(layout_1 -> {
								layout_1.next();

								layout_1.apply(this.addRenderableWidget(Button.builder(I18n.APPLY.toComponent(), e -> {
									ConfigurationScreenManager.submitConfiguration(this.host.entity.getBlockPos(), this.host, this.configuration);
								}).build()));

								layout_1.apply(this.addRenderableWidget(Button.builder(I18n.DONE.toComponent(), e -> {
									ConfigurationScreenManager.submitConfiguration(this.host.entity.getBlockPos(), this.host, this.configuration);
									this.onClose();
								}).build()));
							})
							.build();
				})
				.setGap(5)
				.centerAround(this.width / 2, this.height / 2)
				.build();
	}
}
