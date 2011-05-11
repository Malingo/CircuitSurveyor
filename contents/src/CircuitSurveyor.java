/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import static java.lang.Math.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.SwingUtilities;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.FileOpenService;
import javax.jnlp.FileContents;
import javax.jnlp.UnavailableServiceException;

@SuppressWarnings("serial")
public class CircuitSurveyor extends JPanel implements ActionListener, ItemListener {

	/*  *************** CLASS CONSTANTS *************** */

	private static final int NO_OF_LAYERS = 7;
	private static final int GRADIENT = 0, FLOW_LINES = 1, SLOPE_FIELD = 2, CIRCUIT = 3,
			CIRCUIT_LABELS = 4, CLICKED_ARROWS = 5, ROVING_ARROWS = 6;

	private static final int INSET = 10, DIGITS = 3;
	private static final String NEW_LINE = "\n",
			DIR_NAME = "/circuits/", // where circuit files are stored
			CIRCUIT_FILE_NAME = DIR_NAME + "contents.txt", // listing of included circuits
			LIST_BOX_TEXT = "-- Select a circuit --",
			HELP_FILE_NAME = "http://www.oberlin.edu/physics/dstyer/CircuitSurveyor/help.html";

	private static final Color ELECTRIC_COLOR = Color.BLUE, // blue
			MAGNETIC_COLOR = new Color(0, 128, 0), // dark green
			POYNTING_COLOR = new Color(255, 165, 0); // yellow-orange

	/*  *************** DATA MEMBERS *************** */

	private Dimension size;
	private Dimension offset;
	private int scaleFactor;

	private JPanel mainFrame;
	private JLayeredPane layeredPane;
	private JPanel[] frames;
	private BufferedImage[] images;
	private Graphics2D[] graphics;

	private JPanel sideFrame;
	private JComboBox listBox;
	private JButton okayButton, addButton, clearButton, helpButton;
	private JPanel topFrame, checkFrame, textFrame;
	private JCheckBox circuitCheck, circuitLabelCheck, gradientCheck, slopeFieldCheck,
			flowLinesCheck;
	private JCheckBox[] checkboxes;
	private JTextArea electricText, magneticText, poyntingText;

	private boolean arrowsAllowedFlag;
	private Hashtable<String, BufferedReader> addedFiles;
	private CircuitBoard board;
	private Hashtable<String, CircuitBoard> boards;

	/*  *************** CONSTRUCTOR *************** */

	/**
	 * Constructor. Creates an instance of CircuitSurveyor by determining the appropriate size for the
	 * main window and creating and laying out the main and side frames.
	 */
	public CircuitSurveyor() {
		super();
		arrowsAllowedFlag = false;
		determineSize();
		createMainFrame();
		createSideFrame();
		createMouseListener();
		performLayout();
		boards = new Hashtable<String, CircuitBoard>();
		addedFiles = new Hashtable<String, BufferedReader>();
	}

	/*  *************** PRIVATE METHODS *************** */

	/**
	 * 
	 */
	private void determineSize() {
		int width = Toolkit.getDefaultToolkit().getScreenSize().width - 300; // 200 for sidebar
		int height = Toolkit.getDefaultToolkit().getScreenSize().height - 100;
		size = new Dimension(width, height);
		offset = new Dimension();
	}

	private void createMainFrame() {

		// Create a Panel for the main image frame.
		mainFrame = new JPanel();
		mainFrame.setPreferredSize(size);
		mainFrame.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

		// Create a LayeredPane in the main frame.
		layeredPane = new JLayeredPane();
		layeredPane.setPreferredSize(size);
		mainFrame.add(layeredPane);

		// Create arrays of Panels, Images, and Graphics.
		frames = new JPanel[NO_OF_LAYERS];
		images = new BufferedImage[NO_OF_LAYERS];
		graphics = new Graphics2D[NO_OF_LAYERS];

		// Create a Panel for a frame to house an Image (with a Graphics) for each layer in the
		// LayeredPane.
		for (int layer = 0; layer < NO_OF_LAYERS; layer++) {

			// Create frame.
			JPanel frame = new JPanel();
			frame.setPreferredSize(size);
			frame.setOpaque(false);
			frame.setBounds(0, 0, size.width, size.height);
			frames[layer] = frame;

			// Create image and add to frame.
			BufferedImage img =
					new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
			images[layer] = img;
			frame.add(new JLabel(new ImageIcon(img)));
			frame.setVisible(false);

			// Add frame to layered pane.
			layeredPane.add(frame, new Integer(layer));

			// Create graphics for image.
			Graphics2D gr = img.createGraphics();
			graphics[layer] = gr;
		}
	}

