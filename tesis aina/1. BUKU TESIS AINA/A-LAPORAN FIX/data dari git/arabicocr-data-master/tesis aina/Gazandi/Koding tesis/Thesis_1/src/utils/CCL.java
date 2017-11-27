package utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import model.Pixel;

/**
 *
 * @author Bening Ranum
 */
public class CCL {

    public static int[][] labelling(BufferedImage input) {
        int height = input.getHeight();
        int width = input.getWidth();
        int[][] conversion = new int[height][width];
        int[][] result = new int[height][width];
        int label = 1;
        List<Pixel> connectedness = new ArrayList<>();

        // turns every black pixels into 1
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (input.getRGB(x, y) == 0xff000000) {
                    conversion[y][x] = 1;
                }
            }
        }

        // first pass
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (conversion[y][x] == 1) {
                    int[] neighbors = new int[8];
                    neighbors[0] = result[y - 1][x - 1];
                    neighbors[1] = result[y - 1][x];
                    neighbors[2] = result[y - 1][x + 1];
                    neighbors[3] = result[y][x - 1];
                    neighbors[4] = result[y][x + 1];
                    neighbors[5] = result[y + 1][x - 1];
                    neighbors[6] = result[y + 1][x];
                    neighbors[7] = result[y + 1][x + 1];

                    if (isAllZero(neighbors)) {
                        result[y][x] = label;
                        label++;
                    } else {
                        result[y][x] = smallestNeighbor(neighbors);
                        addChildren(neighbors, connectedness);
                    }
                }
            }
        }

        return result;
    }

    private static boolean isAllZero(int[] neighbors) {
        for (int n : neighbors) {
            if (n != 0) {
                return false;
            }
        }

        return true;
    }

    private static int smallestNeighbor(int[] neighbors) {
        int result = Integer.MAX_VALUE;

        for (int n : neighbors) {
            if (n != 0) {
                result = Integer.min(result, n);
            }
        }

        return result;
    }

    // penanganan terhadap parent yang menjadi child pixel lain belum terlaksana
    private static void addChildren(int[] neighbors, List<Pixel> connectedness) {
        int smallestN = smallestNeighbor(neighbors);

        for (int n : neighbors) {
            if (n != smallestN && n != 0) {
                boolean isAdded = false;
                
                for (Pixel p : connectedness) {
                    if (p.getParent() == smallestN && p.getChild() == n) {
                        isAdded = true;
                    }
                }
                
                if (!isAdded) {
                    connectedness.add(new Pixel(smallestN, n));
                }
            }
        }
    }

    public static void test() {
        int[] test1 = {0, 0, 0, 0, 0, 0, 0, 0};
        int[] test2 = {0, 1, 0, 0, 0, 0, 0, 0};

        System.out.println(isAllZero(test1));
        System.out.println(smallestNeighbor(test2));
        System.out.println(new Pixel(1, 1) == new Pixel(1, 1));
    }
}
