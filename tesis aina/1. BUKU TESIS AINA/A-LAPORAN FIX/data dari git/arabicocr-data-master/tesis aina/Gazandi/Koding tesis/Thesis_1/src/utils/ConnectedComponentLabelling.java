package utils;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ConnectedComponentLabelling {

    public static int[][] borderingArray(int[][] input) {
        int height = input.length;
        int width = input[0].length;
        int[][] result = new int[height + 2][width + 2];
        
        for (int y = 0; y < height; y++) {
            System.arraycopy(input[y], 0, result[y + 1], 1, width);
        }

        return result;
    }

    public static void printArray(int[][] input) {
        for (int[] input1 : input) {
            for (int x = 0; x < input1.length; x++) {
                System.out.format("%d ", input1[x]);
            }
            System.out.println("");
        }
    }

    private static int findMin(int[] input) {
        Arrays.sort(input);

        int result = input[0];

        for (int i : input) {
            if (i > result) {
                result = i;
                break;
            }
        }

        return result;
    }

    /**
     * Background = 0 Foreground = 1
     *
     * @param input Bordered 2D Array
     * @return
     */
    public static int[][] createLabel(int[][] input) {
        input = borderingArray(input);
        int height = input.length;
        int width = input[0].length;
        int[][] marker = new int[height - 2][width - 2];
        int[][] borderedMarker = new int[height][width];
        int label = 1;
        Map<Integer, Integer> connectedness = new HashMap<>();

        // the first pass
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (input[y][x] == 0) {
                    int[] neighbor
                            = {
                                borderedMarker[y - 1][x - 1],
                                borderedMarker[y - 1][x],
                                borderedMarker[y - 1][x + 1],
                                borderedMarker[y][x + 1],
                                borderedMarker[y][x - 1],
                                borderedMarker[y + 1][x - 1],
                                borderedMarker[y + 1][x],
                                borderedMarker[y + 1][x + 1]
                            };

                    int cLabel = findMin(neighbor);

                    if (cLabel == 0) {
                        borderedMarker[y][x] = label;
                        label++;
                    } else {
                        borderedMarker[y][x] = cLabel;

                        for (int iterate = 0; iterate < neighbor.length; iterate++) {
                            if (neighbor[iterate] > cLabel) {
                                if (!connectedness.containsKey(neighbor[iterate])) {
                                    connectedness.put(neighbor[iterate], cLabel);
                                }
                            }
                        }
                    }
                }
            }
        }

        // second pass
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int cLabel = borderedMarker[y][x];

                if (connectedness.containsKey(cLabel)) {
                    borderedMarker[y][x] = connectedness.get(cLabel);
                }

                marker[y - 1][x - 1] = borderedMarker[y][x];
            }
        }

        return marker;
    }

    public static Map<Integer, Color> colorLibrary(int[][] input) {
        Map<Integer, Color> result = new HashMap<>();
        int height = input.length;
        int width = input[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int label = input[y][x];
                Color color = randomizeColor();

                if (input[y][x] != 0) {
                    if (!result.containsKey(label) && !result.containsValue(color)) {
                        result.put(label, color);
                    }
                }
            }
        }

        return result;
    }

    private static Color randomizeColor() {
        Color result;
        Random random = new Random();

        float r = random.nextFloat();
        float g = random.nextFloat();
        float b = random.nextFloat();

        result = new Color(r, g, b);

        return result;
    }
}