	private void createSideFrame() {

		// Create the side panel frame.
		sideFrame = new JPanel();
		sideFrame.setPreferredSize(new Dimension(200, size.height));
		sideFrame.setLayout(new BoxLayout(sideFrame, BoxLayout.Y_AXIS));

		// Create the side panel top frame.
		topFrame = new JPanel();
		topFrame.setPreferredSize(new Dimension(200, size.height - 160));
		topFrame.setLayout(new BoxLayout(topFrame, BoxLayout.Y_AXIS));
		topFrame.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

		// Create a frame for the text areas.
		textFrame = new JPanel();
		textFrame.setPreferredSize(new Dimension(200, 150));
		textFrame.setLayout(new BoxLayout(textFrame, BoxLayout.Y_AXIS));
		textFrame.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

		// Create the drop-down menu for the side panel.
		String[] list = populateDropDownList();
		listBox = new JComboBox(list);
		listBox.setMaximumSize(new Dimension(200, 50));
		listBox.addActionListener(this);

		// Create the okay button for the side panel.
		okayButton = new JButton("Okay");
		okayButton.setMaximumSize(new Dimension(200, 50));
		okayButton.addActionListener(this);

		// Create the add button for the side panel.
		addButton = new JButton("Add new...");
		addButton.setMaximumSize(new Dimension(200, 50));
		addButton.addActionListener(this);

		// Create the clear button for the side panel.
		clearButton = new JButton("Clear arrows");
		clearButton.setMaximumSize(new Dimension(200, 50));
		clearButton.setMargin(new Insets(0, 0, 0, 0));
		clearButton.setHorizontalTextPosition(SwingConstants.CENTER);
		clearButton.setVerticalTextPosition(SwingConstants.CENTER);
		clearButton.addActionListener(this);

		// Create the help button for the side panel.
		helpButton = new JButton("Help");
		helpButton.setMaximumSize(new Dimension(200, 50));
		helpButton.addActionListener(this);

		// Create the check boxes for the side panel.
		circuitCheck = new JCheckBox("Circuit");
		circuitLabelCheck = new JCheckBox("Circuit labels");
		gradientCheck = new JCheckBox("Gradient");
		slopeFieldCheck = new JCheckBox("Slope field");
		flowLinesCheck = new JCheckBox("Flow lines");
		JCheckBox[] temp = {circuitCheck, circuitLabelCheck, gradientCheck, slopeFieldCheck,
				flowLinesCheck};
		checkboxes = temp;
		for (JCheckBox checkbox : checkboxes) {
			checkbox.addItemListener(this);
			checkbox.setEnabled(false);
		}
		circuitCheck.doClick();
		circuitLabelCheck.doClick();

		// Create a frame for the check boxes.
		checkFrame = new JPanel(new GridLayout(0, 1));
		checkFrame.setMaximumSize(new Dimension(200, 125));
		for (JCheckBox checkbox : checkboxes)
			checkFrame.add(checkbox);

		// Create text areas for the numerical information.
		electricText = new JTextArea();
		electricText.setMaximumSize(new Dimension(200, 50));
		electricText.setOpaque(false);
		electricText.setForeground(ELECTRIC_COLOR);
		electricText.setEditable(false);
		magneticText = new JTextArea();
		magneticText.setMaximumSize(new Dimension(200, 50));
		magneticText.setOpaque(false);
		magneticText.setForeground(MAGNETIC_COLOR);
		magneticText.setEditable(false);
		poyntingText = new JTextArea();
		poyntingText.setMaximumSize(new Dimension(200, 50));
		poyntingText.setOpaque(false);
		poyntingText.setForeground(POYNTING_COLOR);
		poyntingText.setEditable(false);
	}

