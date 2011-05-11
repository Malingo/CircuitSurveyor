/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.awt.Graphics;
import java.awt.Point;
import java.util.Iterator;

/**
 * @author Noah Morris
 */
public class Resistor extends NotWire {
	
	/*  *************** CONSTRUCTOR *************** */

	/**
	 * Constructor. 
	 * @param start
	 * @param end
	 * @param resistance
	 */
	public Resistor(Poynt start, Poynt end, double resistance) {
		super(start, end, resistance);
	}
	
	/*  *************** INHERITED METHODS *************** */

	@Override
	public char getChar() {
		return 'r';
	}
	
	@Override
	public String getUnit() {
		return "½";
	}

	@Override
	public double setVoltages(double oldVoltage, Loop loop) {
		int sign = getAssignedDirection().equals(getDirection(loop)) ? -1 : 1;
		double voltageChange = value * getCurrent() * sign;
		double newVoltage = oldVoltage + voltageChange;
		double increment = voltageChange / (poynts.size() - 1);
		for (Iterator<Poynt> it = iterateClockwise(loop); it.hasNext();) {
			it.next().potential = oldVoltage;
			oldVoltage += increment;
		}
		voltage = Math.abs(newVoltage - oldVoltage);
		return newVoltage;
	}

	@Override
	public void draw(Graphics g, int sF) {

		Point s = start.toPixel(sF);
		s.translate(sF / 2, sF / 2);

		for (int i = 0; i < poynts.size() - 1; i++)
			if (isHorizontal()) {
				int sign = i % 2 == 0 ? 1 : -1;
				int x = s.x + i * (sF + 1), y = s.y;
				g.drawLine(x, y, x + sF / 2 + 1, y + sign * sF);
				g.drawLine(x + sF / 2 + 1, y + sign * sF, x + sF + 1, y);

			} else {
				int sign = i % 2 == 0 ? 1 : -1;
				int x = s.x, y = s.y + i * (sF + 1);
				g.drawLine(x, y, x + sign * sF, y + sF / 2 + 1);
				g.drawLine(x + sign * sF, y + sF / 2 + 1, x, y + sF + 1);
			}
	}

	/*  *************** PUBLIC METHODS *************** */

	public double getResistance() {
		return value;
	}

}
