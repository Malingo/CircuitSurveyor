/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.awt.Graphics;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public abstract class Element implements Iterable<Poynt> {

	/*  *************** DATA MEMBERS *************** */

	protected Poynt start;
	protected Poynt end;
	private int length;
	private boolean isHorizontal;
	private DirTable directions;
	protected LinkedList<Poynt> poynts;
	private double current;
	private Dir assignedDirection;

	/*  *************** CONSTRUCTOR *************** */

	public Element(Poynt s, Poynt e) throws IllegalCircuitException {
		
		// Check that element is not of zero length
		if (s.equals(e)) throw new IllegalCircuitException("Elements may not be of zero length.");
		start = Poynt.minimum(s, e);
		end = start.equals(s) ? e : s;
		try {
			isHorizontal = Dir.getDirection(s, e).isHorizontal();
		} catch (DirException ex) {
			throw new IllegalCircuitException("Element must be positioned either horizontally or vertically.");
		}
		
		if (isHorizontal)
			length = e.x - s.x;
		else
			length = e.y - s.y;
		assert length >= 0;
		
		poynts = new LinkedList<Poynt>();
		poynts.add(start);
		Poynt prev, next;
		prev = next = start;
		start.setIsOnCircuit();
		start.addLoop();
		start.addElement(this);
		while (!next.equals(end)) {
			try {
				if (isHorizontal)
					next = prev.get(Dir.EAST);
				else
					next = prev.get(Dir.SOUTH);
			} catch (IndexOutOfBoundsException ex) {
				System.out.println("THE ERROR OCCURED DURING THE CONSTRUCTION OF ELEMENT " + this + ".");
				throw ex;
			}
			poynts.add(next);
			next.setIsOnCircuit();
			next.addLoop();
			next.addElement(this);
			prev.addNeighbor(next);
			next.addNeighbor(prev);
			prev = next;
			assert length == poynts.size();
			directions = new DirTable();
		}
	}
	
	/*  *************** ABSTRACT METHODS *************** */

	public abstract double setVoltages(double voltage, Loop loop);

	public abstract char getChar();

	public abstract void draw(Graphics g, int scaleFactor);
	
	public abstract boolean isWire();

	/*  *************** PUBLIC METHODS *************** */

	public boolean isHorizontal() {
		return isHorizontal;
	}

	public Dir getDirection(Loop l) {
		return directions.get(l);
	}
	
	public Poynt getStart(Loop l) {
		if(getDirection(l).signum() > 0) return start;
		else return end;
	}
	
	public Poynt getEnd(Loop l) {
		if(getDirection(l).signum() > 0) return end;
		else return start;
	}

	public void addLoop(Loop loop, Dir dir) {
		try {
			directions.add(loop, dir);
		} catch (DirException e) {
			throw new DirException("Error with " + this + ": " + e.getMessage());
		}
	}

	@Override
	public String toString() {
		return getChar() + " " + start + " " + end;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Element))
			return false;
		else
			return ((Element) o).isEndPoint(start) && ((Element) o).isEndPoint(end);
	}

	@Override
	public int hashCode() {
		return poynts.hashCode();
	}

	public Iterator<Poynt> iterator() {
		return poynts.iterator();
	}
	
	public Iterator<Poynt> iterateClockwise(Loop loop) {
		if(directions.get(loop).signum() > 0)
			return poynts.iterator();
		else
			return poynts.descendingIterator();
	}

	public boolean hasDirectionAssigned() {
		return assignedDirection != null;
	}
	
	public void assignDirection(Dir direction) {
		if (direction != null)
			assignedDirection = direction;
	}
	
	public Dir getAssignedDirection() {
		return assignedDirection;
	}

	public int getLoopsIndex(int noOfLoops) {
		int[] index = directions.getIndices();
		return index[0] * noOfLoops + index[1];
	}

	public void setCurrent(double current) {
		this.current = current;
	}
	
	public boolean isEndPoint(Poynt p) {
		if (p.equals(start)) return true;
		if (p.equals(end)) return true;
		return false;
	}

	public double getCurrent() {
		return current;
	}
	
	public Collection<Loop> getLoops() {
		return directions.getLoops();
	}
}