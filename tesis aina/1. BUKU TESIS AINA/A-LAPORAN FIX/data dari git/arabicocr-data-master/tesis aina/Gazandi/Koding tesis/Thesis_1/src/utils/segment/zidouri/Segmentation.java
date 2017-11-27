package utils.segment.zidouri;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import model.Point;

public class Segmentation {

    private static final int black = 0xff000000;
    private static final int white = 0xffffffff;
    
    public static final int Ls = 3; // I still don't know where this number came from
    public static final int Lsa = 3;
    public static final int Lm = 3;

    public static List<Point> characterSegmentation(BufferedImage skeletonedImage) {
        List<Point> result = new ArrayList();
        int height = skeletonedImage.getHeight();
        int width = skeletonedImage.getWidth();
        int start, end = 0;

        for (int y = 0; y < height; y++) {
            for (int x = width - 1; x >= 0; x--) {
                if (skeletonedImage.getRGB(x, y) == black) { // I decided to keep black as our foreground
                    start = x;

                    for (int i = x; i >= 0; i--) {
                        if (skeletonedImage.getRGB(i, y) == white) {
                            end = i;
                            x = i;
                            break;
                        }
                    }

                    if (start - end >= Ls) {
                        for (int i = start; i >= end; i--) {
                            boolean check = true;

                            for (int iter = 0; iter < height; iter++) {
                                int pixel = skeletonedImage.getRGB(i, iter);

                                if (pixel == black && iter != y) {
                                    check = false;
                                    break;
                                }
                            }

                            if (check) {
                                result.add(new Point(i, y));
                            }
                        }
                    }
                }
            }
        }

        return result;
    }
    
    public static List<int[]> bandRange(BufferedImage empty) {
        List<int[]> result = new ArrayList<>();
        int tHeight = empty.getHeight() / 2;
        
        for (int x = empty.getWidth() - 1; x >= 0; x--) {
            int pixel = empty.getRGB(x, tHeight);

            if (pixel == black) {
                int start = x, end = 0;

                for (int x1 = x; x1 >= 0; x1--) {
                    if (empty.getRGB(x1, tHeight) == white) {
                        end = x1 + 1;
                        x = x1;
                        break;
                    }
                }

                result.add(new int[]{start, end});
            }
        }
        
        return result;
    }

    public static List<int[]> extractFeatures(BufferedImage empty) {
        List<int[]> result = new ArrayList<>();
        int tHeight = empty.getHeight() / 2;

        // find out every guide bands in the image
        List<int[]> guideBands = bandRange(empty);

        // extract all the features
        if (!guideBands.isEmpty()) {
            for (int i = 0; i < guideBands.size(); i++) {
                int[] features = new int[5];
                features[3] = 1; // special case
                int[] band = guideBands.get(i);
                
                // first feature: width of the guide band
                features[0] = Math.abs(band[0] - band[1]);
                
                // second feature: distance from the first rightmost band
                if (i == 0) {
                    features[1] = 0;
                } else {
                    int[] firstBand = guideBands.get(0);
                    
                    features[1] = Math.abs(band[0] - firstBand[1]);
                }
                
                // third feature: distance from the second rightmost band
                if (i < 2) {
                    features[2] = 0;
                } else {
                    int[] secondBand = guideBands.get(1);
                    
                    features[2] = Math.abs(band[0] - secondBand[1]);
                }
                
                // fourth feature is a special case for now. let's just skip it.
                
                // fifth feature: midpoint of the guide band
                features[4] = Math.abs(band[0] - band[1]);
                
                result.add(features);
            }
        } else {
            result = null;
        }
        
        return result;
    }
    
    public static boolean guideBandClassification(int[] features, int total, int current) {
        boolean result = false;
        
        boolean f1 = true;
        boolean f2, f3;
        if (current == 1 || current == 2) {
            f2 = features[3] == 1;
            f3 = true;
        } else {
            f2 = features[1] > Ls && features[3] == 1;
            f3 = features[1] <= Ls && features[2] > Lsa && current != total;
        }
        
        boolean f4 = features[0] >= Lm && features[3] == 1;
        
        if (f1 && (f2 || f3) && f4) {
            result = true;
        }
        
        return result;
    }
}
