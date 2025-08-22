package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import bt7s7k7.supervisory.I18n;
import bt7s7k7.supervisory.Supervisory;
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
	protected void init() {
		super.init();

		Supervisory.LOGGER.info("Opening screen RemoteTerminalUnitScreen, width: " + this.width + ", height: " + this.height);

		GridLayout.builder()
				.addColumn(50)
				.addColumn(Button.BIG_WIDTH)
				.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
					layout.colspan().apply(this.addRenderableWidget(new StringWidget(this.title, this.font)));
				})
				.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
					layout.apply(this.addRenderableWidget(new StringWidget(I18n.REMOTE_TERMINAL_UNIT_DOMAIN.toComponent(), this.font)).alignLeft());
					var domainInput = this.addRenderableWidget(new EditBox(this.font, 0, 0, Component.literal("Test")));
					layout.apply(domainInput);
					domainInput.setValue(this.configuration.domain);
					domainInput.setResponder(v -> this.configuration.domain = v);
				})
				.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
					layout.apply(this.addRenderableWidget(new StringWidget(I18n.REMOTE_TERMINAL_UNIT_NAME.toComponent(), this.font)).alignLeft());
					var nameInput = this.addRenderableWidget(new EditBox(this.font, 0, 0, Component.literal("Test")));
					layout.apply(nameInput);
					nameInput.setValue(this.configuration.name);
					nameInput.setResponder(v -> this.configuration.name = v);
				})
				.addRow(5)
				.addRow(Button.DEFAULT_HEIGHT).render(layout -> {
					layout.nextRow().colspan(2).cell().childLayout()
							.addGrowColumn()
							.addColumn(50)
							.addRow().render(layout_1 -> {
								layout_1.next().apply(
										this.addRenderableWidget(Button.builder(I18n.REMOTE_TERMINAL_UNIT_DONE.toComponent(), e -> {
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
