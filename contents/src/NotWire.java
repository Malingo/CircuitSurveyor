/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

public abstract class NotWire extends Element {

	protected double value;
	protected double voltage;
	
	public NotWire(Poynt start, Poynt end, double value) {
		super(start, end);
		this.value = value;
	}
	
	public double getPotential() {
		return voltage;
	}
	
	public double getValue() {
		return value;
	}
	
	@Override
	public boolean isWire() {
		return false;
	}

	public abstract String getUnit();
	
}
