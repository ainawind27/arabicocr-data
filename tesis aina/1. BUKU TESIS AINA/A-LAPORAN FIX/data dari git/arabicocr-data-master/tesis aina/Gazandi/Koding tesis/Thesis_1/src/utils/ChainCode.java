package utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import model.Point;

public class ChainCode {

    public static String chain(int[][] input) {
        String result = "";
        boolean done = false;
        Point p = findFirstPixel(input);

        if (p != null) {
            Point next = p;
            int x = p.getX(), y = p.getY();

            while (!done) {
                int[] n = neighbors(input, new Point(x, y));
                int total = sumIntArray(n);

                if (total == 0) {
                    input[y][x] = 0;
                    result += "0";
                    done = true;
                } else {
                    int direction = 0;

                    for (int i = 0; i < n.length; i++) {
                        if (n[i] == 1) {
                            direction = i + 1;
                            break;
                        }
                    }

                    result += "" + direction;

                    input[y][x] = 0;
                    next = decider(next, direction);
                    x = next.getX();
                    y = next.getY();
                }
            }
        }

        return result;
    }

    public static List<String> multipleChain(int[][] input) {
        List<String> result = new ArrayList<>();
        String temp = "";
        boolean doneAll = false;
        boolean done = false;

        while (!doneAll) {
            Point p = findFirstPixel(input);

            if (p == null) {
                doneAll = true;
            } else {
                int x = p.getX();
                int y = p.getY();
                Point next = p;

                while (!done) {
                    int[] n = neighbors(input, new Point(x, y));
                    int total = sumIntArray(n);

                    if (total == 0) {
                        input[y][x] = 0;
                        if (!temp.equals("")) {
                            result.add(temp);
                        }
                        done = true;
                    } else {
                        int direction = 0;

                        for (int i = 0; i < n.length; i++) {
                            if (n[i] == 1) {
                                direction = i + 1;
                                break;
                            }
                        }

                        temp += "" + direction;

                        input[y][x] = 0;
                        next = decider(next, direction);
                        x = next.getX();
                        y = next.getY();
                    }
                }

                temp = "";
                done = false;
            }
        }

        return result;
    }

    public static List<String> postProcess(List<String> input) {
        List<String> result = new ArrayList<>();
        int average = 0;

        for (String s : input) {
            average += s.length();
        }

        average = average / input.size();

        for (int i = 0; i < input.size(); i++) {
            if (input.get(i).length() >= average) {
                result.add(input.get(i));
            }
        }

        return result;
    }

    // finds the first foreground pixel that has only one neighbor
    // if there is none, take the first foreground pixel you meet
    private static Point findFirstPixel(int[][] input) {
        Point result = null;
        boolean firstPixelFound = false;

        for (int y = 1; y < input.length - 1; y++) {
            for (int x = 1; x < input[y].length - 1; x++) {
                if (input[y][x] == 1) {
                    int[] n = neighbors(input, new Point(x, y));
                    int total = sumIntArray(n);

                    if (total == 1) {
                        result = new Point(x, y);
                        firstPixelFound = true;
                        break;
                    }
                }
            }
            if (firstPixelFound) {
                break;
            }
        }

        if (!firstPixelFound) {
            for (int y = 1; y < input.length - 1; y++) {
                for (int x = 1; x < input[y].length - 1; x++) {
                    if (input[y][x] == 1) {
                        result = new Point(x, y);
                        break;
                    }
                }
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    private static int[] neighbors(int[][] input, Point p) {
        int x = p.getX();
        int y = p.getY();

        int[] result = new int[]{
                input[y - 1][x - 1],
                input[y - 1][x],
                input[y - 1][x + 1],
                input[y][x + 1],
                input[y + 1][x + 1],
                input[y + 1][x],
                input[y + 1][x - 1],
                input[y][x - 1]
        };

        return result;
    }

    private static int sumIntArray(int[] input) {
        int result = 0;

        for (int i : input) {
            result += i;
        }

        return result;
    }

    private static Point decider(Point p, int input) {
        Point result;
        int x = p.getX();
        int y = p.getY();

        switch (input) {
            case 1:
                x--;
                y--;
                break;
            case 2:
                y--;
                break;
            case 3:
                x++;
                y--;
                break;
            case 4:
                x++;
                break;
            case 5:
                x++;
                y++;
                break;
            case 6:
                y++;
                break;
            case 7:
                x--;
                y++;
                break;
            case 8:
                x--;
                break;
        }

        result = new Point(x, y);
        return result;
    }
    
    public static int[][] imageToArray(BufferedImage input) {
        int height = input.getHeight(), width = input.getWidth();
        int[][] result = new int[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color c = new Color(input.getRGB(x, y));
                result[y][x] = c.getRed() == 255 ? 0 : 1;
            }
        }
        
        return result;
    }
}
