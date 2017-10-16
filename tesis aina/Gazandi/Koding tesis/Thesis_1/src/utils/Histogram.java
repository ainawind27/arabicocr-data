package utils;

import java.awt.image.BufferedImage;

public class Histogram {
    public static int[] horizontalProjection(BufferedImage input) {
        int height = input.getHeight();
        int width = input.getWidth();
        int[] result = new int[height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (input.getRGB(x, y) == 0xff000000) {
                    result[y] += 1;
                }
            }
        }
        
        return result;
    }
    
    public static int[] verticalProjection(BufferedImage input) {
        int height = input.getHeight();
        int width = input.getWidth();
        int[] result = new int[width];
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (input.getRGB(x, y) == 0xff000000) {
                    result[x] += 1;
                }
            }
        }
        
        return result;
    }
}
