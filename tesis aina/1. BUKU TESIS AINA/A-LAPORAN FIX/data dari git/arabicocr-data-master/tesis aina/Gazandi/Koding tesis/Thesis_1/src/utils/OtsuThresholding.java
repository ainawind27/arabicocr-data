package utils;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class OtsuThresholding {

    private static int histogramData[] = new int[256];
    private static int threshold;

    public static int findThreshold(BufferedImage bmpGray) {
        int width = bmpGray.getWidth();
        int height = bmpGray.getHeight();
        int pointer = 0;

        pointer = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int colorCode = 0xFF & new Color(bmpGray.getRGB(j, i)).getRed();
                histogramData[colorCode]++;
            }
        }

        int total = width * height;
        float sum = 0;
        for (int i = 0; i < histogramData.length; i++) {
            sum += i * histogramData[i];
        }

        float sumB = 0;
        int wBg = 0;
        int wFg = 0;
        float varMax = 0;
        threshold = 0;

        for (int i = 0; i < histogramData.length; i++) {
            wBg += histogramData[i];
            if (wBg == 0) {
                continue;
            }

            wFg = total - wBg;
            if (wFg == 0) {
                break;
            }

            sumB += (float) i * histogramData[i];

            float meanBg = sumB / wBg;
            float meanFg = (sum - sumB) / wFg;
            float varBetween = (float) wBg * (float) wFg * (meanBg - meanFg) * (meanBg - meanFg);
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }

        return threshold;
    }
}
