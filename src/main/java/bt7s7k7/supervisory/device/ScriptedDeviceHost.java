package bt7s7k7.supervisory.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.ComponentSignal;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.configuration.Configurable;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.network.NetworkDeviceHost;
import bt7s7k7.supervisory.network.RemoteValueReactiveDependency;
import bt7s7k7.supervisory.support.LogEventRouter;
import bt7s7k7.treeburst.runtime.GlobalScope;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;

public class ScriptedDeviceHost extends BlockEntityComponent implements Configurable<ScriptedDeviceHost.Configuration> {
	public final NetworkDeviceHost deviceHost;

	public static record ScopeInitializationEvent(ScriptedDevice device) {
		public GlobalScope getGlobalScope() {
			return this.device.getGlobalScope();
		}
	}

	public final ComponentSignal.Emitter<ScopeInitializationEvent> onScopeInitialization = new ComponentSignal.Emitter<>();

	@Override
	public EventPriority priority() {
		// Because our code is executed during NetworkDevice initialization, ensure our code is
		// loaded before the NetworkDevice is loaded.
		return EventPriority.HIGH;
	}

	@Override
	public void teardown() {
		super.teardown();
		this.onScopeInitialization.teardown();
	}

	public ScriptedDeviceHost(CompositeBlockEntity entity) {
		super(entity);

		this.deviceHost = entity.ensureComponent(NetworkDeviceHost.class, NetworkDeviceHost::new);

		this.connect(this.deviceHost.onInitializeNetworkDevice, event -> {
			var savedState = event.oldDevice();
			var device = event.device();

			if (savedState != null) {
				device.state.putAll(savedState.state);
				device.cache.putAll(savedState.cache);
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

			if (this.desiredDomain != null) {
				device.domain = this.desiredDomain;
			}
		});

		this.connect(this.deviceHost.onDeviceInitialized, device -> {
			if (device.isConnected()) {
				this.log(Component.literal("Connected to domain: " + device.domain).withStyle(ChatFormatting.BLUE));
			}
		});

		this.connect(this.deviceHost.onNetworkUpdate, event -> {
			var key = event.key();
			var value = event.value();

			var reactivityManager = this.scriptEngine.reactivityManager;
			if (reactivityManager == null) return;

			var dependency = RemoteValueReactiveDependency.get(reactivityManager, key, value);
			dependency.updateValue(value);
		});

		NeoForge.EVENT_BUS.post(new ScriptedDeviceInitializationEvent(this, this::connect));
	}

	protected String desiredDomain = null;

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
		if (this.entity.getLevel() == null) {
			this.pendingLogEntries.add(message);
			return;
		}

		this.configuration.log.add(message);
		while (this.configuration.log.size() > 100) {
			this.configuration.log.removeFirst();
		}

		LogEventRouter.getInstance().sendLogEvent(this.entity.getLevel(), this.entity.getBlockPos(), message);
		this.entity.setChanged();
	}

	public Configuration configuration = new Configuration();
	public ScriptedDevice scriptEngine = new ScriptedDevice(this);

	@Override
	public void write(CompoundTag tag, Provider registries) {
		var result = (CompoundTag) Configuration.CODEC.encodeStart(NbtOps.INSTANCE, this.configuration).getOrThrow();
		result.remove("command");
		tag.merge(result);
	}

	@Override
	public void read(CompoundTag tag, Provider registries) {
		Configuration.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(configuration -> {
			this.configuration = configuration;
		});
	}

	@Override
	public Configuration getConfiguration() {
		return this.configuration;
	}

	public void executeCommand(String command) {

		this.scriptEngine.executeCode("command", command);
	}

	@Override
	public void setConfiguration(Configuration configuration) {
		if (configuration.command.isEmpty()) {
			this.configuration.code = configuration.code;
			this.configuration.log.clear();
			this.scriptEngine.clear();
		} else {
			this.log(Component.literal("> ").withStyle(ChatFormatting.GREEN).append(Component.literal(configuration.command).withStyle(ChatFormatting.BLUE)));
			Supervisory.LOGGER.debug("Executed command: " + configuration.command);
			this.executeCommand(configuration.command);
		}

		this.entity.setChanged();
	}

	@Override
	public Codec<Configuration> getConfigurationCodec() {
		return Configuration.CODEC;
	}

	@OnlyIn(Dist.CLIENT)
	private void displayScreen(Configuration configuration) {
		Minecraft.getInstance().setScreen(new ScriptEditorScreen(this, configuration));
	}

	@Override
	public void openConfigurationScreen(Configuration configuration) {
		if (FMLLoader.getDist() == Dist.CLIENT) {
			this.displayScreen(configuration);
		}
	}

	@Override
	public void tick() {
		if (!this.pendingLogEntries.isEmpty()) {
			for (var entry : this.pendingLogEntries) {
				this.log(entry);
			}

			this.pendingLogEntries.clear();
		}

		if (this.scriptEngine.isEmpty()) {
			this.deviceHost.setDevice(new NetworkDevice(""), true);
		}

		this.scriptEngine.processTasks();
	}
}
