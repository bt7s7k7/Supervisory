package bt7s7k7.supervisory;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.tterrag.registrate.providers.ProviderType;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	private static final ArrayList<Pair<String, ModConfigSpec.ConfigValue<?>>> VALUES = new ArrayList<>();

	private static <T extends ModConfigSpec.ConfigValue<?>> T addValue(String name, T value) {
		VALUES.add(new ImmutablePair<>(name, value));
		return value;
	}

	public static final ModConfigSpec.IntValue TEST_NUMBER = addValue("Test Number", BUILDER
			.comment("Test number")
			.defineInRange("testNumber", 42, 0, Integer.MAX_VALUE));

	static {
		Supervisory.REGISTRATE.addDataGenerator(ProviderType.LANG, lang -> {
			var prefix = Supervisory.MOD_ID + ".configuration.";
			for (var entry : VALUES) {
				lang.add(prefix + String.join(".", entry.getRight().getPath()), entry.getLeft());
			}
		});
	}

	static final ModConfigSpec SPEC = BUILDER.build();
}
