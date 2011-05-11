/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.awt.Point;

public class Dir {

	/*  *************** CLASS CONSTANTS *************** */

	public static final Dir WEST = new Dir(0);
	public static final Dir NORTH = new Dir(1);
	public static final Dir EAST = new Dir(2);
	public static final Dir SOUTH = new Dir(3);
	
	public static final int LEFT = -1;
	public static final int STRAIGHT = 0;
	public static final int RIGHT = 1;

	/*  *************** DATA MEMBERS *************** */

	protected int dir;

	/*  *************** CONSTRUCTOR *************** */

	public Dir(int direction) {
		this.dir = direction;
	}

	/*  *************** STATIC METHODS *************** */

	public static Dir getDirection(Point a, Point b) {
		if (isHorizontal(a, b)) {
			if (a.x < b.x)
				return EAST;
			else if (a.x > b.x)
				return WEST;
			else throw new DirException("Error finding direction from " + a + " to " + b + ".");
		} else {
			if (a.y < b.y)
				return SOUTH;
			else if (a.y > b.y)
				return NORTH;
			else throw new DirException("Error finding direction from " + a + " to " + b + ".");
		}
	}

	public static boolean isHorizontal(Point a, Point b) {
		boolean v = a.x == b.x;
		boolean h = a.y == b.y;
		if (h)
			return true;
		else if (v)
			return false;
		else throw new DirException("Points " + a + " and " + b + " are positioned neither horizontally nor vertically.");
	}

	public static boolean isVertical(Point a, Point b) {
		return !isHorizontal(a, b);
	}

	public static int getTurn(Poynt p1, Poynt p2, Poynt p3) {
		return getTurn(getDirection(p1, p2), getDirection(p2, p3));
	}

	private static int getTurn(Dir d1, Dir d2) {
		if (d1.equals(NORTH)) {
			if (d2.equals(WEST)) return LEFT;
			else if (d2.equals(NORTH)) return STRAIGHT;
			else if (d2.equals(EAST)) return RIGHT;
		} else if (d1.equals(SOUTH)) {
			if (d2.equals(EAST)) return LEFT;
			else if (d2.equals(SOUTH)) return STRAIGHT;
			else if (d2.equals(WEST)) return RIGHT;
		} else if (d1.equals(WEST)) {
			if (d2.equals(SOUTH)) return LEFT;
			else if (d2.equals(WEST)) return STRAIGHT;
			else if (d2.equals(NORTH)) return RIGHT;
		} else if (d1.equals(EAST)) {
			if (d2.equals(NORTH)) return LEFT;
			else if (d2.equals(EAST)) return STRAIGHT;
			else if (d2.equals(SOUTH)) return RIGHT;
		}
		throw new DirException(d1 + " to " + d2 + " is not a valid turn.");
	}

	/*  *************** PUBLIC METHODS *************** */

	@Override
	public String toString() {
		if (equals(NORTH)) return "NORTH";
		else if (equals(SOUTH)) return "SOUTH";
		else if (equals(WEST)) return "WEST";
		else if (equals(EAST)) return "EAST";
		else throw new DirException(dir + " is not a valid direction.");
	}

	public boolean isHorizontal() {
		return equals(EAST) || equals(WEST);
	}

	public boolean isVertical() {
		return !isHorizontal();
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(dir).hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Dir)
			return ((Dir) other).dir == this.dir;
		return false;
	}

	public int signum() {
		return (int) Math.signum(dir - 1.5);
	}

	public Dir flip() {
		return new Dir((dir + 2) % 4);
	}
	
	public Dir turn(int turn) {
		return new Dir((dir + turn + 4) % 4);
	}
}
