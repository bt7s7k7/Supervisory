package bt7s7k7.supervisory.items.device_config_buffer;

import net.minecraft.resources.ResourceLocation;

public interface ConfigBufferSubject {
	BufferContent getConfigBuffer();

	ResourceLocation getDeviceType();

	void applyDomain(String domain);

	void applyConfig(BufferContent config);
}
