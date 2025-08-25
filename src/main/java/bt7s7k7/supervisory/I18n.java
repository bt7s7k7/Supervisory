package bt7s7k7.supervisory;

import net.minecraft.network.chat.Component;

public class I18n {
	public static class Key {
		public final String name;
		public final String defaultLang;

		public Key(String name, String defaultLang) {
			this.name = name;
			this.defaultLang = defaultLang;
		}

		public Component toComponent() {
			return Component.translatable(translate(this.name));
		}
	}

	public static String translate(String key) {
		return Supervisory.MOD_ID + "." + key;
	}

	private static Key register(Key key) {
		Supervisory.REGISTRATE.addRawLang(translate(key.name), key.defaultLang);
		return key;
	}

	public static final Key REMOTE_TERMINAL_UNIT_TITLE = register(new Key("gui.remote_terminal_unit.title", "Remote Terminal Unit Configuration"));
	public static final Key REMOTE_TERMINAL_UNIT_DOMAIN = register(new Key("gui.remote_terminal_unit.domain", "Domain"));
	public static final Key REMOTE_TERMINAL_UNIT_INPUT = register(new Key("gui.remote_terminal_unit.input", "Input"));
	public static final Key REMOTE_TERMINAL_UNIT_OUTPUT = register(new Key("gui.remote_terminal_unit.output", "Output"));

	public static final Key PROGRAMMABLE_LOGIC_CONTROLLER_TITLE = register(new Key("gui.programmable_logic_controller.title", "PLC Configuration"));
	public static final Key PROGRAMMABLE_LOGIC_CONTROLLER_COMPILE = register(new Key("gui.programmable_logic_controller.compile", "Compile"));
	public static final Key PROGRAMMABLE_LOGIC_CONTROLLER_COMMAND = register(new Key("gui.programmable_logic_controller.command", "Enter command..."));
	public static final Key PROGRAMMABLE_LOGIC_CONTROLLER_CODE = register(new Key("gui.programmable_logic_controller.code", "Enter code..."));

	public static final Key DONE = register(new Key("gui.done", "Done"));
	public static final Key APPLY = register(new Key("gui.apply", "Apply"));
	public static final Key CLOSE = register(new Key("gui.close", "Close"));

	public static void register() {
		// Load this class
	}
}
