package bt7s7k7.supervisory.composition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.configuration.Configurable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CompositeBlockEntity extends BlockEntity implements Configurable<Object, Object> {
	protected final ArrayList<BlockEntityComponent> components = new ArrayList<>();

	protected boolean failed = false;

	public CompositeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}

	public <T extends BlockEntityComponent> T addComponent(T component) {
		this.components.add(component);
		this.components.sort(Comparator.comparing(BlockEntityComponent::priority));
		component.initialize();
		return component;
	}

	public boolean deleteComponent(BlockEntityComponent component) {
		var success = this.components.remove(component);
		if (!success) return false;

		component.teardown();
		return true;
	}

	public <T extends BlockEntityComponent> T ensureComponent(Class<T> type, Function<CompositeBlockEntity, T> ctor) {
		var existing = this.components.stream().filter(type::isInstance).findFirst();
		if (existing.isPresent()) return type.cast(existing.get());

		var instance = ctor.apply(this);
		this.components.add(instance);
		this.components.sort(Comparator.comparing(BlockEntityComponent::priority));
		instance.initialize();
		return instance;
	}

	public <T> Optional<T> getComponent(Class<T> type) {
		return this.components.stream().filter(type::isInstance).map(type::cast).findFirst();
	}

	public List<BlockEntityComponent> getComponents() {
		return Collections.unmodifiableList(this.components);
	}

	public void deleteComponents(Predicate<BlockEntityComponent> predicate) {
		for (var it = this.components.iterator(); it.hasNext();) {
			var component = it.next();
			var shouldDelete = predicate.test(component);

			if (shouldDelete) {
				component.teardown();
				it.remove();
			}
		}
	}

	public void tick() {
		if (this.failed) return;

		try {
			// Create a copy of the current components, so components can create or delete component
			// during their tick handler
			for (var component : new ArrayList<>(this.components)) {
				component.tick();
			}
		} catch (Exception exception) {
			this.failed = true;
			Supervisory.LOGGER.error("Failed to process tick for block entity at " + this.getBlockPos(), exception);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();

		for (var component : this.components) {
			component.teardown();
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();

		for (var component : this.components) {
			component.teardown();
		}
	}

	public void handleNeighbourChange(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		for (var component : this.components) {
			component.handleNeighbourChange(state, level, pos, block, fromPos, isMoving);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, Provider registries) {
		try {
			super.loadAdditional(tag, registries);

			// Create copy of the current components, since some component would like to create
			// components based on loaded data
			for (var component : new ArrayList<>(this.components)) {
				component.read(tag, registries);
			}
		} catch (Exception exception) {
			this.failed = true;
			Supervisory.LOGGER.error("Failed to load block entity at " + this.getBlockPos(), exception);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, Provider registries) {
		try {
			super.saveAdditional(tag, registries);

			for (var component : this.components) {
				component.write(tag, registries);
			}
		} catch (Exception exception) {
			this.failed = true;
			Supervisory.LOGGER.error("Failed to load block entity at " + this.getBlockPos(), exception);
		}
	}

	public Direction getFront() {
		return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
	}

	@SuppressWarnings("unchecked")
	private Configurable<Object, Object> getConfigurable() {
		return this.getComponent(Configurable.class).get();
	}

	@Override
	public Object getConfiguration() {
		return this.getConfigurable().getConfiguration();
	}

	@Override
	public void updateConfiguration(Object configuration) {
		this.getConfigurable().updateConfiguration(configuration);
	}

	@Override
	public Codec<Object> getConfigurationCodec() {
		return this.getConfigurable().getConfigurationCodec();
	}

	@Override
	public Codec<Object> getUpdateCodec() {
		return this.getConfigurable().getUpdateCodec();
	}

	@Override
	public void openConfigurationScreen(Object configuration) {
		this.getConfigurable().openConfigurationScreen(configuration);
	}

	public static BlockEntityFactory<CompositeBlockEntity> createFactory(Consumer<CompositeBlockEntity> initializer) {
		return (type, pos, blockState) -> {
			var instance = new CompositeBlockEntity(type, pos, blockState);
			initializer.accept(instance);
			return instance;
		};
	}
}
