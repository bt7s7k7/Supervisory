package bt7s7k7.supervisory.blocks.programmableLogicController;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import bt7s7k7.supervisory.redstone.RedstoneReactiveDependency;
import bt7s7k7.supervisory.redstone.RedstoneState;
import bt7s7k7.supervisory.support.Side;
import bt7s7k7.supervisory.system.ScriptedSystem;
import bt7s7k7.supervisory.system.ScriptedSystemInitializationEvent;
import bt7s7k7.supervisory.system.ScriptedSystemIntegration;
import bt7s7k7.treeburst.runtime.GlobalScope;
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Primitive;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class RedstoneIntegration extends LazyTable implements ScriptedSystemIntegration {
	protected final ScriptedSystem system;
	protected final RedstoneState redstone;
	protected final RedstoneReactiveDependency[] handlers = new RedstoneReactiveDependency[Side.values().length];

	public RedstoneIntegration(ScriptedSystem system, GlobalScope globalScope, RedstoneState redstone) {
		super(globalScope.TablePrototype, globalScope);
		this.system = system;
		this.redstone = redstone;
	}

	@Override
	protected void initialize() {
		var front = this.system.host.entity.getFront();

		for (var direction : Side.values()) {
			var absoluteDirection = direction.getDirection(front);
			var redstoneValue = this.redstone.getInput(absoluteDirection);
			var dependency = RedstoneReactiveDependency.get(this.system.reactivityManager, direction, redstoneValue);
			this.handlers[direction.index] = dependency;

			this.declareProperty(direction.name, dependency.getHandle());

			this.declareProperty("set" + StringUtils.capitalize(direction.name), NativeFunction.simple(this.globalScope, List.of("strength"), List.of(Primitive.Number.class), (args, scope, result) -> {
				var strength = args.get(0).getNumberValue();
				this.redstone.setOutput(absoluteDirection, (int) strength);
				result.value = null;
			}));
		}
	}

	@Override
	public void teardown() {
		Arrays.fill(this.handlers, null);
	}

	@SubscribeEvent
	public static void registerIntegration(ScriptedSystemInitializationEvent init) {
		var redstone = init.entity.ensureComponent(RedstoneState.class, RedstoneState::new);

		init.signalConnector.connect(redstone.onRedstoneInputChanged, event -> {
			var direction = event.direction();
			var strength = event.strength();

			var integration = init.getScriptEngine().integrations.getInstance(RedstoneIntegration.class);
			if (integration == null) return;

			var relative = Side.from(init.entity.getFront(), direction);
			integration.handlers[relative.index].updateValue(Primitive.from(strength));
		});

		init.signalConnector.connect(init.systemHost.onScopeInitialization, event -> {
			var globalScope = event.system().getGlobalScope();
			var integration = new RedstoneIntegration(event.system(), globalScope, redstone);
			event.system().integrations.putInstance(RedstoneIntegration.class, integration);
			globalScope.declareGlobal("Redstone", integration);
		});
	}
}
