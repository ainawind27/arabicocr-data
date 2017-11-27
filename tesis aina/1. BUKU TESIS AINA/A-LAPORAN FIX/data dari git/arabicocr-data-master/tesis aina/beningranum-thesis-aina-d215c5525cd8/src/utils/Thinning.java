/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import model.Point;

public class Thinning {

    private static final int[] X_RULE = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] Y_RULE = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static BufferedImage imageSource;
    private static int width;
    private static int height;
    private static int threshold;
    private final int[][] binaryImage;

    public Thinning(BufferedImage imgSrc) {
        imageSource = imgSrc;
        width = imgSrc.getWidth();
        height = imgSrc.getHeight();
        binaryImage = new int[height][width];
    }

    public BufferedImage doZhangSuen() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (new Color(imageSource.getRGB(j, i)).getRed() == 0) {
                    binaryImage[i][j] = 1;
                } else {
                    binaryImage[i][j] = 0;
                }
            }
        }

        int hWidth = 0;
        int sumHWidth = 0;
        int countHwidth = 0;
        for (int[] binaryImage1 : binaryImage) {
            for (int j = 0; j < binaryImage1.length; j++) {
                if (binaryImage1[j] == 0) {
                    if (hWidth > 0) {
                        sumHWidth += hWidth;
                        countHwidth++;
                    }
                    hWidth = 0;
                } else if (j == (width - 1)) {
                    if (hWidth > 0) {
                        hWidth++;
                        sumHWidth += hWidth;
                        countHwidth++;
                    }
                    hWidth = 0;
                }
                if (binaryImage1[j] == 1) {
                    hWidth++;
                } else {
                    hWidth = 0;
                }
            }
        }

