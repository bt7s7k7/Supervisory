package bt7s7k7.supervisory.configuration;

import bt7s7k7.supervisory.Supervisory;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigurationRequest(BlockPos target, CompoundTag configuration) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ConfigurationRequest> TYPE = new CustomPacketPayload.Type<>(Supervisory.resource("configuration_request"));

	public static final StreamCodec<ByteBuf, ConfigurationRequest> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			ConfigurationRequest::target,
			ByteBufCodecs.COMPOUND_TAG,
			ConfigurationRequest::configuration,
			ConfigurationRequest::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
