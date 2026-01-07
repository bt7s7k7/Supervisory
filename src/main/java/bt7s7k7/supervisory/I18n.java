package bt7s7k7.supervisory;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import bt7s7k7.supervisory.blocks.remoteTerminalUnit.IOManager;
import bt7s7k7.supervisory.support.Side;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class I18n {
	public record Key(String domain, String name, String defaultLang) {
		public MutableComponent toComponent() {
			return Component.translatable(this.translate());
		}

		public MutableComponent toComponent(Object... args) {
			return Component.translatable(this.translate(), args);
		}

		public String translate() {
			if (this.domain.equals("configuration")) {
				return Supervisory.MOD_ID + "." + this.domain + "." + this.name;
			}

			return this.domain + "." + Supervisory.MOD_ID + "." + this.name;
		}
	}

	private static Key register(Key key) {
		Supervisory.REGISTRATE.addRawLang(key.translate(), key.defaultLang);
		return key;
	}

	public static final Key REMOTE_TERMINAL_UNIT_TITLE = register(new Key("gui", "remote_terminal_unit.title", "Remote Terminal Unit Configuration"));
	public static final Key REMOTE_TERMINAL_UNIT_DESC = register(new Key("gui", "remote_terminal_unit.desc", """
			Functions as a remote connectivity point; contains simple
			redstone I/O and enables PLCs to interface with remote
			blocks via network sockets."""));

	public static final Key DOMAIN = register(new Key("gui", "domain", "Domain"));
	public static final Key SIDE = register(new Key("gui", "side", "Side"));
	public static final Key NAME = register(new Key("gui", "name", "Name"));
	public static final Key TYPE = register(new Key("gui", "type", "Type"));

	public static final Key PROGRAMMABLE_LOGIC_CONTROLLER_TITLE = register(new Key("gui", "programmable_logic_controller.title", "PLC Configuration"));
	public static final Key PROGRAMMABLE_LOGIC_CONTROLLER_DESC = register(new Key("gui", "programmable_logic_controller.desc", """
			Fully programmable block which allows for complex
			logic execution, interacting with redstone, detecting
			block states and scanning inventories."""));

	public static final Key COMPILE = register(new Key("gui", "compile", "Compile"));
	public static final Key ENTER_COMMAND = register(new Key("gui", "placeholder.command", "Enter command..."));
	public static final Key ENTER_CODE = register(new Key("gui", "placeholder.code", "Enter code..."));
	public static final Key BLOCK_FAILED = register(new Key("gui", "failed", "This block failed to load and cannot be accessed"));
	public static final Key TOOLTIP_PREV_MODULE = register(new Key("gui", "module.prev", "Switches to the previous module. Hold [Shift] to move the current module."));
	public static final Key TOOLTIP_NEXT_MODULE = register(new Key("gui", "module.next", "Switches to the next module. Hold [Shift] to move the current module."));
	public static final Key TOOLTIP_ADD_MODULE = register(new Key("gui", "module.add", "Creates a new module. Hold [Shift] to delete the current module."));

	public static final Key DONE = register(new Key("gui", "done", "Done"));
	public static final Key APPLY = register(new Key("gui", "apply", "Apply"));
	public static final Key CLOSE = register(new Key("gui", "close", "Close"));
	public static final Key CANCEL = register(new Key("gui", "cancel", "Cancel"));
	public static final Key CONFIRM = register(new Key("gui", "confirm", "Confirm"));
	public static final Key CLOSE_CONFIRM = register(new Key("gui", "close_confirm", "Are you sure you want exit?"));
	public static final Key CLOSE_CONFIRM_DESC = register(new Key("gui", "close_confirm_desc", "All unsaved changes will be lost."));

	public static final Key DEVICE_CONFIG_BUFFER_DESC = register(new Key("gui", "device_config_buffer_desc.desc", """
			Allows you to easily copy configuration between devices.
			Press [Shift + %s] to copy settings and [%s] to paste.
			Press [Shift + %s] again to clear the stored data.
			Press [%s] to switch modes.
			In domain only mode, only the selected domain will be pasted,
			otherwise the entire configuration will be applied."""));
	public static final Key TOOLTIP_DOMAIN_ONLY = register(new Key("gui", "device_config_buffer.domain_only", "Paste domain only"));
	public static final Key TOOLTIP_TRUE = register(new Key("gui", "true", "True"));
	public static final Key TOOLTIP_FALSE = register(new Key("gui", "false", "False"));
	public static final Key TOOLTIP_DEVICE_TYPE = register(new Key("gui", "device_config_buffer.device_type", "Device type"));
	public static final Key INCOMPATIBLE_DEVICE = register(new Key("gui", "device_config_buffer.incompatible_device", "This device is not compatible with the stored configuration"));

	public static final Key TOOLTIP_HOLD_SHIFT = register(new Key("gui", "hold_shift", "Hold [Shift] for more info"));

	public static final Map<Side, Key> SIDES = Arrays.stream(Side.values()).collect(Collectors.toMap(v -> v, v -> register(new Key("gui", "side." + v.name, StringUtils.capitalize(v.name)))));
	public static final Map<IOManager.SideType, Key> SIDE_TYPES = Map.of(
			IOManager.SideType.SOCKET, register(new Key("gui", "side_type.socket", "Socket")),
			IOManager.SideType.INPUT, register(new Key("gui", "side_type.input", "Signal In")),
			IOManager.SideType.OUTPUT, register(new Key("gui", "side_type.output", "Signal Out")));

	public static void register() {
		// Load this class
	}
}
