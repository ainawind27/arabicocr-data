package utils;

import java.awt.Color;
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
                if (new Color(imageSource.getRGB(j, i)).getRed() >= threshold) {
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
}
