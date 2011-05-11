/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.util.ArrayList;
import java.util.Collection;

public class DirTable {
	private Loop loop1;
	private Dir dir1;
	private Loop loop2;
	private Dir dir2;
	
	public void add(Loop loop, Dir dir) {
		if (loop1 == null) {
			loop1 = loop;
			dir1 = dir;
		} else if (dir.equals(dir1)) { 
			throw new DirException("A DirTable can't hold two loops going in the same direction. " + this);
		} else if (loop2 == null) {
			loop2 = loop;
			dir2 = dir;
		} else
			throw new DirException("A DirTable can't hold more than two loops. " + this);
	}
	
	public Dir get(Loop loop) {
		if (loop.equals(loop1)) return dir1;
		else if (loop.equals(loop2)) return dir2;
		else throw new DirException("This DirTable does not contain the specified loop. " + this);
	}
	
	public Dir getFirst() {
		return dir1;
	}
	
	public int[] getIndices() {
		if (loop1 == null || loop1.getIndex() < 0) {
			if (loop2 == null || loop2.getIndex() < 0)
				throw new DirException("This DirTable does not contain any loops. " + this);
			else
				return new int[] { loop2.getIndex(), loop2.getIndex() };

		} else {
			if (loop2 == null || loop2.getIndex() < 0)
				return new int[] { loop1.getIndex(), loop1.getIndex() };
			else if (loop1.getIndex() > loop2.getIndex())
				return new int[] { loop2.getIndex(), loop1.getIndex() };
			else
				return new int[] { loop1.getIndex(), loop2.getIndex() };
		}
	}
	
	public Collection<Loop> getLoops() {
		ArrayList<Loop> set = new ArrayList<Loop>(2);
		if (loop2 != null && loop2.getIndex() >= 0) set.add(loop2);
		if (loop1 != null && loop1.getIndex() >= 0) set.add(loop1);
		return set;
	}

	public Collection<Dir> getEntries() {
		ArrayList<Dir> set = new ArrayList<Dir>(2);
		if (dir2 != null) set.add(dir2);
		if (dir1 != null) set.add(dir1);
		return set;
	}
	
	public String toString() {
		return "{ (" + loop1 + ", " + dir1 + ") (" + loop2 + ", " + dir2 + ") }";
	}
	
	public double getCurrent(Loop loop) {
		if (loop.equals(loop1))
			return 4;
		else if (loop.equals(loop2))
			return 6;
		else throw new DirException(" This DirTable does not contain the specified loop. " + this);
	}
}
