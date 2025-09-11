package bt7s7k7.supervisory.blocks.remoteTerminalUnit;

import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.network.NetworkDeviceHost;
import bt7s7k7.supervisory.redstone.RedstoneState;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.Direction;

public interface IOComponent {
	public static class RedstoneInput extends BlockEntityComponent implements IOComponent {
		public final String name;
		public final Direction direction;

		public RedstoneInput(CompositeBlockEntity entity, String name, Direction direction) {
			super(entity);
			this.name = name;
			this.direction = direction;

			var networkDeviceHost = entity.ensureComponent(NetworkDeviceHost.class, NetworkDeviceHost::new);
			var redstone = entity.ensureComponent(RedstoneState.class, RedstoneState::new);

			this.connect(networkDeviceHost.onInitializeDevice, event -> {
				if (event.fresh()) {
					event.device().publishResource(this.name, Primitive.from(redstone.getInput(this.direction)));
				}
			});

			this.connect(redstone.onRedstoneInputChanged, event -> {
				if (event.direction() != this.direction) return;
				if (!networkDeviceHost.hasDevice()) return;

				networkDeviceHost.getDevice().publishResource(this.name, Primitive.from(event.strength()));
			});
		}
	}

	public static class RedstoneOutput extends BlockEntityComponent implements IOComponent {
		public final String name;
		public final Direction direction;

		public RedstoneOutput(CompositeBlockEntity entity, String name, Direction direction) {
			super(entity);
			this.name = name;
			this.direction = direction;

			var networkDeviceHost = entity.ensureComponent(NetworkDeviceHost.class, NetworkDeviceHost::new);
			var redstone = entity.ensureComponent(RedstoneState.class, RedstoneState::new);

			this.connect(networkDeviceHost.onInitializeDevice, event -> {
				event.device().subscribe(this.name);
			});

			this.connect(networkDeviceHost.onNetworkUpdate, event -> {
				if (event.key().equals(this.name) && event.value() instanceof Primitive.Number strength) {
					redstone.setOutput(this.direction, (int) strength.value);
				}
			});
		}
	}
}
