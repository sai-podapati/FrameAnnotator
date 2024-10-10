package com.image;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDisplay extends JFrame {
	private JLabel imageLabel;
	private JLabel frameNumberLbl;
	private JTextField frameSearchField;
	private List<BufferedImage> imageFrames;
	private int currentIndex;
	private JPanel leftPanel;
	private JPanel rightPanel;

	private JRadioButton blurButton;
	private JRadioButton annotationButton;

	private BufferedImage currentImage;
	private Rectangle selection;
	private Point startPoint;
	private List<Rectangle> annotations = new ArrayList<>();
	private List<Rectangle> drawnRectangles = new ArrayList<>();
	private List<String> annotationLabels = new ArrayList<>();
	private List<String> confidenceLevels = new ArrayList<>();
	private List<String> frameQualities = new ArrayList<>(); // Store frame qualities separately
	private List<String> azimustList = new ArrayList<>();
	private List<String> angleList = new ArrayList<>();
	private List<String> positionList = new ArrayList<>();
	private List<String> heightList = new ArrayList<>();
	private static final Logger logger = LoggerFactory.getLogger(ImageDisplay.class);
	private Map<String, Color> categoryColors = new HashMap<>() {
		{
			put("pedestrian", Color.RED);
			put("construction vehicle", Color.BLUE);
			put("pile", Color.GREEN);
			put("bucket", Color.ORANGE);
			put("fork", Color.ORANGE);
			put("stone", Color.YELLOW);
			put("truck", Color.MAGENTA);
			put("car", Color.CYAN);
		}
	};

	private double scaleFactor = 1.0;

	public ImageDisplay(List<BufferedImage> images, String videoName) {
	    this.imageFrames = images;
	    this.currentIndex = 0;

	    setTitle("Annotator");
	    try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    setLayout(new BorderLayout());

	    leftPanel = new JPanel();
	    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

	    frameNumberLbl = new JLabel("");

	    JPanel goPanel = new JPanel();
	    goPanel.setLayout(new FlowLayout(FlowLayout.LEFT, -10, 5));

	    JLabel goToLabel = new JLabel(" 		   Go To:  					 ");
	    goPanel.add(goToLabel);
	    frameSearchField = new JTextField(3);
	    frameSearchField.setPreferredSize(new Dimension(150, 30));
	    frameSearchField.addActionListener(e -> searchFrame());
	    goPanel.add(frameSearchField);

	    blurButton = createRadioButtonWithShortcut("Masking", e -> buttonChange(), 'M');
	    annotationButton = createRadioButtonWithShortcut("Annotation", e -> buttonChange(), 'A');
	    blurButton.setSelected(true);
	    blurButton.addActionListener(e -> buttonChange());
	    annotationButton.addActionListener(e -> buttonChange());

	    ButtonGroup group = new ButtonGroup();
	    group.add(blurButton);
	    group.add(annotationButton);

	    leftPanel.add(frameNumberLbl);
	    leftPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing
	    leftPanel.add(blurButton);
	    leftPanel.add(annotationButton);
	    leftPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add spacing
	    leftPanel.add(goPanel);
		
		add(leftPanel, BorderLayout.WEST);

		leftPanel.add(Box.createRigidArea(new Dimension(0, 30))); // Add some space

		imageLabel = new JLabel() {
			@Override
			protected void paintComponent(Graphics g) {

				super.paintComponent(g);
				if (selection != null) {
					g.setColor(Color.YELLOW);
					g.drawRect(selection.x, selection.y, selection.width, selection.height);
				}

				for (int i = 0; i < annotations.size(); i++) {
					Rectangle annotation = annotations.get(i);
					String label = annotationLabels.get(i);
					g.setColor(categoryColors.getOrDefault(label, Color.WHITE));
					g.drawRect((int) (annotation.x * scaleFactor), (int) (annotation.y * scaleFactor),
							(int) (annotation.width * scaleFactor), (int) (annotation.height * scaleFactor));
					g.setFont(new Font("Arial", Font.BOLD, 16));
					g.drawString(label, (int) (annotation.x * scaleFactor), (int) ((annotation.y) * scaleFactor));
				}
			}
		};
		imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		updateImage();
		rightPanel = new JPanel();
		rightPanel.setBounds(0, 0, currentImage.getWidth(), currentImage.getHeight());
		imageLabel.setBounds(0, 0, currentImage.getWidth(), currentImage.getHeight());
		rightPanel.add(imageLabel);
		add(rightPanel, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel();
		
		bottomPanel.add(createButtonWithShortcut("Previous", e -> showPreviousImage(), 'P'));
		bottomPanel.add(createButtonWithShortcut("Next", e -> showNextImage(), 'N'));
		bottomPanel.add(createButtonWithShortcut("Save", e -> saveImage(videoName), 'S'));
		bottomPanel.add(createButtonWithShortcut("Undo", e -> undoLastAction(), 'U'));
		bottomPanel.add(createButtonWithShortcut("Reset", e -> resetAnnotations(), 'R'));

		add(bottomPanel, BorderLayout.SOUTH);

		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setVisible(true);

		SwingUtilities.invokeLater(this::updateImage);

		imageLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (blurButton.isSelected()) {
					startPoint = e.getPoint();
					selection = new Rectangle(startPoint);
				} else {
					startPoint = e.getPoint();
					selection = new Rectangle(startPoint);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (selection != null) {
					Rectangle scaledSelection = scaleRectangleToOriginal(selection);

					if (blurButton.isSelected()) {
						repaint();
						blurSelection(scaledSelection);
					} else {
						drawnRectangles.add(new Rectangle(scaledSelection));
						getAnnotationPanel(scaledSelection);
					}
					selection = null;
					repaint();
				}
			}

		});

		imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (selection != null) {
					int x = Math.min(Math.max(e.getX(), 0), imageLabel.getWidth());
					int y = Math.min(Math.max(e.getY(), 0), imageLabel.getHeight());
					selection.setBounds(Math.min(startPoint.x, x), Math.min(startPoint.y, y),
							Math.abs(startPoint.x - x), Math.abs(startPoint.y - y));
					repaint();
				}
			}
		});

		imageLabel.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "removeSelection");
		imageLabel.getActionMap().put("removeSelection", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selection = null;
				repaint();
			}
		});
		
		updateFrameNumber(currentIndex);
	}
	
	private void updateFrameNumber(int frameNumber) {
		int frameNum = frameNumber + 1;
		frameNumberLbl.setText(" Frame Number: "+ frameNum);
	}
	
	private JButton createButtonWithShortcut(String text, ActionListener action, char shortcut) {
	    JButton button = new JButton(text);
	    button.addActionListener(action);
	    
	    button.setMnemonic(shortcut);
	    
	    ActionMap actionMap = button.getActionMap();
	    actionMap.put("action", new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	            action.actionPerformed(e);
	        }
	    });
	    
	    return button;
	}
	
	private JRadioButton createRadioButtonWithShortcut(String text, ActionListener action, char shortcut) {
	    JRadioButton radioButton = new JRadioButton(text);
	    radioButton.addActionListener(action);
	    
	    radioButton.setMnemonic(shortcut);
	    
	    ActionMap actionMap = radioButton.getActionMap();
	    actionMap.put("action", new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	            radioButton.setSelected(true);
	            action.actionPerformed(e);
	        }
	    });
	    
	    return radioButton;
	}

	
	private void searchFrame() {
	    String input = frameSearchField.getText();
	    try {
	        int frameNumber = Integer.parseInt(input);
	        if (frameNumber >= 1 && frameNumber <= imageFrames.size()) {
	            currentIndex = frameNumber - 1; // Adjust for zero-based index
	            updateImage(); // Update the image to the specified frame
	        } else {
	            JOptionPane.showMessageDialog(this, "Frame number must be between 1 and " + imageFrames.size());
	        }
	    } catch (NumberFormatException e) {
	        JOptionPane.showMessageDialog(this, "Please enter a valid frame number.");
	    }
	    resetAnnotations();
	}
	
	private void resetAnnotations() {
	    annotations.clear();
	    drawnRectangles.clear();
	    annotationLabels.clear();
	    confidenceLevels.clear();
	    frameQualities.clear();
	    azimustList.clear();
		angleList.clear();
		positionList.clear();
		heightList.clear();
	    selection = null;
	    updateImage();
	    revalidate();
	    repaint();
	}
	
	private void undoLastAction() {
	    if (!annotations.isEmpty()) {
	        annotations.remove(annotations.size() - 1);
	    }
	    if (!drawnRectangles.isEmpty()) {
	        drawnRectangles.remove(drawnRectangles.size() - 1);
	    }
	    if (!annotationLabels.isEmpty()) {
	        annotationLabels.remove(annotationLabels.size() - 1);
	    }
	    if (!confidenceLevels.isEmpty()) {
	        confidenceLevels.remove(confidenceLevels.size() - 1);
	    }
	    if (!frameQualities.isEmpty()) {
	        frameQualities.remove(frameQualities.size() - 1);
	    }
	    if (!azimustList.isEmpty()) {
	    	azimustList.remove(azimustList.size() - 1);
	    }
	    if (!angleList.isEmpty()) {
	    	angleList.remove(angleList.size() - 1);
	    }
	    if (!positionList.isEmpty()) {
	    	positionList.remove(positionList.size() - 1);
	    }
	    if (!heightList.isEmpty()) {
	    	heightList.remove(heightList.size() - 1);
	    }
	    updateImage();
	    repaint();
	}


	private void getAnnotationPanel(Rectangle scaledSelection) {
		String[] options = { "pedestrian", "construction vehicle", "pile", "bucket", "fork", "stone", "truck", "car" };
		JComboBox<String> annotationComboBox = new JComboBox<>(options); // Using JComboBox for better selection

		// Create the main panel for all selections
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding around the panel

		// Annotation selection
		JPanel annotationPanel = new JPanel();
		annotationPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // Align left
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

		// Add space between sections
		panel.add(Box.createRigidArea(new Dimension(0, 10))); // Add space before dialog

		// Sensor Position section
		JPanel sensorPanel = new JPanel();
		sensorPanel.setBorder(BorderFactory.createTitledBorder("Sensor Position"));
		sensorPanel.setLayout(new GridLayout(4, 2)); // 4 rows, 2 columns

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

		panel.add(sensorPanel); // Add the sensor panel to the main panel

		// Show dialog
		int result = JOptionPane.showConfirmDialog(null, panel,
				"Select Annotation, Quality, Confidence, and Sensor Position", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			String label = (String) annotationComboBox.getSelectedItem();
			if (label != null) {
				annotations.add(new Rectangle(scaledSelection));
				annotationLabels.add(label);
				String selectedQuality = clearButton.isSelected() ? "Clear" : "Blur";
				frameQualities.add(selectedQuality); // Save the selected quality

				// Add confidence level for this annotation
				confidenceLevels.add(highConfidenceButton.isSelected() ? "High"
						: mediumConfidenceButton.isSelected() ? "Medium" : "Low");
				azimustList.add(azimuthField.getText());
				angleList.add(angleField.getText());
				positionList.add(positionField.getText());
				heightList.add(heightField.getText());
			}
		}
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

	private void buttonChange() {
		leftPanel.revalidate(); // Revalidate to update the UI layout
		leftPanel.repaint();
		repaint();
	}

	private void showPreviousImage() {
		currentIndex--;
		if (currentIndex < 0) {
			currentIndex = imageFrames.size() - 1;
		}
		clearAnnotations();
		updateImage();
		updateFrameNumber(currentIndex);
	}

	private void showNextImage() {
		currentIndex++;
		if (currentIndex >= imageFrames.size()) {
			currentIndex = 0;
		}
		clearAnnotations();
		updateImage();
		updateFrameNumber(currentIndex);
	}

	private void clearAnnotations() {
		annotations.clear();
		annotationLabels.clear();
		confidenceLevels.clear(); // Clear confidence levels
		frameQualities.clear(); // Clear frame qualities
	}

	private void updateImage() {
		currentImage = imageFrames.get(currentIndex);
		if (currentImage != null) {
			scaleFactor = Math.min((double) (getWidth() - 200) / currentImage.getWidth(),
					(double) getHeight() / currentImage.getHeight());

			Image scaledImage = currentImage.getScaledInstance((int) (currentImage.getWidth() * scaleFactor),
					(int) (currentImage.getHeight() * scaleFactor), Image.SCALE_SMOOTH);

			imageLabel.setIcon(new ImageIcon(scaledImage));
			imageLabel.setText("");
		}
		updateFrameNumber(currentIndex);
		repaint();
	}

	private void saveImage(String videoName) {
		try {
			BufferedImage imageToSave = currentImage;
			Graphics2D g2d = imageToSave.createGraphics();

			StringBuilder annotationDetails = new StringBuilder();
			annotationDetails.append(
					"Frame Number; Annotation; Top-left; Bottom-right; Frame Quality; Confidence Level; Azimuth; Angle_of_inclination; position_on_the_vehicle; height_of_mounting\n");

			// draw rectangles
			for (int i = 0; i < annotations.size(); i++) {
				Rectangle annotation = annotations.get(i);
				String label = annotationLabels.get(i);
				String confidence = confidenceLevels.get(i); // Get the confidence level
				String frameQuality = frameQualities.get(i); // Get the frame quality for this annotation
				String azimuth = azimustList.get(i);
				String angle = angleList.get(i);
				String position = positionList.get(i);
				String height_of_mounting = heightList.get(i);

				g2d.setColor(categoryColors.getOrDefault(label, Color.WHITE));
				g2d.setStroke(new BasicStroke(4));
				g2d.drawRect(annotation.x, annotation.y, annotation.width, annotation.height);
				g2d.setFont(new Font("Arial", Font.BOLD, 50));
				g2d.drawString(label, annotation.x, annotation.y - 5);

				// Append annotation details including confidence level
				annotationDetails.append((currentIndex + 1) + "; " + label + "; (" + annotation.x + ", " + annotation.y
						+ "); (" + (annotation.x + annotation.width) + ", " + (annotation.y + annotation.height) + "); "
						+ frameQuality + "; " + confidence + "; " + azimuth + "; " + angle + "; " + position + "; "
						+ height_of_mounting + "\n");
			}

			g2d.dispose();

			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HH_mm_ss");
			String formattedDateTime = now.format(formatter);
			String videoNameWithoutExtension = videoName.substring(0, videoName.lastIndexOf('.'));
			File video = new File(videoNameWithoutExtension);
			if (!video.exists()) {
				video.mkdir();
			}
			File imagesDir = new File(video, "images");
			if (!imagesDir.exists()) {
				imagesDir.mkdir();
			}
			File outputFile = new File(imagesDir, formattedDateTime + "_frame_" + (currentIndex + 1) + ".jpg");
			ImageIO.write(imageToSave, "jpg", outputFile);
			File txtDir = new File(video, "yolo_txt");
			if (!txtDir.exists()) {
				txtDir.mkdir();
			}
			File textFile = new File(txtDir, formattedDateTime + "_frame_" + (currentIndex + 1) + ".txt");
			try (java.io.FileWriter writer = new java.io.FileWriter(textFile)) {
				writer.write(annotationDetails.toString());
			}

			JOptionPane.showMessageDialog(this, "Image saved: " + outputFile.getAbsolutePath() + "\nText file saved: "
					+ textFile.getAbsolutePath());
			logger.info("Image saved: " + outputFile.getAbsolutePath() + "\nText file saved: "
					+ textFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void blurSelection(Rectangle rect) {
		// Ensure the rectangle is within the bounds of the current image
		int startX = Math.max(rect.x, 0);
		int startY = Math.max(rect.y, 0);
		int endX = Math.min(rect.x + rect.width, currentImage.getWidth());
		int endY = Math.min(rect.y + rect.height, currentImage.getHeight());

		// Apply blur directly on the original image
		for (int x = startX; x < endX; x++) {
			for (int y = startY; y < endY; y++) {
				// Get the average color of the surrounding pixels
				if (x <= 160) {
					int blurredRGB = getAverageColor(currentImage, 0, y, 10); // Adjust kernel size as needed
					currentImage.setRGB(0, y, blurredRGB);
				} else {
					int blurredRGB = getAverageColor(currentImage, x, y, 10); // Adjust kernel size as needed
					currentImage.setRGB(x, y, blurredRGB);
				}
			}
		}

		updateImage(); // Update the displayed image
	}

	// Increase the blur strength by increasing the kernel size
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

		r /= count;
		g /= count;
		b /= count;

		return new Color(r, g, b).getRGB();
	}

	private Rectangle scaleRectangleToOriginal(Rectangle rect) {
		int scaledX = (int) (rect.x / scaleFactor);
		int scaledY = (int) (rect.y / scaleFactor);
		int scaledWidth = (int) (rect.width / scaleFactor);
		int scaledHeight = (int) (rect.height / scaleFactor);
		return new Rectangle(scaledX, scaledY, scaledWidth, scaledHeight);
	}

}
