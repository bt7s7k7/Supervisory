package bt7s7k7.supervisory.compat.computercraft;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.Support;
import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.ManagedTable;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.Scope;
import bt7s7k7.treeburst.standard.NativeHandleWrapper;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.api.peripheral.IPeripheral;

public class PeripheralConnection {
	public static class Adapter {
		@FunctionalInterface
		public interface BindingCallback {
			void invoke(IPeripheral peripheral, List<ManagedValue> args, Scope scope, ExpressionResult result);
		}

		public static record MethodBinding(List<String> names, List<String> parameters, List<Class<?>> types, BindingCallback callback) {}

		protected ArrayList<MethodBinding> bindings = new ArrayList<>();

		@FunctionalInterface
		public interface ValueExporter {
			Object invoke(IArguments arguments, int index) throws LuaException;
		}

		protected Adapter() {}

		public static Adapter build(Class<? extends IPeripheral> peripheralType) {
			var adapter = new Adapter();
			var annotatedFunctions = Support.getMethodsAnnotatedWith(peripheralType, LuaFunction.class);

			eachFunction: for (var annotatedFunction : annotatedFunctions) {
				var function = annotatedFunction.method();
				var annotation = annotatedFunction.annotation();

				MethodBinding binding = null;
				var names = annotation.value().length > 0 ? Arrays.asList(annotation.value()) : List.of(function.getName());

				tryStaticTyping: do {
					var parameters = new ArrayList<String>();
					var types = new ArrayList<Class<?>>();
					var exporters = new ArrayList<ValueExporter>();

					for (var parameter : function.getParameters()) {
						parameters.add(parameter.getName());

						var coerced = false;
						Class<?> type;
						do {
							if (parameter.getParameterizedType() instanceof ParameterizedType genericInstantiation) {
								var rawType = genericInstantiation.getRawType();
								if (rawType.equals(Coerced.class)) {
									coerced = true;
									var targetType = genericInstantiation.getActualTypeArguments()[0];
									if (targetType instanceof Class<?> rawTargetType) {
										type = rawTargetType;
										break;
									} else {
										// The generic parameter to Coerced cannot be used, this should
										// not happen normally, we expect the parameter to be basically
										// always String, but just in case handle this case by skipping
										// this function
										Supervisory.LOGGER.error("Failed to make exporter for type '" + genericInstantiation.getTypeName() + "', for LuaFunction " + peripheralType.getName() + "." + function.getName());
										continue eachFunction;
									}
								}
							}

							type = parameter.getType();
						} while (false);

						if (type == String.class) {
							types.add(Primitive.String.class);
							exporters.add(coerced ? coerced(IArguments::getStringCoerced) : IArguments::getString);
						} else if (type == IArguments.class) {
							// This is a dynamically typed function
							break tryStaticTyping;
						} else if (type == Boolean.class || type == boolean.class) {
							types.add(Primitive.Boolean.class);
							exporters.add(IArguments::getBoolean);
						} else if (type == ByteBuffer.class) {
							types.add(Primitive.String.class);
							exporters.add(coerced ? coerced(IArguments::getBytesCoerced) : IArguments::getBytes);
						} else if (type == Integer.class || type == int.class) {
							types.add(Primitive.Number.class);
							exporters.add(IArguments::getInt);
						} else if (type == Double.class || type == double.class) {
							types.add(Primitive.Number.class);
							exporters.add(IArguments::getDouble);
						} else if (type == Long.class || type == long.class) {
							types.add(Primitive.Number.class);
							exporters.add(IArguments::getLong);
						} else if (ClassUtils.isAssignable(type, Number.class, true)) {
							types.add(Primitive.Number.class);
							exporters.add((args, i) -> type.cast(args.getDouble(i)));
						} else {
							// If we cannot export the type, do not wrap this function
							Supervisory.LOGGER.error("Failed to make exporter for type '" + type.getTypeName() + "', for LuaFunction " + peripheralType.getName() + "." + function.getName());
							continue eachFunction;
						}
					}

					binding = new MethodBinding(names, parameters, types, (peripheral, args, scope, result) -> {
						invokeLuaFunction(function, peripheral, scope, result, new ObjectArguments(args.stream().map(ComputerObject::fromManagedValue).toList()), exporters);
					});
				} while (false);

				if (binding == null) {
					// Method uses a IArguments parameter, which means it is dynamically typed
					binding = new MethodBinding(names, null, null, (peripheral, args, scope, result) -> {
						invokeLuaFunction(function, peripheral, scope, result, new ObjectArguments(args.stream().map(ComputerObject::fromManagedValue).toList()), null);
					});
				}

				adapter.bindings.add(binding);
			}

			return adapter;
		}

