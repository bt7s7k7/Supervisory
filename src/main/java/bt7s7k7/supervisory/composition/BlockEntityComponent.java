package bt7s7k7.supervisory.composition;

import java.util.ArrayList;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;

public class BlockEntityComponent {
	public final CompositeBlockEntity entity;

	private final ArrayList<ComponentSignal<?>> signals = new ArrayList<>();

	public BlockEntityComponent(CompositeBlockEntity entity) {
		this.entity = entity;
	}

	public EventPriority priority() {
		return EventPriority.NORMAL;
	}

	public void initialize() {}

	public void tick() {}

	public void teardown() {
		for (var signal : this.signals) {
			signal.invalidate();
		}

		this.signals.clear();
	}

	public void read(CompoundTag tag, Provider registries) {}

	public void write(CompoundTag tag, Provider registries) {}

	public void handleNeighbourChange(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {}

	protected <T> ComponentSignal<T> connect(ComponentSignal.Emitter<T> emitter, Consumer<T> handler) {
		var signal = new ComponentSignal<>(handler);
		emitter.connect(signal);
		this.signals.add(signal);
		return signal;
	}

	@FunctionalInterface
	public interface EventConnectionDelegate {
		<T> ComponentSignal<T> connect(ComponentSignal.Emitter<T> emitter, Consumer<T> handler);
	}
}
