package utils.segment.zidouri2;

import java.awt.image.BufferedImage;
import utils.Histogram;

public class Segmentation {    
    public static int[] findBaseLine(int[] horizontal, int imageHeight) {
        int biggestNum = Integer.MIN_VALUE;
        int start = 0, end = 0;
        // find the biggest number of the projection
        for (int i : horizontal) {
            if (i > biggestNum) {
                biggestNum = i;
            }
        }
        int threshold = (int) (biggestNum - (0.5 * imageHeight));
        for (int i = 0; i < horizontal.length; i++) {
            if (horizontal[i] > threshold) {
                start = i;
                
                for (int j = i; j < horizontal.length; j++) {
                    if (horizontal[j] < threshold) {
                        end = j - 1;
                        break;
                    }
                }
                break;
            }
        }
        return new int[] {start, end};
    }
    
    public static int[] findMiddleLine(int baselineStart, int baselineEnd) {
        int baseThick = baselineEnd - baselineStart + 1;
        int middleThick = baseThick * 2;
        int middleStart = (baselineStart - 1 - middleThick) < 0 ? 0 : baselineStart - 1 - middleThick;
        
        return new int[] {middleStart, baselineStart - 1};
    }
    
    public static BufferedImage characterSegmentation(BufferedImage input) {
        BufferedImage result = input;
        int height = input.getHeight(), width = input.getWidth();
        int[] horizontal = Histogram.horizontalProjection(input);
        int[] baseline = findBaseLine(horizontal, height);
        int baseThick = baseline[1] - baseline[0];
        
        // do vertical projection on upper side of baseline
        int[] midVertical = new int[width];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < baseline[0] - 1; y++) {
                if (input.getRGB(x, y) == 0xff000000) {
                    midVertical[x]++;
                }
            }
        }
        boolean[] check = new boolean[width];       
        for (int i = 0; i < midVertical.length; i++) {
            check[i] = midVertical[i] >= ((double) baseThick / 2);
            if (!check[i]) {
                // check the area below the baseline
                for (int k = baseline[1] + 1; k < height; k++) {
                    if (input.getRGB(i, k) == 0xff000000) {
                        check[i] = true;
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < width; i++) {
            if (!check[i]) {
                for (int j = 0; j < height; j++) {
                    result.setRGB(i, j, 0xffffffff);
                }
            }
        }
        
        return result;
    }
    
    public static void test() {
//        BufferedImage tBw = bwImage;
//        int[] horizontal = Segmentation.horizontalProjection(tBw);
//        int[] baseline = Segmentation.findBaseLine(horizontal, tBw.getHeight());
//        for (int x = 0; x < tBw.getWidth(); x++) {
//            tBw.setRGB(x, baseline[0], 0xffff0000);
//            tBw.setRGB(x, baseline[1], 0xffff0000);
//        }
//        int[] middlezone = Segmentation.findMiddleLine(baseline[0], baseline[1]);
//        for (int x = 0; x < tBw.getWidth(); x++) {
//            tBw.setRGB(x, middlezone[0], 0xff00ff00);
//            tBw.setRGB(x, middlezone[1], 0xff00ff00);
//        }
//        ImageIO.write(tBw, "bmp", new File(mainLocation + "testbase.bmp"));
    }
}
