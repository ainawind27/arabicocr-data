/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * @author Albadr
 * @author Gazandi
 */
package image.segmentator;

import image.BWC;
import image.BinaryImageShell;

public class Hilditch {

    BinaryImageShell letter;
    BinaryImageShell thinnedLetter;
    int width;
    int height;
    int[][][] rttOne = new int[4][][];
    int[][][] rttTwo = new int[4][][];
    int[][] offset1 = new int[][]{
        {-1, -1}, {0, -1}, {1, -1}, {2, -1},
        {-1, 0}, {0, 0}, {1, 0}, {2, 0},
        {-1, 1}, {0, 1}, {1, 1}, {2, 1},};
    int[][] offset2 = new int[][]{
        {-1, -1}, {0, -1}, {1, -1},
        {-1, 0}, {0, 0}, {1, 0},
        {-1, 1}, {0, 1}, {1, 1},
        {-1, 2}, {0, 2}, {1, 2},};
    int[][] offset3 = new int[][]{
        {-1, -1}, {0, -1}, {1, -1},
        {-1, 0}, {0, 0}, {1, 0},
        {-1, 1}, {0, 1}, {1, 1},};

    String[] template1 = new String[4];
    String[] template2 = new String[4];
    String[] template3_1 = new String[4];
    String[] template3_2 = new String[4];
    String[] template3_3 = new String[4];

    public Hilditch(BinaryImageShell originalLetter) {
        initTemplate();
        this.letter = originalLetter;
        this.width = originalLetter.getWidth();
        this.height = originalLetter.getHeight();
        this.thinnedLetter = new BinaryImageShell(width, height);

        this.hilditch();
        thinnedLetter.updateImage();
    }

    public BinaryImageShell getSkeleton() {
        return this.thinnedLetter;
    }

    public BinaryImageShell getOriginal() {
        return this.letter;
    }

    private void initTemplate() {
        /* Template Unnecessary Interest Point*/
        //  *#** *#** **#* **#*
        //  *##* *##* *##* *##*
        //  **#* **## *#** ##**
        template1[0] = "010001100010";
        template1[1] = "010001100011";
        template1[2] = "001001100100";
        template1[3] = "001001101100";

        //  *** *** *** *** 
        //  *## ##* *## ##*
        //  ##* *## ##* *##
        //  *** *** #** **#
        template2[0] = "000011110000";
        template2[1] = "000011110100";
        template2[2] = "000110011000";
        template2[3] = "000110011001";

        //  *#* *#* #**
        //  *## ### ##*
        //  *** *** *##
        template3_1[0] = "010011000";
        template3_1[1] = "000011010";
        template3_1[2] = "000110010";
        template3_1[3] = "010110000";
        template3_2[0] = "010111000";
        template3_2[1] = "010011010";
        template3_2[2] = "000111010";
        template3_2[3] = "010110010";
        template3_3[0] = "100110011";
        template3_3[1] = "011110100";
        template3_3[2] = "110011001";
        template3_3[3] = "001011110";


        /* Template Superflous Tail sebagaimana dijelaskan oleh (Cowell & Hussain, 2001)*/
        rttOne[0] = new int[][]{
            {-1, 1, 1, 0, 0}, {-1, 1, 1, 0, 0}, {-1, 1, 0, 1, 1}, {-1, 1, 1, 1, 1}, {-1, -1, -1, -1, -1}};
        rttOne[1] = new int[][]{
            {-1, -1, -1, -1, -1}, {-1, 1, 1, 1, 1}, {-1, 1, 0, 1, 1}, {-1, 1, 1, 0, 0}, {-1, 1, 1, 0, 0}};
        rttOne[2] = new int[][]{
            {-1, -1, -1, -1, -1}, {1, 1, 1, 1, -1}, {1, 1, 0, 1, -1}, {0, 0, 1, 1, -1}, {0, 0, 1, 1, -1}};
        rttOne[3] = new int[][]{
            {0, 0, 1, 1, -1}, {0, 0, 1, 1, -1}, {1, 1, 0, 1, -1}, {1, 1, 1, 1, -1}, {-1, -1, -1, -1, -1}};
        rttTwo[0] = new int[][]{
            {-1, 1, 1, 1, 0}, {-1, 1, 1, 1, 0}, {-1, 1, 0, 0, 0}, {-1, 1, 1, 1, 0}, {-1, -1, -1, -1, -1}};
        rttTwo[1] = new int[][]{
            {-1, -1, -1, -1, -1}, {-1, 1, 1, 1, 1}, {-1, 1, 0, 1, 1}, {-1, 1, 0, 1, 1}, {-1, 0, 0, 0, 0}};
        rttTwo[2] = new int[][]{
            {-1, -1, -1, -1, -1}, {0, 1, 1, 1, -1}, {0, 0, 0, 1, -1}, {0, 1, 1, 1, -1}, {0, 1, 1, 1, -1}};
        rttTwo[3] = new int[][]{
            {0, 0, 0, 0, -1}, {1, 1, 0, 1, -1}, {1, 1, 0, 1, -1}, {1, 1, 1, 1, -1}, {-1, -1, -1, -1, -1}};
    }

