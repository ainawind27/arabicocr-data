package image.segmentator;

import common.MyMath;
import image.BWC;
import image.BinaryImageShell;
import image.MainbodySOSet;
import image.RectAndBlackPoints;
import image.classifier.FeatureExtraction;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Albadr
 * @author Gazandi
 */
public class SegmentatorSubword {

    // ***************************** PROPERTY ******************************
    /**
     * Menyimpan citra asal yang akan disegment. Citra bisa berupa citra baris
     * atau citra dokumen.
     */
    BinaryImageShell image;
    /**
     * Menyimpan koleksi piksel terhubung yg diwaliki oleh RectAndBlackPoints.
     */
    ArrayList<RectAndBlackPoints> allConnectedPixelsGroup;
    ArrayList<RectAndBlackPoints> mainBodyBlocks;
    ArrayList<RectAndBlackPoints> secondaryBlocks;
    /**
     * Menyimpan seluruh subword. Setiap objek dalam array mengandung mainbody
     * dan secondary object.
     */
    ArrayList<MainbodySOSet> subwords;
    int peakBaselinePosition = 0;
    int maxBaselinePosition = 0;

    // *************************** CONSTRUCTOR ********************************
    public SegmentatorSubword(BinaryImageShell image) {
        this.image = image;

        //Fetch baseline
        int[] baselineBand = FeatureExtraction.baselineHP(image);
        this.peakBaselinePosition = baselineBand[1];
        this.maxBaselinePosition = baselineBand[2];

        //init arraylist
        allConnectedPixelsGroup = new ArrayList<>();
        mainBodyBlocks = new ArrayList<>();
        secondaryBlocks = new ArrayList<>();
        subwords = new ArrayList<>();
    }

    // *************************** FUNCTIONS **********************************
    /**
     * Memotong image menjadi bagian block. Block didefinisikan sebagai tubuh
     * piksel hitam yang saling terhubung alias menempel satu sama lain. Hasil
     * dari method ini adalah daftar piksel hitam terkoneksi pada properti
     * allConnectedPixelsGroup.
     */
    public void blockSegment() {
        ArrayList<RectAndBlackPoints> farBlocks = new ArrayList<RectAndBlackPoints>();
        ArrayList<RectAndBlackPoints> nearBlocks = new ArrayList<RectAndBlackPoints>();

        for (int ynow = 0; ynow < image.getHeight(); ynow++) {
            /*Penyimpan block piksel menempel pada y tertentu*/
            ArrayList<RectAndBlackPoints> lineBlockY = new ArrayList<RectAndBlackPoints>();

            Point pointLeft = new Point(9999, 9999);

            boolean foundBlack;
            boolean beforeBlack = false;
            /*Mencari piksel menempel pada satu baris*/
            for (int xnow = 0; xnow <= image.getWidth(); xnow++) {
                foundBlack = xnow == image.getWidth() ? false
                        : image.getBiner(xnow, ynow) == BWC.BLACK_BOOL ? true : false;

                // Perpindahan dari putih ke hitam, piksel sekarang hitam
                if (!beforeBlack && foundBlack) {
                    pointLeft = new Point(xnow, ynow);
                }
                // Perpindahan dari hitam ke putih, piksel sekarang putih
                if (beforeBlack && !foundBlack) {
                    //Posisi hitam yang terakhir adalah sebelum x sekarang (xs) sehingga lebar xs-x, bukan xs-x+1
                    lineBlockY.add(new RectAndBlackPoints(pointLeft, new Dimension(xnow - pointLeft.x, 1)));
                    pointLeft = new Point(99999, 99999);
                }
                beforeBlack = foundBlack;
            }/* END FOR XNOW */

            //Cek ada yang menempel tidak untuk setiap block ditemukan yg dekat
            for (RectAndBlackPoints formerBlock : nearBlocks) {
                //Cek setiap rectangle line
                for (RectAndBlackPoints rline : lineBlockY) {
                    if (formerBlock.isTouched(rline)) {
                        formerBlock.absorb(rline);
                    }
                }
            }

            //Hapus seluruh block dekat yang terserap blok lain
            //pindah ke mainblock jika sudah tidak dekat (ymax sudah jauh dari ynow)
            int nearBlocksStart = nearBlocks.size() - 1;
            for (int index = nearBlocksStart; index >= 0; index--) {
                if (nearBlocks.get(index).isAbsorbed()) {
                    nearBlocks.remove(index);
                }
                if (nearBlocks.get(index).getRect().getMaxY() < ynow) {
                    farBlocks.add(nearBlocks.get(index));
                    nearBlocks.remove(index);
                }
            }

            //Promosikan seluruh lineBlock yang belum diserap menjadi mainBlock
            for (RectAndBlackPoints rline : lineBlockY) {
                if (!rline.isAbsorbed()) {
                    nearBlocks.add(rline);
                }
            }
        }

        this.allConnectedPixelsGroup.addAll(farBlocks);
        this.allConnectedPixelsGroup.addAll(nearBlocks);
        //System.out.println("State: segment block finished with " + this.allConnectedPixelsGroup.size() + " blocks");

        //Update baseline
        int[] baselineBand = FeatureExtraction.baselineHP(image);
        this.peakBaselinePosition = baselineBand[1];
        this.maxBaselinePosition = baselineBand[2];
        //image.drawLine(peakBaselinePosition, true, Color.gray);
    }

