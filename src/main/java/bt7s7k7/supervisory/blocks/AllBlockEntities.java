package bt7s7k7.supervisory.blocks;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

import bt7s7k7.supervisory.Supervisory;
import bt7s7k7.supervisory.blocks.programmableLogicController.ProgrammableLogicControllerBlockEntity;
import bt7s7k7.supervisory.blocks.remoteTerminalUnit.RemoteTerminalUnitBlockEntity;

public class AllBlockEntities {
	public static final BlockEntityEntry<RemoteTerminalUnitBlockEntity> REMOTE_TERMINAL_UNIT = Supervisory.REGISTRATE.blockEntity("remote_terminal_unit", RemoteTerminalUnitBlockEntity::new)
			.validBlock(AllBlocks.REMOTE_TERMINAL_UNIT)
			.register();

	public static final BlockEntityEntry<ProgrammableLogicControllerBlockEntity> PROGRAMMABLE_LOGIC_CONTROLLER = Supervisory.REGISTRATE.blockEntity("programmable_logic_controller", ProgrammableLogicControllerBlockEntity::new)
			.validBlock(AllBlocks.PROGRAMMABLE_LOGIC_CONTROLLER)
			.register();

	public static void register() {
		// Load this class
	}
}
