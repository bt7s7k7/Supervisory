package bt7s7k7.supervisory.script;

import bt7s7k7.treeburst.parsing.TreeBurstParser;
import bt7s7k7.treeburst.runtime.ExecutionLimitReachedException;
import bt7s7k7.treeburst.runtime.ExpressionEvaluator;
import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.InputDocument;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public abstract class ScriptEngine {
    private GlobalScope globalScope;

    protected abstract void handleError(Diagnostic error);

    protected abstract void initializeGlobals(GlobalScope globalScope);

    public GlobalScope getGlobalScope() {
        if (this.globalScope == null) {
            this.globalScope = new GlobalScope();
            this.initializeGlobals(globalScope);
        }

        return this.globalScope;
    }

    public ManagedValue executeCode(String path, String code) {
        var globalScope = this.getGlobalScope();
        var inputDocument = new InputDocument(path, code);
        var parser = new TreeBurstParser(inputDocument);
        var root = parser.parse();
        if (!parser.diagnostics.isEmpty()) {
            parser.diagnostics.forEach(this::handleError);
            return null;
        }

        var result = new ExpressionResult();
        result.executionLimit = 5000;
        try {
            ExpressionEvaluator.evaluateExpression(root, globalScope, result);
        } catch (ExecutionLimitReachedException exception) {
            this.handleError(new Diagnostic(exception.getMessage(), Position.INTRINSIC));
            return null;
        }

        var diagnostic = result.terminate();
        if (diagnostic != null) {
            this.handleError(diagnostic);
        }

        return result.value;
    }

    public void clear() {
        this.globalScope = null;
    }

    public boolean isEmpty() {
        return this.globalScope == null;
    }

    public static Component formatValue(ManagedValue value) {
        if (value == Primitive.VOID || value == Primitive.NULL) {
            return Component.literal(ExpressionEvaluator.getValueName(value)).withStyle(ChatFormatting.GRAY);
        }

        if (value instanceof Primitive.Boolean booleanValue) {
            return Component.literal(Boolean.toString(booleanValue.value)).withStyle(ChatFormatting.GOLD);
        }

        if (value instanceof Primitive.Number numberValue) {
            return Component.literal(Double.toString(numberValue.value)).withStyle(ChatFormatting.GOLD);
        }

        if (value instanceof Primitive.String stringValue) {
            return Component.literal(stringValue.value);
        }

        return Component.literal(value.toString()).withStyle(ChatFormatting.BLUE);
    }
}
