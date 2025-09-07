package bt7s7k7.supervisory.sockets;

import bt7s7k7.supervisory.script.reactivity.ReactiveDependency;
import bt7s7k7.supervisory.script.reactivity.ReactivityManager;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public abstract class SocketBasedDependency<TCapability> extends ReactiveDependency<ManagedValue> {
    public SocketProvider provider;

    public SocketBasedDependency(ReactivityManager owner, String name) {
        super(owner, name, Primitive.VOID);
    }

    @Override
    public boolean isReady() {
        return this.value != Primitive.VOID;
    }

    public abstract TCapability tryGetCachedCapability();

    public abstract TCapability tryAcquireCapability(ServerLevel level, BlockPos position);

    public void reset() {
        this.provider = null;
    }
}
