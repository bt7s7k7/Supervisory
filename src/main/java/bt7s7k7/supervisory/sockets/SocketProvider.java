package bt7s7k7.supervisory.sockets;

import bt7s7k7.supervisory.blocks.remoteTerminalUnit.IOComponent;
import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.network.NetworkDeviceHost;
import bt7s7k7.supervisory.network.NetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class SocketProvider extends BlockEntityComponent implements NetworkService, IOComponent {
	protected String name;
	protected Direction direction;
	protected boolean valid;

	public SocketProvider(CompositeBlockEntity entity, String name, Direction direction) {
		super(entity);
		this.name = name;
		this.direction = direction;

		var deviceHost = entity.ensureComponent(NetworkDeviceHost.class, NetworkDeviceHost::new);

		this.connect(deviceHost.onInitializeNetworkDevice, event -> {
			event.device().addService(this);
		});
	}

	@Override
	public boolean isValid() {
		return this.valid;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public Direction getDirection() {
		return this.direction;
	}

	public BlockPos getTarget() {
		return this.entity.getBlockPos().relative(this.direction);
	}

	@Override
	public void teardown() {
		super.teardown();
		this.valid = false;
	}
}
