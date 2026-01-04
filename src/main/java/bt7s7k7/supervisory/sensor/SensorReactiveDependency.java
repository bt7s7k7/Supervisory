package bt7s7k7.supervisory.sensor;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.sockets.SocketBasedDependency;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class SensorReactiveDependency extends SocketBasedDependency<Sensor> { // @symbol: Sensor.SensorReactiveDependency
	// @prototype: ReactiveDependency.prototype
	// @summary: Triggers each time the scanned block changes. The value is a {@link Sensor.SensorReading} which contains the block state.
	public Sensor cachedSensor = null;

	public SensorReactiveDependency(ReactivityManager owner, String name) {
		super(owner, name);
	}

	@Override
	public void reset() {
		super.reset();

		if (this.cachedSensor != null) {
			this.cachedSensor.entity.addComponent(this.cachedSensor);
			this.cachedSensor = null;
			Supervisory.LOGGER.info("Sensor torn down");
		}
	}

	@Override
	public Sensor tryGetCachedCapability() {
		return this.cachedSensor;
	}

	@Override
	public Sensor tryAcquireCapability(ServerLevel level, BlockPos position) {
		var entity = this.provider.entity;
		var sensor = entity.addComponent(new Sensor(entity, position, this));

		this.cachedSensor = sensor;

		sensor.update();

		return sensor;
	}
}
