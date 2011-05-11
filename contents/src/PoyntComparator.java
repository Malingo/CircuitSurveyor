/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.util.Comparator;

public class PoyntComparator implements Comparator<Poynt> {
	public int compare(Poynt p1, Poynt p2) {
		if (p1 == null && p2 == null) return 0;
		else if (p1 == null) return 1;
		else if (p2 == null) return -1;
		else if (p1.getX() > p2.getX()) return 1;
		else if (p1.getX() < p2.getX()) return -1;
		else if (p1.getY() > p2.getY()) return 1;
		else if (p1.getY() < p2.getY()) return -1;
		else return 0;
	}
}
