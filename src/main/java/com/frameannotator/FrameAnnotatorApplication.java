package com.frameannotator;
import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.image.ImageDisplay;
import com.video.VideoFrameExtractor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

@SpringBootApplication
public class FrameAnnotatorApplication {
	private static final Logger logger = LoggerFactory.getLogger(FrameAnnotatorApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(FrameAnnotatorApplication.class, args);
        
        SwingUtilities.invokeLater(() -> {
        	try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Create a file chooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select a Video File");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            
            
            // Add a filter for video files (optional)
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Video Files", "mp4", "mkv", "avi"));

            // Show the dialog and get the user's selection
            int userSelection = fileChooser.showOpenDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File videoFile = fileChooser.getSelectedFile();
                String videoPath = videoFile.getAbsolutePath(); // Get the selected video path
                
                // Extract frames from the selected video
                
                List<BufferedImage> extractedImages = VideoFrameExtractor.extractFrames(videoPath, 30);
                logger.info("Extracted frames from the video.");
                // Display the extracted images in a new window
                logger.info("Loading the the extracted images in tool.");
                new ImageDisplay(extractedImages, videoFile.getName());
            } else {
            	logger.error("No video file selected.");
                System.out.println("No file selected.");
            }
        });
    }
}
