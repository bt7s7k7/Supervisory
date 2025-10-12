package bt7s7k7.supervisory.configuration;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public interface Configurable<TConfiguration, TUpdate> {
	public TConfiguration getConfiguration();

	public void updateConfiguration(TUpdate configuration);

	public Codec<TConfiguration> getConfigurationCodec();

	public Codec<TUpdate> getUpdateCodec();

	public void openConfigurationScreen(TConfiguration configuration);

	default CompoundTag getConfigurationPayload() {
		var configuration = this.getConfiguration();
		var codec = this.getConfigurationCodec();
		var payloadData = (CompoundTag) codec.encodeStart(NbtOps.INSTANCE, configuration).getOrThrow();
		return payloadData;
	}

	default CompoundTag getUpdatePayload(TUpdate update) {
		var codec = this.getUpdateCodec();
		var payloadData = (CompoundTag) codec.encodeStart(NbtOps.INSTANCE, update).getOrThrow();
		return payloadData;
	}
}