    // *************************** SETTER GETTER **********************************
    /**
     * Mengembalikan baseline dari image yang menjadi input segmentator subword
     * ini. Baseline dihitung pada posisi terbawah pita baseline.
     *
     * @return Posisi baseline (y) thd. titik nol (titik teratas) citra baris
     */
    public int getBaseline() {
        return maxBaselinePosition;
    }

    /**
     * Mengembalikan segment body group (group bisa berupa subword atau letter)
     * pada index tertentu. Perhatikan bahwa index tidak divalidasi.
     *
     * @param index Posisi dalam array (juga berarti posisi segment dari sisi
     * paling kanan)
     * @return Segment pada posisi yg ditentukan
     */
    public MainbodySOSet getSegment(int index) {
        return this.subwords.get(index);
    }

    public MainbodySOSet[] getAllSegments() {
        return this.subwords.toArray(new MainbodySOSet[0]);
    }

    /**
     * Mengembalikan index mainbody yang berada pada posisi mouse panel line
     * yang diklik. Area yang dipertimbangkan termasuk area secondary object
     * juga.
     *
     * @param mouseX posisi x mouse
     * @param mouseY posisi y mouse
     * @return index mainbody. Jika tidak ditemukan mainbody apapun,
     * mengembalikan nilai -1.
     * @todo Compliance with ABG
     */
    public int getBlock_onClick(int mouseX, int mouseY) {
        double minJarak = 99999;
        RectAndBlackPoints selectedRect = null;
        int indexSelected = -1;

        for (int i = 0; i < this.subwords.size(); i++) {
            Rectangle rect = this.subwords.get(i).getBlock_withSecondary().getRect();
            if (rect.contains(mouseX, mouseY)) {
                double cx = rect.getCenterX();
                double cy = rect.getCenterY();
                /*Hitung jarak, ambil block yg jarak ke pusatnya terdekat*/
                double jarak = MyMath.sqr(cx - mouseX) + MyMath.sqr(cy - mouseY);
                if (jarak < minJarak) {
                    minJarak = jarak;
                    indexSelected = i;
                }
            }
        }

        return indexSelected;
    }

    /**
     * Mengembalikan main block saja TANPA secondary object.
     *
     * @param index posisi main block pada array. Tidak ada validasi range.
     * @return Image main block sebuah sub word TANPA secondary object.
     */
    public BinaryImageShell getSegmentImage(int index) {
        return this.subwords.get(index).getImage_bodyOnly();
    }

    /**
     * Mengembalikan main block saja DENGAN secondary object.
     *
     * @param index posisi main block pada array. Tidak ada validasi range.
     * @return Image main block sebuah sub word DENGAN secondary object.
     */
    public BinaryImageShell getSegmentImage_withSecondary(int index) {
        return this.subwords.get(index).getImage_withSecondary();
    }

    /**
     * Mengembalikan jumlah main block yang ada.
     *
     * @return jumlah main block yg disegmentasi objek ini
     */
    public int sizeSegments() {
        return this.subwords.size();
    }

    /**
     * Mengelompokkan blok mejadi blok mainbody dan blok secondary objek. Dicari
     * juga kedekatan antara mainbody dan secondary sehingga didapat blok yang
     * mengandung secondary objek.
     */
    public void groupBlocks() {
        //Mengelompokkan setiap blok piksel terhubung berdasarkan ukuran
        for (RectAndBlackPoints rab : this.allConnectedPixelsGroup) {
            int size = rab.getSize();

            //pisahkan menurut ukurannya
            if (size >= 400 || rab.getRect().getHeight() >= 25
                    || (size >= 16 && rab.getRect().intersectsLine(0, peakBaselinePosition, this.image.getWidth(), peakBaselinePosition))) {
                //main body adalah yang ukurannya lebih dari 400
                mainBodyBlocks.add(rab);
            } else if (rab.getRect().getHeight() < 25 && size >= 16 && size < 400) {
                //secondary adalah yang ukurannya 40-400, tapi tingginya < 20
                secondaryBlocks.add(rab);
                image.drawRectangle(rab.getRect(), Color.YELLOW);
            } else {
                //selain itu dianggap noise
                image.drawRectangle(rab.getRect(), Color.LIGHT_GRAY);
            }
        }

        //Mengurutkan manbody dari kanan ke kiri
        Collections.sort(this.mainBodyBlocks);

        // Menempatkan secondary objek pada mainbody yang menaunginya
        for (RectAndBlackPoints main : mainBodyBlocks) {
            MainbodySOSet abg = new MainbodySOSet(main);
            //Set baseline relative thp posisi vertikal mainbody
            abg.setBaseline(maxBaselinePosition);
            //cek taungan secondary objek
            for (RectAndBlackPoints sec : secondaryBlocks) {
                if (main.isSecondaryWith(sec)) {
                    abg.addSecondaryObject(sec);
                }
            }
            //menyimpan subword
            subwords.add(abg);
            //pengotakan, untuk visualisasi
            image.drawRectangle(abg.getBlock_bodyOnly().getRect(), Color.GREEN);
            image.drawRectangle(abg.getBlock_withSecondary().getRect(), Color.RED);
        }
    }
}
