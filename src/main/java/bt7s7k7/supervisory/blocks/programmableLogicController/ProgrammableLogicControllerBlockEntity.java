package bt7s7k7.supervisory.blocks.programmableLogicController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.directControlDevice.DirectControlDeviceBlockEntity;
import bt7s7k7.supervisory.blocks.programmableLogicController.reactivity.RemoteValueReactiveDependency;
import bt7s7k7.supervisory.blocks.programmableLogicController.support.LogEventRouter;
import bt7s7k7.supervisory.configuration.Configurable;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.support.Side;
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

	protected NetworkDevice savedState = null;
	protected String desiredDomain = null;
	protected boolean failed = false;

	@Override
	public void setDevice(NetworkDevice device, boolean fresh) {
		var oldDevice = this.tryGetDevice();
		if (oldDevice != null) {
			savedState = oldDevice;
		}

		super.setDevice(device, fresh);

		if (device.isConnected()) {
			this.log(Component.literal("Connected to domain: " + device.domain).withStyle(ChatFormatting.BLUE));
		}
	}

	@Override
	protected void initializeNetworkDevice(NetworkDevice device, boolean fresh) {
		if (savedState != null) {
			device.state.putAll(savedState.state);
			device.cache.putAll(savedState.cache);
			savedState = null;
		}

		this.scriptEngine.clear();
		this.scriptEngine.executeCode("code", this.configuration.code);

		// Remove entries that are not longer subscribed to from the cache
		for (var it = device.cache.keySet().iterator(); it.hasNext();) {
			var key = it.next();
			if (!device.subscriptions.contains(key)) {
				it.remove();
			}
		}

		if (desiredDomain != null) {
			device.domain = desiredDomain;
		}
	}

	@Override
	protected void handleNetworkUpdate(String key, ManagedValue value) {
		var reactivityManager = this.scriptEngine.reactivityManager;
		if (reactivityManager == null) return;

		var dependency = RemoteValueReactiveDependency.get(reactivityManager, key, value);
		dependency.updateValue(value);
	}

	@Override
	protected void handleRedstoneInputChange(Direction direction, int strength) {
		var handlers = this.scriptEngine.reactiveRedstone;
		if (handlers == null) return;
		var relative = Side.from(this.getFront(), direction);
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

	private ArrayList<Component> pendingLogEntries = new ArrayList<>();

	public void log(Component message) {
		if (this.level == null) {
			this.pendingLogEntries.add(message);
			return;
		}

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
		var result = (CompoundTag) Configuration.CODEC.encodeStart(NbtOps.INSTANCE, this.configuration).getOrThrow();
		result.remove("command");
		tag.merge(result);

		super.saveAdditional(tag, registries);
	}

	@Override
	public void loadAdditional(CompoundTag tag, Provider registries) {
		Configuration.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(configuration -> {
			this.configuration = configuration;
		});

		try {
			// Load Configuration before NetworkDevice so the code is already loaded when it's executed during initializeNetworkDevice()
			super.loadAdditional(tag, registries);
		} catch (Exception exception) {
			this.failed = true;
			Supervisory.LOGGER.error("Failed to load ProgrammableLogicControllerBlockEntity at " + this.getBlockPos(), exception);
		}
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
			this.log(Component.literal("> ").withStyle(ChatFormatting.GREEN).append(Component.literal(configuration.command).withStyle(ChatFormatting.BLUE)));
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
		if (this.failed) return;

		if (!this.pendingLogEntries.isEmpty()) {
			for (var entry : this.pendingLogEntries) {
				this.log(entry);
			}

			this.pendingLogEntries.clear();
		}

		try {
			super.tick(level, pos, state);

			if (this.scriptEngine.isEmpty()) {
				this.setDevice(new NetworkDevice(""), true);
			}

			this.scriptEngine.processTasks();
		} catch (Exception exception) {
			this.failed = true;
			Supervisory.LOGGER.error("Failed to process tick for ProgrammableLogicControllerBlockEntity at " + this.getBlockPos(), exception);
		}
	}
}
