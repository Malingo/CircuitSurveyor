/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.awt.Graphics;
import java.awt.Point;

/**
 * Class representing a wire circuit element.
 * 
 * @author Noah Morris
 */
public class Wire extends Element {
	
	/*  *************** DATA MEMBERS *************** */
	
	/**
	 * A unique identifier for <tt>Wire</tt>s, since they don't get individual names like <tt>Battery</tt>s and <tt>Resistor</tt>s.
	 */
	private String wireCode;

	/*  *************** CONSTRUCTOR *************** */
	
	/**
	 * Constructor. Creates a new <tt>Wire</tt> on the specified <tt>CircuitBoard</tt> connecting the specified endpoints.
	 * @param start One endpoint of the wire.
	 * @param end The other endpoint of the wire. Must be situated either horizontally or vertically with respect to the <tt>start</tt> endpoint.
	 * @param board The <tt>CircuitBoard</tt> on which the <tt>Wire</tt> is to be constructed.
	 */
	public Wire(Poynt start, Poynt end, CircuitBoard board) {
		super(start, end);
		wireCode = getWireCode(start, end);
	}

	/*  *************** INHERITED METHODS *************** */
	
	@Override
	public char getChar() {
		return 'w';
	}

	@Override
	public double setVoltages(double oldVoltage, Loop loop) {
		for (Poynt p : poynts)
			p.potential = oldVoltage;
		return oldVoltage;
	}

	@Override
	public void draw(Graphics g, int sF) {
		Point s = start.toPixel(sF);
		s.translate(sF / 2, sF / 2);
		Point e = end.toPixel(sF);
		e.translate(sF / 2, sF / 2);
		g.drawLine(s.x, s.y, e.x, e.y);
	}
	
	@Override
	public boolean isWire() {
		return true;
	}

	/*  *************** PUBLIC METHODS *************** */
	
	/**
	 * Returns the unique identifier code of this <tt>Wire</tt>.
	 * @return this <tt>Wire</tt>'s unique identifier code.
	 */
	public String getCode() {
		return wireCode;
	}
	
	/*  *************** STATIC METHODS *************** */

	/**
	 * Determines a <tt>Wire</tt> code identifier for a given start and end point.
	 * @param start One endpoint of the <tt>Wire</tt> whose code is to be determined.
	 * @param end The other endpoint of the <tt>Wire</tt> whose code is to be determined.
	 * @return A unique code identifier serving as the "name" of the <tt>Wire</tt>.
	 */
	public static String getWireCode(Point start, Point end) {
		String a = start.toString();
		String b = end.toString();
		if (start.x < end.x)
			return a + b;
		else if (start.x > end.x)
			return b + a;
		else {
			if (start.y < end.y)
				return a + b;
			else
				return b + a;
		}
	}
}