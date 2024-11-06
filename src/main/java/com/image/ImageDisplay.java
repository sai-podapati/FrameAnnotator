package com.image;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDisplay extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JLabel imageLabel;
	private JLabel frameNumberLbl;
	private JTextField frameSearchField;
	private List<BufferedImage> imageFrames;
	private int currentIndex;
	private JPanel leftPanel;
	private JPanel rightPanel;
	private Map<String, Color> categoryColors;
	private ColorConfigLoader colorConfigLoader;

	private JRadioButton blurButton;
	private JRadioButton annotationButton;

	private JButton rectangleButton;
	private JButton circleButton;
	private String selectedShape = "Rectangle";

	private BufferedImage currentImage;
	private Rectangle selection;
	private Ellipse2D.Double selectionCircle;
	private Point startPoint;
	private List<Rectangle> annotations = new ArrayList<>();
	private List<Rectangle> drawnRectangles = new ArrayList<>();
	private List<Ellipse2D.Double> drawnCircles = new ArrayList<>();
	private List<String> annotationLabels = new ArrayList<>();
	private List<String> annotationLabelsCircles = new ArrayList<>();
	private List<String> confidenceLevels = new ArrayList<>();
	private List<String> frameQualities = new ArrayList<>();
	private List<String> azimustList = new ArrayList<>();
	private List<String> angleList = new ArrayList<>();
	private List<String> positionList = new ArrayList<>();
	private List<String> heightList = new ArrayList<>();
	private List<String> confidenceLevelsForCircle = new ArrayList<>();
	private List<String> frameQualitiesForCircle = new ArrayList<>();
	private List<String> azimustListForCircle = new ArrayList<>();
	private List<String> angleListForCircle = new ArrayList<>();
	private List<String> positionListForCircle = new ArrayList<>();
	private List<String> heightListForCircle = new ArrayList<>();

	private JButton lineButton;
	private List<Point> linePoints = new ArrayList<>();
	private Map<Integer, Map<String, List<Point>>> drawnLines = new HashMap<>();
	private boolean isDrawingLine = false;
	private Point firstPoint = null;
	private Point lastPoint = null;
	private Color currentLineColor = Color.BLACK;
	private String currentLineCategory = "";
	private List<Point> currentLinePoints = new ArrayList<>(); // Stores points for the current line
	private List<String> annotationLabelsLines = new ArrayList<>();
	private List<String> confidenceLevelsForLines = new ArrayList<>();
	private List<String> frameQualitiesForLines = new ArrayList<>();
	private List<String> azimustListForLines = new ArrayList<>();
	private List<String> angleListForLines = new ArrayList<>();
	private List<String> positionListForLines = new ArrayList<>();
	private List<String> heightListForLines = new ArrayList<>();

	private Stack<UndoAction> undoActionStack = new Stack<>();

	private static final double SCALE_FACTOR_INITIAL = 1.0;
	private double scaleFactor = SCALE_FACTOR_INITIAL;
	private static final Logger logger = LoggerFactory.getLogger(ImageDisplay.class);

	public ImageDisplay(List<BufferedImage> images, String videoName) {
		this.imageFrames = images;
		this.currentIndex = 0;

		setTitle("Annotator");
		setLookAndFeel();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		JPanel topPanel = createTopPanel();
		add(topPanel, BorderLayout.NORTH);

		colorConfigLoader = new ColorConfigLoader();
		categoryColors = colorConfigLoader.getCategoryColors();

		// Set up control panel with Masking and Annotation options
		JPanel controlPanel = createControlPanel();

		// Left panel setup
		leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		leftPanel.setPreferredSize(new Dimension(180, 400)); // Fixed size for leftPanel

		leftPanel.add(controlPanel);
		add(leftPanel, BorderLayout.WEST);

		imageLabel = createImageLabel();
		JScrollPane scrollPane = new JScrollPane(imageLabel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				int x = (int) (e.getX() / scaleFactor);
				int y = (int) (e.getY() / scaleFactor);

				if ("Rectangle".equals(selectedShape) && selection != null) {
					selection.setBounds(Math.min(startPoint.x, x), Math.min(startPoint.y, y),
							Math.abs(startPoint.x - x), Math.abs(startPoint.y - y));
				} else if ("Circle".equals(selectedShape) && selectionCircle != null) {
					selectionCircle.setFrameFromDiagonal(startPoint, new Point(x, y));
				}
				repaint(); // Update the display as the shape is drawn
			}
		});

		// Create the action for ending the line drawing
		Action endLineDrawingAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isDrawingLine) {
					finalizeCurrentLine();
					repaint();
				}
			}
		};

		// Add key binding for Escape key to the imageLabel
		imageLabel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),
				"endLineDrawing");
		imageLabel.getActionMap().put("endLineDrawing", endLineDrawingAction);

		rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(scrollPane, BorderLayout.CENTER);
		add(rightPanel, BorderLayout.CENTER);

		JPanel bottomPanel = createBottomPanel(videoName);
		add(bottomPanel, BorderLayout.SOUTH);

		initShapeShortcuts();

		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setVisible(true);

		SwingUtilities.invokeLater(this::updateImage);

		addImageLabelListeners();
		blurButton.setSelected(true);
	}

	private BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	private JPanel createTopPanel() {
		JPanel topPanel = new JPanel(new BorderLayout());
		JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

		rectangleButton = new JButton(createRectangleIcon());
		rectangleButton.setPreferredSize(new Dimension(30, 30));
		rectangleButton.addActionListener(e -> {
			selectedShape = "Rectangle";
			finalizeCurrentLine();
		});

		circleButton = new JButton(createCircleIcon());
		circleButton.setPreferredSize(new Dimension(30, 30));
		circleButton.addActionListener(e -> {
			selectedShape = "Circle";
			finalizeCurrentLine();
		});

		lineButton = new JButton(createLineIcon());
		lineButton.setPreferredSize(new Dimension(30, 30));
		lineButton.addActionListener(e -> {
			finalizeCurrentLine();
			selectedShape = "Line";
			isDrawingLine = true; // Allow drawing a new line when the line button is selected
			getAnnotationPanel(selectionCircle, selectedShape); // Show annotation panel to choose the category first
		});

		lineButton.setEnabled(false); // Initially hidden

		// Initially hidden buttons for shape selection
		rectangleButton.setEnabled(false);
		circleButton.setEnabled(false);
		lineButton.setEnabled(false);

		rectangleButton.setOpaque(true);
		circleButton.setOpaque(true);
		lineButton.setOpaque(true);

		centerPanel.add(rectangleButton);
		centerPanel.add(circleButton);
		centerPanel.add(lineButton);

		topPanel.add(centerPanel, BorderLayout.CENTER);
		return topPanel;
	}

	private Icon createCircleIcon() {
		BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(Color.BLACK);
		g2d.drawOval(2, 2, 16, 16);
		g2d.dispose();
		return new ImageIcon(image);
	}

	private Icon createLineIcon() {
		int width = 20;
		int height = 20;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();

		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(2));
		g2d.drawLine(2, height - 2, width - 2, 2); // Diagonal line from bottom-left to top-right

		g2d.dispose();
		return new ImageIcon(image);
	}

	private void finalizeCurrentLine() {
		if (!currentLinePoints.isEmpty()) {
			// Clear the current line's temporary points
			currentLinePoints = new ArrayList<>();
			// Reset for drawing the next line
			isDrawingLine = false; // Enable line drawing mode again
			firstPoint = null;
			lastPoint = null;
			repaint();
		}
	}

	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Minimal padding around the control
																					// panel

		// Frame Number Label
		frameNumberLbl = new JLabel("Frame Number: ...");
		frameNumberLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		controlPanel.add(frameNumberLbl);

		// Go-To Panel
		JPanel goPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2)); // Minimal padding within goPanel
		goPanel.add(new JLabel("Go To: "));
		frameSearchField = new JTextField(3);
		frameSearchField.setPreferredSize(new Dimension(50, 25));
		frameSearchField.addActionListener(e -> searchFrame());
		goPanel.add(frameSearchField);
		goPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		controlPanel.add(goPanel);

		// Add small fixed-size vertical space instead of RigidArea for fine-tuned
		// control
		controlPanel.add(Box.createVerticalStrut(5));

		// Annotation Panel
		JPanel annotationPanel = new JPanel();
		annotationPanel.setLayout(new BoxLayout(annotationPanel, BoxLayout.Y_AXIS));
		annotationPanel.setBorder(BorderFactory.createTitledBorder("Annotation"));
		blurButton = createRadioButtonWithShortcut("Masking", e -> toggleShapeButtons(false), 'M');
		annotationButton = createRadioButtonWithShortcut("Annotation", e -> toggleShapeButtons(true), 'A');
		blurButton.requestFocusInWindow();

		blurButton.addActionListener(e -> {
			if (blurButton.isSelected()) {
				selectedShape = "Rectangle";
				rectangleButton.doClick();
				isDrawingLine = false;
				finalizeCurrentLine();
			}
		});

		blurButton.setOpaque(true);
		annotationButton.setOpaque(true);

		ButtonGroup annotationGroup = new ButtonGroup();
		annotationGroup.add(blurButton);
		annotationGroup.add(annotationButton);

		annotationPanel.add(blurButton);
		annotationPanel.add(annotationButton);
		annotationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		controlPanel.add(annotationPanel);

		controlPanel.add(Box.createVerticalStrut(5)); // Small spacing between panels

		// Shapes Panel
		JPanel shapePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2)); // Minimal padding within shapePanel
		shapePanel.setBorder(BorderFactory.createTitledBorder("Shapes"));
		shapePanel.setPreferredSize(new Dimension(90, 80));
		shapePanel.setMaximumSize(new Dimension(130, 80)); // Prevents it from expanding vertically

		rectangleButton.setEnabled(false);
		circleButton.setEnabled(false);
		lineButton.setEnabled(false);

		shapePanel.add(rectangleButton);
		shapePanel.add(circleButton);
		shapePanel.add(lineButton);
		shapePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		controlPanel.add(shapePanel);

		controlPanel.add(Box.createVerticalStrut(5)); // Small spacing between panels

		// Zoom Panel
		JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2)); // Minimal padding within zoomPanel
		zoomPanel.setBorder(BorderFactory.createTitledBorder("Zoom"));
		zoomPanel.setPreferredSize(new Dimension(90, 80));
		zoomPanel.setMaximumSize(new Dimension(100, 80)); // Matches size to the Shapes panel for alignment

		JButton zoomInButton = createCustomZoomButton("+", e -> zoomImage(1.1));
		JButton zoomOutButton = createCustomZoomButton("-", e -> zoomImage(0.9));
		Dimension buttonSize = new Dimension(30, 30);
		zoomInButton.setPreferredSize(buttonSize);
		zoomOutButton.setPreferredSize(buttonSize);

		zoomInButton.setMnemonic(KeyEvent.VK_EQUALS); // This works as Alt + PLUS
		zoomOutButton.setMnemonic(KeyEvent.VK_MINUS); // This works as Alt + MINUS

		zoomPanel.add(zoomInButton);
		zoomPanel.add(zoomOutButton);
		zoomPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		controlPanel.add(zoomPanel);

		return controlPanel;
	}

	private void toggleShapeButtons(boolean visible) {
		rectangleButton.setEnabled(visible);
		circleButton.setEnabled(visible);
		lineButton.setEnabled(visible);
	}

	private JLabel createImageLabel() {
		return new JLabel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;

				if (selection != null) {
					g2d.setColor(Color.YELLOW);
					g2d.drawRect((int) (selection.x * scaleFactor), (int) (selection.y * scaleFactor),
							(int) (selection.width * scaleFactor), (int) (selection.height * scaleFactor));
				}
				// Draw shapes based on scaleFactor for rendering

				// Draw rectangles
				for (int i = 0; i < drawnRectangles.size(); i++) {
					Rectangle annotation = drawnRectangles.get(i);
					if (annotationLabels.size() > i) {
						String label = annotationLabels.get(i);
						g.setColor(categoryColors.getOrDefault(label, Color.WHITE));
						g.drawRect((int) (annotation.x * scaleFactor), (int) (annotation.y * scaleFactor),
								(int) (annotation.width * scaleFactor), (int) (annotation.height * scaleFactor));
						g.setFont(new Font("Arial", Font.BOLD, 16));
						g.drawString(label, (int) (annotation.x * scaleFactor), (int) ((annotation.y) * scaleFactor));
					}
				}

				// Draw circles
				for (int i = 0; i < drawnCircles.size(); i++) {
					Ellipse2D.Double circle = drawnCircles.get(i);
					String label = annotationLabelsCircles.get(i);
					g2d.setColor(categoryColors.getOrDefault(label, Color.WHITE));

					double x = circle.x * scaleFactor;
					double y = circle.y * scaleFactor;
					double width = circle.width * scaleFactor;
					double height = circle.height * scaleFactor;

					g2d.draw(new Ellipse2D.Double(x, y, width, height));
					g2d.drawString(label, (int) x, (int) y - 5);
				}

				// Draw the rectangle being dragged (for preview)
				if (selection != null) {
					g2d.setColor(Color.YELLOW);
					int previewX = (int) (selection.x * scaleFactor);
					int previewY = (int) (selection.y * scaleFactor);
					int previewWidth = (int) (selection.width * scaleFactor);
					int previewHeight = (int) (selection.height * scaleFactor);
					g2d.drawRect(previewX, previewY, previewWidth, previewHeight);
				}

				// Draw the circle being dragged (for preview)
				if (selectionCircle != null) {
					g2d.setColor(Color.YELLOW);
					g2d.draw(new Ellipse2D.Double(selectionCircle.x * scaleFactor, selectionCircle.y * scaleFactor,
							selectionCircle.width * scaleFactor, selectionCircle.height * scaleFactor));
				}

				// Draw lines by scaling coordinates for rendering
				for (Map<String, List<Point>> linePointsWithCategory : drawnLines.values()) {
					for (Map.Entry<String, List<Point>> entry : linePointsWithCategory.entrySet()) {
						String currentCategory = entry.getKey();
						List<Point> pointsList = entry.getValue();
						g2d.setColor(categoryColors.getOrDefault(currentCategory, Color.BLACK));

						for (int k = 0; k < pointsList.size(); k++) {
							Point point = pointsList.get(k);
							int x = (int) (point.x * scaleFactor);
							int y = (int) (point.y * scaleFactor);

							g2d.fillOval(x - 3, y - 3, 6, 6);

							if (k > 0) {
								Point previousPoint = pointsList.get(k - 1);
								int prevX = (int) (previousPoint.x * scaleFactor);
								int prevY = (int) (previousPoint.y * scaleFactor);
								g2d.drawLine(prevX, prevY, x, y);
								g2d.drawString(currentCategory, (prevX + x) / 2, (prevY + y) / 2);
							}
						}
					}
				}
			}
		};
	}

	private JPanel createBottomPanel(String videoName) {
		JPanel bottomPanel = new JPanel(new BorderLayout()); // Use BorderLayout for bottom panel

		// Center panel to hold main action buttons
		JPanel centerPanel = new JPanel();
		centerPanel.add(createButtonWithShortcut("Previous", e -> showPreviousImage(), 'P'));
		centerPanel.add(createButtonWithShortcut("Next", e -> showNextImage(), 'N'));
		centerPanel.add(createButtonWithShortcut("Save", e -> saveImage(videoName), 'S'));
		centerPanel.add(createButtonWithShortcut("Undo", e -> undoLastAction(), 'U'));
		centerPanel.add(createButtonWithShortcut("Reset", e -> resetAnnotations(), 'R'));

		// Right panel to hold Info button
		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton infoButton = new JButton("Info");
		infoButton.setPreferredSize(new Dimension(60, 30)); // Set preferred size if needed
		infoButton.addActionListener(e -> showAnnotationInfo());
		rightPanel.add(infoButton);

		// Add panels to bottomPanel
		bottomPanel.add(centerPanel, BorderLayout.CENTER); // Center section with main buttons
		bottomPanel.add(rightPanel, BorderLayout.EAST); // Right section with Info button

		return bottomPanel;
	}

	private void addImageLabelListeners() {
		imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				handleAnnotationHover(e.getPoint());
			}
		});

		imageLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {

				int x = (int) (e.getX() / scaleFactor);
				int y = (int) (e.getY() / scaleFactor);
				startPoint = new Point(x, y); // Corrected start point with scaling
				if ("Line".equals(selectedShape) && isDrawingLine) {

					currentLinePoints.add(new Point(x, y));
					Point scaledPoint = new Point((int) (e.getX() / scaleFactor), (int) (e.getY() / scaleFactor));

					// Add the scaled point to linePoints for tracking
					linePoints.add(scaledPoint);

					// If there's a previous point, draw a line between the last point and current
					// point
					if (lastPoint == null) {
						firstPoint = scaledPoint;
					}

					if (firstPoint != null && lastPoint == null) {
						Map<String, List<Point>> linePointsWithCategory = new HashMap<>();
						linePointsWithCategory.put(currentLineCategory, currentLinePoints);
						drawnLines.put(drawnLines.size(), new HashMap<>(linePointsWithCategory));
					} else {
						Map<String, List<Point>> previousLinePointsWithCategory = drawnLines.get(drawnLines.size());
						Map<String, List<Point>> currentLinePointsWithCategory = new HashMap<>();
						currentLinePointsWithCategory.put(currentLineCategory, currentLinePoints);
						replaceLastValueIfMatch(drawnLines, previousLinePointsWithCategory,
								currentLinePointsWithCategory);
					}
					repaint();

					lastPoint = scaledPoint;
				} else {
					// Handle other shapes like Rectangle, Circle, etc.
					if ("Rectangle".equals(selectedShape) || blurButton.isSelected()) {
						selection = new Rectangle(x, y, 0, 0);
						selectionCircle = null;
					} else if ("Circle".equals(selectedShape)) {
						selectionCircle = new Ellipse2D.Double(startPoint.x, startPoint.y, 0, 0);
						selection = null;
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e) && isDrawingLine) {
					finalizeCurrentLine();
				} else {
					// Handle other shapes' finalization
					if (blurButton.isSelected() && selection != null) {
//	                        Rectangle scaledSelection = scaleRectangleToOriginal(selection);
						int x = (int) (e.getX() / scaleFactor);
						int y = (int) (e.getY() / scaleFactor);
						Rectangle originalSelection = new Rectangle((int) (selection.x), (int) (selection.y - 40),
								(int) (x - selection.x), (int) (y - selection.y));
						blurSelection(originalSelection);

					} else if ("Rectangle".equals(selectedShape) && selection != null) {
						Rectangle finalRect = new Rectangle(selection);
						drawnRectangles.add(finalRect);

						// Open the annotation panel
						getAnnotationPanel(finalRect, "Rectangle");

					} else if ("Circle".equals(selectedShape) && selectionCircle != null) {
						drawnCircles.add(selectionCircle);
						getAnnotationPanel(selectionCircle, "Circle");
					}
					selection = null;
					selectionCircle = null;
					repaint();
				}
			}

		});
	}

	private void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static <K, V> void replaceLastValueIfMatch(Map<K, V> map, V specifiedValue, V newValue) {
		if (map.isEmpty()) {
			return; // Map is empty, nothing to replace
		}

		// Get the last entry in the map
		Map.Entry<K, V> lastEntry = null;
		for (Map.Entry<K, V> entry : map.entrySet()) {
			lastEntry = entry;
		}

		// Check if the last value matches the specified value, then replace
		if (lastEntry != null && lastEntry.getValue().equals(specifiedValue)) {
			map.put(lastEntry.getKey(), newValue);
		}
	}

	private void saveImage(String videoName) {
		try {
			// Create a copy of the original image to draw shapes onto
			BufferedImage imageToSave = new BufferedImage(currentImage.getWidth(), currentImage.getHeight(),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = imageToSave.createGraphics();

			// Draw the original image onto this new BufferedImage
			g2d.drawImage(currentImage, 0, 0, null);

			// Setup drawing properties
			g2d.setStroke(new BasicStroke(2));
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Draw rectangles
			for (int i = 0; i < drawnRectangles.size(); i++) {
				Rectangle annotation = drawnRectangles.get(i);
				String label = annotationLabels.get(i);
				g2d.setColor(categoryColors.getOrDefault(label, Color.WHITE));

				// Position and size in original dimensions
				int x = annotation.x;
				int y = annotation.y;
				int width = annotation.width;
				int height = annotation.height;

				g2d.drawRect(x, y, width, height);
				g2d.setFont(new Font("Arial", Font.BOLD, 16));
				g2d.drawString(label, x, y - 5);
			}

			// Draw circles
			for (int i = 0; i < drawnCircles.size(); i++) {
				Ellipse2D.Double circle = drawnCircles.get(i);
				String label = annotationLabelsCircles.get(i);
				g2d.setColor(categoryColors.getOrDefault(label, Color.WHITE));

				// Draw circle directly in original coordinates
				g2d.draw(new Ellipse2D.Double(circle.x, circle.y, circle.width, circle.height));
				g2d.drawString(label, (int) circle.x, (int) circle.y - 5);
			}

			// Draw lines
			for (int i = 0; i < drawnLines.size(); i++) {
				Map<String, List<Point>> linePointsWithCategory = drawnLines.get(i);
				for (Map.Entry<String, List<Point>> entry : linePointsWithCategory.entrySet()) {
					String currentCategory = entry.getKey();
					List<Point> pointsList = entry.getValue();
					g2d.setColor(categoryColors.getOrDefault(currentCategory, Color.BLACK));

					for (int k = 0; k < pointsList.size(); k++) {
						Point point = pointsList.get(k);

						int x = point.x;
						int y = point.y;
						g2d.fillOval(x - 3, y - 3, 6, 6);

						if (k > 0) { // Draw line segments
							Point previousPoint = pointsList.get(k - 1);
							g2d.drawLine(previousPoint.x, previousPoint.y, x, y);
							g2d.drawString(currentCategory, (previousPoint.x + x) / 2, (previousPoint.y + y) / 2);
						}
					}
				}
			}

			// Dispose graphics and save image
			g2d.dispose();

			// File saving
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HH_mm_ss");
			String formattedDateTime = now.format(formatter);
			String videoNameWithoutExtension = videoName.substring(0, videoName.lastIndexOf('.'));
			File video = new File(videoNameWithoutExtension);
			if (!video.exists())
				video.mkdir();

			File imagesDir = new File(video, "images");
			if (!imagesDir.exists())
				imagesDir.mkdir();

			File outputFile = new File(imagesDir, formattedDateTime + "_frame_" + (currentIndex + 1) + ".jpg");
			ImageIO.write(imageToSave, "jpg", outputFile);
			
			// Generate text file content
	        StringBuilder annotationDetails = new StringBuilder();
	        annotationDetails.append("Frame Number; Annotation; Top-left; Bottom-right; Frame Quality; Confidence Level; Azimuth; Angle_of_inclination; position_on_the_vehicle; height_of_mounting\n");

	        // Append rectangle annotations
	        for (int i = 0; i < drawnRectangles.size(); i++) {
	            Rectangle annotation = drawnRectangles.get(i);
	            String label = annotationLabels.get(i);
	            String confidence = confidenceLevels.get(i);
	            String frameQuality = frameQualities.get(i);
	            String azimuth = azimustList.get(i);
	            String angle = angleList.get(i);
	            String position = positionList.get(i);
	            String height_of_mounting = heightList.get(i);

	            annotationDetails.append((currentIndex + 1) + "; " + label + "; (" + annotation.x + ", " + annotation.y
	                    + "); (" + (annotation.x + annotation.width) + ", " + (annotation.y + annotation.height) + "); "
	                    + frameQuality + "; " + confidence + "; " + azimuth + "; " + angle + "; " + position + "; "
	                    + height_of_mounting + "\n");
	        }

	        // Append circle annotations
	        for (int i = 0; i < drawnCircles.size(); i++) {
	            Ellipse2D.Double circle = drawnCircles.get(i);
	            String label = annotationLabelsCircles.get(i);
	            String confidence = confidenceLevelsForCircle.get(i);
	            String frameQuality = frameQualitiesForCircle.get(i);
	            String azimuth = azimustListForCircle.get(i);
	            String angle = angleListForCircle.get(i);
	            String position = positionListForCircle.get(i);
	            String height_of_mounting = heightListForCircle.get(i);

	            annotationDetails.append((currentIndex + 1) + "; " + label + "; (" + (int) circle.x + ", " + (int) circle.y
	                    + "); (" + (int) (circle.x + circle.width) + ", " + (int) (circle.y + circle.height) + "); "
	                    + frameQuality + "; " + confidence + "; " + azimuth + "; " + angle + "; " + position + "; "
	                    + height_of_mounting + "\n");
	        }

	        // Append line annotations
	        for (int i = 0; i < annotationLabelsLines.size(); i++) {
	            String label = annotationLabelsLines.get(i);
	            String confidence = confidenceLevelsForLines.get(i);
	            String frameQuality = frameQualitiesForLines.get(i);
	            String azimuth = azimustListForLines.get(i);
	            String angle = angleListForLines.get(i);
	            String position = positionListForLines.get(i);
	            String height_of_mounting = heightListForLines.get(i);

	            // Take the first and last points of the line as the bounding box
	            List<Point> linePoints = drawnLines.get(i).values().iterator().next();
	            Point start = linePoints.get(0);
	            Point end = linePoints.get(linePoints.size() - 1);

	            annotationDetails.append((currentIndex + 1) + "; " + label + "; (" + start.x + ", " + start.y + "); ("
	                    + end.x + ", " + end.y + "); " + frameQuality + "; " + confidence + "; " + azimuth + "; " + angle
	                    + "; " + position + "; " + height_of_mounting + "\n");
	        }

	        // Save annotation details to text file
	        File txtDir = new File(video, "yolo_txt");
	        if (!txtDir.exists()) txtDir.mkdir();
	        File textFile = new File(txtDir, formattedDateTime + "_frame_" + (currentIndex + 1) + ".txt");
	        try (java.io.FileWriter writer = new java.io.FileWriter(textFile)) {
	            writer.write(annotationDetails.toString());
	        }

	        JOptionPane.showMessageDialog(this, "Image saved: " + outputFile.getAbsolutePath() + "\nText file saved: " + textFile.getAbsolutePath());
	        logger.info("Image saved: " + outputFile.getAbsolutePath() + "\nText file saved: " + textFile.getAbsolutePath());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleAnnotationHover(Point hoverPoint) {
		boolean hoveringOverAnnotation = false;

		// Check for hovering over rectangles
		for (int i = 0; i < drawnRectangles.size(); i++) {
			Rectangle annotation = drawnRectangles.get(i);
			Rectangle scaledAnnotation = scaleRectangle(annotation);
			if (scaledAnnotation.contains(hoverPoint)) {
				showCustomTooltip(hoverPoint, generateTooltipText(i, "Rectangle"));
				hoveringOverAnnotation = true;
				break;
			}
		}

		// Check for hovering over circles
		if (!hoveringOverAnnotation) {
			for (int i = 0; i < drawnCircles.size(); i++) {
				Ellipse2D.Double circle = drawnCircles.get(i);
				Ellipse2D.Double scaledCircle = scaleEllipse(circle);
				if (scaledCircle.contains(hoverPoint)) {
					showCustomTooltip(hoverPoint, generateTooltipText(i, "Circle"));
					hoveringOverAnnotation = true;
					break;
				}
			}
		}

		// Check for hovering over lines
		if (!hoveringOverAnnotation) {
			for (int i = 0; i < drawnLines.size(); i++) {
				Map<String, List<Point>> linePointsWithCategory = drawnLines.get(i);
				for (Map.Entry<String, List<Point>> entry : linePointsWithCategory.entrySet()) {
					List<Point> pointsList = entry.getValue();

					// Check if hoverPoint is close to any segment of the line
					for (int j = 1; j < pointsList.size(); j++) {
						Point p1 = pointsList.get(j - 1);
						Point p2 = pointsList.get(j);
						Line2D.Double lineSegment = new Line2D.Double(p1.x * scaleFactor, p1.y * scaleFactor,
								p2.x * scaleFactor, p2.y * scaleFactor);

						// Use a distance threshold for hover detection
						if (lineSegment.ptSegDist(hoverPoint) < 5.0) { // Adjust tolerance as needed
							showCustomTooltip(hoverPoint, generateTooltipText(i, "Line"));
							hoveringOverAnnotation = true;
							break;
						}
					}
					if (hoveringOverAnnotation)
						break;
				}
				if (hoveringOverAnnotation)
					break;
			}
		}

		// If not hovering over any annotation, hide the tooltip
		if (!hoveringOverAnnotation) {
			hideCustomTooltip();
		}
	}

	private Rectangle scaleRectangle(Rectangle rect) {
		return new Rectangle((int) (rect.x * scaleFactor), (int) (rect.y * scaleFactor),
				(int) (rect.width * scaleFactor), (int) (rect.height * scaleFactor));
	}

	private Ellipse2D.Double scaleEllipse(Ellipse2D.Double ellipse) {
		return new Ellipse2D.Double(ellipse.x * scaleFactor, ellipse.y * scaleFactor, ellipse.width * scaleFactor,
				ellipse.height * scaleFactor);
	}

	private Icon createRectangleIcon() {
		int width = 20;
		int height = 20;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();

		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(2));
		g2d.drawRect(2, 5, width - 4, height - 10); // Draw a rectangle within the icon bounds

		g2d.dispose();
		return new ImageIcon(image);
	}

	private String generateTooltipText(int annotationIndex, String shapeType) {
		StringBuilder tooltip = new StringBuilder();
		tooltip.append("<html>");
		tooltip.append("<strong>Shape:</strong> ").append(shapeType).append("<br>");

		if ("Rectangle".equals(shapeType)) {
			tooltip.append("<strong>Annotation:</strong> ").append(annotationLabels.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Confidence:</strong> ").append(confidenceLevels.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Quality:</strong> ").append(frameQualities.get(annotationIndex)).append("<br>");
			tooltip.append("<strong>Azimuth:</strong> ").append(azimustList.get(annotationIndex)).append("<br>");
			tooltip.append("<strong>Angle:</strong> ").append(angleList.get(annotationIndex)).append("<br>");
			tooltip.append("<strong>Position:</strong> ").append(positionList.get(annotationIndex)).append("<br>");
			tooltip.append("<strong>Height:</strong> ").append(heightList.get(annotationIndex));
		} else if ("Circle".equals(shapeType)) {
			tooltip.append("<strong>Annotation:</strong> ").append(annotationLabelsCircles.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Confidence:</strong> ").append(confidenceLevelsForCircle.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Quality:</strong> ").append(frameQualitiesForCircle.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Azimuth:</strong> ").append(azimustListForCircle.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Angle:</strong> ").append(angleListForCircle.get(annotationIndex)).append("<br>");
			tooltip.append("<strong>Position:</strong> ").append(positionListForCircle.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Height:</strong> ").append(heightListForCircle.get(annotationIndex));
		} else if ("Line".equals(shapeType)) {
			tooltip.append("<strong>Annotation:</strong> ").append(annotationLabelsLines.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Confidence:</strong> ").append(confidenceLevelsForLines.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Quality:</strong> ").append(frameQualitiesForLines.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Azimuth:</strong> ").append(azimustListForLines.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Angle:</strong> ").append(angleListForLines.get(annotationIndex)).append("<br>");
			tooltip.append("<strong>Position:</strong> ").append(positionListForLines.get(annotationIndex))
					.append("<br>");
			tooltip.append("<strong>Height:</strong> ").append(heightListForLines.get(annotationIndex));
		}

		tooltip.append("</html>");
		return tooltip.toString();
	}

	// Define the JWindow for the custom tooltip
	private JWindow tooltipWindow = new JWindow();

	// Method to show the custom tooltip near the mouse pointer
	private void showCustomTooltip(Point location, String text) {
		// Create a JLabel to show the tooltip text
		JLabel tooltipLabel = new JLabel(text);
		tooltipLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		tooltipLabel.setOpaque(true);
		tooltipLabel.setBackground(new Color(255, 255, 204)); // Light yellow background

		// Set the tooltip content
		tooltipWindow.getContentPane().removeAll();
		tooltipWindow.getContentPane().add(tooltipLabel);
		tooltipWindow.pack();

		// Position the tooltip near the mouse pointer
		tooltipWindow.setLocation(location.x + 15, location.y + 15); // Offset the tooltip from the cursor
		tooltipWindow.setVisible(true);
	}

	// Method to hide the custom tooltip
	private void hideCustomTooltip() {
		tooltipWindow.setVisible(false);
	}

	private void showAnnotationInfo() {
		// Create a new frame to display annotation information
		JFrame infoFrame = new JFrame("Annotation Information");
		infoFrame.setSize(900, 400);
		infoFrame.setLocationRelativeTo(this); // Center relative to the main window

		// Define column names for the table
		String[] columnNames = { "Shape", "Annotation", "Confidence", "Quality", "Azimuth", "Angle", "Position",
				"Height" };

		// Prepare the data for the table by combining rectangle and circle annotations
		int totalAnnotations = annotationLabels.size() + annotationLabelsCircles.size() + annotationLabelsLines.size();
		Object[][] tableData = new Object[totalAnnotations][8];

		// Fill in rectangle annotations
		int row = 0;
		for (int i = 0; i < annotationLabels.size(); i++, row++) {
			tableData[row][0] = "Rectangle"; // Shape type
			tableData[row][1] = annotationLabels.get(i); // Annotation
			tableData[row][2] = confidenceLevels.get(i); // Confidence
			tableData[row][3] = frameQualities.get(i); // Quality
			tableData[row][4] = azimustList.get(i); // Azimuth
			tableData[row][5] = angleList.get(i); // Angle
			tableData[row][6] = positionList.get(i); // Position
			tableData[row][7] = heightList.get(i); // Height
		}

		// Fill in circle annotations
		for (int i = 0; i < annotationLabelsCircles.size(); i++, row++) {
			tableData[row][0] = "Circle"; // Shape type
			tableData[row][1] = annotationLabelsCircles.get(i); // Annotation
			tableData[row][2] = confidenceLevelsForCircle.get(i); // Confidence
			tableData[row][3] = frameQualitiesForCircle.get(i); // Quality
			tableData[row][4] = azimustListForCircle.get(i); // Azimuth
			tableData[row][5] = angleListForCircle.get(i); // Angle
			tableData[row][6] = positionListForCircle.get(i); // Position
			tableData[row][7] = heightListForCircle.get(i); // Height
		}

		for (int i = 0; i < annotationLabelsLines.size(); i++, row++) {
			tableData[row][0] = "Line";
			tableData[row][1] = annotationLabelsLines.get(i);
			tableData[row][2] = confidenceLevelsForLines.get(i);
			tableData[row][3] = frameQualitiesForLines.get(i);
			tableData[row][4] = azimustListForLines.get(i);
			tableData[row][5] = angleListForLines.get(i);
			tableData[row][6] = positionListForLines.get(i);
			tableData[row][7] = heightListForLines.get(i);
		}

		// Create the table with data
		JTable infoTable = new JTable(tableData, columnNames);

		// Create combo box editors for the dropdown columns
		String[] annotationOptions = { "pedestrian", "construction vehicle", "pile", "bucket", "fork", "stone", "truck",
				"car" };
		String[] confidenceOptions = { "High", "Medium", "Low" };
		String[] qualityOptions = { "Clear", "Blur" };

		// Set combo boxes as editors for specific columns
		TableColumn annotationColumn = infoTable.getColumnModel().getColumn(1);
		annotationColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(annotationOptions)));

		TableColumn confidenceColumn = infoTable.getColumnModel().getColumn(2);
		confidenceColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(confidenceOptions)));

		TableColumn qualityColumn = infoTable.getColumnModel().getColumn(3);
		qualityColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(qualityOptions)));

		// Add the table to a scroll pane
		JScrollPane scrollPane = new JScrollPane(infoTable);
		infoFrame.add(scrollPane, BorderLayout.CENTER);

		// Add a save button at the bottom to confirm updates
		JButton saveButton = new JButton("Save Changes");
		infoFrame.add(saveButton, BorderLayout.SOUTH);

		// Action listener to update the annotation data on save
		saveButton.addActionListener(e -> {
			int rowIndex = 0;

			// Update rectangle annotations from table data
			for (; rowIndex < annotationLabels.size(); rowIndex++) {
				annotationLabels.set(rowIndex, infoTable.getValueAt(rowIndex, 1).toString());
				confidenceLevels.set(rowIndex, infoTable.getValueAt(rowIndex, 2).toString());
				frameQualities.set(rowIndex, infoTable.getValueAt(rowIndex, 3).toString());
				azimustList.set(rowIndex, infoTable.getValueAt(rowIndex, 4).toString());
				angleList.set(rowIndex, infoTable.getValueAt(rowIndex, 5).toString());
				positionList.set(rowIndex, infoTable.getValueAt(rowIndex, 6).toString());
				heightList.set(rowIndex, infoTable.getValueAt(rowIndex, 7).toString());
			}

			// Update circle annotations from table data
			for (int i = 0; i < annotationLabelsCircles.size(); i++, rowIndex++) {
				annotationLabelsCircles.set(i, infoTable.getValueAt(rowIndex, 1).toString());
				confidenceLevelsForCircle.set(i, infoTable.getValueAt(rowIndex, 2).toString());
				frameQualitiesForCircle.set(i, infoTable.getValueAt(rowIndex, 3).toString());
				azimustListForCircle.set(i, infoTable.getValueAt(rowIndex, 4).toString());
				angleListForCircle.set(i, infoTable.getValueAt(rowIndex, 5).toString());
				positionListForCircle.set(i, infoTable.getValueAt(rowIndex, 6).toString());
				heightListForCircle.set(i, infoTable.getValueAt(rowIndex, 7).toString());
			}

			// Update line annotations from table data
			for (int i = 0; i < annotationLabelsLines.size(); i++, rowIndex++) {
				String updatedLabel = infoTable.getValueAt(rowIndex, 1).toString();
				annotationLabelsLines.set(i, updatedLabel); // Update label

				confidenceLevelsForLines.set(i, infoTable.getValueAt(rowIndex, 2).toString());
				frameQualitiesForLines.set(i, infoTable.getValueAt(rowIndex, 3).toString());
				azimustListForLines.set(i, infoTable.getValueAt(rowIndex, 4).toString());
				angleListForLines.set(i, infoTable.getValueAt(rowIndex, 5).toString());
				positionListForLines.set(i, infoTable.getValueAt(rowIndex, 6).toString());
				heightListForLines.set(i, infoTable.getValueAt(rowIndex, 7).toString());

				// Update the label in drawnLines map by finding the entry for this line
				if (i < drawnLines.size()) {
					Map<String, List<Point>> lineData = drawnLines.get(i);
					if (lineData != null && !lineData.isEmpty()) {
						// Remove the old label and add the new one with points
						String originalLabel = lineData.keySet().iterator().next();
						List<Point> linePoints = lineData.remove(originalLabel);
						lineData.put(updatedLabel, linePoints); // Replace with the updated label
					}
				}
			}

			// Refresh the UI after saving changes
			updateImage();
			infoFrame.dispose(); // Close the frame after saving
		});

		// Show the new frame
		infoFrame.setVisible(true);
	}

	private JButton createButtonWithShortcut(String text, ActionListener action, char shortcut) {
		JButton button = new JButton(text);
		button.addActionListener(action);
		button.setMnemonic(shortcut);
		return button;
	}

	private JRadioButton createRadioButtonWithShortcut(String text, ActionListener action, char shortcut) {
		JRadioButton radioButton = new JRadioButton(text);
		radioButton.addActionListener(action);
		radioButton.setMnemonic(shortcut);
		return radioButton;
	}

	private JButton createZoomButton(String symbol, ActionListener action, KeyStroke keyStroke) {
		JButton button = new JButton(symbol);
		button.setFont(new Font("SansSerif", Font.BOLD, 18)); // Explicitly use a common font like SansSerif
		button.addActionListener(action);

		// Set the button's preferred size to ensure consistency with other buttons
		button.setPreferredSize(new Dimension(30, 30));

		// Add keyboard shortcut for zoom functionality
		InputMap inputMap = button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(keyStroke, symbol);
		ActionMap actionMap = button.getActionMap();
		actionMap.put(symbol, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.actionPerformed(e);
			}
		});

		return button;
	}

	private JButton createCustomZoomButton(String symbol, ActionListener action) {
		JButton button = new JButton() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(Color.BLACK);
				g2d.setStroke(new BasicStroke(2)); // Bolder lines for visibility

				// Reduce size a bit more for padding
				int padding = 8; // Adjust padding as necessary for more or less space around the symbol
				int size = Math.min(getWidth(), getHeight()) - padding * 2;
				int centerX = getWidth() / 2;
				int centerY = getHeight() / 2;

				if ("+".equals(symbol)) {
					// Draw horizontal and vertical lines for the "+" symbol
					g2d.drawLine(centerX - size / 2, centerY, centerX + size / 2, centerY); // Horizontal line
					g2d.drawLine(centerX, centerY - size / 2, centerX, centerY + size / 2); // Vertical line
				} else if ("-".equals(symbol)) {
					// Draw only the horizontal line for the "-" symbol
					g2d.drawLine(centerX - size / 2, centerY, centerX + size / 2, centerY);
				}
			}
		};
		button.setPreferredSize(new Dimension(30, 30)); // Consistent button size
		button.addActionListener(action);
		return button;
	}

	private void zoomImage(double zoomFactor) {
		scaleFactor *= zoomFactor;
		updateImage();
	}

	private void setInitialScaleFactor() {
		// Check if currentImage is null before proceeding
		if (currentImage == null) {
			return; // Exit early if no image is loaded
		}

		// Get the viewport size of the scroll pane
		Dimension viewportSize = rightPanel.getSize();

		// Calculate scale factor based on the image dimensions and viewport dimensions
		int imageWidth = currentImage.getWidth() + 10;
		int imageHeight = currentImage.getHeight();

		if (viewportSize.width > 0 && viewportSize.height > 0) {
			double widthScaleFactor = viewportSize.getWidth() / imageWidth;
			double heightScaleFactor = viewportSize.getHeight() / imageHeight;

			// Choose the smaller scale factor to fit the image within viewport dimensions
			scaleFactor = Math.min(widthScaleFactor, heightScaleFactor);
		} else {
			// Fall back to a default scale if viewport size is not ready
			scaleFactor = 1.0;
		}

		updateImage(); // Call updateImage to apply the scaling initially
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		// Ensure initial scale factor is set only once when the window is opened
		if (scaleFactor == SCALE_FACTOR_INITIAL && currentImage != null) {
			setInitialScaleFactor();
		}
	}

	private void updateImage() {
		if (imageFrames != null && !imageFrames.isEmpty()) {
			currentImage = imageFrames.get(currentIndex);
			if (currentImage != null) {
				// Calculate the scaled dimensions based on the scale factor
				int scaledWidth = (int) (currentImage.getWidth() * scaleFactor);
				int scaledHeight = (int) (currentImage.getHeight() * scaleFactor);

				// Scale the image
				Image scaledImage = currentImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);

				// Update the label with the scaled image
				imageLabel.setIcon(new ImageIcon(scaledImage));
				imageLabel.setText("");

				// Update the preferred size of imageLabel so it fits within the scroll pane
				imageLabel.setPreferredSize(new Dimension(scaledWidth, scaledHeight));

				// Trigger revalidation and repaint of scroll pane and label
				rightPanel.revalidate();
				imageLabel.revalidate();
				imageLabel.repaint();
			}
			updateFrameNumber(currentIndex);
			repaint();
		}
	}

	private void searchFrame() {
		String input = frameSearchField.getText();
		try {
			int frameNumber = Integer.parseInt(input);
			if (frameNumber >= 1 && frameNumber <= imageFrames.size()) {
				currentIndex = frameNumber - 1;
				updateImage();
			} else {
				JOptionPane.showMessageDialog(this, "Frame number must be between 1 and " + imageFrames.size());
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Please enter a valid frame number.");
		}
		resetAnnotations();
	}

	private void resetAnnotations() {
		clearAllAnnotations();
		updateImage();
		revalidate();
		repaint();
	}

	// Initialize KeyBindings for Rectangle, Circle, and Line with shortcuts
	private void initShapeShortcuts() {
		// Rectangle shortcut: Alt + R
		imageLabel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt R"),
				"selectRectangle");
		imageLabel.getActionMap().put("selectRectangle", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (annotationButton.isSelected()) { // Only if Annotation radio button is selected
					selectedShape = "Rectangle";
					finalizeCurrentLine(); // Finalize any current line before switching shape
				}
			}
		});

		// Circle shortcut: Alt + C
		imageLabel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt C"), "selectCircle");
		imageLabel.getActionMap().put("selectCircle", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (annotationButton.isSelected()) { // Only if Annotation radio button is selected
					selectedShape = "Circle";
					finalizeCurrentLine();
				}
			}
		});

		// Line shortcut: Alt + L
		imageLabel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt L"), "selectLine");
		imageLabel.getActionMap().put("selectLine", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (annotationButton.isSelected()) { // Only if Annotation radio button is selected
					selectedShape = "Line";
					isDrawingLine = true; // Enable line drawing mode
					finalizeCurrentLine();
					getAnnotationPanel(null, "Line"); // Show annotation panel for line category selection
				}
			}
		});
	}

	private void showPreviousImage() {
		currentIndex--;
		if (currentIndex < 0) {
			currentIndex = imageFrames.size() - 1;
		}
		clearAllAnnotations();
		updateImage();
		updateFrameNumber(currentIndex);
	}

	private void showNextImage() {
		currentIndex++;
		if (currentIndex >= imageFrames.size()) {
			currentIndex = 0;
		}
		clearAllAnnotations();
		updateImage();
		updateFrameNumber(currentIndex);
	}

	private void clearAllAnnotations() {
		annotations.clear();
		drawnRectangles.clear();
		annotationLabels.clear();
		confidenceLevels.clear();
		frameQualities.clear();
		azimustList.clear();
		angleList.clear();
		positionList.clear();
		heightList.clear();

		drawnCircles.clear();
		annotationLabelsCircles.clear();
		confidenceLevelsForCircle.clear();
		frameQualitiesForCircle.clear();
		azimustListForCircle.clear();
		angleListForCircle.clear();
		positionListForCircle.clear();
		heightListForCircle.clear();

		drawnLines.clear();
		annotationLabelsLines.clear();
		confidenceLevelsForLines.clear();
		frameQualitiesForLines.clear();
		azimustListForLines.clear();
		angleListForLines.clear();
		positionListForLines.clear();
		heightListForLines.clear();

		// Reset any temporary selections
		selection = null;
		selectionCircle = null;
		currentLinePoints.clear();
	}

	private void updateFrameNumber(int frameNumber) {
		int frameNum = frameNumber + 1;
		frameNumberLbl.setText("Frame Number: " + frameNum + "/" + imageFrames.size());
	}

	private void getAnnotationPanel(Shape shape, String shapeType) {
		String[] options = { "pedestrian", "construction vehicle", "pile", "bucket", "fork", "stone", "truck", "car" };
		JComboBox<String> annotationComboBox = new JComboBox<>(options);

		// Create the main panel for all selections
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Annotation selection
		JPanel annotationPanel = new JPanel();
		annotationPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		annotationPanel.add(new JLabel("Choose an annotation:"));
		annotationPanel.add(annotationComboBox);
		panel.add(annotationPanel);

		// Quality selection
		JPanel qualityPanel = new JPanel();
		qualityPanel.setBorder(BorderFactory.createTitledBorder("Quality"));
		ButtonGroup qualityGroup = new ButtonGroup();
		JRadioButton clearButton = new JRadioButton("Clear");
		JRadioButton blurRadioButton = new JRadioButton("Blur");
		clearButton.setSelected(true);
		qualityGroup.add(clearButton);
		qualityGroup.add(blurRadioButton);
		qualityPanel.add(clearButton);
		qualityPanel.add(blurRadioButton);
		panel.add(qualityPanel);

		// Confidence Level selection
		JPanel confidencePanel = new JPanel();
		confidencePanel.setBorder(BorderFactory.createTitledBorder("Confidence Level"));
		ButtonGroup confidenceGroup = new ButtonGroup();
		JRadioButton highConfidenceButton = new JRadioButton("High");
		JRadioButton mediumConfidenceButton = new JRadioButton("Medium");
		JRadioButton lowConfidenceButton = new JRadioButton("Low");
		highConfidenceButton.setSelected(true);
		confidenceGroup.add(highConfidenceButton);
		confidenceGroup.add(mediumConfidenceButton);
		confidenceGroup.add(lowConfidenceButton);
		confidencePanel.add(highConfidenceButton);
		confidencePanel.add(mediumConfidenceButton);
		confidencePanel.add(lowConfidenceButton);
		panel.add(confidencePanel);

		// Sensor Position section
		JPanel sensorPanel = new JPanel();
		sensorPanel.setBorder(BorderFactory.createTitledBorder("Sensor Position"));
		sensorPanel.setLayout(new GridLayout(4, 2));

		JTextField azimuthField = createLimitedTextField(50);
		JTextField angleField = createLimitedTextField(50);
		JTextField positionField = createLimitedTextField(50);
		JTextField heightField = createLimitedTextField(50);

		sensorPanel.add(new JLabel("Azimuth:"));
		sensorPanel.add(azimuthField);
		sensorPanel.add(new JLabel("Angle of inclination:"));
		sensorPanel.add(angleField);
		sensorPanel.add(new JLabel("Position on the vehicle:"));
		sensorPanel.add(positionField);
		sensorPanel.add(new JLabel("Height of mounting:"));
		sensorPanel.add(heightField);

		panel.add(sensorPanel);

		// Create a custom dialog without the default icon
		JDialog dialog = new JDialog((Frame) null, "Select Annotation, Quality, Confidence, and Sensor Position", true);
		dialog.getContentPane().add(panel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");

		okButton.addActionListener(e -> {
			// Process the OK action here
			String label = (String) annotationComboBox.getSelectedItem();
			String selectedQuality = clearButton.isSelected() ? "Clear" : "Blur";
			String confidenceLevel = highConfidenceButton.isSelected() ? "High"
					: mediumConfidenceButton.isSelected() ? "Medium" : "Low";
			currentLineCategory = label;
			currentLineColor = categoryColors.getOrDefault(currentLineCategory, Color.BLACK);
			isDrawingLine = true;

			if ("Rectangle".equals(shapeType)) {
				annotationLabels.add(label);
				frameQualities.add(selectedQuality);
				confidenceLevels.add(confidenceLevel);
				azimustList.add(azimuthField.getText());
				angleList.add(angleField.getText());
				positionList.add(positionField.getText());
				heightList.add(heightField.getText());
				undoActionStack.push(new UndoAction("Rectangle", drawnRectangles.size() - 1));
			} else if ("Circle".equals(shapeType)) {
				annotationLabelsCircles.add(label);
				frameQualitiesForCircle.add(selectedQuality);
				confidenceLevelsForCircle.add(confidenceLevel);
				azimustListForCircle.add(azimuthField.getText());
				angleListForCircle.add(angleField.getText());
				positionListForCircle.add(positionField.getText());
				heightListForCircle.add(heightField.getText());
				undoActionStack.push(new UndoAction("Circle", drawnCircles.size() - 1));
			} else if ("Line".equals(shapeType)) {
				annotationLabelsLines.add(label);
				frameQualitiesForLines.add(selectedQuality);
				confidenceLevelsForLines.add(confidenceLevel);
				azimustListForLines.add(azimuthField.getText());
				angleListForLines.add(angleField.getText());
				positionListForLines.add(positionField.getText());
				heightListForLines.add(heightField.getText());
//	                undoActionStack.push(new UndoAction("Line", drawnLines.size() - 1));

				undoActionStack.push(new UndoAction("Line", annotationLabelsLines.size() - 1, drawnLines.size()));
			}

			dialog.dispose();
			repaint();
		});

		cancelButton.addActionListener(e -> dialog.dispose());

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private void blurSelection(Rectangle rect) {
		if (currentImage == null) {
			JOptionPane.showMessageDialog(this, "No image loaded to apply blur.");
			return;
		}

		// Ensure the blur region is within the bounds of the original image
		int startX = Math.max(0, Math.min(rect.x, currentImage.getWidth() - 1));
		int startY = Math.max(0, Math.min(rect.y, currentImage.getHeight() - 1));
		int width = Math.min(rect.width, currentImage.getWidth() - startX);
		int height = Math.min(rect.height, currentImage.getHeight() - startY);

		int kernelSize = 5; // Define blur intensity
		for (int x = startX; x < startX + width; x++) {
			for (int y = startY; y < startY + height; y++) {
				int blurredRGB = getAverageColor(currentImage, x, y, kernelSize);
				currentImage.setRGB(x, y, blurredRGB);
			}
		}

		// Update displayed image to reflect changes
		updateImage();
	}

	private int getAverageColor(BufferedImage img, int x, int y, int kernelSize) {
		int r = 0, g = 0, b = 0;
		int count = 0;

		for (int i = -kernelSize; i <= kernelSize; i++) {
			for (int j = -kernelSize; j <= kernelSize; j++) {
				int nx = x + i;
				int ny = y + j;
				if (nx >= 0 && nx < img.getWidth() && ny >= 0 && ny < img.getHeight()) {
					Color pixel = new Color(img.getRGB(nx, ny));
					r += pixel.getRed();
					g += pixel.getGreen();
					b += pixel.getBlue();
					count++;
				}
			}
		}

		return new Color(r / count, g / count, b / count).getRGB();
	}

	private JTextField createLimitedTextField(int maxLength) {
		JTextField textField = new JTextField();
		((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
			@Override
			public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
					throws BadLocationException {
				if (string != null && (fb.getDocument().getLength() + string.length() <= maxLength)) {
					super.insertString(fb, offset, string, attr);
				}
			}

			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
					throws BadLocationException {
				if (text != null && (fb.getDocument().getLength() + text.length() - length <= maxLength)) {
					super.replace(fb, offset, length, text, attrs);
				}
			}

			@Override
			public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
				super.remove(fb, offset, length);
			}
		});
		return textField;
	}

//	    private void undoLastAction() {
//	        if (!annotations.isEmpty()) {
//	            annotations.remove(annotations.size() - 1);
//	        }
//	        if (!drawnRectangles.isEmpty()) {
//	            drawnRectangles.remove(drawnRectangles.size() - 1);
//	        }
//	        if (!annotationLabels.isEmpty()) {
//	            annotationLabels.remove(annotationLabels.size() - 1);
//	        }
//	        if (!confidenceLevels.isEmpty()) {
//	            confidenceLevels.remove(confidenceLevels.size() - 1);
//	        }
//	        if (!frameQualities.isEmpty()) {
//	            frameQualities.remove(frameQualities.size() - 1);
//	        }
//	        if (!azimustList.isEmpty()) {
//	            azimustList.remove(azimustList.size() - 1);
//	        }
//	        if (!angleList.isEmpty()) {
//	            angleList.remove(angleList.size() - 1);
//	        }
//	        if (!positionList.isEmpty()) {
//	            positionList.remove(positionList.size() - 1);
//	        }
//	        if (!heightList.isEmpty()) {
//	            heightList.remove(heightList.size() - 1);
//	        }
//	        updateImage();
//	        repaint();
//	    }

	private void undoLastAction() {
		if (undoActionStack.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No actions to undo.");
			return;
		}

		// Pop the last action
		UndoAction lastAction = undoActionStack.pop();

		// Remove the last action based on its type and index or key
		switch (lastAction.shapeType) {
		case "Rectangle":
			if (lastAction.index >= 0 && lastAction.index < drawnRectangles.size()) {
				drawnRectangles.remove(lastAction.index);
				annotationLabels.remove(lastAction.index);
				confidenceLevels.remove(lastAction.index);
				frameQualities.remove(lastAction.index);
				azimustList.remove(lastAction.index);
				angleList.remove(lastAction.index);
				positionList.remove(lastAction.index);
				heightList.remove(lastAction.index);
			}
			break;

		case "Circle":
			if (lastAction.index >= 0 && lastAction.index < drawnCircles.size()) {
				drawnCircles.remove(lastAction.index);
				annotationLabelsCircles.remove(lastAction.index);
				confidenceLevelsForCircle.remove(lastAction.index);
				frameQualitiesForCircle.remove(lastAction.index);
				azimustListForCircle.remove(lastAction.index);
				angleListForCircle.remove(lastAction.index);
				positionListForCircle.remove(lastAction.index);
				heightListForCircle.remove(lastAction.index);
			}
			break;

		case "Line":
			if (lastAction.lineKey != null && drawnLines.containsKey(lastAction.lineKey)) {
				Map<String, List<Point>> linePointsWithCategory = drawnLines.get(lastAction.lineKey);
				List<Point> pointsToRemove = linePointsWithCategory.values().stream().findFirst().orElse(null);

				// Remove the line and clear points if necessary
				drawnLines.remove(lastAction.lineKey);
				if (pointsToRemove != null && pointsToRemove.equals(currentLinePoints)) {
					currentLinePoints.clear();
				}

				// Remove associated data
				annotationLabelsLines.remove(lastAction.index);
				confidenceLevelsForLines.remove(lastAction.index);
				frameQualitiesForLines.remove(lastAction.index);
				azimustListForLines.remove(lastAction.index);
				angleListForLines.remove(lastAction.index);
				positionListForLines.remove(lastAction.index);
				heightListForLines.remove(lastAction.index);
			}
			break;
		}

		// Final repaint to ensure the UI reflects the undo action
		updateImage();
		repaint();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			List<BufferedImage> images = new ArrayList<>();
			try {
				images.add(ImageIO.read(new File(
						"C:\\Users\\podap\\Downloads\\FrameAnnotator-master\\FrameAnnotator-master\\src\\main\\resources\\image1.jpg")));
				images.add(ImageIO.read(new File(
						"C:\\Users\\podap\\Downloads\\FrameAnnotator-master\\FrameAnnotator-master\\src\\main\\resources\\image2.jpg")));
				images.add(ImageIO.read(new File(
						"C:\\Users\\podap\\Downloads\\FrameAnnotator-master\\FrameAnnotator-master\\src\\main\\resources\\image3.jpg")));
			} catch (IOException e) {
				e.printStackTrace();
			}
			new ImageDisplay(images, "sample_video.mp4");
		});
	}
}
