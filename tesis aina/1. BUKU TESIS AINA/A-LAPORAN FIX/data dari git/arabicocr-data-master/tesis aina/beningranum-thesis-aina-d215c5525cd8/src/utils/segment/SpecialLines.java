/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils.segment;

import java.awt.image.BufferedImage;

public class SpecialLines {
    
    public int baselineStart;
    public int baselineEnd;
    public int upperLine;
    public int lowerLine;
    
    public SpecialLines(BufferedImage input, int[] horizontalProjection) {
        int[] idx = indexOfTwoBiggestValue(horizontalProjection);
        baselineStart = Math.min(idx[0], idx[1]);
        baselineEnd = Math.max(idx[0], idx[1]);
        upperLine = (baselineStart + 1) / 3;
        lowerLine = input.getHeight() - 1;
    }

    private int[] indexOfTwoBiggestValue(int[] histogram) {
        int[] result = new int[2];
        int high1 = Integer.MIN_VALUE;
        int high2 = Integer.MIN_VALUE;
        int index1 = 0, index2 = 0;

        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > high1) {
                high2 = high1;
                high1 = histogram[i];
                index2 = index1;
                index1 = i;
            } else if (histogram[i] > high2) {
                high2 = histogram[i];
                index2 = i;
            }
        }

        result[0] = index1;
        result[1] = index2;

        return result;
    }
    
    public static BufferedImage drawRedLines(BufferedImage input, SpecialLines special) {
        BufferedImage result = input;
        
        for (int i = 0; i < result.getHeight(); i++) {
            if (i == special.baselineEnd || i == special.upperLine || i == special.lowerLine) {
                for (int j = 0; j < result.getWidth(); j++) {
                    result.setRGB(j, i, 0xffff0000);
                }
            }
        }
        
        return result;
    }
}
