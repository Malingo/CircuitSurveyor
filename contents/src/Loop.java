/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Loop {

	/*  *************** DATA MEMBERS *************** */

	private int index;
	private CircularLinkedList<Element> loop; // the Elements that make up the loop
	private CircularLinkedList<Poynt> perimeter;
	private Set<Poynt> poynts = new HashSet<Poynt>();
	private double current; // Positive = clockwise
	private boolean potentialFilled;
	private double minV;
	private double maxV;

	/*  *************** CONSTRUCTOR *************** */
	
	 public Loop(int index) {
		this.index = index;
		loop = new CircularLinkedList<Element>();
		perimeter = new CircularLinkedList<Poynt>();
		potentialFilled = false;
	}

	/*  *************** PUBLIC METHODS *************** */

	public Set<Poynt> getPoynts() {
		return poynts;
	}
	 
	public int getIndex() {
		return index;
	}

	public CircularLinkedList<Element> getElements() {
		return loop;
	}

	public CircularLinkedList<Poynt> getPerimeter() {
		return perimeter;
	}
		
	public void addElement(Element elt) {
		loop.add(elt);
	}

	public double getCurrent() {
		return current;
	}

	public void setCurrent(double current) {
		this.current = current;
	}

	public void fillCurrent() {
		for (Element elt : loop)
			if (elt.getDirection(this).equals(Dir.NORTH))
				for (Poynt p : elt)
					for (Poynt q = p.get(Dir.EAST); !q.isOnCircuit(); q = q.get(Dir.EAST)) {
						q.current = current;
						poynts.add(q);
						q.addLoop();
					}
	}
	
	public void setPotentialExtremes() {
		// Potentials already range from 0.0 to 1.0 at this point
		minV = java.lang.Double.POSITIVE_INFINITY;
		maxV = java.lang.Double.NEGATIVE_INFINITY;
		for (Poynt p : perimeter) {
			if (minV > p.potential) minV = p.potential;
			if (maxV < p.potential) maxV = p.potential;
		}
	}

	public void fillPotential(double newV, Element start) {
		potentialFilled = true;
		for (Iterator<Element> it = loop.iterator(start, true); it.hasNext();) {
			Element elt = it.next();
			newV = elt.setVoltages(newV, this);
			for (Loop l : elt.getLoops())
				if (!l.potentialFilled)
					l.fillPotential(newV, elt);
		}
	}
	
	 public boolean contains(Poynt p) {
		return poynts.contains(p);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Loop)) return false;
		return this.perimeter.equals(((Loop) o).perimeter);
	}

	@Override
	public int hashCode() {
		return perimeter.hashCode();
	}
	
	@Override
	public String toString() {
		return "loop: " + index;
	}

	public void addPoynt(Poynt p) {
		perimeter.add(p);
		poynts.add(p);
	}

	public void setIndex(int i) {
		index = i;
	}

	public void drawFlowLines(Graphics g, int noOfRegions, int scaleFactor) {

		for (double level = minV; level < maxV - 0.001; level += (maxV - minV) / noOfRegions) {
			if (level == minV)
				continue;
			Iterator<Poynt> perimeter = getPerimeter().iterator();
			Poynt[] p = { perimeter.next(), null };
			while (perimeter.hasNext()) {
				p[1] = p[0];
				p[0] = perimeter.next();
				if ((p[1].potential - level) * (p[0].potential - level) <= 0) {
					if (!(p[0].potential < p[1].potential ? p[0] : p[1]).isMarked(level)) {
						List<Point> list = new ArrayList<Point>();
						Poynt[] q = { p[0], p[1] };
						list.add(interpolate(q, level, scaleFactor));
						while (getNewPoynts(q, level))
							list.add(interpolate(q, level, scaleFactor));
						(q[0].potential < q[1].potential ? q[0] : q[1]).mark(level);

						// Draw the flow line!
						Point prevPoint = list.get(0);
						for (Point point : list) {
							g.drawLine(prevPoint.x, prevPoint.y, point.x, point.y);
							prevPoint = point;
						}
					}
				}
			}
		}
	}
	
	private boolean getNewPoynts(Poynt[] p, double level) {
		Dir dir = Dir.getDirection(p[0], p[1]);
		Poynt[] left = new Poynt[2], straight = new Poynt[2], right = new Poynt[2];
		left[0] = p[0];
		right[1] = p[1];
		
		try {
			straight[0] = left[1] = p[0].get(dir.turn(Dir.LEFT));
			straight[1] = right[0] = p[1].get(dir.turn(Dir.LEFT));
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
		
		if(!contains(straight[0]) || !contains(straight[1]))
			return false;
		
		if ((straight[1].potential - level) * (straight[0].potential - level) <= 0) {
			p[0] = straight[0];
			p[1] = straight[1];
		} else if ((left[1].potential - level) * (left[0].potential - level) <= 0) {
			p[0] = left[0];
			p[1] = left[1];
		} else if ((right[1].potential - level) * (right[0].potential - level) <= 0) {
			p[0] = right[0];
			p[1] = right[1];
		} else
			throw new IndexOutOfBoundsException("Problem drawing flow lines at (" + p[0] + ", " + p[1] + ")");

		return true;
	}

	private Point interpolate(Poynt[] p, double level, int scaleFactor) {
		double diff = (level - p[0].potential) / (p[1].potential - p[0].potential);
		Dir dir = Dir.getDirection(p[0], p[1]);
		Double pt;
		if (dir.equals(Dir.EAST))
			pt = new Double(p[0].x + diff, p[0].y);
		else if (dir.equals(Dir.SOUTH))
			pt = new Double(p[0].x, p[0].y + diff);
		else if (dir.equals(Dir.WEST))
			pt = new Double(p[0].x - diff, p[0].y);
		else if (dir.equals(Dir.NORTH))
			pt = new Double(p[0].x, p[0].y - diff);
		else throw new DirException(dir + " is not a direction.");
		return toPixel(pt, scaleFactor);
	}
	
	private Point toPixel(Double pt, int scaleFactor) {
		int x = (int) (pt.x * (scaleFactor + 1)); 
		int y = (int) (pt.y * (scaleFactor + 1)); 
		return new Point(x, y);
	}

}