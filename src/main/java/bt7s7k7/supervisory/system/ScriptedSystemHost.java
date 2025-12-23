package bt7s7k7.supervisory.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.ComponentSignal;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.configuration.Configurable;
import bt7s7k7.supervisory.items.device_config_buffer.BufferContent;
import bt7s7k7.supervisory.items.device_config_buffer.ConfigBufferSubject;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.network.NetworkDeviceHost;
import bt7s7k7.supervisory.network.RemoteValueReactiveDependency;
import bt7s7k7.supervisory.support.LogEventRouter;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.support.InputDocument;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;

public class ScriptedSystemHost extends BlockEntityComponent implements Configurable<ScriptedSystemHost.Configuration, ScriptedSystemHost.ConfigurationUpdate>, ConfigBufferSubject {
	public final NetworkDeviceHost networkDeviceHost;

	// This array is only used on the client. It is unique per player and is not saved.
	public final ArrayList<String> commandHistory = new ArrayList<>();

	public static record ScopeInitializationEvent(ScriptedSystem system) {
		public Realm getRealm() {
			return this.system.getRealm();
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

		if (this.system != null) {
			this.system.clear();
		}
	}

	public ScriptedSystemHost(CompositeBlockEntity entity) {
		super(entity);

		this.networkDeviceHost = entity.ensureComponent(NetworkDeviceHost.class, NetworkDeviceHost::new);

		this.connect(this.networkDeviceHost.onInitializeDevice, event -> {
			var savedState = event.oldDevice();
			var device = event.device();

			if (savedState != null) {
				device.state.putAll(savedState.state);
				device.cache.putAll(savedState.cache);
			}

			if (!this.configuration.domain.isEmpty()) {
				device.domain = this.configuration.domain;
			}

			this.system.clear();
			this.system.executeCode(this.configuration.getModuleDocuments());

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

		this.connect(this.networkDeviceHost.onDeviceInitialized, device -> {
			if (device.isConnected()) {
				this.log(Component.literal("Connected to domain: " + device.domain).withStyle(ChatFormatting.BLUE));
			}
		});

		this.connect(this.networkDeviceHost.onNetworkUpdate, event -> {
			var key = event.key();
			var value = event.value();

			var reactivityManager = this.system.reactivityManager;
			if (reactivityManager == null) return;

			var dependency = RemoteValueReactiveDependency.get(reactivityManager, key, value);
			dependency.updateValue(value);
		});

		NeoForge.EVENT_BUS.post(new ScriptedSystemInitializationEvent(this, this::connect));
	}

	protected String desiredDomain = null;

	public static class Configuration {
		public String domain = "";
		public ArrayList<String> modules = new ArrayList<>();
		public ArrayList<Component> log = new ArrayList<>();

		public List<InputDocument> getModuleDocuments() {
			var result = new ArrayList<InputDocument>(this.modules.size());

			for (int i = 0; i < this.modules.size(); i++) {
				result.add(new InputDocument("module" + i, this.modules.get(i)));
			}

			return result;
		}

		public Configuration(String domain, List<String> modules, List<Component> log) {
			this.domain = domain;
			this.modules = modules instanceof ArrayList<String> arrayList ? arrayList : new ArrayList<>(modules);
			this.log = log instanceof ArrayList<Component> arrayList ? arrayList : new ArrayList<>(log);
		}

		public Configuration() {};

		@Override
		public Configuration clone() {
			try {
				return (Configuration) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String toString() {
			return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow().toString();
		}

		public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
				Codec.STRING.fieldOf("domain").orElse("").forGetter(v -> v.domain),
				Codec.list(Codec.STRING).fieldOf("modules").orElse(Collections.emptyList()).forGetter(v -> v.modules),
				Codec.list(ComponentSerialization.CODEC).fieldOf("log").orElse(Collections.emptyList()).forGetter(v -> v.log))
				.apply(instance, Configuration::new)));
	}

	public static class ConfigurationUpdate {
		public Optional<String> command;
		public Optional<String> domain;
		public Optional<List<String>> modules;

		public ConfigurationUpdate(Optional<String> command, Optional<String> domain, Optional<List<String>> modules) {
			this.command = command;
			this.domain = domain;
			this.modules = modules;
		}

		public ConfigurationUpdate() {
			this.command = Optional.empty();
			this.domain = Optional.empty();
			this.modules = Optional.empty();
		}

		public static final Codec<ConfigurationUpdate> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
				Codec.STRING.optionalFieldOf("command").forGetter(v -> v.command),
				Codec.STRING.optionalFieldOf("domain").forGetter(v -> v.domain),
				Codec.list(Codec.STRING).optionalFieldOf("modules").forGetter(v -> v.modules))
				.apply(instance, ConfigurationUpdate::new)));
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

