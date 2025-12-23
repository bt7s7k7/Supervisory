package bt7s7k7.supervisory.items.device_config_buffer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record BufferContent(String domain, ResourceLocation source, CompoundTag value) {
	public static final Codec<BufferContent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("domain").forGetter(BufferContent::domain),
			ResourceLocation.CODEC.fieldOf("source").forGetter(BufferContent::source),
			CompoundTag.CODEC.fieldOf("value").forGetter(BufferContent::value))
			.apply(instance, BufferContent::new));

	public static StreamCodec<ByteBuf, BufferContent> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
