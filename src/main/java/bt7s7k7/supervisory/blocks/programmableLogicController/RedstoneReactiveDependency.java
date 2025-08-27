package bt7s7k7.supervisory.blocks.programmableLogicController;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.supervisory.support.RelativeDirection;
import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.runtime.ManagedObject;
import bt7s7k7.treeburst.runtime.UnmanagedHandle;
import bt7s7k7.treeburst.support.Primitive;

public class RedstoneReactiveDependency extends ReactiveDependency<Primitive.Number> {
	public class Handle extends UnmanagedHandle {
		@Override
		public boolean getProperty(String name, ExpressionResult result) {
			if (name.equals("value")) {
				result.value = RedstoneReactiveDependency.this.value;
				return true;
			}

			return super.getProperty(name, result);
		}

		public Handle(ManagedObject prototype) {
			super(prototype, RedstoneReactiveDependency.this);
		}
	}

	public RedstoneReactiveDependency(ReactivityManager owner, String name, Primitive.Number value) {
		super(owner, name, value);
	}

	public Handle makeHandle(ManagedObject prototype) {
		return new Handle(prototype);
	}

	public static RedstoneReactiveDependency get(ReactivityManager manager, RelativeDirection direction, int value) {
		var key = "redstone:" + direction.name;
		var instance = manager.ensureDependency(key, RedstoneReactiveDependency.class, Primitive.from(value));
		return instance;
	}
}
