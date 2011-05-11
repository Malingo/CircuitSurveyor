/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

@SuppressWarnings("serial")
public class IllegalCircuitException extends IllegalArgumentException {
	private int lineNo;
	private String line;
	public IllegalCircuitException() {
		super();
		lineNo = -1;
	}
	public IllegalCircuitException(String message) {
		super(message);
		lineNo = -1;
	}
	public IllegalCircuitException(int lineNo, String line, String message) {
		super(message);
		setLine(lineNo, line);
	}
	@Override
	public String getMessage() {
		if (lineNo < 0) return super.getMessage();
		else return ("Error in line " + lineNo + ": " + line + "\n" + super.getMessage());
	}
	public void setLine(int lineNo, String line) {
		this.lineNo = lineNo;
		this.line = line;
	}
}
