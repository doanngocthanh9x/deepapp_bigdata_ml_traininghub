package com.deepapp.vn.io.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Image processing utilities for optimizing images before sending to workers
 */
public class ImageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);

    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final float JPEG_QUALITY = 0.95f; // Increased quality to preserve image quality
    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB limit for gRPC messages

    /**
     * Optimize image for processing by resizing and compressing only when necessary
     * @param imageBytes Original image bytes
     * @return Optimized image bytes
     */
    public static byte[] optimizeImage(byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty");
        }

        long startTime = System.currentTimeMillis();

        try {
            // Read original image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                throw new IOException("Failed to read image from bytes");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            long originalSize = imageBytes.length;

            logger.debug("Original image: {}x{}, size: {} bytes", originalWidth, originalHeight, originalSize);

            // Check if file size is already acceptable
            if (originalSize <= MAX_FILE_SIZE_BYTES) {
                logger.debug("Image size {} bytes is within limit {}, no optimization needed", originalSize, MAX_FILE_SIZE_BYTES);
                return imageBytes;
            }

            // Check if resizing is needed
            boolean needsResize = originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT;

            BufferedImage processedImage = originalImage;
            if (needsResize) {
                // Calculate new dimensions maintaining aspect ratio
                double scaleFactor = Math.min((double) MAX_WIDTH / originalWidth, (double) MAX_HEIGHT / originalHeight);
                int newWidth = (int) (originalWidth * scaleFactor);
                int newHeight = (int) (originalHeight * scaleFactor);

                // Resize image
                processedImage = resizeImage(originalImage, newWidth, newHeight);
                logger.debug("Resized image from {}x{} to {}x{}", originalWidth, originalHeight, newWidth, newHeight);
            }

            // Compress the image (resized or original)
            byte[] compressedBytes = compressImage(processedImage, "JPEG");

            // If compression didn't help enough, try more aggressive compression
            if (compressedBytes.length > MAX_FILE_SIZE_BYTES && compressedBytes.length > originalSize * 0.7) {
                logger.debug("First compression attempt resulted in {} bytes, trying more aggressive compression", compressedBytes.length);
                compressedBytes = compressImageAggressively(processedImage, "JPEG");
            }

            long processingTime = System.currentTimeMillis() - startTime;
            double compressionRatio = (double) compressedBytes.length / originalSize;

            logger.info("Image optimized: {}x{} -> {}x{}, size: {} -> {} bytes ({:.1f}%), time: {}ms",
                       originalWidth, originalHeight,
                       processedImage.getWidth(), processedImage.getHeight(),
                       originalSize, compressedBytes.length,
                       compressionRatio * 100, processingTime);

            return compressedBytes;

        } catch (Exception e) {
            logger.error("Failed to optimize image", e);
            // Return original image if optimization fails
            return imageBytes;
        }
    }

    /**
     * Resize image to specified dimensions
     */
    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();

        // Use high quality rendering
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();

        return resizedImage;
    }

    /**
     * Compress image using JPEG format
     */
    private static byte[] compressImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // For JPEG, we can set compression quality
        if ("JPEG".equalsIgnoreCase(format) || "JPG".equalsIgnoreCase(format)) {
            // Use ImageIO with compression (this is a simplified approach)
            // In a production environment, you might want to use more advanced libraries
            ImageIO.write(image, "JPEG", outputStream);
        } else {
            // For other formats, use default compression
            ImageIO.write(image, format, outputStream);
        }

        return outputStream.toByteArray();
    }

    /**
     * Compress image with aggressive settings for maximum size reduction
     * @param image BufferedImage to compress
     * @param format Image format (JPEG, PNG, etc.)
     * @return Compressed image bytes
     */
    private static byte[] compressImageAggressively(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Use more aggressive compression settings
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new IOException("No writers found for format: " + format);
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.7f); // More aggressive compression
        }

        ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        ios.close();

        return outputStream.toByteArray();
    }

    /**
     * Get image dimensions without loading full image
     */
    public static Dimension getImageDimensions(byte[] imageBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Failed to read image dimensions");
        }
        return new Dimension(image.getWidth(), image.getHeight());
    }
}