package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import java.util.function.Consumer;

import bt7s7k7.supervisory.I18n;
import bt7s7k7.supervisory.configuration.ConfigurationScreenManager;
import bt7s7k7.supervisory.support.GridLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RemoteTerminalUnitScreen extends Screen {
	public final RemoteTerminalUnitBlockEntity blockEntity;
	public final RemoteTerminalUnitBlockEntity.Configuration configuration;

	protected RemoteTerminalUnitScreen(RemoteTerminalUnitBlockEntity blockEntity, RemoteTerminalUnitBlockEntity.Configuration configuration) {
		super(I18n.REMOTE_TERMINAL_UNIT_TITLE.toComponent());

		this.blockEntity = blockEntity;
		this.configuration = configuration;
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderTransparentBackground(guiGraphics);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void addStringField(GridLayout layout, Component label, String value, Consumer<String> responder) {
		layout.apply(this.addRenderableWidget(new StringWidget(label, this.font)).alignLeft());
		var inputField = this.addRenderableWidget(new EditBox(this.font, 0, 0, label));
		layout.apply(inputField);
		inputField.setValue(value);
		inputField.setResponder(responder);
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
					this.addStringField(layout, I18n.REMOTE_TERMINAL_UNIT_DOMAIN.toComponent(), this.configuration.domain, v -> this.configuration.domain = v);
				})
				.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
					this.addStringField(layout, I18n.REMOTE_TERMINAL_UNIT_INPUT.toComponent(), this.configuration.input, v -> this.configuration.input = v);
				})
				.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
					this.addStringField(layout, I18n.REMOTE_TERMINAL_UNIT_OUTPUT.toComponent(), this.configuration.output, v -> this.configuration.output = v);
				})
				.addRow(5)
				.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
					layout.nextRow().colspan(2).cell().childLayout()
							.addGrowColumn()
							.addColumn(50)
							.addColumn(50)
							.setGap(5)
							.addRow().render(layout_1 -> {
								layout_1.next();

								layout_1.apply(this.addRenderableWidget(Button.builder(I18n.APPLY.toComponent(), e -> {
									ConfigurationScreenManager.submitConfiguration(this.blockEntity.getBlockPos(), this.blockEntity, this.configuration);
								}).build()));

								layout_1.apply(this.addRenderableWidget(Button.builder(I18n.DONE.toComponent(), e -> {
									ConfigurationScreenManager.submitConfiguration(this.blockEntity.getBlockPos(), this.blockEntity, this.configuration);
									this.onClose();
								}).build()));
							})
							.build();
				})
				.setGap(5)
				.centerAround(width / 2, height / 2)
				.build();
	}
}
