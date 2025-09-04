package bt7s7k7.supervisory.storage;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.items.IItemHandler;

public class StorageReactiveDependency extends ReactiveDependency<ManagedValue> {
	public BlockCapabilityCache<IItemHandler, Direction> capabilityCache;
	public StorageProvider provider;

	public StorageReactiveDependency(ReactivityManager owner, String name) {
		super(owner, name, Primitive.VOID);
	}

	@Override
	public boolean isReady() {
		return this.value != Primitive.VOID;
	}
}
