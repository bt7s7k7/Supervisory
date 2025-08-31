package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;

public class RemoteTerminalUnitBlockEntity extends DirectControlDeviceBlockEntity implements Configurable<RemoteTerminalUnitBlockEntity.Configuration> {
	public RemoteTerminalUnitBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
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
	protected void initializeNetworkDevice(NetworkDevice device, boolean fresh) {
		if (fresh) {
			if (!this.configuration.output.isEmpty()) {
				for (var direction : Direction.values()) {
					this.handleRedstoneInputChange(direction, this.getInput(direction));
				}
			}
		}

		if (!this.configuration.input.isEmpty()) {
			for (var direction : RelativeDirection.values()) {
				device.subscribe(this.configuration.input + "." + direction.name);
			}
		}
	}

	@Override
	protected void handleNetworkUpdate(String key, ManagedValue value) {
		if (value instanceof Primitive.Number numberValue && !this.configuration.input.isEmpty()) {
			for (var direction : RelativeDirection.values()) {
				var name = this.configuration.input + "." + direction.name;
				if (name.equals(key)) {
					this.setOutput(direction.getAbsolute(this.getFront()), (int) numberValue.value);
				}
			}
		}
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);

		Configuration.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(configuration -> {
			this.configuration = configuration;
		});
	}

	@Override
	public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);

		var result = (CompoundTag) Configuration.CODEC.encodeStart(NbtOps.INSTANCE, this.configuration).getOrThrow();
		result.remove("domain");
		tag.merge(result);
	}

	@Override
	public Configuration getConfiguration() {
		this.configuration.domain = this.hasDevice() ? this.getDevice().domain : "";
		return this.configuration;
	}

	@Override
	public void setConfiguration(Configuration configuration) {
		if ((this.hasDevice() && configuration.domain == this.getDevice().domain)
				&& configuration.input == this.configuration.input
				&& configuration.output == this.configuration.output) {
			return;
		}

		this.configuration = configuration;
		this.setDevice(new NetworkDevice(configuration.domain), true);
		this.setChanged();
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

	@Override
	protected void handleRedstoneInputChange(Direction direction, int strength) {
		if (this.configuration.output.isEmpty()) return;
		if (!this.hasDevice()) return;
		var relativeDirection = RelativeDirection.from(this.getFront(), direction);
		this.getDevice().publishResource(this.configuration.output + "." + relativeDirection.name, Primitive.from(strength));
	}
}
