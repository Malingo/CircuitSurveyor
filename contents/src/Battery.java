/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.awt.Graphics;
import java.awt.Point;
import java.util.Iterator;

public class Battery extends NotWire {

	/*  *************** DATA MEMBERS *************** */
	
	private boolean isForward; // True :: positive terminal of battery points downward or rightward

	/*  *************** CONSTRUCTOR *************** */
	
	public Battery(Poynt start, Poynt end, double voltage) {
		super(start, end, voltage);
		this.isForward = Poynt.minimum(start, end) == start;
		voltage = Math.abs(value);
	}
	
	/*  *************** PUBLIC METHODS *************** */
	
	public double getVoltage() {
		return value;
	}
	
	public Dir batteryDirection() {
		if (isHorizontal()) {
			if (isForward) return Dir.EAST;
			else return Dir.WEST;
		} else {
			if (isForward) return Dir.SOUTH;
			else return Dir.NORTH;
		}
	}

	/*  *************** INHERITED METHODS *************** */
	
	@Override
	public String getUnit() {
		return "V";
	}
	
	@Override
	public char getChar() {
		return 'b';
	}

	@Override
	public double setVoltages(double oldVoltage, Loop loop) {
		int sign = getDirection(loop).signum() * (isForward ? 1 : -1);
		double voltageChange = value * sign;
		double newVoltage = oldVoltage + voltageChange;
		double increment = voltageChange / (poynts.size() - 1);
				for (Iterator<Poynt> it = iterateClockwise(loop); it.hasNext();) {
			it.next().potential = oldVoltage;
			oldVoltage += increment;
		}
		return newVoltage;
	}

	@Override
	public void draw(Graphics g, int sF) {
		Point s = (isForward ? start : end).toPixel(sF);
		s.translate(sF / 2, sF / 2);
		Point e = (isForward ? end : start).toPixel(sF);
		e.translate(sF / 2, sF / 2);
		int k = (sF + 2) / 3;

		if (isHorizontal()) {
			int x0 = s.x;
			int x1 = (2 * s.x + e.x) / 3;
			int x2 = (s.x + 2 * e.x) / 3;
			int x3 = e.x;
			int y = s.y;

			g.drawLine(x0, y - k, x0, y + k);
			g.drawLine(x1, y - 3 * k, x1, y + 3 * k);
			g.drawLine(x2, y - k, x2, y + k);
			g.drawLine(x3, y - 3 * k, x3, y + 3 * k);

		} else {
			int y0 = s.y;
			int y1 = (2 * s.y + e.y) / 3;
			int y2 = (s.y + 2 * e.y) / 3;
			int y3 = e.y;
			int x = s.x;

			g.drawLine(x - k, y0, x + k, y0);
			g.drawLine(x - 3 * k, y1, x + 3 * k, y1);
			g.drawLine(x - k, y2, x + k, y2);
			g.drawLine(x - 3 * k, y3, x + 3 * k, y3);
		}
	}
}