	private void performLayout() {

		// Lay out the top side panel.
		topFrame.add(Box.createRigidArea(new Dimension(200, 3)));
		topFrame.add(listBox);
		topFrame.add(okayButton);
		topFrame.add(addButton);
		topFrame.add(checkFrame);
		topFrame.add(Box.createRigidArea(new Dimension(0, 30)));
		topFrame.add(clearButton);
		topFrame.add(helpButton);
		topFrame.add(Box.createVerticalGlue());

		// Lay out the bottom side panel.
		textFrame.add(electricText);
		textFrame.add(magneticText);
		textFrame.add(poyntingText);
		textFrame.add(Box.createRigidArea(new Dimension(207, 5)));

		// Lay out the side panel.
		sideFrame.add(topFrame);
		sideFrame.add(Box.createRigidArea(new Dimension(0, 5)));
		sideFrame.add(textFrame);
		add(sideFrame);

		// Lay out the main panel.
		add(mainFrame);
	}

	private void createMouseListener() {

		MouseInputAdapter listener = new MouseInputAdapter() {
			/**
			 * Displays the electric field, magnetic field, and Poynting vectors at the point where
			 * the mouse cursor is located.
			 * 
			 * @param e
			 *            the mouse event triggering this method
			 */
			public void mouseMoved(MouseEvent e) {
				if (arrowsAllowedFlag) { // If arrows are currently allowed
											 // Should not do anything if no board is currently loaded
					mouseExited(e);
					try {
						Point pt = e.getPoint();
						pt.translate(-8, -8);
						Poynt p = board.get(pixelToPoint(pt));
						if (!p.isOnCircuit()) {
							setText(p);
							drawArrows(pt, graphics[ROVING_ARROWS]);
							frames[ROVING_ARROWS].repaint();
						}
					} catch (IndexOutOfBoundsException ex) {}
				}
			}

			public void mouseExited(MouseEvent e) {
				clearLayer(ROVING_ARROWS);
				electricText.setText("");
				magneticText.setText("");
				poyntingText.setText("");
			}

			public void mousePressed(MouseEvent e) {
				if (arrowsAllowedFlag) { // Should not do anything if no board has been loaded yet
					try {
						Point pt = e.getPoint();
						pt.translate(-8, -8);
						Poynt p = board.get(pixelToPoint(pt));
						if (!p.isOnCircuit()) {
							drawArrows(pt, graphics[CLICKED_ARROWS]);
							frames[CLICKED_ARROWS].repaint();
						}
					} catch (IndexOutOfBoundsException ex) { }
				}
			}
		};
		layeredPane.addMouseListener(listener);
		layeredPane.addMouseMotionListener(listener);
	}

	private void setText(Poynt p) {

		Poynt max = board.getMax();
		electricText.append("  Electric field" + NEW_LINE);
		electricText.append("     x: " + trunc(p.eFieldX * max.eFieldMag()) + " N/C" + NEW_LINE);
		electricText.append("     y: " + trunc(-p.eFieldY * max.eFieldMag()) + " N/C");
		magneticText.append("  Magnetic field" + NEW_LINE);
		magneticText.append("     " + trunc(p.bFieldZ * max.bFieldZ) + " mT" + NEW_LINE);
		magneticText.append("     " + (p.bFieldDir() ? "Into" : "Out of") + " the screen");
		poyntingText.append("  Poynting vector" + NEW_LINE);
		poyntingText.append("     x: " + trunc(p.poyntX * max.poyntMag()) + " W/m^2" + NEW_LINE);
		poyntingText.append("     y: " + trunc(-p.poyntY * max.poyntMag()) + " W/m^2");
	}

	private double trunc(double no) {
		return (int) (no * pow(10, DIGITS)) / pow(10, DIGITS);
	}

