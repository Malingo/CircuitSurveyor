/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("serial")
public class Poynt extends Point {

	/*  *************** DATA MEMBERS *************** */
	
	protected double current; // in amperes; positive = clockwise
	protected double potential; // in volts
	protected double eFieldX; // in newtons/coulomb
	protected double eFieldY;
	protected double bFieldZ; // in milliamps/meter
	protected double poyntX; // in watts/meter^2
	protected double poyntY;

	private boolean isOnCircuit;
	private double scratch; // for general mark-up; gets used in building circuit and in drawing flow-lines
	private double loopCount;
	private boolean[] neighbors;
	private int neighborCount;
	private List<Element> elements;
	private CircuitBoard board;

	/*  *************** CONSTRUCTOR *************** */

	public Poynt(int x, int y, CircuitBoard board) {
		this(new Point(x, y), board);
	}

	public Poynt(Point p, CircuitBoard board) {
		x = p.x;
		y = p.y;
		potential = java.lang.Double.NEGATIVE_INFINITY;
		current = 0.0;
		loopCount = 0;
		this.board = board;
	}

	/*  *************** PUBLIC METHODS *************** */

	public boolean isOnCircuit() {
		return isOnCircuit;
	}

	public void setIsOnCircuit() {
		isOnCircuit = true;
	}
	
	public double addLoop() {
		loopCount++;
		return loopCount;
	}
	
	public void addNeighbor(Poynt p) {
		if (neighbors == null) neighbors = new boolean[] {false, false, false, false};
		neighbors[Dir.getDirection(this, p).dir] = true;
		neighborCount++;
		if (neighborCount > 2) board.addNode(this);
	}
	
	public void addNeighbor(Dir direction) {
		
	}

	public double getLoopCount() {
		return loopCount;
	}
	
	public Poynt get(Dir direction) {
		if (direction.equals(Dir.SOUTH)) return board.get(x, y + 1);
		else if (direction.equals(Dir.EAST)) return board.get(x + 1, y);
		else if (direction.equals(Dir.NORTH)) return board.get(x, y - 1);
		else if (direction.equals(Dir.WEST)) return board.get(x - 1, y);
		else throw new DirException(direction + " is not a valid direction.");
	}
	
	public static Poynt minimum(Poynt... poynts) {

		if (poynts.length > 0) {
			
			Arrays.sort(poynts, new PoyntComparator());
					
			if (poynts[0] != null)
				return poynts[0];
			else
				throw new IllegalArgumentException("Cannot call Poynt.minimum() with all null arguments.");
			
		} else {
			throw new IllegalArgumentException("Cannot call Poynt.minimum() with no arguments.");
		}
	}

	public Collection<Poynt> getNeighbors() {
		Collection<Poynt> set = new ArrayList<Poynt>(neighborCount);
		for (int i = 0; i < 3; i++)
			if (neighbors[i])
				set.add(getNeighbor(new Dir(i)));
		return set;
	}

	public Poynt getRightmostNeighbor(Dir direction) {
		Poynt p = null;
		if (direction.equals(Dir.NORTH)) {
			p = getNeighbor(Dir.EAST);
			if (p == null) p = getNeighbor(Dir.NORTH);
			if (p == null) p = getNeighbor(Dir.WEST);
		} else if (direction.equals(Dir.SOUTH)) {
			p = getNeighbor(Dir.WEST);
			if (p == null) p = getNeighbor(Dir.SOUTH);
			if (p == null) p = getNeighbor(Dir.EAST);
		} else if (direction.equals(Dir.WEST)) {
			p = getNeighbor(Dir.NORTH);
			if (p == null) p = getNeighbor(Dir.WEST);
			if (p == null) p = getNeighbor(Dir.SOUTH);
		} else if (direction.equals(Dir.EAST)) {
			p = getNeighbor(Dir.SOUTH);
			if (p == null) p = getNeighbor(Dir.EAST);
			if (p == null) p = getNeighbor(Dir.NORTH);
		}
		if (p == null) throw new IllegalCircuitException("Error at point " + this + ": Dead end.");
		return p;
	}

	public Poynt getNeighbor(Dir direction) {
		if (hasNeighbor(direction))
			return get(direction);
		else return null;
	}
	
	public boolean hasNeighbor(Dir direction) {
		if (neighbors == null) return false;
		return neighbors[direction.dir];
	}
	
	public void addElement(Element elt) {
		if (elements == null) elements = new ArrayList<Element>(2);
		elements.add(elt);
	}
	
	public List<Element> elements() {
		return elements;
	}
	
	public Point toPixel(int scaleFactor) {
		int x = this.x * (scaleFactor + 1) - scaleFactor / 2;
		int y = this.y * (scaleFactor + 1) - scaleFactor / 2;
		return new Point(x, y);
	}
	
	public double mark(double d) {
		scratch = d;
		return scratch;
	}

	public boolean isMarked(double d) {
		return scratch == d;
	}
	
	public double eFieldMag() {
		return Math.sqrt(eFieldX * eFieldX + eFieldY * eFieldY);
	}

	public double poyntMag() {
		return Math.sqrt(poyntX * poyntX + poyntY * poyntY);
	}
	
	public boolean bFieldDir() {
		return bFieldZ > 0;
	}
}