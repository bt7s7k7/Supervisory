package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.configuration.Configurable;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.network.NetworkDeviceHost;
import bt7s7k7.supervisory.redstone.RedstoneState;
import bt7s7k7.supervisory.sockets.SocketProvider;
import bt7s7k7.supervisory.support.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.loading.FMLLoader;

public class IOManager extends BlockEntityComponent implements Configurable<IOManager.Configuration> {
	protected final NetworkDeviceHost networkDeviceHost;

	@Override
	public EventPriority priority() {
		// Make sure our configuration is loaded before NetworkDevice, so we can set the domain,
		// setup subscriptions and publish resources before we are connected
		return EventPriority.HIGH;
	}

	public IOManager(CompositeBlockEntity entity) {
		super(entity);

		this.networkDeviceHost = entity.ensureComponent(NetworkDeviceHost.class, NetworkDeviceHost::new);
		entity.ensureComponent(RedstoneState.class, RedstoneState::new);
	}

	public static class Configuration implements Cloneable {
		public String domain = "";
		public String input = "";
		public String output = "";
		public String socket = "";

		public Configuration(String domain, String input, String output, String storage) {
			this.domain = domain;
			this.input = input;
			this.output = output;
			this.socket = storage;
		}

		public Configuration() {};

		@Override
		public String toString() {
			return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow().toString();
		}

		@Override
		public Configuration clone() {
			try {
				return (Configuration) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
		}

		public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
				Codec.STRING.fieldOf("domain").orElse("").forGetter(v -> v.domain),
				Codec.STRING.fieldOf("input").orElse("").forGetter(v -> v.input),
				Codec.STRING.fieldOf("output").orElse("").forGetter(v -> v.output),
				Codec.STRING.fieldOf("socket").orElse("").forGetter(v -> v.socket))
				.apply(instance, Configuration::new)));
	}

	protected Configuration configuration = new Configuration();

	protected static List<Map.Entry<Direction, String>> parseLinkage(Direction front, String linkage) {
		var result = new ArrayList<Map.Entry<Direction, String>>();

		for (var input : linkage.split(",")) {
			input = input.trim();
			if (input.isEmpty()) continue;

			var tokens = input.split(":", 2);
			if (tokens.length == 1) {
				for (var side : Side.values()) {
					var direction = side.getDirection(front);
					result.add(new AbstractMap.SimpleEntry<>(direction, tokens[0] + "." + side.name));
					continue;
				}
			}

			var sideToken = tokens[0].trim();
			var side = Side.getByName(sideToken);
			if (side != null) {
				var direction = side.getDirection(front);
				result.add(new AbstractMap.SimpleEntry<>(direction, tokens[1].trim()));
				continue;
			}

			var direction = Direction.byName(sideToken);
			if (direction != null) {
				result.add(new AbstractMap.SimpleEntry<>(direction, tokens[1].trim()));
				continue;
			}
		}

		return result;
	}

	protected void updateIOComponents() {
		this.entity.deleteComponents(IOComponent.class::isInstance);

		var front = this.entity.getFront();

		for (var kv : parseLinkage(front, this.configuration.input)) {
			this.entity.addComponent(new IOComponent.RedstoneInput(this.entity, kv.getValue(), kv.getKey()));
		}

		for (var kv : parseLinkage(front, this.configuration.output)) {
			this.entity.addComponent(new IOComponent.RedstoneOutput(this.entity, kv.getValue(), kv.getKey()));
		}

		for (var kv : parseLinkage(front, this.configuration.socket)) {
			this.entity.addComponent(new SocketProvider(this.entity, kv.getValue(), kv.getKey()));
		}
	}

	@Override
	public void read(CompoundTag tag, HolderLookup.Provider registries) {
		super.read(tag, registries);

		// Migration
		if (tag.contains("storage") && !tag.contains("socket")) {
			tag.put("socket", tag.get("storage"));
		}

		Configuration.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(configuration -> {
			this.configuration = configuration;
			this.updateIOComponents();
		});
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries) {
		super.write(tag, registries);

		var result = (CompoundTag) Configuration.CODEC.encodeStart(NbtOps.INSTANCE, this.configuration).getOrThrow();
		result.remove("domain");
		tag.merge(result);
	}

	@Override
	public Configuration getConfiguration() {
		this.configuration.domain = this.networkDeviceHost.hasDevice() ? this.networkDeviceHost.getDevice().domain : "";
		return this.configuration;
	}

	@Override
	public void setConfiguration(Configuration configuration) {
		if ((this.networkDeviceHost.hasDevice() && configuration.domain == this.networkDeviceHost.getDevice().domain)
				&& configuration.input == this.configuration.input
				&& configuration.output == this.configuration.output) {
			return;
		}

		this.configuration = configuration;
		this.updateIOComponents();
		this.networkDeviceHost.setDevice(new NetworkDevice(configuration.domain), true);
		this.entity.setChanged();
	}

	@Override
	public Codec<Configuration> getConfigurationCodec() {
		return Configuration.CODEC;
	}

	@OnlyIn(Dist.CLIENT)
	protected void displayScreen(Configuration configuration) {
		Minecraft.getInstance().setScreen(new RemoteTerminalUnitScreen(this, configuration));
	}

	@Override
	public void openConfigurationScreen(Configuration configuration) {
		if (FMLLoader.getDist() == Dist.CLIENT) {
			this.displayScreen(configuration);
		}
	}
}