    /// *************************************************************************
    /// **************************** HILDITCH ALGORITM **************************    
    /// *************************************************************************
    /**
     * Menipiskan image yang ada dengan algoritma hilditch dan menghilangkan
     * beberapa ketebalan dan ekor yang berlebihan.
     */
    private void hilditch() {
        /* Variabel */
        int[][] matrixTemp = new int[this.height][this.width];    // simpanan matriks yang diolah
        int nPixelChange;                           // jumlah piksel yang berubah

        /* offset tetangga dari atas searah jarum jam */
        /*
         * |n8|n1|n2|                                                   
         * |n6|n0|n3| : n0, n1, n2, n3, n4, n5, n6, n7, n8, n9
         * |n6|n5|n4|
         */
        int[][] offset = new int[][]{
            {0, 0}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1},
            {-1, 1}, {-1, 0}, {-1, -1}};

        /* Copy binary matrix of image to new matrix */
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                matrixTemp[y][x] = this.letter.getBinerAsInt(x, y);
            }
        }

        /* Kalang utama ke seluruh iterasi penghilangan titik pada gambar */
        do {
            /* Mengembalikan jumlah piksel yang berubah ke nol pada awal kalang */
            nPixelChange = 0;

            /* Iterasi VERTIKAL */
            for (int y = 0; y < this.height; y++) {
                /* Iterasi HORIZONTAL */
                for (int x = 0; x < this.width; x++) {
                    boolean[] hilditchCondition = new boolean[]{false, false, false, false};   // empat kondisi hilditch sehingga piksel bisa dihapus
                    int[] neighbors = new int[9];                    // Daftar tetangga dari titik tinjau sekarang

                    /* LAKUKAN HANYA JIKA TITIK TINJAU HITAM */
                    if (matrixTemp[y][x] == BWC.BLACK_INT) {
                        /*
                         * Mengambil 9 tetangga dari titik tinjau, matriks: 
                         * |n8|n1|n2|
                         * |n6|n0|n3| , n0 = neighbors[0] = titik tinjau
                         * |n6|n5|n4|
                         */
                        for (int nt = 0; nt < 9; nt++) {
                            int dx = x + offset[nt][0];
                            int dy = y + offset[nt][1];

                            if (dx >= 0 && dx < this.width && dy >= 0 && dy < this.height) {
                                if (matrixTemp[dy][dx] == BWC.BLACK_INT) {
                                    neighbors[nt] = BWC.BLACK_INT;
                                } else if (matrixTemp[dy][dx] == BWC.GRAY_INT) {
                                    /*
                                     * Jika nilai tetangga abu-abu, artinya
                                     * tadinya hitam tapi akan dihilangkan
                                     */
                                    neighbors[nt] = BWC.GRAY_INT;
                                } else {
                                    neighbors[nt] = BWC.WHITE_INT;
                                }
                            }
                        }

                        /*
                         * Kondisi 1: 2 < = B(n0) < = 6
                         */
                        int B_focusPoint = hilditchB(matrixTemp, x, y);
                        if (2 <= B_focusPoint && B_focusPoint <= 6) {
                            hilditchCondition[0] = true;
                        }

                        /*
                         * Kondisi 2: A(n0)=1
                         */
                        if (hilditchA(matrixTemp, x, y) == 1) {
                            hilditchCondition[1] = true;
                        }

                        /*
                         * Kondisi 3: n1.n3.n7=0 or A(n1)!= 1
                         */
                        int A_n1 = hilditchA(matrixTemp, x + offset[1][0], y + offset[1][1]);
                        boolean n1n3n7 = neighbors[1] == BWC.WHITE_INT
                                || neighbors[3] == BWC.WHITE_INT
                                || neighbors[7] == BWC.WHITE_INT;
                        //int n1n3n7 = neighbors[1]*neighbors[3]*neighbors[7];
                        if (n1n3n7 || A_n1 != 1) {
                            hilditchCondition[2] = true;
                        }

                        /*
                         * Kondisi 4: n1.n3.n5=0 or A(n3)!= 1
                         */
                        int A_n3 = hilditchA(matrixTemp, x + offset[3][0], y + offset[3][1]);
                        boolean n1n3n5 = neighbors[1] == BWC.WHITE_INT
                                || neighbors[3] == BWC.WHITE_INT
                                || neighbors[5] == BWC.WHITE_INT;
                        //int n1n3n5 = neighbors[1]*neighbors[3]*neighbors[5];
                        if (n1n3n5 || A_n3 != 1) {
                            hilditchCondition[3] = true;
                        }

                        /*
                         * KEPUTUSAN: Tandai titik hitam yang memenuhi syarat
                         * dihapus dengan warna abu
                         */
                        if (hilditchCondition[0] && hilditchCondition[1]
                                && hilditchCondition[2] && hilditchCondition[3]) {
                            matrixTemp[y][x] = BWC.GRAY_INT;
                            nPixelChange++;
                        }
                    }
                }// End of Iterasi HORIZONTAL
            }//End of Iterasi VERTIKAL

            /* Ubah yang ditandai menjadi putih jika ada */
            if (nPixelChange != 0) {
                for (int y = 0; y < this.height; y++) {
                    for (int x = 0; x < this.width; x++) {
                        if (matrixTemp[y][x] == BWC.GRAY_INT) {
                            matrixTemp[y][x] = BWC.WHITE_INT;
                        }
                    }
                }
            }

            /* Hapus kemungkinan ekor */
            for (int y = 0; y < this.height; y++) {
                for (int x = 0; x < this.width; x++) {
                    preventSuperflousTail(matrixTemp, x, y);
                }
            }
        } while (nPixelChange != 0);
        // END OF KALANG UTAMA

        /* Menyalin matriks olahan ke matriks penyimpan skeleton huruf */
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (matrixTemp[y][x] == BWC.BLACK_INT) {
                    this.thinnedLetter.setAsBlack(x, y);
                } else {
                    this.thinnedLetter.setAsWhite(x, y);
                }
            }
        }

        this.deleteUnnecessaryInterestPoint();
    }

    /**
     * Jumlah pola putih-hitam pada tetangga sekitar titik fokus
     *
     * @param matrixTemp
     * @param xFocus Koordinat horizontal titik fokus
     * @param yFocus Koordinat vertikal titik fokus
     * @return
     */
    private int hilditchA(int[][] matrixTemp, int xFocus, int yFocus) {
        // Nilai jumlah tetangga tak putih dari titik fokus
        int A = 0;
        /*
         * Offset tetangga titik fokus dari atas searah jarum jam |n8|n1|n2|
         * |n6|n0|n3| : n0, n1, n2, n3, n4, n5, n6, n7, n8, n9 |n6|n5|n4|
         */
        int[][] offset = new int[][]{{0, 0}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        int[] neighbors = new int[9];

        /*
         * Mengambil 9 tetangga dari titik tinjau
         */
        for (int nt = 1; nt <= 8; nt++) {
            int dx = xFocus + offset[nt][0];
            int dy = yFocus + offset[nt][1];

            if (dx >= 0 && dx < this.width && dy >= 0 && dy < this.height) {
                if (matrixTemp[dy][dx] == BWC.WHITE_INT) {
                    neighbors[nt] = BWC.WHITE_INT;
                } else {
                    neighbors[nt] = BWC.BLACK_INT;
                }
            } else {
                neighbors[nt] = BWC.WHITE_INT;
            }
        }
        // Tetangga 0 adalah titik fokus, ubah menjadi salinan tetangga akhir
        neighbors[0] = neighbors[8];
        // menghitung perulangan pola W-H
        for (int nt = 1; nt <= 8; nt++) {
            if (neighbors[nt - 1] == BWC.WHITE_INT
                    && neighbors[nt] == BWC.BLACK_INT) {
                A++;
            }
        }
        return A;
    }

    /**
     * Jumlah tetangga tak putih di sekitar titik fokus
     *
     * @param matrixTemp
     * @param xFocus Koordinat horizontal titik fokus
     * @param yFocus Koordinat vertikal titik fokus
     * @return
     */
    private int hilditchB(int[][] matrixTemp, int xFocus, int yFocus) {
        // Nilai jumlah tetangga tak putih dari titik fokus
        int B = 0;
        /*
         * Offset tetangga dari atas searah jarum jam |n8|n1|n2| |n6|n0|n3| :
         * n0, n1, n2, n3, n4, n5, n6, n7, n8, n9 |n6|n5|n4|
         */
        int[][] offset = new int[][]{{0, 0}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};

        // Mengambil ke-8 tetangga dari titik tinjau
        for (int nt = 1; nt <= 8; nt++) {
            int dx = xFocus + offset[nt][0];
            int dy = yFocus + offset[nt][1];

            if (dx >= 0 && dx < this.width && dy >= 0 && dy < this.height) {
                if (matrixTemp[dy][dx] != BWC.WHITE_INT) {
                    B++;
                }
            }
        }

        return B;
    }

    /**
     * Menghapus ketebalan tertentu (yg menyebabkan tulang tidak satu piksel dan
     * menambah-nambahi interest point) dengan template.
     */
    private void deleteUnnecessaryInterestPoint() {
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (this.thinnedLetter.isBlack(x, y)) {
                    String neighbor1 = "";
                    String neighbor2 = "";
                    String neighbor3 = "";
                    for (int nt = 0; nt < 12; nt++) {
                        int dx1 = x + offset1[nt][0];
                        int dy1 = y + offset1[nt][1];
                        int dx2 = x + offset2[nt][0];
                        int dy2 = y + offset2[nt][1];

                        if (dx1 >= 0 && dx1 < this.width && dy1 >= 0 && dy1 < this.height) {
                            neighbor1 += this.thinnedLetter.getBinerAsInt(dx1, dy1) == BWC.BLACK_INT ? 1 : 0;
                        } else {
                            neighbor1 += 0;
                        }
                        if (dx2 >= 0 && dx2 < this.width && dy2 >= 0 && dy2 < this.height) {
                            neighbor2 += this.thinnedLetter.getBinerAsInt(dx2, dy2) == BWC.BLACK_INT ? 1 : 0;
                        } else {
                            neighbor2 += 0;
                        }
                    }
                    for (int nt = 0; nt < 9; nt++) {
                        int dx3 = x + offset3[nt][0];
                        int dy3 = y + offset3[nt][1];

                        if (dx3 >= 0 && dx3 < this.width && dy3 >= 0 && dy3 < this.height) {
                            neighbor3 += this.thinnedLetter.getBinerAsInt(dx3, dy3) == BWC.BLACK_INT ? 1 : 0;
                        } else {
                            neighbor3 += 0;
                        }
                    }

                    for (int tmpl = 0; tmpl < 4; tmpl++) {
                        if (neighbor1.equals(template1[tmpl])) {
                            this.thinnedLetter.setAsWhite(x, y);
                            break;
                        }
                        if (neighbor2.equals(template2[tmpl])) {
                            this.thinnedLetter.setAsWhite(x, y);
                            break;
                        }
                        if (neighbor3.equals(template3_1[tmpl])
                                || neighbor3.equals(template3_2[tmpl])
                                || neighbor3.equals(template3_3[tmpl])) {
                            this.thinnedLetter.setAsWhite(x, y);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Menekan keberadaan ekor berlebih dengan template yang dijelaskan oleh
     * (Cowell & Hussain, 2001).
     *
     * @param matrixTemp matriks hitam putih piksel pada iterasi sekarang
     * @param xFocus axis piksel yang menjadi fokus
     * @param yFocus ordinat piksel yang menjadi fokus
     */
    private void preventSuperflousTail(int[][] matrixTemp, int xFocus, int yFocus) {
        //Jika tidak Black Cuekin
        if (matrixTemp[yFocus][xFocus] == BWC.BLACK_INT) {
            //Cari tetangga yang hanya satu berdasarkan template
            int tetangga = hilditchB(matrixTemp, xFocus, yFocus);
            if (tetangga == 1) {

                /* CEK TEMPLATE SATU DAN DUA 2 */
                for (int nt = 0; nt < 4; nt++) {
                    boolean boolTemplOne = true;
                    boolean boolTemplTwo = true;
                    for (int i = 0; i < 5; i++) {
                        for (int j = 0; j < 5; j++) {
                            int dx = xFocus - 2 + i;
                            int dy = yFocus - 2 + j;

                            if (dx >= 0 && dx < this.width && dy >= 0 && dy < this.height) {
                                int point = matrixTemp[dy][dx] == BWC.BLACK_INT ? 0 : 1;

                                //Template 1
                                if (boolTemplOne && rttOne[nt][j][i] != -1) {
                                    boolTemplOne = rttOne[nt][j][i] == point;
                                }
                                //Template 2
                                if (boolTemplTwo && rttTwo[nt][j][i] != -1) {
                                    boolTemplTwo = rttTwo[nt][j][i] == point;
                                }
                            }
                        }// END OF X

                        //Jika keduanya sudah tidak cocok, break supaya tidak dicek lagi
                        if (!boolTemplOne && !boolTemplOne) {
                            break;
                        }
                    }//END OF Y

                    // Jika satu template cocok, hapus titik fokus, lalu break
                    if (boolTemplOne || boolTemplTwo) {
                        matrixTemp[yFocus][xFocus] = BWC.WHITE_INT;
                        break;
                    }
                }//END OF TEMPLATE MATCHING
            }//END IF TETANGGA == 1
        }// END IF BLACK
    }
}