		LogEventRouter.sendLogEvent(this.entity.getLevel(), this.entity.getBlockPos(), message);
		this.entity.setChanged();
	}

	public Configuration configuration = new Configuration();
	public ScriptedSystem system = new ScriptedSystem(this);

	@Override
	public void write(CompoundTag tag, Provider registries) {
		var result = (CompoundTag) Configuration.CODEC.encodeStart(NbtOps.INSTANCE, this.configuration).getOrThrow();
		tag.merge(result);
	}

	@Override
	public void read(CompoundTag tag, Provider registries) {
		Configuration.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(configuration -> {
			this.configuration = configuration;
		});

		// Migration
		if (tag.contains("code")) {
			this.configuration.modules.add(tag.getString("code"));
		}
	}

	@Override
	public Configuration getConfiguration() {
		return this.configuration;
	}

	public void executeCommand(String command) {
		this.system.executeCommand(command);
	}

	@Override
	public void updateConfiguration(ConfigurationUpdate configuration) {
		if (configuration.modules.isPresent()) {
			this.configuration.modules.clear();
			this.configuration.modules.addAll(configuration.modules.get());
			this.configuration.log.clear();
			this.system.clear();
			this.entity.setChanged();
		}

		if (configuration.command.isPresent()) {
			var command = configuration.command.get();
			this.log(Component.literal("> ").withStyle(ChatFormatting.GREEN).append(Component.literal(command).withStyle(ChatFormatting.BLUE)));
			Supervisory.LOGGER.debug("Executed command: " + command);
			this.executeCommand(command);
		}

		if (configuration.domain.isPresent()) {
			this.configuration.domain = configuration.domain.get();
			this.system.clear();
			this.entity.setChanged();
		}
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
	public Codec<ConfigurationUpdate> getUpdateCodec() {
		return ConfigurationUpdate.CODEC;
	}

	@Override
	public void tick() {
		if (!this.pendingLogEntries.isEmpty()) {
			for (var entry : this.pendingLogEntries) {
				this.log(entry);
			}

			this.pendingLogEntries.clear();
		}

		if (this.system.isEmpty()) {
			this.networkDeviceHost.setDevice(new NetworkDevice(""), true);
		}

		this.system.processTasks();
	}

	@Override
	public BufferContent getConfigBuffer() {
		var config = new Configuration(this.configuration.domain, this.configuration.modules, Collections.emptyList());

		var value = (CompoundTag) Configuration.CODEC.encodeStart(NbtOps.INSTANCE, config).getOrThrow();
		return new BufferContent(this.configuration.domain, this.getDeviceType(), value);
	}

	@Override
	public ResourceLocation getDeviceType() {
		var block = this.entity.getBlockState().getBlock();
		return BuiltInRegistries.BLOCK.getKeyOrNull(block);
	}

	@Override
	public void applyDomain(String domain) {
		var update = new ConfigurationUpdate();
		update.domain = Optional.of(domain);
		this.updateConfiguration(update);
	}

	@Override
	public void applyConfig(BufferContent config) {
		Configuration.CODEC.parse(NbtOps.INSTANCE, config.value()).ifSuccess(configuration -> {
			var update = new ConfigurationUpdate(Optional.empty(), Optional.of(configuration.domain), Optional.of(configuration.modules));
			this.updateConfiguration(update);
		});
	}
}
