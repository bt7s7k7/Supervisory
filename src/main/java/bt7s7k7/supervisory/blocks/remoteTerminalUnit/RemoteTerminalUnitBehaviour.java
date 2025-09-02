package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.configuration.Configurable;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.network.NetworkDeviceHost;
import bt7s7k7.supervisory.redstone.RedstoneStateComponent;
import bt7s7k7.supervisory.support.Side;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;

public class RemoteTerminalUnitBehaviour extends BlockEntityComponent implements Configurable<RemoteTerminalUnitBehaviour.Configuration> {
	public final NetworkDeviceHost deviceHost;
	public final RedstoneStateComponent redstone;

	public RemoteTerminalUnitBehaviour(CompositeBlockEntity entity) {
		super(entity);

		this.deviceHost = entity.useComponent(NetworkDeviceHost.class, NetworkDeviceHost::new);
		this.redstone = entity.useComponent(RedstoneStateComponent.class, RedstoneStateComponent::new);

		this.connect(this.deviceHost.onInitializeNetworkDevice, event -> {
			var device = event.device();
			var fresh = event.fresh();

			if (fresh) {
				if (!this.configuration.output.isEmpty()) {
					for (var direction : Direction.values()) {
						this.handleRedstoneInputChange(direction, this.redstone.getInput(direction));
					}
				}
			}

			if (!this.configuration.input.isEmpty()) {
				for (var direction : Side.values()) {
					device.subscribe(this.configuration.input + "." + direction.name);
				}
			}
		});

		this.connect(this.deviceHost.onNetworkUpdate, event -> {
			var value = event.value();
			var key = event.key();

			if (value instanceof Primitive.Number numberValue && !this.configuration.input.isEmpty()) {
				for (var direction : Side.values()) {
					var name = this.configuration.input + "." + direction.name;
					if (name.equals(key)) {
						this.redstone.setOutput(direction.getDirection(this.entity.getFront()), (int) numberValue.value);
					}
				}
			}
		});

		this.connect(this.redstone.onRedstoneInputChanged, event -> {
			this.handleRedstoneInputChange(event.direction(), event.strength());
		});
	}

	private void handleRedstoneInputChange(Direction direction, int strength) {
		if (this.configuration.output.isEmpty()) return;
		if (!this.deviceHost.hasDevice()) return;
		var relativeDirection = Side.from(this.entity.getFront(), direction);
		this.deviceHost.getDevice().publishResource(this.configuration.output + "." + relativeDirection.name, Primitive.from(strength));
	}

	public static class Configuration implements Cloneable {
		public String domain = "";
		public String input = "";
		public String output = "";

		public Configuration(String domain, String input, String output) {
			this.domain = domain;
			this.input = input;
			this.output = output;
		}

		public Configuration() {};

		@Override
		public String toString() {
			return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow().toString();
		}

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
				Codec.STRING.fieldOf("output").orElse("").forGetter(v -> v.output))
				.apply(instance, Configuration::new)));
	}

	public Configuration configuration = new Configuration();

	@Override
	public void read(CompoundTag tag, HolderLookup.Provider registries) {
		super.read(tag, registries);

		Configuration.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(configuration -> {
			this.configuration = configuration;
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
		this.configuration.domain = this.deviceHost.hasDevice() ? this.deviceHost.getDevice().domain : "";
		return this.configuration;
	}

	@Override
	public void setConfiguration(Configuration configuration) {
		if ((this.deviceHost.hasDevice() && configuration.domain == this.deviceHost.getDevice().domain)
				&& configuration.input == this.configuration.input
				&& configuration.output == this.configuration.output) {
			return;
		}

		this.configuration = configuration;
		this.deviceHost.setDevice(new NetworkDevice(configuration.domain), true);
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
			displayScreen(configuration);
		}
	}
}
