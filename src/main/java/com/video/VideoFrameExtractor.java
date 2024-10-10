package com.video;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VideoFrameExtractor {
	private static final Logger logger = LoggerFactory.getLogger(VideoFrameExtractor.class);

    public static List<BufferedImage> extractFrames(String videoPath, int framesPerMinute) {
    	
        if(!loadLib("opencv_java490")) {
        	System.exit(0);
        }
        List<BufferedImage> images = new ArrayList<>();
        VideoCapture capture = new VideoCapture(videoPath);
        logger.info("Generating frame for the video.");
        // Generate video metadata
        generateMetadata(videoPath, capture);
        
        Mat frame = new Mat();
        int frameCount = 0;

        // Calculate the interval of frames to extract based on the provided frames per minute
        int interval = (int) Math.round(30.0 / (framesPerMinute / 60.0));

        while (capture.read(frame)) {
            if (frameCount % interval == 0) {
                BufferedImage bufferedImage = matToBufferedImage(frame);
                images.add(bufferedImage);
            }
            frameCount++;
        }
        capture.release();
        

        
        return images;
    }
    
    private static BufferedImage matToBufferedImage(Mat mat) {
        // Convert BGR Mat to BufferedImage
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();
        byte[] data = new byte[width * height * channels];
        mat.get(0, 0, data);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = data[y * width * channels + x * channels + 2] & 0xFF;
                int g = data[y * width * channels + x * channels + 1] & 0xFF;
                int b = data[y * width * channels + x * channels] & 0xFF;
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    private static void generateMetadata(String videoPath, VideoCapture capture) {
        // Check if the video capture is opened successfully
        if (!capture.isOpened()) {
        	logger.error("Could not open video file.");
            System.err.println("Error: Could not open video file.");
            return;
        }
        File videoFile = new File(videoPath);
        String title = videoFile.getName(); 
        // Get video properties
        //String title = videoPath.substring(videoPath.lastIndexOf('/') + 1); // Simple title extraction
        double duration = capture.get(Videoio.CAP_PROP_FRAME_COUNT) / capture.get(Videoio.CAP_PROP_FPS);
        int width = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double frameRate = capture.get(Videoio.CAP_PROP_FPS);
        String dateTime = java.time.LocalDateTime.now().toString();

        // Format metadata
        String metadata = String.format("Video Title; Duration (seconds); Resolution; Frame Rate; Annotation Type; Confidence Level; Annotation Tool; Date & Time\n" +
                        "%s; %.2f; %dx%d; %.2f; %s; %s; %s; %s\n",
                title, duration, width, height, frameRate,"Bounding Box", " ","Cyient Annotator", dateTime);

        // Print metadata for debugging
		String videoNameWithoutExtension = title.substring(0, title.lastIndexOf('.'));
        File video = new File(videoNameWithoutExtension);
		if (!video.exists()) {
			video.mkdir();
		}
		File textFile = new File(video,title + "_metadata.txt");
        // Write metadata to text file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(textFile))) {
            writer.write(metadata);
            logger.info("Metadata file created successfully: " + title + "_metadata.txt");
            System.out.println("Metadata file created successfully: " + title + "_metadata.txt");
        } catch (IOException e) {
        	logger.error("Error writing metadata file: ", e);
            System.err.println("Error writing metadata file: " + e.getMessage());
        }
    }
    
    private static boolean loadLib(final String libName) {
        final String fileName = libName + ".dll";
        try {
            File dllFile = new File(fileName);
            if (!dllFile.exists()) {
                logger.error("DLL file not found: " + dllFile.getAbsolutePath());
                return false;
            }

            System.load(dllFile.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            logger.error("UnsatisfiedLinkError: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Cannot load DLL: " + e);
            return false;
        }
        return true;
    }


}
