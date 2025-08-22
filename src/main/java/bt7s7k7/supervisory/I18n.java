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
	public static final Key REMOTE_TERMINAL_UNIT_NAME = register(new Key("gui.remote_terminal_unit.name", "Name"));
	public static final Key REMOTE_TERMINAL_UNIT_DONE = register(new Key("gui.remote_terminal_unit.done", "Done"));

	public static void register() {
		// Load this class
	}
}
