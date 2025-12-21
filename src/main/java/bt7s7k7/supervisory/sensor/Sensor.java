package bt7s7k7.supervisory.sensor;

import java.util.Objects;

import bt7s7k7.supervisory.composition.BlockEntityComponent;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class Sensor extends BlockEntityComponent {
	public final BlockPos position;
	public final SensorReactiveDependency dependency;

	protected SensorReading reading;

	public Sensor(CompositeBlockEntity entity, BlockPos position, SensorReactiveDependency dependency) {
		super(entity);

		this.position = position;
		this.dependency = dependency;
	}

	@Override
	public void handleNeighbourChange(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		if (!fromPos.equals(this.position)) return;
		this.update();
	}

	public void update() {
		var level = this.entity.getLevel();
		var blockState = level.getBlockState(this.position);
		var entity = blockState.hasBlockEntity() ? level.getBlockEntity(this.position) : null;
		var newReading = new SensorReading(blockState, entity);

		if (Objects.equals(newReading, this.reading)) return;

		this.reading = newReading;
		this.dependency.updateValue(newReading.blockState.isEmpty() ? Primitive.NULL : SensorReading.WRAPPER.getHandle(newReading, this.dependency.owner.realm));
	}

}
