package bt7s7k7.supervisory.support;

import java.util.ArrayList;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;

public class ManagedValueCodec implements Codec<ManagedValue> {
	public enum EncodedType {
		VOID, NULL, NUMBER, STRING, BOOLEAN, ARRAY,
	}

	public static final Codec<EncodedType> ENCODED_TYPE_FIELD = Codec.stringResolver(EncodedType::toString, EncodedType::valueOf);
	public static final Codec<EncodedType> ENCODED_HEADER = RecordCodecBuilder.create(instance -> (instance.group(
			ENCODED_TYPE_FIELD.fieldOf("type").forGetter(v -> {
				throw new NotImplementedException();
			})).apply(instance, v -> v)));

	private static <T, U> Codec<U> createPrimitiveCodec(EncodedType encodedType, Codec<T> valueCodec, Function<U, T> valueGetter, Function<T, U> primitiveCreator) {
		return RecordCodecBuilder.create(instance -> instance.group(
				ENCODED_TYPE_FIELD.fieldOf("type").forGetter(v -> encodedType),
				valueCodec.fieldOf("value").forGetter(valueGetter))
				.apply(instance, (type, value) -> primitiveCreator.apply(value)));
	}

	public static final ManagedValueCodec INSTANCE = new ManagedValueCodec();

	public static final Codec<ManagedValue> NUMBER_CODEC = createPrimitiveCodec(EncodedType.NUMBER, Codec.DOUBLE, v -> ((Primitive.Number) v).value, v -> Primitive.from(v));
	public static final Codec<ManagedValue> STRING_CODEC = createPrimitiveCodec(EncodedType.STRING, Codec.STRING, v -> ((Primitive.String) v).value, v -> Primitive.from(v));
	public static final Codec<ManagedValue> BOOLEAN_CODEC = createPrimitiveCodec(EncodedType.BOOLEAN, Codec.BOOL, v -> ((Primitive.Boolean) v).value, v -> Primitive.from(v));

	public static final Codec<ManagedValue> ARRAY_CODEC = createPrimitiveCodec(EncodedType.ARRAY, Codec.list(INSTANCE), v -> ((ManagedArray) v).elements, v -> new ManagedArray(null, new ArrayList<>(v)));

	@Override
	public <T> DataResult<T> encode(ManagedValue input, DynamicOps<T> ops, T prefix) {
		if (input == Primitive.VOID) {
			return ENCODED_HEADER.encode(EncodedType.VOID, ops, prefix);
		}

		if (input == Primitive.NULL) {
			return ENCODED_HEADER.encode(EncodedType.NULL, ops, prefix);
		}

		if (input instanceof Primitive.Number) {
			return NUMBER_CODEC.encode(input, ops, prefix);
		}

		if (input instanceof Primitive.String) {
			return STRING_CODEC.encode(input, ops, prefix);
		}

		if (input instanceof Primitive.Boolean) {
			return BOOLEAN_CODEC.encode(input, ops, prefix);
		}

		if (input instanceof ManagedArray) {
			return ARRAY_CODEC.encode(input, ops, prefix);
		}

		throw new IllegalArgumentException("Managed value type " + input.getClass() + " is not supported for encoding");
	}

	@Override
	public <T> DataResult<Pair<ManagedValue, T>> decode(DynamicOps<T> ops, T input) {
		var typeResult = ENCODED_HEADER.decode(ops, input);
		if (typeResult.isError()) {
			return DataResult.error(() -> "Invalid value type: " + typeResult.error().orElseThrow().message());
		} else {
			var type = typeResult.getOrThrow().getFirst();

			Codec<ManagedValue> codec;

			switch (type) {
				case VOID:
					return DataResult.success(Pair.of(Primitive.VOID, ops.empty()));
				case NULL:
					return DataResult.success(Pair.of(Primitive.NULL, ops.empty()));
				case NUMBER:
					codec = NUMBER_CODEC;
					break;
				case STRING:
					codec = STRING_CODEC;
					break;
				case BOOLEAN:
					codec = BOOLEAN_CODEC;
					break;
				case ARRAY:
					codec = ARRAY_CODEC;
					break;
				default:
					throw new RuntimeException("Invalid result of EncodedType parse");
			}

			return codec.decode(ops, input);
		}
	}
}
