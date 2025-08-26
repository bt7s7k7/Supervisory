package bt7s7k7.supervisory.blocks.programmableLogicController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.blocks.directControlDevice.DirectControlDeviceBlockEntity;
import bt7s7k7.supervisory.configuration.Configurable;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.ManagedFunction;
import bt7s7k7.treeburst.runtime.ManagedMap;
import bt7s7k7.treeburst.runtime.ManagedTable;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;

public class ProgrammableLogicControllerBlockEntity extends DirectControlDeviceBlockEntity implements Configurable<ProgrammableLogicControllerBlockEntity.Configuration> {

	public ProgrammableLogicControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}

	protected HashMap<String, ManagedValue> savedState = null;
	protected String desiredDomain = null;

	@Override
	public void setDevice(NetworkDevice device, boolean fresh) {
		var oldDevice = this.tryGetDevice();
		if (oldDevice != null) {
			savedState = oldDevice.state;
		}

		super.setDevice(device, fresh);
	}

	@Override
	protected void initializeNetworkDevice(NetworkDevice device, boolean fresh) {
		if (savedState != null) {
			device.state.putAll(savedState);
			savedState = null;
		}

		if (fresh) {
			this.scriptEngine.clear();
			this.scriptEngine.executeCode("code", this.configuration.code);
		}

		if (desiredDomain != null) {
			device.domain = desiredDomain;
		}
	}

	@Override
	protected void handleNetworkUpdate(String key, ManagedValue value) {}

	@Override
	protected void handleRedstoneInputChange(Direction direction, int strength) {}

	public static class Configuration {
		public String command = "";
		public String code = "";
		public ArrayList<Component> log = new ArrayList<>();

		public Configuration(String command, String code, List<Component> log) {
			this.command = command;
			this.code = code;
			this.log = log instanceof ArrayList<Component> arrayList ? arrayList : new ArrayList<>(log);
		}

		public Configuration() {};

		@Override
		public String toString() {
			return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow().toString();
		}

		public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
				Codec.STRING.fieldOf("command").orElse("").forGetter(v -> v.command),
				Codec.STRING.fieldOf("code").orElse("").forGetter(v -> v.code),
				Codec.list(ComponentSerialization.CODEC).fieldOf("log").orElse(Collections.emptyList()).forGetter(v -> v.log))
				.apply(instance, Configuration::new)));
	}

	public void log(Component message) {
		this.configuration.log.add(message);
		while (this.configuration.log.size() > 100) {
			this.configuration.log.removeFirst();
		}

		LogEventRouter.getInstance().sendLogEvent(level, worldPosition, message);
		this.setChanged();
	}

	public Configuration configuration = new Configuration();
	public ScriptEngine scriptEngine = new ScriptEngine() {
		@Override
		protected void handleError(Diagnostic error) {
			log(Component.literal(error.format()).withStyle(ChatFormatting.RED));
		};

		protected ManagedFunction createStateAccessor(GlobalScope globalScope, Function<String, ManagedValue> getter, BiConsumer<String, ManagedValue> setter) {
			return NativeFunction.simple(globalScope, List.of("name", "value?"), List.of(Primitive.String.class, ManagedValue.class), (args, scope, result) -> {
				var key = ((Primitive.String) args.get(0)).value;
				if (args.size() == 1) {
					result.value = getter.apply(key);
					return;
				}

				var value = args.get(1);
				setter.accept(key, value);
				result.value = value;
			});
		}

		private ManagedValue translateValue(GlobalScope globalScope, ManagedValue value) {
			if (value instanceof Primitive) {
				return value;
			}

			if (value instanceof ManagedTable table) {
				var entries = table.properties.entrySet().stream().collect(Collectors.toMap(
						Map.Entry::getKey,
						kv -> this.translateValue(globalScope, kv.getValue())));

				return new ManagedTable(globalScope == null ? null : globalScope.TablePrototype, entries);
			}

			if (value instanceof ManagedArray array) {
				var elements = array.elements.stream().map(v -> this.translateValue(globalScope, v)).toList();

				return new ManagedArray(globalScope == null ? null : globalScope.ArrayPrototype, elements);
			}

			if (value instanceof ManagedMap table) {
				var entries = table.entries.entrySet().stream().collect(Collectors.toMap(
						kv -> this.translateValue(globalScope, kv.getKey()),
						kv -> this.translateValue(globalScope, kv.getValue())));

				return new ManagedMap(globalScope == null ? null : globalScope.MapPrototype, entries);
			}

			return null;
		}

		public ManagedValue importValue(ManagedValue value) {
			return this.translateValue(this.getGlobalScope(), value);
		}

		@Override
		protected void initializeGlobals(GlobalScope globalScope) {
			log(Component.literal("Device restarted").withStyle(ChatFormatting.DARK_GRAY));

			var device = getDevice();
			for (var key : device.getStateKeys()) {
				var result = this.importValue(device.getState(key));
				if (result == null) {
					log(Component.literal("Failed to import state value: " + key).withStyle(ChatFormatting.RED));
					continue;
				}
				device.setState(key, result);
			}

			globalScope.declareGlobal("print", NativeFunction.simple(globalScope, List.of("message"), (args, scope, result) -> {
				var message = args.get(0);

				log(formatValue(message));
			}));

			globalScope.declareGlobal("l", createStateAccessor(globalScope, (key) -> getDevice().getState(key), (key, value) -> {
				getDevice().setState(key, value);
				setChanged();
			}));
		}

		@Override
		public ManagedValue executeCode(String path, String code) {
			var result = super.executeCode(path, code);

			if (result != null && path.equals("command")) {
				log(formatValue(result));
			}

			return result;
		};
	};

	@Override
	public void saveAdditional(CompoundTag tag, Provider registries) {
		super.saveAdditional(tag, registries);
		var result = (CompoundTag) Configuration.CODEC.encodeStart(NbtOps.INSTANCE, this.configuration).getOrThrow();
		result.remove("command");
		tag.merge(result);
	}

	@Override
	public void loadAdditional(CompoundTag tag, Provider registries) {
		super.loadAdditional(tag, registries);

		Configuration.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(configuration -> {
			this.configuration = configuration;
		});
	}

	@Override
	public Configuration getConfiguration() {
		return this.configuration;
	}

	@Override
	public void setConfiguration(Configuration configuration) {
		if (configuration.command.isEmpty()) {
			this.configuration.code = configuration.code;
			this.configuration.log.clear();
			this.scriptEngine.clear();
		} else {
			this.scriptEngine.executeCode("command", configuration.command);
		}

		this.setChanged();
	}

	@Override
	public Codec<Configuration> getConfigurationCodec() {
		return Configuration.CODEC;
	}

	@OnlyIn(Dist.CLIENT)
	private void displayScreen(Configuration configuration) {
		Minecraft.getInstance().setScreen(new ProgrammableLogicControllerScreen(this, configuration));
	}

	@Override
	public void openConfigurationScreen(Configuration configuration) {
		if (FMLLoader.getDist() == Dist.CLIENT) {
			this.displayScreen(configuration);
		}
	}

	@Override
	public void tick(Level level, BlockPos pos, BlockState state) {
		super.tick(level, pos, state);

		if (this.scriptEngine.isEmpty()) {
			this.setDevice(new NetworkDevice(""), true);
		}
	}
}
