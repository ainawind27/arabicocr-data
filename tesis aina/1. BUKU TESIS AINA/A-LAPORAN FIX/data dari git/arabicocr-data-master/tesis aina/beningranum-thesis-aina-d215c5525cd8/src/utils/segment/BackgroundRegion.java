/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils.segment;

import java.awt.image.BufferedImage;

public class BackgroundRegion {

    public static BufferedImage createBackgroundRegion(BufferedImage input) {
        BufferedImage result = input;
        int black = 0xff000000, white = 0xffffffff, gray = 0xff808080;
        int height = input.getHeight(), width = input.getWidth();
        int start = 0, end = 0;
        int[][] marker = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x != width - 1) {
                    if (input.getRGB(x, y) == black && input.getRGB(x + 1, y) == white) {
                        start = x + 1;
                        boolean check = false;
                        for (int iter = start; iter < width; iter++) {
                            if (input.getRGB(iter, y) == black) {
                                check = true;
                                end = iter - 1;
                                break;
                            }
                        }
                        if (check) {
                            for (int iter = start; iter <= end; iter++) {
                                input.setRGB(iter, y, gray);
                            }
                        }
                        start = end = 0;
                    }
                }
            }
        }

        return result;
    }
}
