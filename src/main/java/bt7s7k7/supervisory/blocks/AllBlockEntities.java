package bt7s7k7.supervisory.blocks;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.programmableLogicController.ScriptedDeviceHost;
import bt7s7k7.supervisory.blocks.remoteTerminalUnit.IOManager;
import bt7s7k7.supervisory.composition.CompositeBlockEntity;

public class AllBlockEntities {
	public static final BlockEntityEntry<CompositeBlockEntity> REMOTE_TERMINAL_UNIT = Supervisory.REGISTRATE.blockEntity("remote_terminal_unit", CompositeBlockEntity.createFactory(instance -> {
		instance.addComponent(new IOManager(instance));
	}))
			.validBlock(AllBlocks.REMOTE_TERMINAL_UNIT)
			.register();

	public static final BlockEntityEntry<CompositeBlockEntity> PROGRAMMABLE_LOGIC_CONTROLLER = Supervisory.REGISTRATE.blockEntity("programmable_logic_controller", CompositeBlockEntity.createFactory(instance -> {
		instance.addComponent(new ScriptedDeviceHost(instance));
	}))
			.validBlock(AllBlocks.PROGRAMMABLE_LOGIC_CONTROLLER)
			.register();

	public static void register() {
		// Load this class
	}
}
