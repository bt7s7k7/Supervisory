package bt7s7k7.supervisory.script;

import java.util.function.Consumer;

import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.ManagedValue;

public interface ManagedWorkDispatcher {
	public void handleError(Diagnostic error);

	public ManagedValue performWork(Consumer<ExpressionResult> worker);
}
