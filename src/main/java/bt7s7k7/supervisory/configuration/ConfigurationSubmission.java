package bt7s7k7.supervisory.configuration;

import bt7s7k7.supervisory.Supervisory;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigurationSubmission(BlockPos target, CompoundTag configuration) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ConfigurationSubmission> TYPE = new CustomPacketPayload.Type<>(Supervisory.resource("configuration_submit"));

	public static final StreamCodec<ByteBuf, ConfigurationSubmission> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			ConfigurationSubmission::target,
			ByteBufCodecs.COMPOUND_TAG,
			ConfigurationSubmission::configuration,
			ConfigurationSubmission::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
