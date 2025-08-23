package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.smartRedstoneComponent.SmartRedstoneComponentBlockEntity;
import bt7s7k7.supervisory.configuration.Configurable;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.support.RelativeDirection;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;

public class RemoteTerminalUnitBlockEntity extends SmartRedstoneComponentBlockEntity implements Configurable<RemoteTerminalUnitBlockEntity.Configuration> {
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

	public NetworkDevice device = null;
	public Configuration configuration = new Configuration();

	public void setDevice(NetworkDevice device, boolean fresh) {
		if (this.device != null) {
			this.device.disconnect();
		}
		this.device = device;

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

		device.onUpdate = (key, value) -> {
			if (value instanceof Primitive.Number numberValue && !this.configuration.input.isEmpty()) {
				for (var direction : RelativeDirection.values()) {
					var name = this.configuration.input + "." + direction.name;
					if (name.equals(key)) {
						this.setOutput(direction.getAbsolute(this.getFront()), (int) numberValue.value);
					}
				}
			}
		};

		device.connect();
		if (fresh) {
			device.sendUpdateRequests();
		}
	}

	public NetworkDevice getDevice() {
		if (this.device == null) {
			this.device = new NetworkDevice("");
		}

		return this.device;
	}

	protected void teardownDevice() {
		if (this.device != null) {
			this.device.disconnect();
		}
	}

	@Override
	public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);

		Configuration.CODEC.parse(NbtOps.INSTANCE, tag).ifSuccess(configuration -> {
			this.configuration = configuration;
		});

		if (tag.contains("device")) {
			NetworkDevice.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("device")).ifSuccess(device -> {
				this.setDevice(device, false);
			}).ifError(error -> {
				Supervisory.LOGGER.error("Failed to load NetworkDevice for RemoteTerminalUnitBlockEntity at " + this.worldPosition, error);
			});
		}
	}

	@Override
	public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);

		tag.merge((CompoundTag) Configuration.CODEC.encodeStart(NbtOps.INSTANCE, this.configuration).getOrThrow());
		tag.remove("domain");

		if (this.device != null) {
			tag.put("device", NetworkDevice.CODEC.encodeStart(NbtOps.INSTANCE, this.device).getOrThrow());
		}
	}

	public void tick(Level level, BlockPos pos, BlockState state) {
		if (this.device != null) {
			this.device.sendPendingUpdates();
		}
	}

	@Override
	public Configuration getConfiguration() {
		this.configuration.domain = this.device == null ? "" : this.device.domain;
		return this.configuration;
	}

	@Override
	public void setConfiguration(Configuration configuration) {
		if ((this.device != null && configuration.domain == this.device.domain)
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
	public void setRemoved() {
		super.setRemoved();
		this.teardownDevice();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		this.teardownDevice();
	}

	protected Direction getFront() {
		return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
	}

	@Override
	protected void handleRedstoneInputChange(Direction direction, int strength) {
		if (this.configuration.output.isEmpty()) return;
		if (this.device == null) return;
		var relativeDirection = RelativeDirection.from(this.getFront(), direction);
		this.device.updateLocalResource(this.configuration.output + "." + relativeDirection.name, Primitive.from(strength));
	}

}