//        threshold = countHwidth != 0 ? (sumHWidth / countHwidth) / 2 : 0;
        threshold = (sumHWidth / countHwidth) / 2;

        int a, b;
        List<Point> pointsToChange = new LinkedList<>();
        boolean hasChange;
        do {
            hasChange = false;
            for (int y = 1; y + 1 < height; y++) {
                for (int x = 1; x + 1 < width; x++) {
                    a = getTransitionWB(binaryImage, y, x);
                    b = getNeighbor(binaryImage, y, x);
                    if (binaryImage[y][x] == 1 && 2 <= b && b <= 6 && a == 1
                            && (binaryImage[y - 1][x] * binaryImage[y][x + 1] * binaryImage[y + 1][x] == 0)
                            && (binaryImage[y][x + 1] * binaryImage[y + 1][x] * binaryImage[y][x - 1] == 0)) {
                        pointsToChange.add(new Point(x, y));
                        hasChange = true;
                    }
                }
            }
            pointsToChange.stream().forEach((pointChange) -> {
                binaryImage[pointChange.getY()][pointChange.getX()] = 0;
            });
            pointsToChange.clear();
            for (int y = 1; y + 1 < height; y++) {
                for (int x = 1; x + 1 < width; x++) {
                    a = getTransitionWB(binaryImage, y, x);
                    b = getNeighbor(binaryImage, y, x);
                    if (binaryImage[y][x] == 1 && 2 <= b && b <= 6 && a == 1
                            && (binaryImage[y - 1][x] * binaryImage[y][x + 1] * binaryImage[y][x - 1] == 0)
                            && (binaryImage[y - 1][x] * binaryImage[y + 1][x] * binaryImage[y][x - 1] == 0)) {
                        pointsToChange.add(new Point(x, y));
                        hasChange = true;
                    }
                }
            }
            pointsToChange.stream().forEach((pointChange) -> {
                binaryImage[pointChange.getY()][pointChange.getX()] = 0;
            });
            pointsToChange.clear();
        } while (hasChange);

        for (int i = 0; i < binaryImage.length; i++) {
            for (int j = 0; j < binaryImage[i].length; j++) {
                if (binaryImage[i][j] == 1) {
                    byte countNeighbor = 0;
                    for (int j2 = 0; j2 < X_RULE.length; j2++) {
                        int new_x = j + X_RULE[j2];
                        int new_y = i + Y_RULE[j2];
                        if (new_x >= 0 && new_y >= 0 && new_x < width && new_y < height) {
                            if (binaryImage[new_y][new_x] == 1) {
                                countNeighbor++;
                            }
                        }
                    }

                    if (countNeighbor == 4 && getTransitionWB(binaryImage, i, j) >= 3) {
                        binaryImage[i][j] = 4;
                    }
                }
            }
        }

        hasChange = true;
        while (hasChange) {
            hasChange = false;
            for (int i = 0; i < binaryImage.length; i++) {
                for (int j = 0; j < binaryImage[i].length; j++) {
                    if (binaryImage[i][j] == 1) {
                        byte doPrunning = 0;
                        for (int j2 = 0; j2 < X_RULE.length; j2++) {
                            int new_x = j + X_RULE[j2];
                            int new_y = i + Y_RULE[j2];
                            if (new_x >= 0 && new_y >= 0 && new_x < width && new_y < height) {
                                //if (binaryImage[new_y][new_x] != 4){
                                doPrunning++;
                                //}
                            }
                        }
                        if (doPrunning > 7 && (binaryImage[i + Y_RULE[0]][j + X_RULE[0]] == 1 || binaryImage[i + Y_RULE[0]][j + X_RULE[0]] == 4)
                                && (binaryImage[i + Y_RULE[2]][j + X_RULE[2]] == 1 || binaryImage[i + Y_RULE[2]][j + X_RULE[2]] == 4)
                                && binaryImage[i + Y_RULE[5]][j + X_RULE[5]] == 0) {
                            binaryImage[i][j] = 0;
                            hasChange = true;
                        }

                        if (doPrunning > 7 && (binaryImage[i + Y_RULE[2]][j + X_RULE[2]] == 1 || binaryImage[i + Y_RULE[2]][j + X_RULE[2]] == 4)
                                && (binaryImage[i + Y_RULE[4]][j + X_RULE[4]] == 1 || binaryImage[i + Y_RULE[4]][j + X_RULE[4]] == 4)
                                && binaryImage[i + Y_RULE[7]][j + X_RULE[7]] == 0) {
                            binaryImage[i][j] = 0;
                            hasChange = true;
                        }

                        if (doPrunning > 7 && (binaryImage[i + Y_RULE[4]][j + X_RULE[4]] == 1 || binaryImage[i + Y_RULE[4]][j + X_RULE[4]] == 4)
                                && (binaryImage[i + Y_RULE[6]][j + X_RULE[6]] == 1 || binaryImage[i + Y_RULE[6]][j + X_RULE[6]] == 4)
                                && binaryImage[i + Y_RULE[1]][j + X_RULE[1]] == 0) {
                            binaryImage[i][j] = 0;
                            hasChange = true;
                        }

                        if (doPrunning > 7 && (binaryImage[i + Y_RULE[6]][j + X_RULE[6]] == 1 || binaryImage[i + Y_RULE[6]][j + X_RULE[6]] == 4)
                                && (binaryImage[i + Y_RULE[0]][j + X_RULE[0]] == 1 || binaryImage[i + Y_RULE[0]][j + X_RULE[0]] == 4)
                                && binaryImage[i + Y_RULE[3]][j + X_RULE[3]] == 0) {
                            binaryImage[i][j] = 0;
                            hasChange = true;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < binaryImage.length; i++) {
            for (int j = 0; j < binaryImage[i].length; j++) {
                if (binaryImage[i][j] == 1 || binaryImage[i][j] == 4) {
                    byte countNeighbor = 0;
                    for (int j2 = 0; j2 < X_RULE.length; j2++) {
                        int new_x = j + X_RULE[j2];
                        int new_y = i + Y_RULE[j2];
                        if (new_x >= 0 && new_y >= 0 && new_x < width && new_y < height) {
                            if (binaryImage[new_y][new_x] == 1) {
                                countNeighbor++;
                            }
                        }
                    }

                    if (countNeighbor == 3 && getTransitionWB(binaryImage, i, j) >= 3) {
                        binaryImage[i][j] = 3;
                    }
                }
            }
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (binaryImage[i][j] == 3) {
                    ArrayList<Point> removeShortBranch = new ArrayList<>();
                    int counterBranch = 0;
                    int counterPrunning = 0;

                    for (int j2 = 0; j2 < X_RULE.length; j2++) {
                        int new_x = j + X_RULE[j2];
                        int new_y = i + Y_RULE[j2];

                        if (new_x >= 0 && new_y >= 0 && new_x < width && new_y < height) {
                            byte isFoundHV = 0;
                            int last_x = new_x;
                            int last_y = new_y;

                            int lastChain = j2;
                            ArrayList<Point> tmpRemoveShortBranch = new ArrayList<>();
                            tmpRemoveShortBranch.add(new Point(new_x, new_y));
                            byte isFoundBranch = 0;
                            boolean[][] visited = new boolean[height][width];
                            while (isFoundHV == 0) {
                                int nextChainPath = 7;
                                int nextChainCode = 7;

                                visited[last_y][last_x] = true;

                                int tmp_x = last_x;
                                int tmp_y = last_y;
                                if (getNeighbor(binaryImage, last_y, last_x) > 1) {
                                    for (int i2 = 0; i2 < X_RULE.length; i2++) {
                                        int new_x1 = last_x + X_RULE[i2];
                                        int new_y1 = last_y + Y_RULE[i2];
                                        int backDir = ((lastChain - 4) % 8 + 8) % 8;
                                        if (backDir != i2) {
                                            if (new_x1 >= 0 && new_y1 >= 0 && new_x1 < width && new_y1 < height) {
                                                if (binaryImage[new_y1][new_x1] == 3 || binaryImage[new_y1][new_x1] == 4) {
                                                    isFoundBranch = 1;
                                                    isFoundHV = 1;
                                                    tmpRemoveShortBranch.clear();
                                                } else {
                                                    if (binaryImage[new_y1][new_x1] == 1 && !visited[new_y1][new_x1]) {
                                                        int pathChain = ((i2 - (((lastChain - 2) % 8 + 8) % 8)) % 8 + 8) % 8;
                                                        if (nextChainPath > pathChain) {
                                                            nextChainPath = pathChain;
                                                            nextChainCode = i2;

                                                            tmp_x = new_x1;
                                                            tmp_y = new_y1;
                                                        }
                                                    }
                                                }

                                                if (visited[new_y1][new_x1]) {
                                                    isFoundHV = 1;
                                                }
                                            } else {
                                                isFoundHV = 1;
                                            }
                                        }
                                    }
                                } else {
                                    isFoundHV = 1;
                                }

                                last_x = tmp_x;
                                last_y = tmp_y;
                                lastChain = nextChainCode;

                                if (!visited[last_y][last_x] && isFoundHV == 0 && binaryImage[last_y][last_x] == 1) {
                                    tmpRemoveShortBranch.add(new Point(last_x, last_y));
                                } else {
                                    break;
                                }
                            }

                            if (isFoundBranch == 0) {
                                if (counterBranch == 0) {
                                    removeShortBranch = tmpRemoveShortBranch;
                                } else if (removeShortBranch.size() > tmpRemoveShortBranch.size()) {
                                    removeShortBranch = tmpRemoveShortBranch;
                                }
                                counterBranch++;
                            }
                            isFoundBranch = 0;
                            counterPrunning++;
                        }
                    }

                    if (counterPrunning > 2 && removeShortBranch.size() <= threshold) {
                        for (Point point : removeShortBranch) {
                            binaryImage[point.getY()][point.getX()] = 0;
                        }
                    }

                }
            }
        }

        BufferedImage resultBitmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int color;
        //String line;
        for (int i = 0; i < binaryImage.length; i++) {
            //line = "";
            for (int j = 0; j < binaryImage[i].length; j++) {
                color = 255;
                if (binaryImage[i][j] == 1 || binaryImage[i][j] == 3 || binaryImage[i][j] == 4) {
                    color = 0;
                }
                resultBitmap.setRGB(j, i, new Color(color, color, color).getRGB());
                //line = line + binaryImage[i][j];
            }
            //Log.i("Line", line);
        }
        return resultBitmap;
    }

    public BufferedImage getBitmap() {
        BufferedImage newBitmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int gsColors = 255;
        for (int i = 0; i < binaryImage.length; i++) {
            for (int j = 0; j < binaryImage[i].length; j++) {
                if (binaryImage[i][j] == 1) {
                    gsColors = 0;
                } else {
                    gsColors = 255;
                }
                newBitmap.setRGB(j, i, new Color(gsColors, gsColors, gsColors).getRGB());
            }
        }
        return newBitmap;
    }

    public int[][] getBinary() {
        return binaryImage;
    }

    private static int getTransitionWB(int[][] binaryImage, int y, int x) {
        int count = 0;

        if (y - 1 >= 0 && x + 1 < binaryImage[0].length && binaryImage[y - 1][x] == 0
                && binaryImage[y - 1][x + 1] == 1) {
            count++;
        }

        if (y - 1 >= 0 && x + 1 < binaryImage[0].length && binaryImage[y - 1][x + 1] == 0
                && binaryImage[y][x + 1] == 1) {
            count++;
        }

        if (y + 1 < binaryImage.length && x + 1 < binaryImage[0].length && binaryImage[y][x + 1] == 0
                && binaryImage[y + 1][x + 1] == 1) {
            count++;
        }

        if (y + 1 < binaryImage.length && x + 1 < binaryImage[0].length && binaryImage[y + 1][x + 1] == 0
                && binaryImage[y + 1][x] == 1) {
            count++;
        }

        if (y + 1 < binaryImage.length && x - 1 >= 0 && binaryImage[y + 1][x] == 0 && binaryImage[y + 1][x - 1] == 1) {
            count++;
        }

        if (y + 1 < binaryImage.length && x - 1 >= 0 && binaryImage[y + 1][x - 1] == 0 && binaryImage[y][x - 1] == 1) {
            count++;
        }

        if (y - 1 >= 0 && x - 1 >= 0 && binaryImage[y][x - 1] == 0 && binaryImage[y - 1][x - 1] == 1) {
            count++;
        }

        if (y - 1 >= 0 && x - 1 >= 0 && binaryImage[y - 1][x - 1] == 0 && binaryImage[y - 1][x] == 1) {
            count++;
        }
        return count;
    }

    private static int getNeighbor(int[][] binaryImage, int y, int x) {
        return binaryImage[y - 1][x] + binaryImage[y - 1][x + 1] + binaryImage[y][x + 1] + binaryImage[y + 1][x + 1]
                + binaryImage[y + 1][x] + binaryImage[y + 1][x - 1] + binaryImage[y][x - 1] + binaryImage[y - 1][x - 1];
    }

    private static int getNeighbor2(int[][] binaryImage, int y, int x) {
        int count = 0;

        if (y - 1 >= 0 && x - 1 >= 0 && (binaryImage[y - 1][x] == 1 || binaryImage[y - 1][x] == 3 || binaryImage[y - 1][x] == 4)) {
            count++;
        }

        if (y - 1 >= 0 && x + 1 < binaryImage[y].length && (binaryImage[y - 1][x + 1] == 1 || binaryImage[y - 1][x + 1] == 3 || binaryImage[y - 1][x + 1] == 4)) {
            count++;
        }

        if (y - 1 >= 0 && x + 1 < binaryImage[y].length && (binaryImage[y][x + 1] == 1 || binaryImage[y][x + 1] == 3 || binaryImage[y][x + 1] == 4)) {
            count++;
        }

        if (y + 1 < binaryImage.length && x + 1 < binaryImage[y].length && (binaryImage[y + 1][x + 1] == 1 || binaryImage[y + 1][x + 1] == 3 || binaryImage[y + 1][x + 1] == 4)) {
            count++;
        }

        if (y + 1 < binaryImage.length && x + 1 < binaryImage[y].length && (binaryImage[y + 1][x] == 1 || binaryImage[y + 1][x] == 3 || binaryImage[y + 1][x] == 4)) {
            count++;
        }

        if (y + 1 < binaryImage.length && x - 1 >= 0 && (binaryImage[y + 1][x - 1] == 1 || binaryImage[y + 1][x - 1] == 3 || binaryImage[y + 1][x - 1] == 4)) {
            count++;
        }

        if (y + 1 < binaryImage.length && x - 1 >= 0 && (binaryImage[y][x - 1] == 1 || binaryImage[y][x - 1] == 3 || binaryImage[y][x - 1] == 4)) {
            count++;
        }

        if (y - 1 >= 0 && x - 1 >= 0 && (binaryImage[y - 1][x - 1] == 1 || binaryImage[y - 1][x - 1] == 3 || binaryImage[y - 1][x - 1] == 4)) {
            count++;
        }
        return count;
    }
}
