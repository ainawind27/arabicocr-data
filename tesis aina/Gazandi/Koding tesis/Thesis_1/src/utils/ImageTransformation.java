package utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImageTransformation {

    private static BufferedImage imageSource;
    private static BufferedImage imageGrayscale;
    private static BufferedImage imageBinary;
    private static int width;
    private static int height;
    private static int threshold;

    public ImageTransformation(BufferedImage imgSrc) {
        imageSource = imgSrc;
        width = imgSrc.getWidth();
        height = imgSrc.getHeight();
    }

    public BufferedImage doGrayscale() {
        imageGrayscale = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = imageSource.getRGB(j, i);
                int redColors = new Color(pixel).getRed();
                int greenColors = new Color(pixel).getGreen();
                int blueColors = new Color(pixel).getBlue();

                int gsColors = (int) ((redColors + greenColors + blueColors) / 3);
                imageGrayscale.setRGB(j, i, new Color(gsColors, gsColors, gsColors).getRGB());
            }
        }
        return imageGrayscale;
    }

    public BufferedImage doBinary() {
        threshold = OtsuThresholding.findThreshold(imageSource);
        imageBinary = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int binaryColor = 0;
                Color c = new Color(imageSource.getRGB(j, i));
                if (new Color(imageSource.getRGB(j, i)).getRed() > threshold) {
                    binaryColor = 255;
                }

                imageBinary.setRGB(j, i, new Color(binaryColor, binaryColor, binaryColor).getRGB());
            }
        }
        return imageBinary;
    }

    public int getThreshold() {
        return threshold;
    }

    public static BufferedImage createWhiteImage(int width, int height) {
        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        for (int y = 0; y < res.getHeight(); y++) {
            for (int x = 0; x < res.getWidth(); x++) {
                res.setRGB(x, y, 0xffffffff);
            }
        }

        return res;
    }

    public static BufferedImage resizeImage(BufferedImage input, int newWidth, int newHeight) {
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(input, 0, 0, newWidth, newWidth, null);
        g.dispose();

        return resizedImage;
    }
    
    public static BufferedImage cropObject(BufferedImage input) {
        BufferedImage res;
        int[] end = new int[4]; // [0] kiri, [1] kanan, [2] atas, [3] bawah
        int black = 0xff000000;
        int w = input.getWidth();
        int h = input.getHeight();
        
        // left-width
        end[0] = 255;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = input.getRGB(x, y);
                
                if (pixel == black) {
                    if (x <= end[0]) {
                        end[0] = x - 1;
                    }
                }
            }
        }
        
        // right-width
        for (int y = 0; y < h; y++) {
            for (int x = w - 1; x >= 0; x--) {
                int pixel = input.getRGB(x, y);
                
                if (pixel == black) {
                    if (x >= end[1]) {
                        end[1] = x + 1;
                    }
                }
            }
        }
        
        // top-height
        end[2] = 255;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int pixel = input.getRGB(x, y);
                
                if (pixel == black) {
                    if (y <= end[2]) {
                        end[2] = y - 1;
                    }
                }
            }
        }
        
        //bottom-height
        for (int x = 0; x < w; x++) {
            for (int y = h - 1; y >= 0; y--) {
                int pixel = input.getRGB(x, y);
                
                if (pixel == black) {
                    if (y >= end[3]) {
                        end[3] = y + 1;
                    }
                }
            }
        }
        
        // pengecekan out-of-bound
        if (end[0] < 0) {
            end[0] = 0;
        }
        if (end[1] > w) {
            end[1] = w - 1;
        }
        if (end[2] < 0) {
            end[2] = 0;
        }
        if (end[3] > h) {
            end[3] = h - 1;
        }
        
        res = input.getSubimage(end[0], end[2], end[1] - end[0], end[3] - end[2]);
        
        return res;
    }
}
