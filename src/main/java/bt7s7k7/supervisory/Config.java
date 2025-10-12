package bt7s7k7.supervisory;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.tterrag.registrate.providers.ProviderType;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
	private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

	private static final ArrayList<Pair<String, ModConfigSpec.ConfigValue<?>>> VALUES = new ArrayList<>();

	private static <T extends ModConfigSpec.ConfigValue<?>> T addValue(String name, T value) {
		VALUES.add(new ImmutablePair<>(name, value));
		return value;
	}

	public static final ModConfigSpec.IntValue EXPRESSION_LIMIT = addValue("Expression Limit", SERVER_BUILDER
			.comment("Maximum amount of expression executed at once in one block")
			.defineInRange("expression_limit", 15000, 1000, Integer.MAX_VALUE));

	public static final ModConfigSpec.BooleanValue ALLOW_STORAGE_TRANSFER = addValue("Allow Storage Transfer", SERVER_BUILDER
			.comment("If the Storage.transfer function should be enabled; with improper use it can be used to achieve overpowered item transfer with infinite range")
			.define("allow_storage_transfer", true));

	static {
		Supervisory.REGISTRATE.addDataGenerator(ProviderType.LANG, lang -> {
			var prefix = Supervisory.MOD_ID + ".configuration.";

			for (var entry : VALUES) {
				var configValue = entry.getRight();
				var path = prefix + String.join(".", configValue.getPath());

				var name = entry.getLeft();
				lang.add(path, name);

				var comment = configValue.getSpec().getComment();
				lang.add(path + ".tooltip", comment);
			}
		});
	}

	public static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();
}
