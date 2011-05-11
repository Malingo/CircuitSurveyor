/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import static java.lang.Math.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class CircuitBoard implements Iterable<Poynt> {

	/* *************** CLASS CONSTANTS *************** */

	private static final double H = 0.01, // 1 cm
							    MU_NAUGHT = 4 * PI * pow(10, -7), // µ_0 = 4¹ E-7 H/m
							    WIRE_THICKNESS = 0.001; // 1 mm
	private static final int MAX_ITERATIONS = 2477; // of potential relaxation method
	private static final int MAX_NO_OF_REGIONS = 31; // for drawing flow lines
	private static final Color CIRCUIT_COLOR = Color.RED,
							   GRADIENT_COLOR = new Color(55, 55, 55),  // dark grey
							   SLOPE_FIELD_COLOR = new Color(64, 0, 128),  // deep violet
							   FLOW_LINES_COLOR = new Color(255, 100, 0);  // orange

	/* *************** DATA MEMBERS *************** */

	private String fileName;
	private Poynt[][] board;
	private Poynt maximum;
	private Dimension size;
	private int scaleFactor;
	private boolean calculatedFlag;
	
	private Set<Loop> loops = new HashSet<Loop>();
	private HashMap<Set<Poynt>, Element> elements = new HashMap<Set<Poynt>, Element>();
	private TreeSet<Poynt> nodes = new TreeSet<Poynt>(new PoyntComparator());

	/* *************** CONSTRUCTOR *************** */

	public CircuitBoard(BufferedReader br, String fileName) throws IOException {
		this.fileName = fileName;
		calculatedFlag = false;
		readCircuitFromFile(br);
		buildCircuit();
	}

	/* *************** PRIVATE METHODS *************** */

	 private void readCircuitFromFile(BufferedReader file) throws IllegalCircuitException {
		String line = "";
	 	
		try {
			for (int lineCount = 1; (line = file.readLine()) != null; lineCount++) {
				if (line.length() == 0) continue;
				else if (line.charAt(0) == '#') continue;
				else if (size == null) {
					try {
						readSizesFromFile(line);
					} catch (IllegalCircuitException e) {
						e.setLine(lineCount, line);
						throw e;
					}
				} else {
					try {
						readElementFromFile(line);
					} catch (IllegalCircuitException e) {
						e.setLine(lineCount, line);
						throw e;
					}
				}
			}
		} catch (IOException e) {
			throw new IllegalCircuitException("Cannot read file " + fileName);
		}
		
		if (size == null)
			throw new IllegalCircuitException("File " + fileName + " is empty.");
		if (elements.size() == 0)
			throw new IllegalCircuitException("File " + fileName + " contains no circuit elements.");
	}

	private void readSizesFromFile(String str) throws IllegalCircuitException {
		int sizeX, sizeY;
		
		str = str.split("#")[0];
		String[] strArr = str.split(",");
		if (strArr.length < 2) throw new IllegalCircuitException("Expected: Maximum y-bound. Found: End of line");
		if (strArr.length > 2) throw new IllegalCircuitException("Expected: End of line. Found: ," + strArr[2]);

		try { 
			sizeX = Integer.parseInt(strArr[0].trim());
		} catch (NumberFormatException e) {
			throw new IllegalCircuitException("Maximum x-bound must be an integer. Found: " + strArr[0].trim());
		}
		try { 
			sizeY = Integer.parseInt(strArr[1].trim());
		} catch (NumberFormatException e) {
			throw new IllegalCircuitException("Maximum y-bound must be an integer. Found: " + strArr[1].trim());
		}		

		if (sizeX < 0) throw new IllegalCircuitException("Maximum x-bound must be positive. Found: " + sizeX);
		if (sizeX == 0) throw new IllegalCircuitException("Maximum x-bound may not be zero. Found: " + sizeX);
		if (sizeY < 0) throw new IllegalCircuitException("Maximum y-bound must be positive. Found: " + sizeY);
		if (sizeY == 0) throw new IllegalCircuitException("Maximum y-bound may not be zero. Found: " + sizeY);
		
		size = new Dimension(sizeX + 1, sizeY + 1);
		
		setUpBoard();
	}

	private void readElementFromFile(String str) throws IllegalCircuitException {
		String letter;
		int startX, startY, endX, endY;
		double value;
		Element elt = null;

		str = str.split("#")[0];
		String[] strArr = str.split("\\p{Blank}+", 0);
		
		// Parse initial character
		if (strArr[0].length() < 1) throw new IllegalCircuitException("Expected: Element type ('w', 'r', or 'b').");
		if (strArr[0].length() > 1) throw new IllegalCircuitException("Expected: Element type ('w', 'r', or 'b'). Found: " + strArr[0]);
		letter = strArr[0].toLowerCase();
		if (!letter.matches("[wrb]")) throw new IllegalCircuitException("Expected: Element type ('w', 'r', or 'b'). Found: " + strArr[0]);

		// Parse line length
		if (strArr.length < 2) throw new IllegalCircuitException("Expected: Start x-coordinate. Found: End of line");
		if (strArr.length < 3) throw new IllegalCircuitException("Expected: End x-coordinate. Found: End of line");
		if (letter.equals("r"))
			if (strArr.length < 4) throw new IllegalCircuitException("Expected: Resistance in ohms. Found: End of line");
		if (letter.equals("b"))
			if (strArr.length < 4) throw new IllegalCircuitException("Expected: Voltage in volts. Found: End of line");
		
		// Parse start coordinate
		String[] startArr = strArr[1].split(",");
		if (startArr.length < 2) throw new IllegalCircuitException("Expected: Start y-coordinate. Found: " + strArr[2]);
		if (startArr.length > 2) throw new IllegalCircuitException("Expected: End coordinate. Found: ," + startArr[2]);
		
		try { 
			startX = Integer.parseInt(startArr[0]);
		} catch (NumberFormatException e) {
			throw new IllegalCircuitException("Start x-coordinate must be an integer. Found: " + startArr[0]);
		}
		try { 
			startY = Integer.parseInt(startArr[1]);
		} catch (NumberFormatException e) {
			throw new IllegalCircuitException("Start y-coordinate must be an integer. Found: " + startArr[1]);
		}		

		if (startX < 0) throw new IllegalCircuitException("Start x-coordinate must be positive. Found: " + startX);
		if (startX >= size.width) throw new IllegalCircuitException("Start coordinates out of bounds. Coordinate: (" + strArr[1] + "), Bounds: (" + size.width + "," + size.height + ")");
		if (startY < 0) throw new IllegalCircuitException("Start y-coordinate must be positive. Found: " + startY);
		if (startY >= size.height) throw new IllegalCircuitException("Start coordinates out of bounds. Coordinate: (" + strArr[1] + "), Bounds: (" + size.width + "," + size.height + ")");

		Poynt start = get(startX, startY);
		
		// Parse end coordinate
		String[] endArr = strArr[2].split(",");
		if (endArr.length < 2) throw new IllegalCircuitException("Expected: End y-coordinate. Found: " + strArr[2]);
		if (endArr.length > 2) 
			if (letter.equals("w")) throw new IllegalCircuitException("Expected: End of line. Found: ," + endArr[2]);
			else throw new IllegalCircuitException("Expected: " + (letter.equals("r") ? "resistance" : "voltage") + ". Found: ," + endArr[2]);
		
		try { 
			endX = Integer.parseInt(endArr[0]);
		} catch (NumberFormatException e) {
			throw new IllegalCircuitException("End x-coordinate must be an integer. Found: " + endArr[0]);
		}
		try {
			endY = Integer.parseInt(endArr[1]);
		} catch (NumberFormatException e) {
			throw new IllegalCircuitException("End y-coordinate must be an integer. Found: " + endArr[1]);
		}		

		if (endX < 0) throw new IllegalCircuitException("End x-coordinate must be positive. Found: " + startX);
		if (endX >= size.width) throw new IllegalCircuitException("End coordinates out of bounds. Coordinate: (" + strArr[2] + "), Bounds: (" + size.width + "," + size.height + ")");
		if (endY < 0) throw new IllegalCircuitException("End y-coordinate must be positive. Found: " + startY);
		if (endY >= size.height) throw new IllegalCircuitException("End coordinates out of bounds. Coordinate: (" + strArr[2] + "), Bounds: (" + size.width + "," + size.height + ")");

		Poynt end = get(endX, endY);
		
		// Parse value, build element
		if (letter.equals("w")) {
			if (strArr.length > 3) throw new IllegalCircuitException("Expected: End of line. Found: " + strArr[3]);
			elt = new Wire(start, end, this);
			
		} else if (letter.equals("r")) {
			if (strArr.length > 4) throw new IllegalCircuitException("Expected: End of line. Found: " + strArr[4]);
			
			try { 
				value = Double.parseDouble(strArr[3]);
			} catch (NumberFormatException e) {
				throw new IllegalCircuitException("Resistance must be an integer or decimal. Found: " + strArr[3]);
			}
			if (value < 0) throw new IllegalCircuitException("Resistance may not be negative. Found: " + strArr[3]);
			
			elt = new Resistor(start, end, value);
			
		} else if (letter.equals("b")) {
			if (strArr.length > 4) throw new IllegalCircuitException("Expected: End of line. Found: " + strArr[4]);

			try { 
				value = Double.parseDouble(strArr[3]);
			} catch (NumberFormatException e) {
				throw new IllegalCircuitException("Voltage must be an integer or decimal. Found: " + strArr[3]);
			}
			
			elt = new Battery(start, end, value);
		}
		
		assert elt != null;
		
		Set<Poynt> set = new HashSet<Poynt>();
		set.add(start); set.add(end);
		Element oldElt = elements.put(set, elt);
		if (oldElt != null) throw new IllegalCircuitException("Elements " + oldElt + " and " + elt + " overlap.");
	}

	private void setUpBoard() {
		board = new Poynt[size.width][size.height];
		for (int i = 0; i < size.width; i++)
			for (int j = 0; j < size.height; j++)
				board[i][j] = new Poynt(i, j, this);

		maximum = new Poynt(-1, -1, this);
		maximum.current = Double.NEGATIVE_INFINITY;
		maximum.potential = Double.NEGATIVE_INFINITY;
		maximum.eFieldX = 0.0;
		maximum.eFieldY = 0.0;
		maximum.bFieldZ = Double.NEGATIVE_INFINITY;
		maximum.poyntX  = 0.0;
		maximum.poyntY = 0.0;
	}

	private void buildCircuit() {

		for (Poynt node : nodes)
			for (Element elt : node.elements())
				if (!elt.isEndPoint(node))
					throw new IllegalCircuitException("Error at point " + node + ": Circuit elements may not overlap except at endpoints.");
		
		if (nodes.isEmpty())
			nodes.add(elements().iterator().next().start);
		
		int loopCount = 0;
		for (Poynt node : nodes)
			for (Poynt neighbor : node.getNeighbors()) {
				Loop loop = new Loop(loopCount++);
				int loopTurns = 0;
				Poynt pt = neighbor;
				Poynt prev = node;
				Poynt latestEndPt = node;
				while (!pt.isMarked(loopCount)) {
					pt.mark(loopCount);
					loop.addPoynt(pt);
					Set<Poynt> pts = new HashSet<Poynt>();
					pts.add(latestEndPt); pts.add(pt);
					if (elements.containsKey(pts)) {
						Element elt = elements.get(pts);
						try { elt.addLoop(loop, Dir.getDirection(prev, pt)); }
						catch (DirException e) { break; }  // Duplicate loop exception
						loop.addElement(elements.get(pts));
						latestEndPt = pt;
					}
					Poynt newPt = pt.getRightmostNeighbor(Dir.getDirection(prev, pt));
					loopTurns += Dir.getTurn(prev, pt, newPt);
					prev = pt;
					pt = newPt;
				}
				if (loopTurns > 0) loops.add(loop);
				else loop.setIndex(-1);
			}
	}

	private int getInOrOut(Element elt, Poynt node) {		
		if (node.equals(elt.start)) {
			if (elt.getAssignedDirection().equals(Dir.EAST) || elt.getAssignedDirection().equals(Dir.SOUTH))
				return +1;
			else if (elt.getAssignedDirection().equals(Dir.WEST) || elt.getAssignedDirection().equals(Dir.NORTH))
				return -1;
			else throw new DirException("Direction error: Element\"" + elt + "\"'s assigned direction is " + elt.getAssignedDirection());
		} else if (node.equals(elt.end)) {
			if (elt.getAssignedDirection().equals(Dir.EAST) || elt.getAssignedDirection().equals(Dir.SOUTH))
				return -1;
			else if (elt.getAssignedDirection().equals(Dir.WEST) || elt.getAssignedDirection().equals(Dir.NORTH))
				return +1;
			else throw new DirException("Direction error: Element\"" + elt + "\"'s assigned direction is " + elt.getAssignedDirection());			
		} else throw new IllegalCircuitException("Element \"" + elt + "\" passes through node " + node + " without having an endpoint there.");
	}
	
	private void fillCurrents() {
		
		int l = 0; // no. of loops
		for (Loop loop : loops) {
			loop.setIndex(l++);
			for (Element elt : loop.getElements())
				if (!elt.hasDirectionAssigned())
					elt.assignDirection(elt.getDirection(loop));
		}
				
		double[][] matrix = new double[nodes.size() + l][l * l + 1];
		int row = 0;
		for (Poynt node : nodes) {
			for (Element elt : node.elements()) {
				int col = elt.getLoopsIndex(l);
				matrix[row][col] += getInOrOut(elt, node);
			}
			row++;
		}
		for (Loop loop : loops) {
			for (Element elt : loop.getElements())
				if (!elt.isWire()) {
					if (elt instanceof Battery) {
						int sign = (elt.getDirection(loop).equals(((Battery) elt).batteryDirection()) ? 1 : -1);
						matrix[row][l * l] += ((Battery) elt).getVoltage() * sign;
					} else if (elt instanceof Resistor) {
						int col = elt.getLoopsIndex(l);
						int sign = (elt.getDirection(loop).equals(elt.getAssignedDirection()) ? 1 : -1);
						matrix[row][col] += ((Resistor) elt).getResistance() * sign;
					}
				}
			row++;
		}
		RowReducer.rref(matrix);
		
		for (Element elt : elements()) {
			int col = elt.getLoopsIndex(l);
			for (double[] r : matrix)
				if (r[col] != 0) {
					double current = r[l * l];
					elt.setCurrent(abs(current));
					if (current < 0)
						elt.assignDirection(elt.getAssignedDirection().flip());
					break;
				}
		}
		
		for (Loop loop : loops) {
			for (Element elt : loop.getElements())
				if (elt.getLoopsIndex(l) % (l + 1) == 0) {
					int sign = elt.getDirection(loop).equals(elt.getAssignedDirection()) ? 1 : -1;
					loop.setCurrent(elt.getCurrent() * sign);
					if (abs(loop.getCurrent()) > maximum.current) maximum.current = abs(loop.getCurrent());
					break;
				}
			loop.fillCurrent();
		}
	}

	private void fillExteriorPotentials() {
		Loop firstLoop = loops.iterator().next();
		firstLoop.fillPotential(0.0, firstLoop.getElements().getFirst());
	}

	private void fillInteriorPotentials() {
				
		// Find average, min, max exterior potentials
		double minPotential = Double.POSITIVE_INFINITY;
		double potentialAverage = 0.0;
		int count = 0;
		for (Poynt p : this)
			if (p.isOnCircuit()) {
				potentialAverage += p.potential;
				count++;
				if (maximum.potential < p.potential)
					maximum.potential = p.potential;
				if (minPotential > p.potential)
					minPotential = p.potential;
			}
		potentialAverage /= count;
		potentialAverage -= minPotential;
		maximum.potential -= minPotential;
		
		// Set all interior potentials to the average; move min. potential to 0
		for (Poynt p : this)
			if (p.isOnCircuit())
				p.potential -= minPotential;
			else if (p.getLoopCount() == 0)
				p.potential = 0;
			else
				p.potential = potentialAverage;
		
		// Relaxation method to find the true interior potentials
		double tolerance = getMinimumVoltage() / 500;
		int iteration = 0;
		double error = 0;
		do {
			error = 0;
			for (Poynt p : this) {
				if (p.getLoopCount() > 0 && !p.isOnCircuit()) {
					double oldPotential = p.potential;
					double v1 = p.get(Dir.WEST).potential,
						   v2 = p.get(Dir.EAST).potential,
						   v3 = p.get(Dir.NORTH).potential,
						   v4 = p.get(Dir.SOUTH).potential;
					double newPotential = (v1 + v2 + v3 + v4) / 4;
					p.potential = newPotential;
					if (abs(newPotential - oldPotential) > error)
						error = abs(newPotential - oldPotential);
				}
			}
		} while (error > tolerance && iteration++ < MAX_ITERATIONS);
		// System.out.println(iteration + " ITERATIONS!");
	}
	
	private double getMinimumVoltage() {
		double minV = Double.POSITIVE_INFINITY;
		for (Element elt : elements())
			if (!elt.isWire())
				if (((NotWire) elt).getPotential() < minV)
					minV = ((NotWire) elt).getPotential();
		return minV;
	}

	private void fillFields() {

		// FIND X & Y GRADIENT OF POTENTIAL; FILL ELECTRIC FIELD

		for (Poynt p : this) {
			if (p.x == 0)
				p.eFieldX = -(p.get(Dir.EAST).potential - p.potential) / H;
			else if (p.x == size.width - 1)
				p.eFieldX = -(p.potential - p.get(Dir.WEST).potential) / H;
			else {
				double mx1 = (p.potential - p.get(Dir.WEST).potential) / H;
				double mx2 = (p.get(Dir.EAST).potential - p.potential) / H;
				double mx3 = (p.get(Dir.EAST).potential - p.get(Dir.WEST).potential) / (2 * H);
				p.eFieldX = -(mx1 + mx2 + 4 * mx3) / 6;
			}
			
			if (p.y == 0)
				p.eFieldY = -(p.get(Dir.SOUTH).potential - p.potential) / H;
			else if (p.y == size.height - 1)
				p.eFieldY = -(p.potential - p.get(Dir.NORTH).potential) / H;
			else {
				double my1 = (p.potential - p.get(Dir.NORTH).potential) / H;
				double my2 = (p.get(Dir.SOUTH).potential - p.potential) / H;
				double my3 = (p.get(Dir.SOUTH).potential - p.get(Dir.NORTH).potential) / (2 * H);
				p.eFieldY = -(my1 + my2 + 4 * my3) / 6;
			}

			// FILL MAGNETIC FIELD

			p.bFieldZ = MU_NAUGHT * (p.current / WIRE_THICKNESS) * 1000;
			
			// FILL POYNTING VECTOR

			p.poyntX = p.bFieldZ * p.eFieldY;
			p.poyntY = -p.bFieldZ * p.eFieldX;

			// UPDATE MAXIMA

			if (maximum.eFieldMag() < p.eFieldMag()) {
				maximum.eFieldX = p.eFieldX;
				maximum.eFieldY = p.eFieldY;
			}
			if (maximum.bFieldZ < p.bFieldZ)
				maximum.bFieldZ = p.bFieldZ;
			if (maximum.poyntMag() < p.poyntMag()) {
				maximum.poyntX = p.poyntX;
				maximum.poyntY = p.poyntY;
			}
		}
	}

	/**
	 * Normalizes the potential, electric field magnitude, magnetic field magnitude, and Poynting
	 * vector magnitude at every Poynt in this board by dividing them by their maximum value on the
	 * board. The output of each of these fields ranges between 0 and 1.
	 */
	private void normalize() {
		for (Poynt p : this) {
			p.potential /= maximum.potential;
			p.eFieldX   /= maximum.eFieldMag();
			p.eFieldY   /= maximum.eFieldMag();
			p.bFieldZ   /= maximum.bFieldZ;
			p.poyntX    /= maximum.poyntMag();
			p.poyntY    /= maximum.poyntMag();
			p.mark(Double.NaN);
		}
	}

	/* *************** PUBLIC METHODS *************** */

	public void calculateCircuit() {
		fillCurrents();
		fillExteriorPotentials();
		fillInteriorPotentials();
		fillFields();
		normalize();
		calculatedFlag = true;
	}
	
	public boolean isCalculated() {
		return calculatedFlag;
	}
	
	public void drawCircuit(Graphics g) {
		g.setColor(CIRCUIT_COLOR);
		for (Element e : elements())
			e.draw(g, scaleFactor);
	}

	public void drawCircuitLabels(Graphics g) {
		g.setColor(CIRCUIT_COLOR);
		for (Element e : elements())
			if (!e.isWire()) {
				Point startCoord = e.start.toPixel(scaleFactor);
				Point endCoord = e.end.toPixel(scaleFactor);

				String label = ((NotWire) e).getValue() + " " + ((NotWire) e).getUnit();
				g.setFont(new Font(g.getFont().getName(), Font.BOLD, g.getFont().getSize()));
				FontMetrics fm = g.getFontMetrics();
				Point coord = startCoord;
					
				if (e.isHorizontal()) {
					coord.x += (endCoord.x - startCoord.x - fm.stringWidth(label)) / 2;
					if (e.start.y > size.height / 2)
						coord.y -= scaleFactor + 3;
					else
						coord.y += fm.getAscent() + scaleFactor + 5;
				} else {
					coord.y += (endCoord.y - startCoord.y + fm.getAscent()) / 2;
					if (e.start.x > size.width / 2)
						coord.x -= fm.stringWidth(label) + scaleFactor + 3;
					else
						coord.x += scaleFactor + 5;
				}
				
				g.drawString(label, coord.x, coord.y);
			}
	}

	public void drawGradient(Graphics g) {
		for (Poynt p : this) {
			if (p.getLoopCount() > 0) {
				int red   = (int) (255 - abs(p.potential) * (255 - GRADIENT_COLOR.getRed()));
				int green = (int) (255 - abs(p.potential) * (255 - GRADIENT_COLOR.getGreen()));
				int blue  = (int) (255 - abs(p.potential) * (255 - GRADIENT_COLOR.getBlue()));
				g.setColor(new Color(red, green, blue));
				Point pt = p.toPixel(scaleFactor);
				g.fillRect(pt.x, pt.y, scaleFactor + 1, scaleFactor + 1);
			}
		}
	}

	public void drawSlopeField(Graphics g) {
		g.setColor(SLOPE_FIELD_COLOR);
		for (Poynt p : this)
			if (p.x % 2 == 1 && p.y % 2 == 1 && p.getLoopCount() > 0 && !p.isOnCircuit()) {

				int h, k;
				if (abs(p.poyntX) > abs(p.poyntY)) {
					h = (int) (scaleFactor * signum(p.poyntX));
					k = (int) (p.poyntY / abs(p.poyntX) * scaleFactor);
				} else {
					k = (int) (scaleFactor * signum(p.poyntY));
					h = (int) (p.poyntX / abs(p.poyntY) * scaleFactor );
				}
				
				if (h != 0 || k != 0) {
					Point pt = p.toPixel(scaleFactor);
					g.drawLine(pt.x, pt.y, pt.x + h, pt.y + k);
					g.fillOval(pt.x + h - 2, pt.y + k - 2, 3, 3);
				}
			}
	}
	
	public void drawFlowLines(Graphics g) {
		g.setColor(FLOW_LINES_COLOR);
		for(Loop loop : loops) {
			loop.setPotentialExtremes();
			int noOfRegions = (int) (abs(loop.getCurrent()) * MAX_NO_OF_REGIONS / maximum.current + 0.5);
			System.out.println("Loop" + loop.getIndex() + ": current = " + loop.getCurrent() + " ; noOfRegions = " + noOfRegions);
			loop.drawFlowLines(g, noOfRegions, scaleFactor);
		}
	}

	public int getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(int scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	public Poynt get(int x, int y) {
		return board[x][y];
	}

	public Poynt get(Point p) {
		return board[p.x][p.y];
	}

	public Poynt getMax() {
		return maximum;
	}

	public Dimension size() {
		return size;
	}

	public Collection<Element> elements() {
		return elements.values();
	}

	public Point toPoint(String str) {
		String[] arr = str.split(",");
		int x = Integer.parseInt(arr[0]);
		int y = Integer.parseInt(arr[1]);
		return new Point(x, y);
	}

	public Iterator<Poynt> iterator() {
		return new Iterator<Poynt>() {
			int i = 0, j = 0;
			boolean hasNext = board.length > 0 && board[0].length > 0;

			public boolean hasNext() {
				return hasNext;
			}

			public Poynt next() {
				Poynt p = board[i][j];
				if (j < size.height - 1) {
					j++;
					hasNext = true;
				} else if (i < size.width - 1) {
					i++;
					j = 0;
					hasNext = true;
				} else hasNext = false;
				return p;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof CircuitBoard))
			return false;
		return ((CircuitBoard) o).fileName.equals(fileName);
	}

	@Override
	public int hashCode() {
		return fileName.hashCode();
	}
	
	public Dimension getBoardSize() {
		return new Dimension(board.length, board[0].length);
	}

	public void addNode(Poynt p) {
		nodes.add(p);
	}

	public Poynt right(Poynt p) {
		return get(p.x + 1, p.y);
	}

	public int getLoops() {
		return loops.size();
	}
}
