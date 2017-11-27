/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.classifier;

import image.BWC;
import image.BinaryImageShell;
import image.RectAndBlackPoints;
import image.segmentator.Hilditch;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * 
 * @author Albadr
 * @author Gazandi
 */
public class FeatureExtraction {

    public static int VERTICAL_PROJECTION = 0;
    public static int HORIZONTAL_PROJECTION = 1;
    public static int DENSITY_RATIO = 2;
    public static int ASPECT_RATIO = 3;
    /* Index Bagian Interest Point */
    public static int IP_END_POINT = 0;
    public static int IP_BRANCH_POINT = 1;
    public static int IP_CROSS_POINT = 2;
    /* Index Bagian Distribution */
    public static int ID_ACRE = 0;
    public static int ID_TOPLEFT_PER_ACRE = 1;
    public static int ID_TOPRIGHT_PER_ACRE = 2;
    public static int ID_BOTTOMRIGHT_PER_ACRE = 3;
    public static int ID_BOTTOMLEFT_PER_ACRE = 4;
    public static int ID_TOP_PER_ACRE = 5;
    public static int ID_RIGHT_PER_ACRE = 6;
    public static int ID_BOTTOM_PER_ACRE = 7;
    public static int ID_LEFT_PER_ACRE = 8;
    public static int ID_LEFT_PER_RIGHT = 9;
    public static int ID_TOP_PER_BOTTOM = 10;
    /* Index Objek Sekunder*/
    public static int ID_SO_TITIK = 0;
    public static int ID_SO_ZIGZAG_HAMZAH = 1;
    public static int ID_SO_ATAS = 0;
    public static int ID_SO_BAWAH = 1;

    /**
     * Hitung posisi baseline dari horizontal projection. Pita baseline adalah
     * posisi-posisi horizontal yang dekat puncak proyeksi dan nilainya masih
     * diatas 85% jumlah piksel hitam pada puncak proyeksi.
     *
     * @param word Baris kata atau upakata
     * @return tiga integer yaitu posisi awal baseline, puncak, dan akhir.
     */
    public static int[] baselineHP(BinaryImageShell word) {
        //get the horizontal projection
        HashMap<String, int[]> projection = FeatureExtraction.projection(word);
        int hp[] = projection.get("horizontal");

        //variable set
        int baselinepos = 0;
        int maxhp = 0;
        for (int i = 0; i < hp.length; ++i) {
            if (maxhp < hp[i]) {
                maxhp = hp[i];
                baselinepos = i;
            }
        }

        int bmax = baselinepos, bmin = baselinepos;
        while (bmin > 0 && hp[bmin] > 0.85 * hp[baselinepos]) {
            bmin--;
        }
        while (bmax < hp.length && hp[bmax] > 0.85 * hp[baselinepos]) {
            bmax++;
        }

        return new int[]{bmin, baselinepos, bmax};
    }

