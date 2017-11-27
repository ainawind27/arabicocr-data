/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import utils.segment.SpecialLines;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import utils.segment.BackgroundRegion;

public class ImageSegmentation {

    public static BufferedImage findBiggestLineSegment(List<BufferedImage> input) {
        BufferedImage result = null;

        if (input.size() == 1) {
            result = input.get(0);
        } else {
            for (int i = 0; i < input.size() - 1; i++) {
                result = input.get(i);

                if (result.getHeight() < input.get(i + 1).getHeight()) {
                    result = input.get(i + 1);
                }
            }
        }

        return result;
    }

    public static List<BufferedImage> lineSegmentation(BufferedImage input) {
        List<BufferedImage> result = new LinkedList<>();
        int[] horizontalProjection = Histogram.horizontalProjection(input);
        int start = 0, end = 0;

        for (int i = 0; i < horizontalProjection.length; i++) {
            if (horizontalProjection[i] != 0) {
                start = i - 1;
                
                for (int j = i; j < horizontalProjection.length; j++) {
                    if (j == horizontalProjection.length - 1 || horizontalProjection[j] == 0) {
                        end = j + 1;
                        i = j;
                        break;
                    }
                }

                result.add(input.getSubimage(0, start, input.getWidth(), end - start));
            }
        }

        return result;
    }

    public static List<BufferedImage> subWordSegmentation(BufferedImage line) {
        List<BufferedImage> result = new LinkedList<>();
        int[] verticalProjection = Histogram.verticalProjection(line);
        int start, end = 0;

        for (int i = 0; i < verticalProjection.length; i++) {
            if (verticalProjection[i] != 0) {
                start = i - 1;

                for (int j = i; j < verticalProjection.length; j++) {
                    if (j == verticalProjection.length - 1 || verticalProjection[j] == 0) {
                        end = j + 1;
                        i = j;
                        break;
                    }
                }

                result.add(line.getSubimage(start, 0, end - start, line.getHeight()));
            }
        }

        return result;
    }
    
    public static List<BufferedImage> characterSegmentation(BufferedImage subWord) {
        List<BufferedImage> result = new LinkedList<>();
        // do horizontal projection on chosen segment
        int[] horizontalProjection = Histogram.horizontalProjection(subWord);
        // get the special lines
        SpecialLines special = new SpecialLines(subWord, horizontalProjection);
        // do vertical projection on chosen segment
        int[] verticalProj = Histogram.verticalProjection(subWord);
        // scan the vertical projection from right to left
        List<Integer> potentialSegPoint = new LinkedList<>();
        for (int i = verticalProj.length - 1; i >= 1; i--) {
            int check = Math.abs(special.baselineEnd - verticalProj[i]);
            if (check <= 3 && check >= 0) {
                int start = i, end = i;
                
                while (verticalProj[end] > verticalProj[end-1] && end - 1 != 0) {
                    end--;
                }
                
                int biggestNum = Integer.MIN_VALUE;
                if (start != end) {
                    for (int x = start; x >= end; x--) {
                        if (biggestNum < verticalProj[x]) {
                            biggestNum = verticalProj[x];
                        }
                    }
                    
                    if (biggestNum > (special.baselineEnd * 1.5 - special.baselineEnd)) {
                        potentialSegPoint.add(i);
                    }
                }
            }
        }
        for (int pot : potentialSegPoint) {
            for (int y = 0; y < subWord.getHeight(); y++) {
                subWord.setRGB(pot, y, 0xff00ff00);
            }
        }
        
        result.add(subWord);
        
        return result;
    }
}
