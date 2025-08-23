package bt7s7k7.supervisory.support;

import net.minecraft.core.Direction;

public enum RelativeDirection {
	DOWN(0, "down"), UP(0, "up"), FORWARD(0, "forward"), BACK(0, "back"), RIGHT(0, "right"), LEFT(0, "left");

	public final int index;
	public final String name;

	public Direction getAbsolute(Direction front) {
		switch (this) {
			case FORWARD:
				return front;
			case BACK:
				return front.getOpposite();
			case LEFT:
				return front.getCounterClockWise();
			case RIGHT:
				return front.getClockWise();
			case UP:
				return Direction.UP;
			case DOWN:
				return Direction.DOWN;
			default:
				throw new IllegalArgumentException("Unsupported relative direction");
		}
	}

	private RelativeDirection(int index, String name) {
		this.index = index;
		this.name = name;
	}

	public static RelativeDirection from(Direction front, Direction target) {
		if (target == front) return FORWARD;
		if (target == front.getOpposite()) return BACK;
		if (target == front.getCounterClockWise()) return LEFT;
		if (target == front.getClockWise()) return RIGHT;
		if (target == Direction.UP) return UP;
		return DOWN;
	}
}