	private void drawArrows(Point mouseLoc, Graphics2D g) {
		g.setStroke(new BasicStroke(2.0f)); // extra-thick

		Poynt p = board.get(pixelToPoint(mouseLoc));
		mouseLoc.translate(-offset.width, -offset.height);

		// Draw electric field vector
		g.setColor(ELECTRIC_COLOR);
		int h = (int) (p.eFieldX * 100 + (p.eFieldMag() == 0 ? 0 : p.eFieldX * p.eFieldX
				/ (p.eFieldMag() * p.eFieldMag()) * 15 * signum(p.eFieldX)));
		int k = (int) (p.eFieldY * 100 + (p.eFieldMag() == 0 ? 0 : p.eFieldY * p.eFieldY
				/ (p.eFieldMag() * p.eFieldMag()) * 15 * signum(p.eFieldY)));
		g.drawLine(mouseLoc.x, mouseLoc.y, mouseLoc.x + h, mouseLoc.y + k); // arrow-body
		g.fillOval(mouseLoc.x + h - 2, mouseLoc.y + k - 2, 5, 5); // arrow-head

		// Draw Poynting vector
		g.setColor(POYNTING_COLOR);
		h = (int) (p.poyntX * 100 + (p.poyntMag() == 0 ? 0 : p.poyntX * p.poyntX
				/ (p.poyntMag() * p.poyntMag()) * 15 * signum(p.poyntX)));
		k = (int) (p.poyntY * 100 + (p.poyntMag() == 0 ? 0 : p.poyntY * p.poyntY
				/ (p.poyntMag() * p.poyntMag()) * 15 * signum(p.poyntY)));
		g.drawLine(mouseLoc.x, mouseLoc.y, mouseLoc.x + h, mouseLoc.y + k); // arrow-body
		g.fillOval(mouseLoc.x + h - 2, mouseLoc.y + k - 2, 5, 5); // arrow-head

		// Draw magnetic field
		g.setColor(MAGNETIC_COLOR); // Dark green
		g.drawOval(mouseLoc.x - 6, mouseLoc.y - 6, 13, 13); // enclosing circle
		if (p.bFieldZ != 0)
			if (p.bFieldDir()) { // (x) for into screen
				g.drawLine(mouseLoc.x - 3, mouseLoc.y - 3, mouseLoc.x + 3, mouseLoc.y + 3);
				g.drawLine(mouseLoc.x - 3, mouseLoc.y + 3, mouseLoc.x + 3, mouseLoc.y - 3);
			} else // (.) for out of screen
				g.fillOval(mouseLoc.x - 2, mouseLoc.y - 2, 5, 5);

		g.setStroke(new BasicStroke());
	}

