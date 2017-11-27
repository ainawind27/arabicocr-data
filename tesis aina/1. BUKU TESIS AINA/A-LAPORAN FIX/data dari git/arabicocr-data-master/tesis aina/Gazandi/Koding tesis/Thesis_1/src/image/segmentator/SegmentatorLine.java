package image.segmentator;

import common.MyMath;
import image.BinaryImageShell;
import image.RectAndBlackPoints;
import image.classifier.FeatureExtraction;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Melakukan pemotongan antar baris tulisan arab. Metode yang digunakan adalah
 * proyeksi horizontal digabung dengan badan piksel terkoneksi. Secara umum,
 * langkah segmentasi citra barisyang dilakukan kelas ini adalah sebagai berikut
 * 1. Mencari proyeksi horizontal dari citra dokumen. Dari sini, titik terendah
 * dari proyeksi horizontal dicari. Titik terendah harus kurang dari 10 piksel.
 * 2. Memotong citra berdasarkan titik terendah yang tersisa.
 *
 * @todo Mengubah teknik pemotongan baris dengan langkah-langah sebagai berikut:
 * 1. Mencari garis-garis batas antar baris tulisan dengan proyeksi horizontal
 * 2. Mencari kotak amplop dari setiap tubuh piksel terkoneksi pada citra baris
 * 3. Menggunakan garis batas untuk memotong citra dengan memperhatikan daftar
 * kotak amplop setiap tubuh piksel terkoneks, kalau-kalau ada yang terpenggal
 * 4. Menyediakan setiap baris yang berhasil dipotong sehingga bisa diambil
 * berdasarkan nomor baris atau piksel posisi klik mouse.
 *
 * @author Albadr
 * @author Gazandi
 */
public class SegmentatorLine {

    /**
     * Menyimpan citra asal yang akan disegment. Citra bisa berupa citra baris
     * atau citra dokumen.
     */
    BinaryImageShell image;
    /**
     * Daftar titik garis pisah antar baris
     */
    ArrayList<Integer> borderline = new ArrayList<>();
    /**
     * Daftar segmen baris yang sudah dipotong
     */
    ArrayList<BinaryImageShell> segment;
    /**
     * Proyeksi horizonal dari citra dokumen
     */
    int[] hpro;

    /**
     *
     * @param docImage Gambar yang sudah diamplopkan
     */
    public SegmentatorLine(BinaryImageShell docImage) {
        this.image = docImage;
        hpro = FeatureExtraction.projection(this.image).get("horizontal");
    }

    /**
     * Mencari garis pembatas antara baris yang digunakan untuk memisahkan antar
     * baris tulisan arab. Menghasilkan ArrayList berisi integer posisi garis
     * pembatas tersebut. Method ini adalah langkah kesatu dari empat langkah
     * kelas ini.
     */
    public void findSeparationLine() {
        int ambang = 20;//(int) (0.02 * this.image.getWidth());
        int hbefore = 0;

        int ya = 0;
        for (int y = 0; y < hpro.length; ++y) {
            int hnow = hpro[y];
            if (hbefore > ambang && hnow <= ambang) {
                //awal kandidat garis pisah
                ya = y;
            } else if (hbefore <= ambang && hnow > ambang || y == this.image.getHeight() - 1) {
                //akhir kandidat garis pisah
                if (y - ya > 2) {
                    //kandidat utama adalah tengahnya daerah kemungkinan
                    int minHproY = (y + ya) / 2;
                    int minHproValue = hpro[minHproY];
                    //dari jeda yang ada, cari yang terkecil.
                    for (int l = ya; l <= y; l++) {
                        if (hpro[l] < minHproValue) {
                            minHproY = l;
                            minHproValue = hpro[l];
                        }
                    }

                    borderline.add(minHproY);
                    this.image.drawLine(minHproY, true, Color.YELLOW);
                }
            }

            //next iteration   
            hbefore = hnow;

            //draw horizontal projection graphic, ignore this on production
            int hsen = hnow / 20;
            this.image.drawLine(new Point(0, y), new Point(hsen, y), Color.RED);
        }

        verifyBorderline();
    }