    /**
     * Mengembalikan nilai proyeksi horizontal dan vertikal dari citra biner.
     * Proyeksi horizontal adalah penghitungan jumlah piksel hitam scr mendatar.
     * Proyeksi vertikal adalah penghitungan jumlah piksel hitam scr menurun.
     *
     * @return Hashmap of array integer. Proyeksi horizontal bisa diakses dengan
     * key "horizontal". Proyeksi vertikal bisa diakses dengan key "vertikal".
     */
    public static HashMap<String, int[]> projection(BinaryImageShell image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] horProjection = new int[height];
        int[] verProjection = new int[width];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (image.isBlack(i, j)) {
                    horProjection[j] += 1;
                    verProjection[i] += 1;
                }
            }
        }

        HashMap<String, int[]> projection = new HashMap<String, int[]>();
        projection.put("horizontal", horProjection);
        projection.put("vertical", verProjection);

        return projection;
    }

    /**
     * Matriks huruf diproyeksikan secara vertikal, artinya dihitung jumlah
     * titik hitam secara tegak atau siku-siku terhadap sumbu X. Pada setiap
     * axis X, jumlah sebelumnya diikutkan pada jumlah sekarang.
     *
     * @param letter
     */
    public static int[] cumulativeVerticalProjection(BinaryImageShell letter) {
        int[] cumulativeXth = new int[letter.getWidth()];
        int cumulativeUntilXth = 0;
        for (int x = 0; x < letter.getWidth(); x++) {
            int sumOnXth = 0;
            for (int y = 0; y < letter.getHeight(); y++) {
                sumOnXth += letter.isBlack(x, y) ? 1 : 0;
            }
            cumulativeUntilXth += sumOnXth;
            cumulativeXth[x] = cumulativeUntilXth;
        }
        return cumulativeXth;
    }

    /**
     * Matriks huruf diproyeksikan secara mendatar atau horizontal, artinya
     * dihitung jumlah titik hitam secara mendatar atau tegak lurus terhadap
     * sumbu Y. Pada setiap ordinat Y, jumlah sebelumnya dikumulatifkan ke
     * jumlah yang sekarang.
     *
     * @param letter
     */
    public static int[] cumulativeHorizontalProjection(BinaryImageShell letter) {
        int[] cumulativeYth = new int[letter.getHeight()];
        int cumulativeUntilYth = 0;
        for (int y = 0; y < letter.getHeight(); y++) {
            int sumOnYth = 0;
            for (int x = 0; x < letter.getWidth(); x++) {
                sumOnYth += letter.isBlack(x, y) ? 1 : 0;
            }
            cumulativeUntilYth += sumOnYth;
            cumulativeYth[y] = cumulativeUntilYth;
        }
        return cumulativeYth;
    }

    /**
     * Menghitung jumlah piksel berwarna hitam pada letter.
     *
     * @param letter
     * @return Luas bagian hitam pada gambar
     */
    public static int acrePixel(BinaryImageShell letter) {
        int acre = 0;
        for (int y = 0; y < letter.getHeight(); y++) {
            for (int x = 0; x < letter.getWidth(); x++) {
                if (letter.isBlack(x, y)) {
                    ++acre;
                }
            }
        }
        return acre;
    }

    /**
     * Menghitung pesebaran piksel pada gambar letter. Jika ingin menghitung
     * dengan amplop gambar dengan kata lain tanpa ada baris atau kolom yang
     * hanya berisi piksel putih, silakan hilangkan amplop sebelum masuk ke
     * method ini.
     *
     * @param letter Citra yang ingin dihitung distribusinya
     * @return array nilai persebaran piksel dengan urutan indeks : [0] acre,
     * [1] TLperA, [2] TRperA, [3] BRperA, [4] BLperA, [5] TperA, [6] RperA, [7]
     * BperA, [8] LperA, [9] LperR, [10] TperB
     */
    public static double[] distribution(BinaryImageShell letter) {
        /* Marker */
        int verMiddleLine = letter.getWidth() / 2;
        int horMiddleLine = letter.getHeight() / 2;
        boolean widthGenap = letter.getWidth() % 2 == 0;
        boolean heightGenap = letter.getHeight() % 2 == 0;
        int verMiddleStart = widthGenap ? verMiddleLine : verMiddleLine + 1;
        int horMiddleStart = heightGenap ? horMiddleLine : horMiddleLine + 1;
        /* Variabel Kepadatan */
        int TLDensity = 0, TRDensity = 0, BLDensity = 0, BRDensity = 0, acre = 0;
        int middleTopDensity = 0, middleBottomDensity = 0;
        int middleLeftDensity = 0, middleRightDensity = 0;

        /* Hitung jumlah titik kiri atas */
        for (int x = 0; x < verMiddleLine; x++) {
            for (int y = 0; y < horMiddleLine; y++) {
                if (letter.getBiner(x, y) == BWC.BLACK_BOOL) {
                    TLDensity += 1;
                }
            }
        }
        /* Hitung jumlah titik kanan atas */
        for (int x = verMiddleStart; x < letter.getWidth(); x++) {
            for (int y = 0; y < horMiddleLine; y++) {
                if (letter.getBiner(x, y) == BWC.BLACK_BOOL) {
                    TRDensity += 1;
                }
            }
        }
        /* Hitung jumlah titik kiri bawah */
        for (int x = 0; x < verMiddleLine; x++) {
            for (int y = horMiddleStart; y < letter.getHeight(); y++) {
                if (letter.getBiner(x, y) == BWC.BLACK_BOOL) {
                    BLDensity += 1;
                }
            }
        }
        /* Hitung jumlah titik kanan atas */
        for (int x = verMiddleStart; x < letter.getWidth(); x++) {
            for (int y = horMiddleStart; y < letter.getHeight(); y++) {
                if (letter.getBiner(x, y) == BWC.BLACK_BOOL) {
                    BRDensity += 1;
                }
            }
        }

        /* Hitung jumlah titik di tengah jika lebar atau tinggi ganjil */
        if (!widthGenap) {
            // Hitung titik di tengah atas yg tak kehitung karena lebar ganjil
            for (int y = 0; y < horMiddleLine; y++) {
                if (letter.getBiner(verMiddleLine, y) == BWC.BLACK_BOOL) {
                    middleTopDensity += 1;
                }
            }

            // Hitung titik di tengah bawah yg tak kehitung karena lebar ganjil
            for (int y = horMiddleStart; y < letter.getHeight(); y++) {
                if (letter.getBiner(verMiddleLine, y) == BWC.BLACK_BOOL) {
                    middleBottomDensity += 1;
                }
            }
        }
        if (!heightGenap) {
            // Hitung titik di tengah kiri yg tak kehitung karena lebar ganjil
            for (int x = 0; x < verMiddleLine; x++) {
                if (letter.getBiner(x, horMiddleLine) == BWC.BLACK_BOOL) {
                    middleLeftDensity += 1;
                }
            }
            //Hitung titik di tengah kanan yang tak kehitung karena lebar ganjil
            for (int x = verMiddleStart; x < letter.getWidth(); x++) {
                if (letter.getBiner(x, horMiddleLine) == BWC.BLACK_BOOL) {
                    middleRightDensity += 1;
                }
            }

        }

        /* Cek titik tengah banget kalau lebar dan tinggi ganjil */
        if (!heightGenap && !widthGenap) {
            if (letter.getBiner(verMiddleLine, horMiddleLine) == BWC.BLACK_BOOL) {
                acre += 1;
            }
        }

        /* =========Hitung fraksi========= */
        /* Jumlahkan bagian atas bawah kiri kanan */
        int topDensity = TLDensity + TRDensity + middleTopDensity;
        int bottomDensity = BLDensity + BRDensity + middleBottomDensity;
        int leftDensity = TLDensity + BLDensity + middleLeftDensity;
        int rightDensity = TRDensity + BRDensity + middleRightDensity;
        acre = TLDensity + TRDensity + BLDensity + BRDensity
                + middleBottomDensity + middleLeftDensity + middleRightDensity + middleTopDensity;

        /* Rasio distribusi */
        double TLperA = TLDensity / (double) acre;
        double TRperA = TRDensity / (double) acre;
        double BRperA = BRDensity / (double) acre;
        double BLperA = BLDensity / (double) acre;
        double TperA = topDensity / (double) acre;
        double RperA = rightDensity / (double) acre;
        double BperA = bottomDensity / (double) acre;
        double LperA = leftDensity / (double) acre;

        /* Rasio densitas */
        double LperR = leftDensity / (double) rightDensity;
        double TperB = topDensity / (double) bottomDensity;

        /* Return sebagai array */
        return new double[]{acre, TLperA, TRperA, BRperA, BLperA, TperA, RperA, BperA, LperA, LperR, TperB};
    }

    /**
     * Mencari envelope dari gambar huruf dan mengembalikan panjang/tinggi.
     *
     * @param letter Citra yang dicari aspek rasionya.
     * @return Nilai panjang/ tinggi.
     */
    public static double aspectRatio(BinaryImageShell letter) {
        Rectangle rect = letter.findEnvelope();

        double aspectRatio = rect.getWidth() / rect.getHeight();
        return aspectRatio;
    }

    /**
     * Mengecek letak x,y pada koordinat gambar berada di quadran berapa sesuai
     * kuadran cartesius.
     *
     * @param x
     * @param y
     * @param widthViewport
     * @param heightViewport
     * @return Nilai kuadran, dengan 0 untuk kuadran 1, 1 untuk kuadran 2, dst.
     * Jika pada sumbu, dikembalikan nilai -1.
     */
    private static int checkQuadran(int x, int y, int widthViewport, int heightViewport) {
        int halfWidth = widthViewport / 2;
        int halfHeight = heightViewport / 2;

        if (x >= halfWidth && y < halfHeight) {
            return 0;
        } else if (x < halfWidth && y < halfHeight) {
            return 1;
        } else if (x < halfWidth && y >= halfHeight) {
            return 2;
        } else if (x >= halfWidth && y >= halfHeight) {
            return 3;
        }
        return -1;
    }

    /**
     * Menghitung titik minat dari citra skeleton. Titik minat dihitung
     * berdasarkan jenis titik minat dan posisi kuadran cartesius mereka.
     *
     * @param skeleton Gambar huruf yang sudah dikenai thinning
     * @return array of array of integer dengan tiga bagian endPoint,
     * brancPoint, and crossPoint. Masing-masing berupa array berisi 4 integer
     * dari kuadran 1 - 4 (pada index array 0 - 3).
     */
    public static int[][] interestPoint(BinaryImageShell skeleton) {
        int[] endPoint = new int[]{0, 0, 0, 0};
        int[] branchPoint = new int[]{0, 0, 0, 0};
        int[] crossPoint = new int[]{0, 0, 0, 0};

        for (int y = 0; y < skeleton.getHeight(); y++) {
            for (int x = 0; x < skeleton.getWidth(); x++) {
                if (skeleton.getBiner(x, y) == BWC.BLACK_BOOL) {
                    int neighbor = skeleton.getNNeighborAt(x, y);
                    if (neighbor == 1) {
                        int q = FeatureExtraction.checkQuadran(x, y, skeleton.getWidth(), skeleton.getHeight());
                        endPoint[q]++;
                        skeleton.setRGB(x, y, 255, 0, 0);
                    } else if (neighbor == 3) {
                        int q = FeatureExtraction.checkQuadran(x, y, skeleton.getWidth(), skeleton.getHeight());
                        branchPoint[q]++;
                        skeleton.setRGB(x, y, 0, 255, 0);
                    } else if (neighbor == 4) {
                        int q = FeatureExtraction.checkQuadran(x, y, skeleton.getWidth(), skeleton.getHeight());
                        crossPoint[q]++;
                        skeleton.setRGB(x, y, 0, 0, 255);
                    }
                }
            }
        }

        return new int[][]{endPoint, branchPoint, crossPoint};
    }

    /**
     * Menghitung jumlah keseluruhan titik minat pada citra sekeleton.
     *
     * Mohon dicatat bahwa method ini menggunakan method interestPoint untuk
     * menghitung jumlah titik minatnya sehingga ada kemungkinan terjadi
     * penghitungan ganda jika Anda juga memakai method tersebut.
     *
     * @param skeleton Citra sekleton yang ingin dihitung jumlahnya.
     * @return
     */
    public static int[] interestPointCount(BinaryImageShell skeleton) {
        int[][] ips = FeatureExtraction.interestPoint(skeleton);
        int[] ipcount = new int[3];

        ipcount[0] = ips[0][0] + ips[0][1] + ips[0][2] + ips[0][3];
        ipcount[1] = ips[1][0] + ips[1][1] + ips[1][2] + ips[1][3];
        ipcount[2] = ips[2][0] + ips[2][1] + ips[2][2] + ips[2][3];

        return ipcount;
    }

    /**
     * Mencari lobang pada huruf berdasarkan jumlah chain code yang dihasilkan
     * dalam huruf tersebut.
     *
     * Note: citra huruf yang dimasukkan harus yakin tidak ada noise/ potongan
     * huruf lain/ objek sekunder selain badan utama. Kalau ada, hasil akan tak
     * akurat. Hasil yang dikembalikan adalah jumlah objek berboundary - 1.
     *
     * @param letter
     * @return
     */
    public static int countLoop_byCC(BinaryImageShell letter) {
        BinaryImageShell leClone = letter.clone();
        leClone = leClone.convertToBoundary();
        HashMap<Point, ArrayList<Integer>> cbm = FeatureExtraction.chainCodeBoundaryMulti(leClone);
        return cbm.size() - 1;
    }

    public static int[][] defineSecObject(ArrayList<RectAndBlackPoints> secObjects, int baseline) {
        int i = 0;
        int[][] secoDefined = new int[2][2];
        for (RectAndBlackPoints secObj : secObjects) {
            int[] oneSecoDefined = defineSecObject(secObj);

            if (secObj.getRect().y < baseline) {
                secoDefined[ID_SO_ATAS][ID_SO_TITIK] += oneSecoDefined[ID_SO_TITIK];
                secoDefined[ID_SO_ATAS][ID_SO_ZIGZAG_HAMZAH] += oneSecoDefined[ID_SO_ZIGZAG_HAMZAH];
            } else {
                secoDefined[ID_SO_BAWAH][ID_SO_TITIK] += oneSecoDefined[ID_SO_TITIK];
                secoDefined[ID_SO_BAWAH][ID_SO_ZIGZAG_HAMZAH] += oneSecoDefined[ID_SO_ZIGZAG_HAMZAH];
            }
        }

        //System.out.println("SO Atas [T,H] : " + secoDefined[ID_SO_ATAS][ID_SO_TITIK] + "," + secoDefined[ID_SO_ATAS][ID_SO_ZIGZAG_HAMZAH]);
        //System.out.println("SO Bawah[T,H] : " + secoDefined[ID_SO_BAWAH][ID_SO_TITIK] + "," + secoDefined[ID_SO_BAWAH][ID_SO_ZIGZAG_HAMZAH]);

        return secoDefined;
    }

    public static int[] defineSecObject(RectAndBlackPoints secObject) {
        /* init penampung jenis dan jumlah objek sekunder, index 0 titik, index 1 hamzah */
        int[] secoDefined = new int[2];

        /* Secondary object original */
        BinaryImageShell image = secObject.toBinaryImage();
        ArrayList<Integer> boundaryChainCode = FeatureExtraction.chainCodeBoundary(image);
        double perimeter = FeatureExtraction.perimeterLength(boundaryChainCode);
        double perimeterToDiag = FeatureExtraction.perimeterToDiagonalRatio(perimeter, image.getWidth(), image.getHeight());
        double compactness = FeatureExtraction.compactnessRatio(perimeter, FeatureExtraction.acrePixel(image));
        double bendingEnergy = FeatureExtraction.bendingEnergy(perimeter, boundaryChainCode);
        //System.out.println("perimeterToDiag: " + perimeterToDiag);
        //System.out.println("compactness: " + compactness);
        //System.out.println("bendingEnergy: " + bendingEnergy);

        /* Thined version */
        Hilditch hilditch = new Hilditch(image);
        BinaryImageShell thin = hilditch.getSkeleton();
        ArrayList<Integer> thinChainCode = FeatureExtraction.chainCodeBoundary(thin);
        thin.cropEnvelope();
        thin.updateImage();
        int thinArea = FeatureExtraction.acrePixel(thin);
        int thinCCSize = thinChainCode.size();
        //System.out.println("Thin: w=" + thin.getWidth() + ", h=" + thin.getHeight());
        //System.out.println("Thin: area=" + thinArea);
        //System.out.println("Thin: ccs=" + thinCCSize + ", " + thinChainCode);

        /* Determine secondary object */
        if (thinChainCode.size() < 5 || (0.8 < compactness && compactness < 1.20)) {
            //System.out.println("Titik 1 biji");
            secoDefined[ID_SO_TITIK] += 1;
        } else if (thinChainCode.size() > 5 && thinArea < 1.5 * thin.getWidth()) {
            //System.out.println("Titik 2 biji");
            secoDefined[ID_SO_TITIK] += 2;
        } else if (thinChainCode.size() > 5 && thinCCSize >= 3 * thin.getWidth()) {
            //System.out.println("Hamzah");
            secoDefined[ID_SO_ZIGZAG_HAMZAH] += 1;
        } else {
            //System.out.println("Titik 3 biji");
            secoDefined[ID_SO_TITIK] += 3;
        }

        return secoDefined;
    }

    /**
     * Mengembalikan kode rantai keliling dari sebuah citra huruf.
     *
     * @param letter Citra huruf yang ingin diambil kode rantainya. Jangan
     * masukkan citra yang sudah ditipiskan ya.
     * @return Kode rantai dalam bentuk array list of integer.
     */
    public static ArrayList<Integer> chainCodeBoundary(BinaryImageShell letter) {
        int xFocus = -1, yFocus = -1;
        int firstY = -1, firstX = -1;

        /* Titik awal */
        //offsets for neighbors, from east CCW
        int[][] offset = new int[][]{{1, 0}, {1, -1}, {0, -1}, {-1, -1},
            {-1, 0}, {-1, 1}, {0, 1}, {1, 1}};

        // Mencari titik paling atas sebagai awal
        boolean topPointFound = false;

        for (int y = 0; y < letter.getHeight(); y++) {
            for (int x = 0; x < letter.getWidth(); x++) {
                if (!topPointFound && letter.getBiner(x, y) == BWC.BLACK_BOOL) {
                    topPointFound = true;
                    firstY = y;
                    firstX = x;
                    break;
                }
            }
            if (topPointFound) {
                break;
            }
        }

        // Simpanan Chain Codenya
        ArrayList<Integer> cc = new ArrayList<Integer>();
        int dir = 1;
        xFocus = firstX;
        yFocus = firstY;

        while (true) {
            int dx = xFocus + offset[dir][0];
            int dy = yFocus + offset[dir][1];

            if (dx >= 0 && dy >= 0 && dx < letter.getWidth() && dy < letter.getHeight()) {
                if (letter.getBiner(dx, dy) == BWC.BLACK_BOOL) {
                    cc.add(dir);
                    xFocus = dx;
                    yFocus = dy;
                    dir = (dir + 2) % 8;
                } else {
                    dir = (dir + 7) % 8;
                }

            } else {
                dir = (dir + 7) % 8;
            }// END IF 

            if (xFocus == firstX && yFocus == firstY && dir == 1) {
                break;
            }

        }
        return cc;
    }

    public static HashMap<Point, ArrayList<Integer>> chainCodeBoundaryMulti(BinaryImageShell multiob) {
        //kumpulan chain code untuk setiap objek dengan titik mula sendiri
        HashMap<Point, ArrayList<Integer>> cbm = new HashMap<>();

        //Copy image ke matrkis
        int[][] mat = new int[multiob.getHeight()][multiob.getWidth()];
        for (int j = 0; j < multiob.getHeight(); j++) {
            for (int i = 0; i < multiob.getWidth(); i++) {
                mat[j][i] = multiob.getBinerAsInt(i, j);
            }
        }

        /* Titik awal */
        // Mencari titik paling atas sebagai awal
        boolean topPointFound = false;

        for (int y = 0; y < multiob.getHeight(); y++) {
            for (int x = 0; x < multiob.getWidth(); x++) {
                //pastikan saat mendapat piksel hitam, dia merupakan titik boundary
                //dan belum pernah dikunjungi
                if (mat[y][x] == BWC.BLACK_INT && multiob.isBoundaryPixel(x, y)) {
                    Point start = new Point(x, y);
                    ArrayList<Integer> cc = chainCodeBoundary(mat, start);
                    cbm.put(start, cc);
                }
            }
        }

        return cbm;
    }

    public static ArrayList<Integer> chainCodeBoundary(int[][] letter, Point start) {
        // Simpanan Chain Codenya
        ArrayList<Integer> cc = new ArrayList<>();
        int dir = 1;
        int xFocus = start.x, yFocus = start.y;

        //offsets for neighbors, from east CCW
        final int[][] offset = new int[][]{{1, 0}, {1, -1}, {0, -1}, {-1, -1},
            {-1, 0}, {-1, 1}, {0, 1}, {1, 1}};

        int missCount = 0;
        while (true) {
            int dx = xFocus + offset[dir][0];
            int dy = yFocus + offset[dir][1];

            if (dx >= 0 && dy >= 0 && dx < letter[0].length && dy < letter.length) {
                if (letter[dy][dx] == BWC.BLACK_INT) {
                    cc.add(dir);
                    xFocus = dx;
                    yFocus = dy;
                    dir = (dir + 2) % 8;
                    missCount = 0;
                } else {
                    dir = (dir + 7) % 8;
                }
            } else {
                dir = (dir + 7) % 8;
            }// END IF 

            if (xFocus == start.x && yFocus == start.y && dir == 1) {
                break;
            }
            //to escape eternity
            if (missCount > 100 || cc.size() > 1000) {
                cc.clear();
                break;
            }
            missCount++;
        }

        //replace all boundary pixel to gray
        xFocus = start.x;
        yFocus = start.y;
        for (Integer code : cc) {
            xFocus = xFocus + offset[code][0];
            yFocus = yFocus + offset[code][1];
            letter[yFocus][xFocus] = BWC.GRAY_INT;
        }
        return cc;
    }

    /**
     * Menghitung keliling dari sebuah huruf dari kode rantai keliling.
     *
     * @param boundaryChainCode Kode rantai keliling yang bisa didapat dari
     * method chainCodeBoundary.
     *
     * Keliling dihitung dengan cara menghitung jarak antar piksel berdekatan.
     * Jika diagonal berarti jaraknya akar 2, jika bersebelahan berarti 1.
     * @return Panjang keliling huruf.
     */
    public static double perimeterLength(ArrayList<Integer> boundaryChainCode) {
        double T = 0;
        for (int cc : boundaryChainCode) {
            int paritas = cc % 2;

            if (paritas == 0) {
                T += 1;
            } else {
                T += 1.414213562;
            }
        }

        return T;
    }

    /**
     * Menghitung rasio antara panjang keliling sebuah citra dan diagonalnya.
     * Rumus ini diambil dari (Abandah & Khedher, 2009).
     *
     * @param perimeterLength Panjang keliling citra
     * @param width Lebar citra
     * @param height Tinggi citra
     * @return rasio keliling ke diagonal citra
     */
    public static double perimeterToDiagonalRatio(double perimeterLength, int width, int height) {
        return (perimeterLength / 2) / Math.sqrt(width * width + height * height);
    }

    /**
     * Menghitung rasio kerapatan dari sebuah citra. Rasio kerapatan dihitung
     * dari perbandingan panjang keliling dengan luas (jumlah piksel hitam) dari
     * citra. Rumus rasio diambil dari (Abandah & Khedher, 2009)
     *
     * @param perimeterLength Panjang keliling citra
     * @param area Luas citra
     * @return rasio kerapatan citra
     */
    public static double compactnessRatio(double perimeterLength, int area) {
        return perimeterLength * perimeterLength / (4 * Math.PI * area);
    }

    /**
     * Menghitung nilai pembelokan dari sebuah citra. Rumus ini diambil dari
     * (Abandah & Khedher, 2009).
     *
     * @param perimeterLength Panjang keliling citra
     * @param boundaryChainCode Kode rantai keliling citra
     * @return Nilai energi pembelokan
     */
    public static double bendingEnergy(double perimeterLength, ArrayList<Integer> boundaryChainCode) {
        double E = 0;
        int chainCodeLength = boundaryChainCode.size();

        for (int cci = 1; cci <= chainCodeLength; cci++) {
            int ki = cci < chainCodeLength
                    ? (boundaryChainCode.get(cci) - boundaryChainCode.get(cci - 1)) % 8
                    : (boundaryChainCode.get(0) - boundaryChainCode.get(chainCodeLength - 1)) & 8;

            if (ki > 4) {
                ki = 8 - ki;
            }

            double EElement = Math.PI * ki / 4;
            E += EElement * EElement;
        }

        return E / perimeterLength;
    }

    public static double moment_M(ArrayList<Integer> chainCodeBoundary, Point startPoint, int p, int q) {
        double M = 0;
        Point now = new Point(startPoint);
        for (Integer in : chainCodeBoundary) {
            if (in == 0) {
                //kanan, berarti menambah x
                now.x++;
            } else if (in == 1) {
                //kanan atas, berarti menambah x dan mengurangi y
                now.x++;
                now.y--;
            } else if (in == 2) {
                //atas, berarti mengurangi y
                now.y--;
            } else if (in == 3) {
                //kiri atas, berarti mengurangi x dan y
                now.x--;
                now.y--;
            } else if (in == 4) {
                //kiri, berarti mengurangi x
                now.x--;
            } else if (in == 5) {
                //kiri bawah, berarti mengurangi x dan menambah y
                now.x--;
                now.y++;
            } else if (in == 6) {
                //bawah, berarti menambah y
                now.y++;
            } else if (in == 7) {
                //kanan bawah, berarti menambah x dan y
                now.x++;
                now.y++;
            }

            M += Math.pow(now.x, p) * Math.pow(now.y, q);
        }

        return M;
    }

    public static Point moment_centroid(ArrayList<Integer> chainCodeBoundary, Point startPoint) {
        double xc = moment_M(chainCodeBoundary, startPoint, 1, 0) / moment_M(chainCodeBoundary, startPoint, 0, 0);
        double yc = moment_M(chainCodeBoundary, startPoint, 0, 1) / moment_M(chainCodeBoundary, startPoint, 0, 0);

        return new Point((int) xc, (int) yc);
    }

    public static double moment_mu(ArrayList<Integer> chainCodeBoundary, Point startPoint, Point centroid, int p, int q) {
        double M = 0;
        Point now = new Point(startPoint);
        for (Integer in : chainCodeBoundary) {
            if (in == 0) {
                //kanan, berarti menambah x
                now.x++;
            } else if (in == 1) {
                //kanan atas, berarti menambah x dan mengurangi y
                now.x++;
                now.y--;
            } else if (in == 2) {
                //atas, berarti mengurangi y
                now.y--;
            } else if (in == 3) {
                //kiri atas, berarti mengurangi x dan y
                now.x--;
                now.y--;
            } else if (in == 4) {
                //kiri, berarti mengurangi x
                now.x--;
            } else if (in == 5) {
                //kiri bawah, berarti mengurangi x dan menambah y
                now.x--;
                now.y++;
            } else if (in == 6) {
                //bawah, berarti menambah y
                now.y++;
            } else if (in == 7) {
                //kanan bawah, berarti menambah x dan y
                now.x++;
                now.y++;
            }

            M += Math.pow(now.x - centroid.x, p) * Math.pow(now.y - centroid.y, q);
        }

        return M;
    }

    public static double moment_skewAngle(ArrayList<Integer> chainCodeBoundary, Point startPoint) {
        Point centroid = moment_centroid(chainCodeBoundary, startPoint);

        double MU11 = moment_mu(chainCodeBoundary, startPoint, centroid, 1, 1);
        double MU20 = moment_mu(chainCodeBoundary, startPoint, centroid, 2, 0);
        double MU02 = moment_mu(chainCodeBoundary, startPoint, centroid, 0, 2);

        double angle = Math.atan(2 * MU11 / (MU20 - MU02)) / 2;
        return angle;
    }

    public static double moment_skewAngleAverage(BinaryImageShell multiObjectImage) {
        HashMap<Point, ArrayList<Integer>> cbm = FeatureExtraction.chainCodeBoundaryMulti(multiObjectImage);
        /*Moment(0,0) sebagai representasi jumlah piksel boundary*/
        double sumM00 = 0;
        /*Jumlah sudut masing-masing dikali Moment(0,0) sebagai bobot*/
        double sumM00xAngle = 0;
        for (Map.Entry entry : cbm.entrySet()) {
            Point startPoint = (Point) entry.getKey();
            ArrayList<Integer> chainCodeBoundary = (ArrayList<Integer>) entry.getValue();

            double angle = FeatureExtraction.moment_skewAngle(chainCodeBoundary, startPoint);
            double M00 = FeatureExtraction.moment_M(chainCodeBoundary, startPoint, 0, 0);
            if (!Double.isNaN(angle)) {
                sumM00 += M00;
                sumM00xAngle += M00 * angle;
            }
        }
        return sumM00xAngle / sumM00;
    }

    public static void main(String[] args) {
        /* Test rotasi otomatis dari citra berobjek banyak*/
        BinaryImageShell testRotate = new BinaryImageShell("_test_result/rotate/4bound_r_23.png");

        double averageAngle = FeatureExtraction.moment_skewAngleAverage(testRotate);
        System.out.println(Math.toDegrees(averageAngle));
        testRotate.rotate(-1 * averageAngle);
        testRotate.saveImage("_test_result/rotate/4bound_r_23_normalized.png");

        /* Test jumlah loop di huruf */
        int loop = FeatureExtraction.countLoop_byCC(new BinaryImageShell("_raw/mainbody/01/133.png"));
        System.out.println(loop);

        /* Test perbaikan skew dari citra dokumen beneran */
        BinaryImageShell arial_2 = new BinaryImageShell("_page/skew/Arial - 2, 7.2 persen.png");
        double arial_2_angle = arial_2.skewEstimation();
        testRotate.rotate(-1 * arial_2_angle);
        arial_2.saveImage("_page/skew/Arial - 2, 7.2 persen - norm_1.png");
        arial_2_angle = arial_2.skewEstimation();
        testRotate.rotate(-1 * arial_2_angle);
        arial_2.saveImage("_page/skew/Arial - 2, 7.2 persen - norm_2.png");
    }
}
