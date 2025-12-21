package bt7s7k7.supervisory.sensor;

import java.util.List;
import java.util.function.Supplier;

import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.supervisory.network.NetworkDevice;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.sockets.SocketConnectionManager;
import bt7s7k7.supervisory.system.ScriptedSystemIntegration;
import bt7s7k7.treeburst.runtime.ManagedObject;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Primitive;

public class SensorAPI extends LazyTable implements ScriptedSystemIntegration { // @symbol: Sensor
	// @summary: Allows you to read block states in the world.
	protected final SocketConnectionManager<Sensor, SensorReactiveDependency> connectionManager;

	public SensorAPI(ManagedObject prototype, Realm realm, CompositeBlockEntity entity, ReactivityManager reactivityManager, Supplier<NetworkDevice> networkDeviceSupplier) {
		super(prototype, realm);

		this.connectionManager = new SocketConnectionManager<Sensor, SensorReactiveDependency>("sensor", 5, entity, reactivityManager, networkDeviceSupplier) {
			@Override
			protected void processDependency(SensorReactiveDependency dependency, Sensor capability) {
				// Sensors are event based, so no processing required
			}

			@Override
			protected SensorReactiveDependency makeHandle(String name) {
				return new SensorReactiveDependency(this.reactivityManager, name);
			}
		};
	}

	@Override
	protected void initialize() {
		this.declareProperty("connect", NativeFunction.simple(this.realm, List.of("target"), List.of(Primitive.String.class), (args, scope, result) -> {
			// @summary[[Connects to a sensor, that will scan for a block. The
			// `target` can be either a side of this system or a name of a socket on the network.
			// This function returns a {@link SensorReactiveDependency} that can watched for
			// changes using the {@link reactive} function.]]
			var target = args.get(0).getStringValue();

			var dependency = this.connectionManager.connect(target);
			result.value = dependency.getHandle();
			return;
		}));
	}

	@Override
	public void teardown() {
		for (var dependency : this.connectionManager.getHandles()) {
			dependency.reset();
		}
	}

	@Override
	public void tick() {
		if (!this.initialized) return;
		this.connectionManager.tick();
	}
}
