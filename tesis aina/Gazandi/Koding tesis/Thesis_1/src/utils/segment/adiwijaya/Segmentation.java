package utils.segment.adiwijaya;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import utils.Histogram;

public class Segmentation {

    public static int threshold = 5;

    public static List<Integer> segment(BufferedImage input) {
        List<Integer> result = new ArrayList<>();
        int[] verticalProjection = Histogram.verticalProjection(input);

        int start = 0, end = 0;

        for (int i = 0; i < verticalProjection.length; i++) {
            if (verticalProjection[i] <= threshold) {
                start = i;

                for (int j = i; j < verticalProjection.length; j++) {
                    if (verticalProjection[j] > threshold) {
                        end = j - 1;
                        break;
                    }
                }

                if (start == 0 || end == verticalProjection.length - 1) {
                    i = end + 1;
                } else {
                    if (end - start > 3) {
                        int middle = (end - start) / 2;

                        result.add(start + middle);

                        i = end + 1;
                    }
                }
            }
        }

        return result;
    }
}