		private static void invokeLuaFunction(Method function, IPeripheral peripheral, Scope scope, ExpressionResult result, IArguments arguments, List<ValueExporter> exporters) {
			try {
				Object[] computerArguments;
				if (exporters != null) {
					computerArguments = new Object[exporters.size()];

					for (var i = 0; i < computerArguments.length; i++) {
						var exporter = exporters.get(i);
						computerArguments[i] = exporter.invoke(arguments, i);
					}
				} else {
					computerArguments = new Object[] { arguments };
				}

				var computerResult = function.invoke(peripheral, computerArguments);
				result.value = ComputerObject.toManagedValue(computerResult, scope.globalScope);
			} catch (IllegalAccessException exception) {
				throw new RuntimeException("Error invoking LuaFunction", exception);
			} catch (InvocationTargetException exception) {
				if (exception.getCause() instanceof LuaException luaException) {
					result.setException(new Diagnostic(luaException.getMessage(), Position.INTRINSIC));
				} else {
					throw new RuntimeException("Error invoking LuaFunction", exception);
				}
			} catch (LuaException exception) {
				result.setException(new Diagnostic(exception.getMessage(), Position.INTRINSIC));
			}
		}

		private static ValueExporter coerced(ValueExporter exporter) {
			return (arguments, index) -> new Coerced<Object>(exporter.invoke(arguments, index));
		}

	}

	protected static HashMap<Class<? extends IPeripheral>, Adapter> adapters = new HashMap<>();

	public static Adapter getAdapterFor(Class<? extends IPeripheral> peripheralType) {
		return adapters.computeIfAbsent(peripheralType, Adapter::build);
	}

	public final IPeripheral peripheral;
	public final PeripheralReactiveDependency source;
	protected final Adapter adapter;

	public PeripheralConnection(IPeripheral peripheral, PeripheralReactiveDependency source) {
		this.peripheral = peripheral;
		this.source = source;
		this.adapter = getAdapterFor(peripheral.getClass());
	}

	public boolean isValid() {
		return this.peripheral == this.source.cachedPeripheral;
	}

	public IPeripheral getPeripheralIfValid() {
		if (this.isValid()) return this.peripheral;
		return null;
	}

	public ManagedTable buildWrappingTable(GlobalScope globalScope) {
		var table = new ManagedTable(globalScope.TablePrototype);
		table.declareProperty("meta", WRAPPER.getHandle(this, globalScope));
		for (var binding : this.adapter.bindings) {
			var handler = (NativeFunction.Handler) (args, scope, result) -> {
				var peripheral = this.getPeripheralIfValid();

				if (peripheral == null) {
					result.setException(new Diagnostic("This peripheral connection is no longer valid", Position.INTRINSIC));
					return;
				}

				binding.callback().invoke(peripheral, args, globalScope, result);
			};

			// Depending if we statically know the parameters of the function, generate either a simple or a variadic NativeFunction
			var function = switch (binding.parameters()) {
				case List<String> parameters -> NativeFunction.simple(globalScope, parameters, binding.types(), handler);
				case null -> new NativeFunction(globalScope.FunctionPrototype, List.of("...arguments"), handler);
			};

			for (var name : binding.names()) {
				table.declareProperty(name, function);
			}
		}
		return table;
	}

	public static NativeHandleWrapper<PeripheralConnection> WRAPPER = new NativeHandleWrapper<>("PeripheralConnection", PeripheralConnection.class, ctx -> ctx
			.addGetter("valid", v -> Primitive.from(v.peripheral.getType()))
			.addGetter("type", v -> Primitive.from(v.peripheral.getType())));
}
