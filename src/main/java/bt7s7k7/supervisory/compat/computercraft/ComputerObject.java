package bt7s7k7.supervisory.compat.computercraft;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedArray;
import bt7s7k7.treeburst.runtime.ManagedMap;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;
import dan200.computercraft.api.lua.LuaTable;

public class ComputerObject implements LuaTable<Object, Object> {
	protected HashMap<Object, Object> properties = new HashMap<>();

	public static Object fromManagedValue(ManagedValue source) {
		return switch (source) {
			case Primitive.String string -> string.value;
			case Primitive.Number number -> number.value;
			case Primitive.Boolean bool -> bool.value;
			case ManagedArray array -> {
				var object = new ComputerObject();

				for (int i = 0; i < array.elements.size(); i++) {
					var element = array.elements.get(i);
					var elementValue = fromManagedValue(element);
					if (elementValue == null) continue;

					object.put(i + 1, elementValue);
				}

				yield object;
			}
			case ManagedMap map -> {
				var object = new ComputerObject();

				for (var kv : map.entries.entrySet()) {
					var key = fromManagedValue(kv.getKey());
					if (key == null) continue;

					var value = fromManagedValue(kv.getValue());
					if (value == null) continue;

					object.put(key, value);
				}

				yield object;
			}
			default -> null;
		};
	}

	// Lua tables that are arrays use boxed Double values as keys. This method ensures we convert a
	// numeric index to a Double instead of an Integer or something.
	private static Double makeTableIndex(double i) {
		return i;
	}

	public static ManagedValue toManagedValue(Object computerObject, GlobalScope globalScope) {
		return switch (computerObject) {
			case Number number -> Primitive.from(number.doubleValue());
			case String string -> Primitive.from(string);
			case Boolean bool -> Primitive.from(bool);
			case Map<?, ?> map -> {
				// Test if the table is an array by verifying that all keys are sequential number keys
				var isArray = true;
				for (var i = 0; i < map.size(); i++) {
					if (map.get(makeTableIndex(i + 1)) == null) {
						isArray = false;
						break;
					}
				}

				if (isArray) {
					var result = new ManagedArray(globalScope == null ? null : globalScope.ArrayPrototype);
					for (var i = 0; i < map.size(); i++) {
						var element = map.get(makeTableIndex(i + 1));
						var managedElement = toManagedValue(element, globalScope);
						result.elements.add(managedElement);
					}
					yield result;
				}

				var result = new ManagedMap(globalScope == null ? null : globalScope.MapPrototype);

				for (var kv : map.entrySet()) {
					var key = toManagedValue(kv.getKey(), globalScope);
					if (key == Primitive.VOID) continue;

					var value = toManagedValue(kv.getValue(), globalScope);
					if (value == Primitive.VOID) continue;

					result.entries.put(key, value);
				}

				yield result;
			}
			case Object[] multiple -> {
				var result = new ManagedArray(globalScope == null ? null : globalScope.ArrayPrototype);
				for (var i = 0; i < multiple.length; i++) {
					var managedElement = toManagedValue(multiple[i], globalScope);
					result.elements.add(managedElement);
				}
				yield result;
			}
			case null -> Primitive.NULL;
			default -> Primitive.VOID;
		};
	}

	@Override
	public int size() {
		return this.properties.size();
	}

	@Override
	public boolean isEmpty() {
		return this.properties.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.properties.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.properties.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return this.properties.get(key);
	}

	@Override
	public Set<Object> keySet() {
		return this.properties.keySet();
	}

	@Override
	public Collection<Object> values() {
		return this.properties.values();
	}

	@Override
	public Set<Entry<Object, Object>> entrySet() {
		return this.properties.entrySet();
	}

	@Override
	public Object put(Object key, Object value) {
		return this.properties.put(key, value);
	}

	@Override
	public Object remove(Object value) {
		return this.properties.remove(value);
	}

	@Override
	public void putAll(Map<?, ?> map) {
		this.properties.putAll(map);
	}

	@Override
	public void clear() {
		this.properties.clear();
	}
}
