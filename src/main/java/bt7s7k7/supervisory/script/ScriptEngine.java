package bt7s7k7.supervisory.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.Config;
import bt7s7k7.treeburst.parsing.Expression;
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

public abstract class ScriptEngine implements ManagedWorkDispatcher {
	private GlobalScope globalScope;

	public static final Codec<InputDocument> INPUT_DOCUMENT_CODEC = RecordCodecBuilder.create(instance -> (instance.group(
			Codec.STRING.fieldOf("path").orElse("").forGetter(v -> v.path),
			Codec.STRING.fieldOf("content").orElse("").forGetter(v -> v.content))
			.apply(instance, InputDocument::new)));

	@Override
	public abstract void handleError(Diagnostic error);

	protected abstract void initializeGlobals(GlobalScope globalScope);

	public GlobalScope getGlobalScope() {
		if (this.globalScope == null) {
			this.globalScope = new GlobalScope();
			this.initializeGlobals(this.globalScope);
		}

		return this.globalScope;
	}

	@Override
	public ManagedValue performWork(Consumer<ExpressionResult> worker) {
		var result = new ExpressionResult();
		result.executionLimit = Config.EXPRESSION_LIMIT.getAsInt();

		try {
			worker.accept(result);
		} catch (ExecutionLimitReachedException exception) {
			this.handleError(new Diagnostic(exception.getMessage(), Position.INTRINSIC));
			return null;
		}

		if (ExpressionResult.LABEL_RETURN.equals(result.label)) {
			return result.value;
		}

		var diagnostic = result.terminate();
		if (diagnostic != null) {
			this.handleError(diagnostic);
		}

		return result.value;
	}

	public ManagedValue executeCode(String path, String code) {
		return this.executeCode(Collections.singletonList(new InputDocument(path, code)));
	}

	public ManagedValue executeCode(List<InputDocument> documents) {
		var globalScope = this.getGlobalScope();
		var units = new ArrayList<Expression>(documents.size());
		var error = false;

		for (var document : documents) {
			var parser = new TreeBurstParser(document);
			units.add(parser.parse());

			if (!parser.diagnostics.isEmpty()) {
				parser.diagnostics.forEach(this::handleError);
				error = true;
			}
		}

		if (error) return null;

		return this.performWork(result -> ExpressionEvaluator.evaluateExpression(new Expression.Group(Position.INTRINSIC, units), globalScope, result));
	}

	public void clear() {
		this.globalScope = null;
	}

	public boolean isEmpty() {
		return this.globalScope == null;
	}

	public static Component formatValue(ManagedValue value, GlobalScope globalScope) {
		if (value == Primitive.VOID || value == Primitive.NULL) {
			return Component.literal(ExpressionEvaluator.getValueName(value)).withStyle(ChatFormatting.GRAY);
		}

		return Component.literal(globalScope.inspect(value)).withStyle(ChatFormatting.GOLD);
	}
}
