package bt7s7k7.supervisory.configuration;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public interface Configurable<TConfiguration> {
	public TConfiguration getConfiguration();

	public void setConfiguration(TConfiguration configuration);

	public Codec<TConfiguration> getConfigurationCodec();

	public void openConfigurationScreen(TConfiguration configuration);

	default CompoundTag getPayloadData(TConfiguration configuration) {
		var codec = this.getConfigurationCodec();
		var payloadData = (CompoundTag) codec.encodeStart(NbtOps.INSTANCE, configuration).getOrThrow();
		return payloadData;
	}

	default CompoundTag getPayloadData() {
		var configuration = this.getConfiguration();
		return this.getPayloadData(configuration);
	}
}
