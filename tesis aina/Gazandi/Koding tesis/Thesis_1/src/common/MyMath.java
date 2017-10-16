/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import java.awt.Point;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 *
 * @author Albadr
 * @author Gazandi
 */
public class MyMath {

    public static int sqr(int a) {
        return a * a;
    }

    public static double sqr(double a) {
        return a * a;
    }

    public static double eucDistance(Point a, Point b) {
        return Math.sqrt(MyMath.sqr(a.x - b.x) + MyMath.sqr(a.y - b.y));
    }

    public static double mean(double[] data) {
        double sum = 0;
        for (double datum : data) {
            sum += datum;
        }
        return sum / data.length;
    }

    public static double standardDeviation(double[] population) {
        return Math.sqrt(variance(population));
    }

    public static double variance(double[] population) {
        double mean = mean(population);
        double sum = 0;

        for (double datum : population) {
            double dev = datum - mean;
            sum += sqr(dev);
        }
        return sum / population.length;
    }

    /**
     * Mengembalikan daftar piksel yang dilewati oleh garis dari (x,y) ke
     * (x2,y2) dengan algoritma bresenham.
     *
     * @param x
     * @param y
     * @param x2
     * @param y2
     * @return Me
     */
    public static ArrayList<Point> lineBresenham(int x, int y, int x2, int y2) {
        ArrayList<Point> points = new ArrayList<>();

        int w = x2 - x;
        int h = y2 - y;
        int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
        if (w < 0) {
            dx1 = -1;
        } else if (w > 0) {
            dx1 = 1;
        }
        if (h < 0) {
            dy1 = -1;
        } else if (h > 0) {
            dy1 = 1;
        }
        if (w < 0) {
            dx2 = -1;
        } else if (w > 0) {
            dx2 = 1;
        }
        int longest = Math.abs(w);
        int shortest = Math.abs(h);
        if (!(longest > shortest)) {
            longest = Math.abs(h);
            shortest = Math.abs(w);
            if (h < 0) {
                dy2 = -1;
            } else if (h > 0) {
                dy2 = 1;
            }
            dx2 = 0;
        }
        int numerator = longest >> 1;
        for (int i = 0; i <= longest; i++) {
            points.add(new Point(x, y));
            numerator += shortest;
            if (!(numerator < longest)) {
                numerator -= longest;
                x += dx1;
                y += dy1;
            } else {
                x += dx2;
                y += dy2;
            }
        }

        return points;
    }

    public static void main(String args[]) {
        double[] pop = new double[]{98, 100, 100, 99, 100, 110, 2, 122, 120};

        System.out.println(mean(pop));
        System.out.println(variance(pop));
        System.out.println(standardDeviation(pop));
        //test print unicode
        System.out.println("\u0627\u0628");
        System.out.println("\u0644\u0623");
        System.out.println("\u0644\u0625");
    }
}
