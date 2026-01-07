package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.configuration.Configurable;
import bt7s7k7.supervisory.items.device_config_buffer.BufferContent;
import bt7s7k7.supervisory.items.device_config_buffer.ConfigBufferSubject;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.network.NetworkDeviceHost;
import bt7s7k7.supervisory.redstone.RedstoneState;
import bt7s7k7.supervisory.sockets.SocketProvider;
import bt7s7k7.supervisory.support.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.loading.FMLLoader;

public class IOManager extends BlockEntityComponent implements Configurable<IOManager.Configuration, IOManager.Configuration>, ConfigBufferSubject {
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

	public enum SideType {
		INPUT, OUTPUT, SOCKET;

		public static final Codec<SideType> CODEC = Codec.stringResolver(SideType::toString, SideType::valueOf);
	}

	public static class SideConfiguration implements Cloneable {
		public String name = "";
		public SideType type = SideType.SOCKET;

		public SideConfiguration(String name, SideType type) {
			this.name = name;
			this.type = type;
		}

		public SideConfiguration() {}

		public static final Codec<SideConfiguration> CODEC = RecordCodecBuilder.create(instance -> (instance.group(
				Codec.STRING.fieldOf("name").orElse("").forGetter(v -> v.name),
				SideType.CODEC.fieldOf("type").orElse(SideType.SOCKET).forGetter(v -> v.type))
				.apply(instance, SideConfiguration::new)));

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
			result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (this.getClass() != obj.getClass()) return false;
			SideConfiguration other = (SideConfiguration) obj;
			if (this.name == null) {
				if (other.name != null) return false;
			} else if (!this.name.equals(other.name)) return false;
			if (this.type != other.type) return false;
			return true;
		}
	}

	public static class Configuration implements Cloneable {
		public String domain;
		public SideConfiguration[] sides;

		public Configuration(String domain, SideConfiguration[] sides) {
			this.domain = domain;
			this.sides = sides;
		}

		public Configuration(String domain, List<SideConfiguration> sides) {
			this.domain = domain;
			this.sides = new SideConfiguration[6];

			if (sides.size() == 6) {
				for (int i = 0; i < this.sides.length; i++) {
					this.sides[i] = sides.get(i);
				}
			} else {
				for (int i = 0; i < this.sides.length; i++) {
					this.sides[i] = new SideConfiguration();
				}
			}
		}

		public Configuration() {
			this.domain = "";
			this.sides = new SideConfiguration[6];

			for (int i = 0; i < this.sides.length; i++) {
				this.sides[i] = new SideConfiguration();
			}
		};

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
				SideConfiguration.CODEC.listOf().fieldOf("sides").orElse(Collections.emptyList()).forGetter(v -> Arrays.asList(v.sides)))
				.apply(instance, Configuration::new)));

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.domain == null) ? 0 : this.domain.hashCode());
			result = prime * result + Arrays.hashCode(this.sides);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (this.getClass() != obj.getClass()) return false;
			Configuration other = (Configuration) obj;
			if (this.domain == null) {
				if (other.domain != null) return false;
			} else if (!this.domain.equals(other.domain)) return false;
			if (!Arrays.equals(this.sides, other.sides)) return false;
			return true;
		}
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

		for (var i = 0; i < this.configuration.sides.length; i++) {
			var side = this.configuration.sides[i];
			if (side == null || side.name.isBlank()) continue;

			var direction = Side.values()[i].getDirection(front);
			this.entity.addComponent(switch (side.type) {
				case INPUT -> new IOComponent.RedstoneInput(this.entity, side.name, direction);
				case OUTPUT -> new IOComponent.RedstoneOutput(this.entity, side.name, direction);
				case SOCKET -> new SocketProvider(this.entity, side.name, direction);
			});
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
	public void updateConfiguration(Configuration configuration) {
		if (this.networkDeviceHost.hasDevice()
				&& configuration.domain.equals(this.networkDeviceHost.getDevice().domain)
				&& configuration.equals(this.configuration)) {
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

	@Override
	public Codec<Configuration> getUpdateCodec() {
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

	@Override
	public BufferContent getConfigBuffer() {
		var config = (CompoundTag) Configuration.CODEC.encodeStart(NbtOps.INSTANCE, this.configuration).getOrThrow();
		return new BufferContent(this.configuration.domain, this.getDeviceType(), config);
	}

	@Override
	public ResourceLocation getDeviceType() {
		var block = this.entity.getBlockState().getBlock();
		return BuiltInRegistries.BLOCK.getKeyOrNull(block);
	}

	@Override
	public void applyDomain(String domain) {
		var configuration = this.configuration.clone();
		configuration.domain = domain;
		this.updateConfiguration(configuration);
	}

	@Override
	public void applyConfig(BufferContent config) {
		Configuration.CODEC.parse(NbtOps.INSTANCE, config.value()).ifSuccess(configuration -> {
			this.updateConfiguration(configuration);
		});
	}
}
