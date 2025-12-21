package bt7s7k7.supervisory.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import bt7s7k7.supervisory.Config;
import bt7s7k7.treeburst.bytecode.ProgramFragment;
import bt7s7k7.treeburst.parsing.Expression;
import bt7s7k7.treeburst.parsing.TreeBurstParser;
import bt7s7k7.treeburst.runtime.EvaluationUtil;
import bt7s7k7.treeburst.runtime.ExecutionLimitReachedException;
import bt7s7k7.treeburst.runtime.ExpressionResult;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.support.Diagnostic;
import bt7s7k7.treeburst.support.InputDocument;
import bt7s7k7.treeburst.support.ManagedValue;
import bt7s7k7.treeburst.support.Position;
import bt7s7k7.treeburst.support.Primitive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public abstract class ScriptEngine implements ManagedWorkDispatcher {
	private Realm realm;

	public static final Codec<InputDocument> INPUT_DOCUMENT_CODEC = RecordCodecBuilder.create(instance -> (instance.group(
			Codec.STRING.fieldOf("path").orElse("").forGetter(v -> v.path),
			Codec.STRING.fieldOf("content").orElse("").forGetter(v -> v.content))
			.apply(instance, InputDocument::new)));

	@Override
	public abstract void handleError(Diagnostic error);

	protected abstract void initializeGlobals(Realm realm);

	public Realm getRealm() {
		if (this.realm == null) {
			this.realm = new Realm();
			this.initializeGlobals(this.realm);
		}

		return this.realm;
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
		var realm = this.getRealm();
		var units = new ArrayList<Expression>(documents.size());
		var error = false;

		for (var document : documents) {
			var parser = new TreeBurstParser(document);
			units.add(parser.parse().getExpression());

			if (!parser.diagnostics.isEmpty()) {
				parser.diagnostics.forEach(this::handleError);
				error = true;
			}
		}

		if (error) return null;

		return this.performWork(result -> new ProgramFragment(new Expression.Group(Position.INTRINSIC, units)).evaluate(realm.globalScope, result));
	}

	public void clear() {
		this.realm = null;
	}

	public boolean isEmpty() {
		return this.realm == null;
	}

	public static Component formatValue(ManagedValue value, Realm realm) {
		if (value == Primitive.VOID || value == Primitive.NULL) {
			return Component.literal(EvaluationUtil.getValueName(value)).withStyle(ChatFormatting.GRAY);
		}

		return Component.literal(realm.inspect(value)).withStyle(ChatFormatting.GOLD);
	}
}
