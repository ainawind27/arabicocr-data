/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image;

import common.MyMath;
import image.classifier.FeatureExtraction;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pemegang komponen gambar hitam-putih. Nilai 0 atau false berarti hitam. Nilai
 * 1 atau true berarti putih.
 *
 * 
 * @author Albadr
 * @author Gazandi
 */
public class BinaryImageShell extends ImageShell {

    /* Lebar dari gambar */
    int width;
    /* Panjang dari gambar */
    int height;
    private boolean[][] matrixBinary;

    public BinaryImageShell(BufferedImage image) {
        super(image);
        this.init();
    }

    public BinaryImageShell(File file) {
        super(file);
        this.init();
    }

    public BinaryImageShell(String fileName) {
        super(fileName);
        this.init();
    }

    public BinaryImageShell(int width, int height) {
        super(width, height);
        this.width = width;
        this.height = height;
        this.matrixBinary = new boolean[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                this.matrixBinary[row][col] = BWC.WHITE_BOOL;
            }
        }
    }

    private void init() {
        this.width = image.getWidth();
        this.height = image.getHeight();

        this.updateMatrix();
    }

    public void updateMatrix() {
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.matrixBinary = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] rgb = this.getARGB(x, y);
                // Jika salah 2/3 komponen RGB nilainya > 100, dinyatakan putih.
                this.matrixBinary[y][x] = BinaryImageShell.checkBiner(rgb[0], rgb[1], rgb[2], rgb[3]);
            }
        }
    }

    public void binarize() {
        for (int y = 0; y < this.getHeight(); y++) {
            for (int x = 0; x < this.getWidth(); x++) {
                int blackWhite = BinaryImageShell.convertBinertoColor(this.getBiner(x, y));
                this.setRGB(x, y, blackWhite, blackWhite, blackWhite);
            }
        }
    }

    public void updateImage() {
        image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        this.binarize();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    public void setBiner(int x, int y, boolean biner) {
        this.matrixBinary[y][x] = biner;
    }

    public void setAsBlack(int x, int y) {
        this.matrixBinary[y][x] = BWC.BLACK_BOOL;
    }

    public void setAsWhite(int x, int y) {
        this.matrixBinary[y][x] = BWC.WHITE_BOOL;
    }

    public int getBinerAsInt(int x, int y) {
        return this.matrixBinary[y][x] == BWC.BLACK_BOOL
                ? BWC.BLACK_INT : BWC.WHITE_INT;
    }

    public boolean getBiner(int x, int y) {
        return this.matrixBinary[y][x];
    }

    public boolean isBlack(int x, int y) {
        if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
            return false;
        }
        return this.getBiner(x, y) == BWC.BLACK_BOOL;
    }

    public boolean isWhite(int x, int y) {
        if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
            return true;
        }
        return this.getBiner(x, y) == BWC.WHITE_BOOL;
    }

    public static BufferedImage getScaledImage(BufferedImage image, int width, int height) throws IOException {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        double scaleX = (double) width / imageWidth;
        double scaleY = (double) height / imageHeight;
        AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
        AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform, AffineTransformOp.TYPE_BILINEAR);

        return bilinearScaleOp.filter(image, new BufferedImage(width, height, image.getType()));
    }

    public void resizeTo64() {
        int targetBoundary = 64; // ukuran ideal gambar

        resizeToHeight(targetBoundary);
    }

    public void resizeToHeight(int preferredHeight) {
        double scale = (double) preferredHeight / this.height;
        int targetWidth = (int) (this.width * scale);
        int targetHeight = (int) (this.height * scale);
        try {
            this.image = BinaryImageShell.getScaledImage(this.image, targetWidth, targetHeight);
        } catch (IOException ex) {
            Logger.getLogger(BinaryImageShell.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            this.width = targetWidth;
            this.height = targetHeight;
            this.updateMatrix();
        }
    }

    /// *************************************************************************
    /// ******************************* ENVELOPE ********************************
    /// *************************************************************************
    /**
     * Mencari amplop dari citra ini. Amplop adalah area kotak yang setidaknya
     * memuat piksel hitam pada setiap baris dan kolomnya. Tidak ada baris yang
     * hanya berisi piksel putih.
     *
     * @return Persegi amplop citra ini
     */
    public Rectangle findEnvelope() {
        int topEnv = -1, bottomEnv = -1, leftEnv = -1, rightEnv = -1;

        // Mencari amplop atas dan bawah
        boolean tintTLFound = false;
        boolean tintBRFound = false;
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (!tintTLFound && this.matrixBinary[y][x] == BWC.BLACK_BOOL) {
                    tintTLFound = true;
                    topEnv = y;
                }

                if (!tintBRFound && this.matrixBinary[this.height - y - 1][x] == BWC.BLACK_BOOL) {
                    tintBRFound = true;
                    bottomEnv = this.height - y - 1;
                }
            }

            if (tintTLFound && tintBRFound) {
                break;
            }
        }

        // Mencari amplop kiri dan kanan
        tintTLFound = false;
        tintBRFound = false;
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                if (!tintTLFound && this.matrixBinary[y][x] == BWC.BLACK_BOOL) {
                    tintTLFound = true;
                    leftEnv = x;
                }

                if (!tintBRFound && this.matrixBinary[y][this.width - x - 1] == BWC.BLACK_BOOL) {
                    tintBRFound = true;
                    rightEnv = this.width - x - 1;
                }
            }

            if (tintTLFound && tintBRFound) {
                break;
            }
        }

        if (topEnv == -1 && bottomEnv == -1 && leftEnv == -1 && rightEnv == -1) {
            return new Rectangle(0, 0, this.width, this.height);
        }
        return new Rectangle(leftEnv, topEnv, rightEnv - leftEnv + 1, bottomEnv - topEnv + 1);
    }

    /**
     * Memutakhirkan matriks biner dengan menghilangkan bagian gambar di luar
     * amplop. Amplop adalah daerah yang melingkupi gambar utama dari layar.
     * Bagian di luar amplop adalah daerah yang tidak memiliki piksel hitam satu
     * pun pada baris/kolomnya.
     *
     * Catatan: Method ini hanya memutakhirkan matriks biner. Panggil method
     * updateImage untuk memutakhirkan image dan Panggil methor repaint untuk
     * menggambar image kembali ke layar.
     */
    public void cropEnvelope() {
        Rectangle crop = this.findEnvelope();
        boolean[][] newMatriks = new boolean[crop.height][crop.width];

        for (int y = 0; y < crop.height; y++) {
            for (int x = 0; x < crop.width; x++) {
                newMatriks[y][x] = this.matrixBinary[y + crop.y][x + crop.x];
            }
        }

        this.matrixBinary = newMatriks;
        this.height = crop.height;
        this.width = crop.width;
    }

    /**
     * Mengembalikan BinaryImage yang merupakan bagian dari BinaryImage ini di
     * dalam area segiempat.
     *
     * @param rect area segiempat yang menjadi batas gambar yang ingin diambil
     * @return Citra yang terletak pada rect
     */
    public BinaryImageShell crop(Rectangle rect) {
        BinaryImageShell bin = new BinaryImageShell(rect.width, rect.height);

        for (int y = 0; y < rect.height; y++) {
            for (int x = 0; x < rect.width; x++) {
                bin.matrixBinary[y][x] = this.matrixBinary[y + rect.y][x + rect.x];
            }
        }
        bin.updateImage();
        return bin;
    }

    /**
     * Mengembalikan objek baru yang sama persis dengan objek ini. Fungsi ini
     * memanfaatkan fungsi crop dengan rect yg besarnya sama dengan ukuran objek
     * ini.
     *
     * @return Objek baru yg sama persis dengan ini
     */
    @Override
    public BinaryImageShell clone() {
        return this.crop(new Rectangle(0, 0, width, height));
    }

    /// *************************************************************************
    /// *************************** STATIC FUNTION ******************************
    /// *************************************************************************
    /**
     * Mengembalikan nilai kebineran dari nilai integer RGB. Jika dua dari nilai
     * tersebut lebih dari THRESHOLD yang ditetapkan, biner dianggap putih. Jika
     * tidak, biner dianggap hitam.
     *
     * @param alpha Nilai transparansi dari citra. 0 = transparan, 255 opaque
     * @param red Nilai merah dari citra. 0 = hitam, 255 merah
     * @param green Nilai hijau dari citra. 0 = hitam, 255 hijau
     * @param blue Nilai biru dari citra. 0 = hitam, 255 biru
     * @return Nilai boolean true berarti putih, false berarti hitam.
     */
    public static boolean checkBiner(int alpha, int red, int green, int blue) {
        int ThresHoldBW = BWC.BW_THRESHOLD;
//        boolean reachThreshold = (red > ThresHoldBW && green > ThresHoldBW)
//                || (red > ThresHoldBW && blue > ThresHoldBW)
//                || (green > ThresHoldBW && blue > ThresHoldBW);
        boolean reachThreshold = (red <= ThresHoldBW && green <= ThresHoldBW)
                || (red <= ThresHoldBW && blue <= ThresHoldBW)
                || (green <= ThresHoldBW && blue <= ThresHoldBW);
//        boolean reachThreshold = (red <= ThresHoldBW || green <= ThresHoldBW || blue <= ThresHoldBW);
        return alpha > ThresHoldBW && reachThreshold ? BWC.BLACK_BOOL : BWC.WHITE_BOOL;
    }

    public static int convertBinertoColor(boolean binerValue) {
        return binerValue == BWC.WHITE_BOOL ? BWC.WHITE_COLOR : BWC.BLACK_COLOR;
    }

    /**
     * Mengembalikan jumlah piksel hitam dikeliling titik fokus.
     *
     * @param xFocus
     * @param yFocus
     * @return Jumlah tetangga (piksel hitam).
     */
    public int getNNeighborAt(int xFocus, int yFocus) {
        /* Nilai jumlah tetangga (warna hitam) dari titik fokus */
        int NNeighbor = 0;
        int[][] offset = new int[][]{{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};

        /* Mengambil ke-8 tetangga dari titik tinjau */
        for (int nt = 0; nt < 8; nt++) {
            int dx = xFocus + offset[nt][0];
            int dy = yFocus + offset[nt][1];

            if (dx >= 0 && dx < this.width && dy >= 0 && dy < this.height) {
                if (this.getBiner(dx, dy) == BWC.BLACK_BOOL) {
                    NNeighbor++;
                }
            }
        }

        return NNeighbor;
    }

    /**
     * Mengecek apakah titik x dan y adalah titik keliling.
     *
     * @param xFocus
     * @param yFocus
     * @return apakah titik fokus titik keliling
     */
    public boolean isBoundaryPixel(int xFocus, int yFocus) {
        int[][] offset = new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        int NN = 0;
        // Mengambil ke-8 tetangga dari titik tinjau
        for (int nt = 0; nt < 4; nt++) {
            int dx = xFocus + offset[nt][0];
            int dy = yFocus + offset[nt][1];

            if (dx >= 0 && dx < this.width && dy >= 0 && dy < this.height) {
                if (this.getBiner(dx, dy) == BWC.BLACK_BOOL) {
                    NN++;
                }
            }
        }

        return this.getBiner(xFocus, yFocus) == BWC.BLACK_BOOL && NN < 4;
    }

    /**
     * Mengubah gambar ini menjadi citra berisi piksel pada kelilingnya saja.
     *
     * @return CItra boundary
     */
    public BinaryImageShell convertToBoundary() {
        BinaryImageShell bound = new BinaryImageShell(this.width, this.height);

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (this.isBoundaryPixel(x, y)) {
                    bound.setBiner(x, y, BWC.BLACK_BOOL);
                } else {
                    bound.setBiner(x, y, BWC.WHITE_BOOL);
                }
            }
        }
        return bound;
    }

    /**
     *
     * @param addImage
     * @param xpos Posisi x ditambahkannya image relatif terhadap image ini
     * @param ypos Posisi y ditambahkannya image relatif terhadap image ini
     * @todo jika, widthnya nambah walaupun tidak pernah digunakan
     * @todo posisi atas relatif bisa berubah saat menambah gambar lebih dari
     * satu
     */
    public void addImage(BinaryImageShell addImage, int xpos, int ypos) {
        //mencari tinggi baru
        int kelebihanBawah = addImage.getHeight() + ypos - this.getHeight();
        kelebihanBawah = kelebihanBawah < 0 ? 0 : kelebihanBawah;
        int kelebihanAtas = ypos < 0 ? -1 * ypos : 0;
        int newHeight = this.getHeight() + kelebihanAtas + kelebihanBawah;

        //matriks baru
        boolean[][] newMatriks = new boolean[newHeight][this.getWidth()];
        //copy matriks lama ke matriks baru
        for (int jn = 0; jn < newHeight; ++jn) {
            for (int in = 0; in < this.getWidth(); ++in) {
                newMatriks[jn][in] = BWC.WHITE_BOOL;

                if (jn > kelebihanAtas && jn - kelebihanAtas < this.getHeight()) {
                    newMatriks[jn][in] = this.matrixBinary[jn - kelebihanAtas][in];
                }
            }
        }

        for (int y = 0; y < addImage.getHeight(); ++y) {
            for (int x = 0; x < addImage.getWidth(); ++x) {
                int yy = y + ypos + kelebihanAtas;
                int xx = x + xpos;
                newMatriks[yy][xx] = addImage.getBiner(x, y);
            }
        }

        this.height = newHeight;
        //this.width = newWidth;
        this.matrixBinary = newMatriks;
        this.updateImage();
    }

    /**
     *
     * @todo rekursif yang ebih mangkus dan nggak stack error
     * @param xmouse
     * @param ymouse
     */
    public void findBlackPixelsGroup(int xmouse, int ymouse) {
        if (this.isBlack(xmouse, ymouse)) {
            Rectangle r = new Rectangle(new Point(xmouse, ymouse));
            this.rec(r, xmouse, ymouse);
            this.updateImage();
            this.drawRectangle(r);
            this.repaint();
        }
    }

    /**
     * @todo rekursif yang ebih mangkus dan nggak stack error
     * @param r
     * @param x
     * @param y
     */
    private void rec(Rectangle r, int x, int y) {
        if (this.isBlack(x, y)) {
            this.setAsWhite(x, y);
            r.add(new Point(x, y));

            if (y != 0) {
                rec(r, x, y - 1);
            }
            if (y < height) {
                rec(r, x, y + 1);
            }
            if (x != 0) {
                rec(r, x - 1, y);
            }
            if (x < width) {
                rec(r, x + 1, y);
            }
        }
    }

    @Override
    public void medianFilter() {
        int[][] offset = new int[][]{{0, 0}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};

        /*Get pixels from image*/
        int filterWidth = this.getWidth() - 1;
        int filterHeight = this.getHeight() - 1;
        int[] srgb = new int[this.width_x_height];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), srgb, 0, image.getWidth());

        /*Init*/
        int[] temp = new int[this.getWxH()];

        /*Do for Red (<<16), Green (<<8), and Blue (<<0)*/
        for (int j = 0; j < this.getHeight(); ++j) {
            for (int i = 0; i < this.getWidth(); ++i) {
                if (i == 0 || i == filterWidth || j == 0 || j == filterHeight) {
                    temp[j * image.getWidth() + i] += (255 << 24) + (255 << 16) + (255 << 8) + 255;
                    continue;
                }

                /*Fetch neighbor*/
                int[] neighbor = new int[9];
                for (int off = 0; off < offset.length; ++off) {
                    int xoff = i - offset[off][0];
                    int yoff = j - offset[off][1];
                    int rawRGB = srgb[yoff * image.getWidth() + xoff];
                    neighbor[off] = (rawRGB >>> 16) % 256;
                }
                /*Find median*/
                Arrays.sort(neighbor);
                int median = neighbor[5];
                /*replace*/
                temp[j * image.getWidth() + i] += (255 << 24) + (median << 16) + (median << 8) + median;
            }
        }

        image.setRGB(0, 0, this.getWidth(), this.getHeight(), temp, 0, this.getWidth());
        this.updateMatrix();
        this.repaint();
    }

    /**
     * Melakukan rotasi terhadap citra. Kode diambil dari
     * http://flyingdogz.wordpress.com/2008/02/11/image-rotate-in-java-2-easier-to-use/
     *
     * @param angle Sudut dalam radian
     */
    public void rotate(double angle) {
        double sin = Math.abs(Math.sin(angle)), cos = Math.abs(Math.cos(angle));
        int w = image.getWidth(), h = image.getHeight();
        int neww = (int) Math.floor(w * cos + h * sin), newh = (int) Math.floor(h * cos + w * sin);

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        BufferedImage result = gc.createCompatibleImage(neww, newh, Transparency.TRANSLUCENT);
        Graphics2D g = result.createGraphics();

        g.translate((neww - w) / 2, (newh - h) / 2);
        g.rotate(angle, w / 2, h / 2);
        g.drawRenderedImage(image, null);
        g.dispose();

        image = result;
    }

    /**
     * Jika dua piksel hitam dalam horizontal terdapat jeda (putih) kurang dari
     * sama dengan N piksel, hitamkan piksel putih tersebut.
     */
    public void smear() {
        int whiteThreshold = 15;
        int whiteThresholdY = 3;
        for (int ynow = 0; ynow < height; ynow++) {
            Point pointLeft = new Point(9999, 9999);

            boolean foundBlack;
            boolean beforeIsBlack = false;
            for (int xnow = 0; xnow < width; xnow++) {
                foundBlack = xnow == image.getWidth() ? false : this.isBlack(xnow, ynow);

                // Perpindahan dari putih ke hitam, piksel sekarang hitam
                if (!beforeIsBlack && foundBlack) {
                    //jeda putih kurang dari ambang
                    int nwhite = xnow - pointLeft.x;
                    if (nwhite <= whiteThreshold) {
                        //putihkan semua piksel putih jeda
                        for (int xq = pointLeft.x; xq < xnow; ++xq) {
                            this.setAsBlack(xq, ynow);
                        }
                    }
                }
                // Perpindahan dari hitam ke putih, piksel sekarang putih
                if (beforeIsBlack && !foundBlack) {
                    pointLeft = new Point(xnow, ynow);
                }
                beforeIsBlack = foundBlack;
            }
        }


        for (int xnow = 0; xnow < width; xnow++) {
            Point pointLeft = new Point(9999, 9999);

            boolean foundBlack;
            boolean beforeIsBlack = false;
            for (int ynow = 0; ynow < height; ynow++) {
                foundBlack = ynow == image.getHeight() ? false : this.isBlack(xnow, ynow);

                // Perpindahan dari putih ke hitam, piksel sekarang hitam
                if (!beforeIsBlack && foundBlack) {
                    //jeda putih kurang dari ambang
                    int nwhite = ynow - pointLeft.y;
                    if (nwhite <= whiteThresholdY) {
                        //putihkan semua piksel putih jeda
                        for (int yq = pointLeft.y; yq < ynow; ++yq) {
                            this.setAsBlack(xnow, yq);
                        }
                    }
                }
                // Perpindahan dari hitam ke putih, piksel sekarang putih
                if (beforeIsBlack && !foundBlack) {
                    pointLeft = new Point(xnow, ynow);
                }
                beforeIsBlack = foundBlack;
            }
        }
    }

    public double skewEstimation() {
        return skewEstimation(this);
    }

    /**
     * Estimate the skew of image by smearing it first.
     *
     * @param bin
     * @return
     */
    public static double skewEstimation(BinaryImageShell bin) {
        BinaryImageShell testRotate = bin.clone();
        testRotate.resizeToHeight(900);
        testRotate.smear();
        double averageAngle = FeatureExtraction.moment_skewAngleAverage(testRotate);
        return averageAngle;
    }

    public void skewCorrection() {
        /*First try*/
        double averageAngle_1 = skewEstimation();
        /*Temp rotation*/
        BinaryImageShell firstRotated = this.clone();
        firstRotated.rotate(-1 * averageAngle_1);
        firstRotated.updateMatrix();
        /*Second try*/
        double averageAngle_2 = skewEstimation(firstRotated);

        /*Sum of the two trial*/
        double averageAngle = averageAngle_1 + averageAngle_2;
        double averageAngleDegree = Math.toDegrees(averageAngle);
        //Info
        System.out.println("Moments found skew degree: " + averageAngleDegree);
        //jika diantara -0.2 - 0.2 derajat, jangan putar
        if (averageAngleDegree >= 0.5 || averageAngleDegree <= -0.5) {
            this.rotate(-1 * averageAngle);
            this.updateMatrix();
            System.out.println("Image is skewed by " + averageAngleDegree + " degrees and has been corrected.");
        } else {
            System.out.println("Skew is ignored");
        }
    }
    
    public void mySkewCorrectionDrawOnly() {
        //****************** Kira-kira Titik Segitiga Miring *******************
        //cari titik di kolom piksel terkanan dan teratas untuk acuan kemiringan

        //cari titik paling kanan, cek apakah dia ada di atas atau bawah relatif thd tengah dokumen
        Point terkanan = new Point();
        int nthTerkananFound = 0;
        boolean terkananFound = false;
        boolean terkananFoundDibawah = false;
        for (int x = this.getWidth() - 1; x >= 0; --x) {
            //cari proyeksi vertikal di posisi x ini
            for (int y = this.getHeight() - 1; y >= 0; --y) {
                if (isBlack(x, y)) {
                    terkanan = y > terkanan.y ? new Point(x, y) : terkanan;
                    nthTerkananFound++;
                    if (nthTerkananFound >= 3) {
                        if (y > this.getHeight() / 2) {
                            terkananFoundDibawah = true;
                        }
                        terkananFound = true;
                    }
                    break;
                }
            }
            if (terkananFound) {
                break;
            }
        }
        //cari lagi dari bawah, jika ternyata ditemukannya di bagian bawah dokumen
        //supaya titik yang ditemukan adalah terbawah dari deretan piksel yg ada
        terkananFound = false;
        nthTerkananFound = 0;
        if (terkananFoundDibawah) {
            for (int x = this.getWidth() - 1; x >= 0; --x) {
                //cari proyeksi vertikal di posisi x ini
                for (int y = 0; y <= terkanan.y; ++y) {
                    if (isBlack(x, y)) {
                        terkanan = y < terkanan.y ? new Point(x, y) : terkanan;
                        nthTerkananFound++;
                        if (nthTerkananFound >= 3) {
                            if (y > this.getHeight() / 2) {
                                terkananFoundDibawah = true;
                            }
                            terkananFound = true;
                        }
                        break;
                    }
                }
                if (terkananFound) {
                    break;
                }
            }
        }

        //cari titik teratas (atau terbawah) bergantung pada letah titik terkanan
        //jika titik terkanan ada di bagian relatif atas, titik teratas ada di bawah
        Point teratas = new Point();
        boolean teratasFinallyFound = false;
        int nthTeratasFound = 0;
        if (terkananFoundDibawah) {
            for (int y = 0; y < this.getHeight(); ++y) {
                for (int x = this.getWidth() - 1; x >= 0; x--) {
                    if (isBlack(x, y)) {
                        teratas = x > teratas.x ? new Point(x, y) : teratas;
                        nthTeratasFound++;
                        if (nthTeratasFound >= 16) {
                            teratasFinallyFound = true;
                        }
                        break;
                    }
                }
                if (teratasFinallyFound) {
                    break;
                }
            }
        } else {
            for (int y = this.getHeight() - 1; y >= 0; --y) {
                for (int x = this.getWidth() - 1; x >= 0; x--) {
                    if (isBlack(x, y)) {
                        teratas = x > teratas.x ? new Point(x, y) : teratas;
                        nthTeratasFound++;
                        if (nthTeratasFound >= 16) {
                            teratasFinallyFound = true;
                        }
                        break;
                    }
                }
                if (teratasFinallyFound) {
                    break;
                }
            }
        }

        //******************** Gambar Garis sbg. Informasi *********************
        this.drawLine(teratas, terkanan, Color.BLUE);
    }
    
    public void mySkewCorrection() {

        //****************** Kira-kira Titik Segitiga Miring *******************
        //cari titik di kolom piksel terkanan dan teratas untuk acuan kemiringan

        //cari titik paling kanan, cek apakah dia ada di atas atau bawah relatif thd tengah dokumen
        Point terkanan = new Point();
        int nthTerkananFound = 0;
        boolean terkananFound = false;
        boolean terkananFoundDibawah = false;
        for (int x = this.getWidth() - 1; x >= 0; --x) {
            //cari proyeksi vertikal di posisi x ini
            for (int y = this.getHeight() - 1; y >= 0; --y) {
                if (isBlack(x, y)) {
                    terkanan = y > terkanan.y ? new Point(x, y) : terkanan;
                    nthTerkananFound++;
                    if (nthTerkananFound >= 3) {
                        if (y > this.getHeight() / 2) {
                            terkananFoundDibawah = true;
                        }
                        terkananFound = true;
                    }
                    break;
                }
            }
            if (terkananFound) {
                break;
            }
        }
        //cari lagi dari bawah, jika ternyata ditemukannya di bagian bawah dokumen
        //supaya titik yang ditemukan adalah terbawah dari deretan piksel yg ada
        terkananFound = false;
        nthTerkananFound = 0;
        if (terkananFoundDibawah) {
            for (int x = this.getWidth() - 1; x >= 0; --x) {
                //cari proyeksi vertikal di posisi x ini
                for (int y = 0; y <= terkanan.y; ++y) {
                    if (isBlack(x, y)) {
                        terkanan = y < terkanan.y ? new Point(x, y) : terkanan;
                        nthTerkananFound++;
                        if (nthTerkananFound >= 3) {
                            if (y > this.getHeight() / 2) {
                                terkananFoundDibawah = true;
                            }
                            terkananFound = true;
                        }
                        break;
                    }
                }
                if (terkananFound) {
                    break;
                }
            }
        }

        //cari titik teratas (atau terbawah) bergantung pada letah titik terkanan
        //jika titik terkanan ada di bagian relatif atas, titik teratas ada di bawah
        Point teratas = new Point();
        boolean teratasFinallyFound = false;
        int nthTeratasFound = 0;
        if (terkananFoundDibawah) {
            for (int y = 0; y < this.getHeight(); ++y) {
                for (int x = this.getWidth() - 1; x >= 0; x--) {
                    if (isBlack(x, y)) {
                        teratas = x > teratas.x ? new Point(x, y) : teratas;
                        nthTeratasFound++;
                        if (nthTeratasFound >= 16) {
                            teratasFinallyFound = true;
                        }
                        break;
                    }
                }
                if (teratasFinallyFound) {
                    break;
                }
            }
        } else {
            for (int y = this.getHeight() - 1; y >= 0; --y) {
                for (int x = this.getWidth() - 1; x >= 0; x--) {
                    if (isBlack(x, y)) {
                        teratas = x > teratas.x ? new Point(x, y) : teratas;
                        nthTeratasFound++;
                        if (nthTeratasFound >= 16) {
                            teratasFinallyFound = true;
                        }
                        break;
                    }
                }
                if (teratasFinallyFound) {
                    break;
                }
            }
        }

        //******************** Gambar Garis sbg. Informasi *********************
        //this.drawLine(teratas, terkanan, Color.BLUE);

        //******************** Hitung Piksel pada Segitiga *********************
        int pikselPadaSegitiga = 0;

        //titik acuan y
        int topY = teratas.y < terkanan.y ? teratas.y : terkanan.y;
        int bottomY = teratas.y > terkanan.y ? teratas.y : terkanan.y;

        //isi titik terkanan dari segitiga yang dibentuk
        int[] triangleBorder = new int[bottomY - topY + 1];
        ArrayList<Point> pointsSeg = MyMath.lineBresenham(teratas.x, teratas.y, terkanan.x, terkanan.y);
        for (Point p : pointsSeg) {
            int yi = p.y - topY;
            triangleBorder[yi] = p.x > triangleBorder[yi] ? p.x : triangleBorder[yi];
        }

        //menghitung piksel hitam pada area segitiga
        for (int y = topY; y < bottomY; ++y) {
            int yi = y - topY;
            for (int x = triangleBorder[yi]; x < this.getWidth(); ++x) {
                if (isBlack(x, y)) {
                    pikselPadaSegitiga++;
                }
            }
        }

        //ukuran segitiga
        int luasSegitiga = Math.abs((terkanan.x - teratas.x) * (terkanan.y - teratas.y)) / 2;
        double panjangHipot = Math.hypot(terkanan.x - teratas.x, terkanan.y - teratas.y);
        double pikselPerLuas = (double) pikselPadaSegitiga / luasSegitiga;
        double pikselPerHipot = pikselPadaSegitiga / panjangHipot;

        System.out.println("Banyak piksel:" + pikselPadaSegitiga);
        System.out.println("Luas segitiga:" + luasSegitiga);
        System.out.println("Panjang hipot:" + panjangHipot);
        System.out.println("Piksel/luas  :" + pikselPerLuas);
        System.out.println("Piksel/hipot :" + pikselPerHipot);

        //hanya hitung kemiringan dan lakukan koreksi jika kira2 dokumen ini miring
        //i.e. jumlah piksel pada segitiga "sedikit", 
        //karena kalau banyak berarti segitiga itu segitiga yang salah
        if (pikselPerHipot < 2 && pikselPerLuas < 0.2) {
            //hitung kemiringan
            double skewAngle = Math.atan2((double) terkanan.x - teratas.x, (double) terkanan.y - teratas.y);
            skewAngle = skewAngle < Math.PI / 2 ? skewAngle : skewAngle - Math.PI;
            double skewAngle_inDegree = Math.toDegrees(skewAngle);
            System.out.println("Triangle found skew degree: " + (-1 * skewAngle_inDegree));
            //lakukan koreksi miring jika kemiringan cukup signifikan
            if (skewAngle_inDegree >= 0.4 || skewAngle_inDegree <= -0.4) {
                this.rotate(skewAngle);
                this.updateMatrix();
                System.out.println("Image is skewed by " + (-1 * skewAngle_inDegree) + " degrees and has been corrected.");
            } else {
                System.out.println("Skew is ignored");
            }
        }
    }

    /// *************************************************************************
    /// *****************************  DRIVER  **********************************
    /// *************************************************************************
    public static void main(String[] args) {
        //Test rotation
        BinaryImageShell bImageShell = new BinaryImageShell("_test_result/rotate/test.png");
        bImageShell.rotate(Math.toRadians(20));
        bImageShell.saveImage("_test_result/rotate/rotated_r_20.png");

        //Test smear
        BinaryImageShell smearBin = new BinaryImageShell("_page/skew/Arial - 2, 7.2 persen - norm_1.png");
        smearBin.resizeToHeight(800);
        smearBin.smear();
        smearBin.updateImage();
        smearBin.saveImage("_test_result/rotate/Arial - 2, 7.2 persen - smeared.png");
    }
}
