package bt7s7k7.supervisory.support;

import java.util.HashMap;

import net.minecraft.core.Direction;

public enum Side {
	BOTTOM(0, "bottom"), TOP(1, "top"), FRONT(2, "front"), BACK(3, "back"), RIGHT(4, "right"), LEFT(5, "left");

	public final int index;
	public final String name;

	protected static final HashMap<String, Side> lookup;
	static {
		lookup = new HashMap<>();
		for (var side : values()) {
			lookup.put(side.name, side);
		}
	}

	public static Side getByName(String name) {
		return lookup.get(name);
	}

	public Direction getDirection(Direction front) {
		return switch (this) {
			case FRONT -> front;
			case BACK -> front.getOpposite();
			case LEFT -> front.getCounterClockWise();
			case RIGHT -> front.getClockWise();
			case TOP -> Direction.UP;
			case BOTTOM -> Direction.DOWN;
			default -> throw new IllegalArgumentException("Unsupported relative direction");
		};
	}

	private Side(int index, String name) {
		this.index = index;
		this.name = name;
	}

	public static Side from(Direction front, Direction target) {
		if (target == front) return FRONT;
		if (target == front.getOpposite()) return BACK;
		if (target == front.getCounterClockWise()) return LEFT;
		if (target == front.getClockWise()) return RIGHT;
		if (target == Direction.UP) return TOP;
		return BOTTOM;
	}
}
