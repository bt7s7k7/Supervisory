package bt7s7k7.supervisory.blocks.programmableLogicController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.blocks.directControlDevice.DirectControlDeviceBlockEntity;
import bt7s7k7.supervisory.configuration.Configurable;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.support.RelativeDirection;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;
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
	protected void handleRedstoneInputChange(Direction direction, int strength) {
		var handlers = this.scriptEngine.reactiveRedstone;
		if (handlers == null) return;
		var relative = RelativeDirection.from(this.getFront(), direction);
		handlers[relative.index].updateValue(Primitive.from(strength));
	}

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
	public PlcScriptEngine scriptEngine = new PlcScriptEngine(this);

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

		this.scriptEngine.processTasks();
	}
}