    /**
     * Melihat kenormalan data lebar baris (jarak antar garis pisah) dengan
     * standar deviasi.
     *
     *
     * Lebar baris normal jika stdDeviasi / mean kurang dari 10%. Jika tidak
     * normal, garis pisah yang membuat baris di bawahnya berjarak kurang dari
     * mean-stdDev akan dihapus.
     *
     * @todo penanganan jika lebar baris lebih besar dari mean + stdDev
     */
    private void verifyBorderline() {
        final double stdDevPerMean_threshold = 0.1;

        //hitung jarak antar dua borderline. 
        //bordeline[0] adalah garis teratas dekat amplop citra dokumen
        //bordeline[N] adalah garis terbawah dekat amplop citra dokumen
        double[] distancePop = new double[borderline.size() + 1];
        for (int i = 0; i < distancePop.length; ++i) {
            if (i == 0) {
                distancePop[i] = borderline.get(i);
            } else if (i == distancePop.length - 1) {
                distancePop[i] = image.getHeight() - borderline.get(i - 1);
            } else {
                distancePop[i] = borderline.get(i) - borderline.get(i - 1);
            }
        }

        double mean = MyMath.mean(distancePop);
        double stdDev = MyMath.standardDeviation(distancePop);
        double stdDevPerMean = stdDev / mean;
        //System.out.println("mean=" + mean);
        //System.out.println("stdd=" + stdDev);
        //System.out.println("stdd/mean=" + stdDevPerMean);

        //jika stdDevPerMean <= 0.1, data jarak dianggap benar, cukein data,
        //Jika stdDevPerMean > 0.1 berarti ada data pencilan, benahi data.
        if (stdDevPerMean >= stdDevPerMean_threshold) {
            double meanPlusStdDev = mean + stdDev;
            double meanMinusStdDev = mean - stdDev;
            //System.out.println("meanMinusStdDev=" + meanMinusStdDev);

            //cari setiap jarak yang kurang dari mean - stddev.
            //garis yang dihapus juga ada di i, dengan kata lain baris itu digabung dengan baris atasnya
            //ini untuk menghilangkan baris yang hanya berisi titik dua ya
            ArrayList<Integer> killIndex = new ArrayList<>();
            //dari belakang supaya killIndex berisi index terbesar duluan
            for (int i = distancePop.length - 1; i >= 0; --i) {
                if (distancePop[i] < meanMinusStdDev) {
                    int ki;
                    if (i == 0) {
                        ki = i;
                    } else if (i == distancePop.length - 1) {
                        ki = i - 1;
                    } else {
                        ki = hpro[borderline.get(i)] > hpro[borderline.get(i - 1)] ? i : i - 1;
                    }
                    if (!killIndex.contains(ki)) {
                        killIndex.add(ki);
                    }
                }
            }
            //hapus setiap garis penyebab jarak < mean - stddev.
            //hapus dari index yang paling besar (di paling awal list)
            for (int ki : killIndex) {
                try {
                    borderline.remove(ki);
                } catch (IndexOutOfBoundsException e) {
                }
            }

        }
    }

    /**
     * Memisahkan antar baris dengan mencari apakah ada objek terpotong pada
     * garis pisah. Jika ada, diambil objek tersebut berdasarkan keterhubungan
     * piksel hitam satu sama lain, kemudian ditentukan objek tersebut milik
     * baris yang mana berdasarkan mayoritas objek pada wilayah baris mana.
     *
     * Asumsi : titik pisah setiap baris sudah jelas.
     */
    public void separate() {
        /* Setiap batas di crop menjadi citra baris */
        for (int i = 0; i < borderline.size(); ++i) {
            int ynow = borderline.get(i);
            int yNumPixel = hpro[ynow];

            /* Jika pada batas ada pikel yang tertabrak */
            /* @todo Segmentation line: kalau ada overlap di batas */
            if (yNumPixel > 0) {
                /* Telusuri batas dan ambil blok piksel yang ditemukan.*/
                ArrayList<RectAndBlackPoints> balokPotong = new ArrayList<>();
                for (int x = 1; x < image.getWidth(); ++x) {
                    if (image.isWhite(x - 1, ynow) && image.isBlack(x, ynow)) {
                        RectAndBlackPoints rab = this.findBlackBlockIntersection(x, ynow);
                        balokPotong.add(rab);
                    }
                }

                int mostTopYBlock = ynow;
                //untuk setiap garis potong yang melewati sesuatu piksel
                for (RectAndBlackPoints r : balokPotong) {
                    int yBalokTop = r.getRect().y;
                    int balokHeight = r.getRect().height;
                    double upPercent = (double) (ynow - yBalokTop) / balokHeight;
                    //geser sesuai tepi atas objek piksel terhubung yang dilewati
                    //jika garis potong tersebut punya kontribusi 35% pemotongan objek piksel
                    if (upPercent < 0.35 && r.getRect().y < mostTopYBlock) {
                        mostTopYBlock = r.getRect().y;
                    }
                }

                this.borderline.set(i, mostTopYBlock);
            }
        }

        //Jika terdapat N borderline, ada N+1 segmen yang dihasilkan
        this.segment = new ArrayList<>();
        for (int i = 0; i < this.borderline.size() + 1; ++i) {
            /* Segmen akhir, batas bawah segmen adalah tinggi citra dokumen*/
            int ynow = i == borderline.size() ? this.image.getHeight() : borderline.get(i);
            /* Segmen awal, batas atas segmen adalah 0.*/
            int ybefore = i == 0 ? 0 : borderline.get(i - 1);
            /* Tinggi citra*/
            int outputHeight = ynow - ybefore;
            //System.out.println(i+", "+ynow+"");
            if (outputHeight > 0) {
                /* Ambil image di daerah itu */
                this.image.drawLine(ynow, true, Color.RED);
                BinaryImageShell newbin = image.crop(new Rectangle(0, ybefore, this.image.getWidth(), outputHeight));
                this.segment.add(newbin);
            } else {
                System.out.println("... There is zero height! Check for error. ...");
            }
        }
    }