	private String[] populateDropDownList() {
		InputStream is = CircuitSurveyor.class.getResourceAsStream(CIRCUIT_FILE_NAME);
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		List<String> list = new ArrayList<String>();
		list.add(LIST_BOX_TEXT);
		try {
			while ((line = br.readLine()) != null)
				list.add(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list.toArray(new String[list.size()]);
	}

	private int determineScaleFactor() {
		int scaleFactorX = (size.width - 2 * INSET) / board.size().width - 1;
		int scaleFactorY = (size.height - 2 * INSET) / board.size().height - 1;
		scaleFactor = max(min(scaleFactorX, scaleFactorY), 2);

		int offsetX = (size.width - board.size().width * (scaleFactor + 1)) / 2 - 3;
		int offsetY = (size.height - board.size().height * (scaleFactor + 1)) / 2;
		for (Graphics2D g : graphics) {
			g.translate(-offset.width, -offset.height);
			g.translate(offsetX, offsetY);
		}
		offset = new Dimension(offsetX, offsetY);

		return scaleFactor;
	}

	private void clearLayer(int layer) {
		frames[layer].removeAll();
		BufferedImage img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		images[layer] = img;
		frames[layer].add(new JLabel(new ImageIcon(img)));
		graphics[layer] = img.createGraphics();
		graphics[layer].translate(offset.width, offset.height);
	}

	private static void createAndShowGUI() {
		// Create and set up the window.
		JFrame frame = new JFrame("CircuitSurveyor: A Circuit Field Visualizer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Add content to the window.
		frame.add(new CircuitSurveyor());

		// Display the window.
		frame.pack();
		frame.setResizable(false);
		frame.setVisible(true);
	}

	private void runAddButton() {
		try {
			FileOpenService fos =
					(FileOpenService) ServiceManager.lookup("javax.jnlp.FileOpenService");
			FileContents[] fileContents = fos.openMultiFileDialog(null, new String[] { "txt" });
			if (fileContents != null) {
				for (FileContents fc : fileContents) {
					String fileName = fc.getName();
					InputStream is = fc.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr);
					BufferedReader old = addedFiles.put(fileName, br);
					if (old == null && !boards.containsKey(fileName))
						listBox.addItem(fileName);
				}
				listBox.setSelectedItem(fileContents[0].getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void runOkayButton() {
		if (board == null)
			JOptionPane.showMessageDialog(mainFrame, "Please select a circuit.",
					"Illegal Circuit Exception", JOptionPane.ERROR_MESSAGE);
		else {
			if (!board.isCalculated()) // if board has not yet been calculated
				board.calculateCircuit();
			board.drawGradient(graphics[GRADIENT]);
			board.drawSlopeField(graphics[SLOPE_FIELD]);
			board.drawFlowLines(graphics[FLOW_LINES]);
			
			for (JCheckBox checkbox : checkboxes) checkbox.setEnabled(true);
			frames[ROVING_ARROWS].setVisible(true);
			frames[CLICKED_ARROWS].setVisible(true);
			arrowsAllowedFlag = true;
		}
	}
	
	private void runListChange() {
		// unselect and disable checkboxes
		for (JCheckBox checkbox : checkboxes) {
			if (checkbox.isSelected())
				checkbox.doClick();
			checkbox.setEnabled(false);
		}
		
		// disable roving arrows
		arrowsAllowedFlag = false;
		
		for (int layer = 0; layer < NO_OF_LAYERS; layer++)
			clearLayer(layer);
		
		String fileName = (String) listBox.getSelectedItem();
		if (fileName.equals(LIST_BOX_TEXT))
			board = null;
		
		else {
			board = boards.get(fileName);
			
			if (board == null) {
				BufferedReader br;
				try {
					if (addedFiles.containsKey(fileName))
						br = addedFiles.get(fileName);
					else {
						try {
							InputStream is = CircuitSurveyor.class.getResourceAsStream(DIR_NAME + fileName);
							InputStreamReader isr = new InputStreamReader(is);
							br = new BufferedReader(isr);
						} catch (NullPointerException e) {
							throw new IllegalCircuitException("Unable to load selected circuit.");
						}
					}
					try {
						board = new CircuitBoard(br, fileName);
						boards.put(fileName, board);
	
					} catch (IllegalCircuitException e) {
						JOptionPane.showMessageDialog(mainFrame, e.getMessage(),
								"Illegal Circuit Exception", JOptionPane.ERROR_MESSAGE);
					}
	
				} catch (IOException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(mainFrame, e.getMessage(), "I/O Exception",
							JOptionPane.ERROR_MESSAGE);
				}
			}
			board.setScaleFactor(determineScaleFactor());
			scaleFactor = board.getScaleFactor();
	
			board.drawCircuit(graphics[CIRCUIT]);
			board.drawCircuitLabels(graphics[CIRCUIT_LABELS]);
		}
		
		circuitCheck.setEnabled(true);
		circuitLabelCheck.setEnabled(true);
		circuitCheck.doClick();
		circuitLabelCheck.doClick();
	}

	private void runClearButton() {
		clearLayer(CLICKED_ARROWS);
		frames[CLICKED_ARROWS].repaint();
	}
	
	private void runHelpButton() {
		try {
			URL resource = new URL(HELP_FILE_NAME);
			BasicService bs = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
			bs.showDocument(resource);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnavailableServiceException e) {
			e.printStackTrace();
		}
	}

	private Point pixelToPoint(Point point) {
		int x = (point.x - offset.width  + scaleFactor / 2) / (scaleFactor + 1);
		int y = (point.y - offset.height + scaleFactor / 2) / (scaleFactor + 1);
		return new Point(x, y);
	}

	/*  *************** PUBLIC METHODS *************** */

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == addButton)
			runAddButton();
		else if (e.getSource() == okayButton)
			runOkayButton();
		else if (e.getSource() == clearButton)
			runClearButton();
		else if (e.getSource() == helpButton)
			runHelpButton();
		else if (e.getSource() == listBox)
			runListChange();
	}

	public void itemStateChanged(ItemEvent e) {

		Object source = e.getItemSelectable();
		boolean selected = e.getStateChange() == ItemEvent.SELECTED;

		if (source == circuitCheck) {
			frames[CIRCUIT].setVisible(selected);
			if (!selected && circuitLabelCheck.isSelected())
				circuitLabelCheck.doClick();
		}

		else if (source == circuitLabelCheck)
			frames[CIRCUIT_LABELS].setVisible(selected);
		else if (source == gradientCheck)
			frames[GRADIENT].setVisible(selected);
		else if (source == slopeFieldCheck)
			frames[SLOPE_FIELD].setVisible(selected);
		else if (source == flowLinesCheck)
			frames[FLOW_LINES].setVisible(selected);
		}

	/*  *************** MAIN METHOD *************** */

	public static void main(String[] args) {
		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}