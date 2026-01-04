package bt7s7k7.supervisory.sensor;

import static bt7s7k7.treeburst.runtime.EvaluationUtil.evaluateInvocation;

import java.util.Collections;

import bt7s7k7.supervisory.support.ManagedValueCodec;
import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.runtime.ManagedMap;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.runtime.Scope;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SensorReading {
	public final BlockState blockState;
	public final BlockEntity blockEntity;

	public SensorReading(BlockState blockState, BlockEntity blockEntity) {
		this.blockState = blockState;
		this.blockEntity = blockEntity;
	}

	public ManagedValue id() {
		if (this.blockState == null || this.blockState.isEmpty()) return Primitive.NULL;
		var block = this.blockState.getBlock();
		var id = BuiltInRegistries.BLOCK.getKeyOrNull(block).toString();
		return Primitive.from(id);
	}

	public String dump(Scope scope, ExpressionResult result) {
		if (this.blockState == null || this.blockState.isEmpty()) return "empty";
		var block = this.blockState.getBlock();
		var id = BuiltInRegistries.BLOCK.getKeyOrNull(block).toString();
		var properties = this.getProperties(scope.realm);

		if (!properties.entries.isEmpty()) {
			evaluateInvocation(properties, properties, "k_string", Position.INTRINSIC, Collections.emptyList(), scope, result);
			if (result.label != null) throw new RuntimeException("Properties dumping for SensorReading failed, " + result.terminate().format());
			id += result.value.getStringValue();
		}

		return id;
	}

	public ManagedMap getProperties(Realm realm) {
		var properties = ManagedMap.empty(realm.MapPrototype);

		for (var property : this.blockState.getProperties()) {
			var name = Primitive.from(property.getName());
			var nativeValue = this.blockState.getValue(property);

			var value = switch (nativeValue) {
				case null -> Primitive.NULL;
				case Integer _1 -> Primitive.from(_1);
				case Boolean _1 -> Primitive.from(_1);
				default -> Primitive.from(nativeValue.toString());
			};

			properties.entries.put(name, value);
		}

		return properties;
	}

	public static final NativeHandleWrapper<SensorReading> WRAPPER = new NativeHandleWrapper<>("Sensor.SensorReading", SensorReading.class, ctx -> ctx
			// @summary: The block state and data read from a sensor using the {@link Sensor} API.
			.addGetter("id", SensorReading::id) // @type: String, @summary: The ID of the block.
			.addGetter("properties", v -> v.getProperties(ctx.realm)) // @type: Map, @summary: The block state properties of the block.
			.addMethod("getData", Collections.emptyList(), Collections.emptyList(), (self, args, scope, result) -> {
				// @summary: Returns the NBT data of the block as a {@link Map}. The data is read every time this method is executed, so it may return different values at different times.
				if (self.blockEntity == null) {
					result.value = Primitive.VOID;
					return;
				}

				var nbt = self.blockEntity.saveWithFullMetadata(self.blockEntity.getLevel().registryAccess());
				var value = ManagedValueCodec.importNbtData(nbt, scope.realm, Primitive.VOID);

				result.value = value;
			})
			.addDumpMethod((self, depth, scope, result) -> self.dump(scope, result)));

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.blockState == null) ? 0 : this.blockState.hashCode());
		result = prime * result + ((this.blockEntity == null) ? 0 : this.blockEntity.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		SensorReading other = (SensorReading) obj;
		if (this.blockState == null) {
			if (other.blockState != null) return false;
		} else if (!this.blockState.equals(other.blockState)) return false;
		if (this.blockEntity == null) {
			if (other.blockEntity != null) return false;
		} else if (!this.blockEntity.equals(other.blockEntity)) return false;
		return true;
	}
}