    /**
     * Mencari tubuh piksel terhubung pada lokasi x, y.
     *
     * @param xmouse Posisi x mula sebagai bagian dari objek
     * @param ymouse Posisi y mula sebagai bagian dari objek
     * @return Objek yang mewaili amplop dan daftar titik piksel terhubung yang
     * dikenai intereksi
     */
    private RectAndBlackPoints findBlackBlockIntersection(int xmouse, int ymouse) {
        RectAndBlackPoints r = new RectAndBlackPoints(new Point(xmouse, ymouse), new Dimension());
        BinaryImageShell bin = this.image.clone();
        this.rec(bin, r, xmouse, ymouse);
        return r;
    }

    /**
     * Secara rekursif mengambil daftar titik piksel terhubung dengan x,y.
     *
     * @param bin Citra acuan
     * @param r Daftar titik yang ingin diambil
     * @param x Posisi x mula sebagai bagian dari objek
     * @param y Posisi y mula sebagai bagian dari objek
     */
    private void rec(BinaryImageShell bin, RectAndBlackPoints r, int x, int y) {
        if (bin.isBlack(x, y)) {
            //set objek sebagai putih supaya tak dikunjungi lagi
            bin.setAsWhite(x, y);
            r.add(new Point(x, y));
            //rekursif
            if (y != 0) {
                rec(bin, r, x, y - 1);
            }
            if (y < bin.getHeight()) {
                rec(bin, r, x, y + 1);
            }
            if (x != 0) {
                rec(bin, r, x - 1, y);
            }
            if (x < image.getWidth()) {
                rec(bin, r, x + 1, y);
            }
        }
    }

    //**************************************************************************/
    //***********************    SETTER / GETTER    ****************************/
    //**************************************************************************/
    /**
     * Mengambil segment baris berdasarkan posisi mouse.
     *
     * @param mouseY Posisi mouse terhadap pojok atas gambar.
     * @return Indeks citra baris pada posisi mouse yg diklik.
     */
    public int getSegmentIndex_byMouse(int mouseY) {
        int indexBorderline = -1;
        //cek dalam batas setiap garis potong, segmen 0 sampai N-1
        for (int i = 0; i < this.borderline.size(); ++i) {
            if (mouseY < this.borderline.get(i)) {
                indexBorderline = i;
                break;
            }
        }
        //jika ternyata masih nggak ditemukan, mungkin dia di segmen plg bawah
        if (indexBorderline == -1) {
            //cek segmen paling bawah
            if (this.borderline.get(this.borderline.size() - 1) <= mouseY
                    && mouseY < this.image.getHeight()) {
                //index segmen terakhir
                indexBorderline = this.segment.size() - 1;
            }
        }
        return indexBorderline;
    }

    /**
     * Ambil segmen citra baris dengan nomor urut tertentu.
     *
     * @param number Nomor urut baris dihitung dari atas. Teratas = baris kenol.
     * @return Citra baris dengan nomor urut number
     */
    public BinaryImageShell getSegment(int number) {
        return this.segment.get(number);
    }

    /**
     * Mengembalikan semua segmen citra baris yang ada sebagai array.
     *
     * @return Array dari segmen citra baris.
     */
    public BinaryImageShell[] getSegments() {
        return this.segment.toArray(new BinaryImageShell[this.segment.size()]);
    }
}
