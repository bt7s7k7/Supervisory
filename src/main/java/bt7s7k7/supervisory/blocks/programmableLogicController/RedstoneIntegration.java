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
import bt7s7k7.treeburst.runtime.NativeFunction;
import bt7s7k7.treeburst.runtime.Realm;
import bt7s7k7.treeburst.standard.LazyTable;
import bt7s7k7.treeburst.support.Primitive;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class RedstoneIntegration extends LazyTable implements ScriptedSystemIntegration { // @symbol: Redstone
	// @summary: Allows for control of redstone input/output.
	protected final ScriptedSystem system;
	protected final RedstoneState redstone;
	protected final RedstoneReactiveDependency[] handlers = new RedstoneReactiveDependency[Side.values().length];

	public RedstoneIntegration(ScriptedSystem system, Realm realm, RedstoneState redstone) {
		super(realm.TablePrototype, realm);
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

			this.declareProperty(direction.name, dependency.getHandle()); // @symbol: <template>redstone_get, @type: Redstone.RedstoneReactiveDependency, @summary: Triggers every time the input redstone signal on the side changes.

			this.declareProperty("set" + StringUtils.capitalize(direction.name), NativeFunction.simple(this.realm, List.of("active"), List.of(Primitive.Boolean.class), (args, scope, result) -> { // @symbol: <template>redstone_set
				// @summary: Allows setting the selected side's output redstone signal.
				var strength = args.get(0).getNumberValue();
				this.redstone.setOutput(absoluteDirection, (int) strength);
				result.value = null;
			}));

			this.declareProperty("set" + StringUtils.capitalize(direction.name) + "Analog", NativeFunction.simple(this.realm, List.of("strength"), List.of(Primitive.Number.class), (args, scope, result) -> { // @symbol: <template>redstone_set_analog
				// @summary: Allows setting the selected side's output redstone signal strength. The strength must be in range `0..15` inclusive, otherwise it will be clamped.
				var strength = args.get(0).getNumberValue();
				this.redstone.setOutput(absoluteDirection, (int) strength);
				result.value = null;
			}));

			// @symbol: Redstone.bottom, @like: <template>redstone_get
			// @symbol: Redstone.top, @like: <template>redstone_get
			// @symbol: Redstone.front, @like: <template>redstone_get
			// @symbol: Redstone.back, @like: <template>redstone_get
			// @symbol: Redstone.right, @like: <template>redstone_get
			// @symbol: Redstone.left, @like: <template>redstone_get
			// @symbol: Redstone.setBottom, @like: <template>redstone_set
			// @symbol: Redstone.setTop, @like: <template>redstone_set
			// @symbol: Redstone.setFront, @like: <template>redstone_set
			// @symbol: Redstone.setBack, @like: <template>redstone_set
			// @symbol: Redstone.setRight, @like: <template>redstone_set
			// @symbol: Redstone.setLeft, @like: <template>redstone_set
			// @symbol: Redstone.setBottomAnalog, @like: <template>redstone_set_analog
			// @symbol: Redstone.setTopAnalog, @like: <template>redstone_set_analog
			// @symbol: Redstone.setFrontAnalog, @like: <template>redstone_set_analog
			// @symbol: Redstone.setBackAnalog, @like: <template>redstone_set_analog
			// @symbol: Redstone.setRightAnalog, @like: <template>redstone_set_analog
			// @symbol: Redstone.setLeftAnalog, @like: <template>redstone_set_analog
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
			if (!integration.initialized) return;

			var relative = Side.from(init.entity.getFront(), direction);
			integration.handlers[relative.index].updateValue(Primitive.from(strength));
		});

		init.signalConnector.connect(init.systemHost.onScopeInitialization, event -> {
			var realm = event.system().getRealm();
			var integration = new RedstoneIntegration(event.system(), realm, redstone);
			event.system().integrations.putInstance(RedstoneIntegration.class, integration);
			realm.declareGlobal("Redstone", integration);
		});
	}
}
