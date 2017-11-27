/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.segmentator;

import image.BinaryImageShell;
import image.MainbodySOSet;
import image.RectAndBlackPoints;
import image.classifier.FeatureExtraction;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Memotong huruf dengan algoritma (Zidouri, 2010).
 *
 * @todo Memastikan bahwa komponen sekunder tidak terpotong dengan menyesuaikan
 * besar badan utama.
 *
 * @author Albadr
 * @author Gazandi
 */
public class SegmentatorChar {
    /* Panjang huruf terkecil yang ada*/
    int Ls = 6;
    /* Panjang huruf terkecil yang ada jika bersentuhan*/
    int Lsa = 12;
    /* Panjang huruf paling besa */
    int Lm = 30;
    /* Titik baseline dari citra upakata, dihutung pada konstruktor*/
    int BaselineLoc;
    /* Blok upakata yang menjadi masukan */
    MainbodySOSet subwordBlock;
    /* Citra kata yang sudah ditipiskan yg akan ditangani algoritma Zidouri*/
    BinaryImageShell thinnedWord;
    /* Citra kata asli yang akan dipotong sesuai titik potong yg didapat nantinya*/
    BinaryImageShell originalWord;
    /* Citra kata asli yang akan dipotong sesuai titik potong yg didapat nantinya*/
    BinaryImageShell originalWord_withSecondary;
    /* Daftar citra huruf tertipiskan yang telah dipotong */
    BinaryImageShell[] thinCars;
    /* Daftar citra huruf asli yang telah dipotong */
    BinaryImageShell[] plainChars;
    /* Daftar citra huruf asli dengan komponen sekundernya yang telah dipotong */
    BinaryImageShell[] fullChars;
    /* Daftar secondary objek yang sudah dimasukkan ke index huruf tertentu*/
    ArrayList<ArrayList<RectAndBlackPoints>> secondaryOnChars;

    // ****************************** KONSTRUKTOR ******************************
    /**
     * Menerima citra upakata yang hanya berisi badan utama (main body) saja.
     * Kelas kemudian memotong citra upakata tersebut dengan algoritma Zidouri.
     *
     * @param wordline Citra badan utama yang ingin dipotong.
     */
    public SegmentatorChar(MainbodySOSet wordline) {
        this.subwordBlock = wordline;
        BinaryImageShell wordImage = subwordBlock.getImage_bodyOnly();

        /* Menghitung garis dasar (baseline) dari citra upakata*/
        BaselineLoc = wordline.getBaseline();

        Hilditch hilditch = new Hilditch(wordImage);
        this.thinnedWord = hilditch.getSkeleton();
        this.originalWord = hilditch.getOriginal();
        this.originalWord_withSecondary = wordline.getImage_withSecondary();
        this.originalWord.drawLine(BaselineLoc, true, Color.LIGHT_GRAY);

//        //Mengecek wordline jumlah objek sekunder beserta posisi x nya.
//        System.out.println("Block Only Rect: " + wordline.getBlock_bodyOnly().getRect());
//        System.out.println("Block Wh/S Rect: " + wordline.getBlock_withSecondary().getRect());
//        System.out.println("N Secondaries: " + wordline.getSecondaryObject().size());
//        for (RectAndBlackPoints s : wordline.getSecondaryObject()) {
//            System.out.println(s.getRect());
//        }
    }

    // ***************************** METHOD UTAMA ******************************
    /**
     * Memotong citra sub word dengan algoritma pemotongan general yang
     * dijelaskan oleh (Zidouri, 2010).
     */
    public void zidouri() {
        final int width = thinnedWord.getWidth();
        final int height = thinnedWord.getHeight();
        double ar = (double) width / height;

        //Penyimpan pita-pita yang ditemukan
        ArrayList<Pita> bandCandidate = new ArrayList<>();
        //Pita-pita yang terpilih (sesuai rule Zioduri)
        ArrayList<Pita> choosenBand = new ArrayList<>();

        //Pengecekan satu huruf atau bukan 
        //@todo lebih bagusin lagi
        if (ar < 1 && width < this.Lm && height < 32) {
            //kalau kira-kira ini sudah satu huruf, jangan disegmen
            //System.out.println("Not segmented because w=" + width + ", h" + height);
        } else {
            //kalau kira-kira citra ini LEBIH DAR SATU HURUF
            /* CARI KANDIDAT PITA */
            bandCandidate = this.zidouriFindBandCandidate();

            /* SORT PITA, awal array pita paling kiri, yang diakhir paling kanan */
            Collections.sort(bandCandidate);

            /* EKSTRAK FITUR ZIDOURI */
            this.zidouriExtractBandFeature(bandCandidate);

            /* Cari indeks pita terlebar */
            int widestBandIndex = this.findWidestBandIndex(bandCandidate);

            /* RULE ZIDOURI */
            choosenBand = this.zidouriRuleSet(bandCandidate, widestBandIndex);

            //jika ternyata belum nemu choosenBand, hapus pita terbesar jika dia
            //dibawah baseline, kemudian laksanakan lagi ruleset Zidouri tadi.
            //Jika tidak ada hal seperti itu, langsung laksanakan rule Zidouri
            //yang lebih longgar
            if (choosenBand.isEmpty() && widestBandIndex >= 0) {
                //Cek apakah pita max ada di bawah baseline
                Pita maxPita = bandCandidate.get(widestBandIndex);
                if (maxPita.F4() != 1) {
                    bandCandidate.remove(widestBandIndex);
                    widestBandIndex = this.findWidestBandIndex(bandCandidate);
                    choosenBand = this.zidouriRuleSet(bandCandidate, widestBandIndex);
                }

                //Cek lagi, jika masih tiada pita terpilih, longgarkan rule
                //@todo aturan yg lebih longgar
            }

            //Geser tengah pita ke kiri JIKA terdapat objek selain linibasis yg ditabrak
            for (Pita p : choosenBand) {
                int line = p.F5();
                int yobj;

                do {
                    yobj = 0;
                    for (int y = 1; y < originalWord_withSecondary.getHeight(); ++y) {
                        if (originalWord_withSecondary.isBlack(line, y) && originalWord_withSecondary.isWhite(line, y - 1)) {
                            yobj++;
                        }
                    }
                    //jika lebih dari satu (ada selain lini basis)
                    if (yobj > 1) {
                        //geser ke kiri
                        line--;
                    }
                } while (yobj > 1 && line >= p.xLeft());
                p.f5 = line;
            }
        }

        //**** Memotong gambar menjadi beberapa bagian sesuai titik potong pita ****
        // Jika tidak ada pita, buat image dari upakata asal
        if (!choosenBand.isEmpty()) {
            //Pengecekan terakhir setiap kandidat segment, 
            //untuk mengurangi oversegmentasi
            this.examineSegments(choosenBand);

            // Jika ada potong berdasarkan pita potong yang sudah terpilih
            // Mengeset jumlah segment karakter. 2 pita batas = 3 huruf
            this.thinCars = new BinaryImageShell[choosenBand.size() + 1];
            this.plainChars = new BinaryImageShell[choosenBand.size() + 1];
            this.fullChars = new BinaryImageShell[choosenBand.size() + 1];
            //mengeset penyimpan secondary object
            this.secondaryOnChars = new ArrayList<>(choosenBand.size() + 1);

            // Menggambar kotak pita pada citra untuk debugging. Hapus jika tidak perlu.
            //System.out.print("Pita potong:");
            for (Pita pita : choosenBand) {
                thinnedWord.drawLine(pita.F5(), false, Color.RED);
                originalWord.drawLine(pita.F5(), false, Color.RED);
                originalWord_withSecondary.drawLine(pita.F5(), false, Color.RED);
                //System.out.print(pita.F5() + ", ");
            }
            //System.out.println("");
            originalWord.repaint();
            thinnedWord.repaint();
            originalWord_withSecondary.repaint();

            //titik potong terakhir
            int lastRightPoint = width;

            //Memotong upakata berdasarkan pita, 
            //potongan paling kanan ditaruh di awal array
            for (int p = choosenBand.size(); p >= 0; --p) {
                int charIndex = choosenBand.size() - p;
                int currentLeftPoint;

                if (p > 0) {
                    Pita pita = choosenBand.get(p - 1);
                    currentLeftPoint = pita.F5();
                } else {
                    currentLeftPoint = 0;
                }

                //crop image
                int rectWidth = lastRightPoint - currentLeftPoint;
                this.thinCars[charIndex] = thinnedWord.crop(new Rectangle(currentLeftPoint, 0, rectWidth, height));
                this.plainChars[charIndex] = originalWord.crop(new Rectangle(currentLeftPoint, 0, rectWidth, height));
                this.fullChars[charIndex] = originalWord_withSecondary.crop(new Rectangle(currentLeftPoint, 0, rectWidth, originalWord_withSecondary.getHeight()));
                //set secondary object
                int x1 = subwordBlock.getBlock_bodyOnly().getRect().x + currentLeftPoint;
                int x2 = subwordBlock.getBlock_bodyOnly().getRect().x + currentLeftPoint + rectWidth;
                this.secondaryOnChars.add(charIndex, subwordBlock.getSecondaryObjectAtArea(x1, x2));
                //System.out.println("p=" + p + ", c=" + charIndex + ", lleft=" + currentLeftPoint + ", cright=" + lastRightPoint + ". x1=" + x1 + " x2=" + x2 + ", NS=" + secondaryOnChars.get(0).size());

                //next iteration
                lastRightPoint = currentLeftPoint;
            }
        } else {
            // Jika ada potong berdasarkan pita potong yang sudah terpilih
            // Mengeset jumlah segment karakter. 2 pita batas = 3 huruf
            this.thinCars = new BinaryImageShell[1];
            this.plainChars = new BinaryImageShell[1];
            this.fullChars = new BinaryImageShell[1];
            //mengeset penyimpan secondary object
            this.secondaryOnChars = new ArrayList<>(1);
            // Jika tidak ada pita yang didapat berarti citra itulah huruf
            this.thinCars[0] = thinnedWord.clone();
            this.plainChars[0] = originalWord.clone();
            this.fullChars[0] = originalWord_withSecondary.clone();
            //set secondary object
            int x1 = subwordBlock.getBlock_bodyOnly().getRect().x;
            int x2 = subwordBlock.getBlock_bodyOnly().getRect().x + width;
            this.secondaryOnChars.add(0, subwordBlock.getSecondaryObjectAtArea(x1, x2));
        }
    }

    /**
     * Mengekstrak fitur dari masing2 kandidat pita, dari kiri ke kanan
     *
     * @param sortedBandCandidate
     */
    private void zidouriExtractBandFeature(ArrayList<Pita> sortedBandCandidate) {
        int rightmostIndex = sortedBandCandidate.size() - 1;
        for (int p = 0; p <= rightmostIndex; p++) {
            Pita pita = sortedBandCandidate.get(p);
            //Set F1, lebar pita
            pita.setF1(pita.width());
            //Set F2, jarak pita ke pita sblh kanannya, 0 jika paling kanan
            if (p == rightmostIndex) {
                pita.setF2(0);
            } else {
                pita.setF2(sortedBandCandidate.get(p + 1).F5() - pita.F5());
            }
            //Set F3, jarak pita ke pita kedua sblh kanannya, 0 jika paling kanan
            if (p >= rightmostIndex - 1) {
                pita.setF3(0);
            } else {
                pita.setF3(sortedBandCandidate.get(p + 2).F5() - pita.F5());
            }
            //Set F4, 1 jika ditemukan di atas baseline, 0 jika di bawah
            if (pita.bandFoundInY <= BaselineLoc || pita.bandFoundInY - BaselineLoc < 3) {
                pita.setF4(1);
            } else {
                pita.setF4(0);
            }

            thinnedWord.drawLine(pita.F5(), false, Color.YELLOW);
            //System.out.println("Pita "+p+", F1="+pita.F1()+", F2="+pita.F2()+", F3="+pita.F3()+", F4="+pita.F4()+", F5="+pita.F5());
        }
    }

    /**
     * Memasukkan masing-masing pita kandidat yang sudah terurut kanan ke kiri,
     * ke set aturan Zidouri.
     *
     * @param sortedBandCandidate
     * @param widestBandIndex
     * @return
     */
    private ArrayList<Pita> zidouriRuleSet(ArrayList<Pita> sortedBandCandidate, int widestBandIndex) {
        //Variabel pembantu
        int rightmostIndex = sortedBandCandidate.size() - 1;

        //Pita-pita yang terpilih (sesuai rule Zioduri)
        ArrayList<Pita> choosenBand = new ArrayList<>();

        //Select the band, note that leftmost band is in the first array
        for (int p = 0; p <= rightmostIndex; ++p) {
            Pita pita = sortedBandCandidate.get(p);
            boolean hasBeenSelected = false;

            //Rule 1, highest relative width AND F4==1
            if (p == widestBandIndex && pita.F4() == 1) {
                choosenBand.add(pita);
                hasBeenSelected = true;
            }

            //Rule 2, Choose guide band if F2 > Ls and F4 == 1
            if (!hasBeenSelected && pita.F2() > this.Ls && pita.F4() == 1) {
                choosenBand.add(pita);
                hasBeenSelected = true;
            }

            //Rule 3, Choose guide band if F2 <= Ls and F3 > Ls' and
            //guide band is not the last one.
            if (!hasBeenSelected && pita.F2() <= this.Ls && pita.F3() > this.Lsa && p != 0) {
                choosenBand.add(pita);
                hasBeenSelected = true;
            }

            //Rule 4, Choose guide band if F1 >= Lm and F4 = 1
            if (!hasBeenSelected && pita.F1() >= this.Lm && pita.F4() == 1) {
                choosenBand.add(pita);
                hasBeenSelected = true;
            }

            //Additional rule, For the 1st guide band in the sets [rightmost band], 
            //even if it fails to qualify Rule 1-4 and the guide band next to it satisfies Rule 2
            //then it should be selected.
            if (!hasBeenSelected && p == rightmostIndex && p >= 1) {
                Pita pitaSecondRightmost = sortedBandCandidate.get(p - 1);
                //rule 2
                if (pitaSecondRightmost.F2() > this.Ls && pitaSecondRightmost.F4() == 1) {
                    choosenBand.add(pita);
                }
            }

        }

        return choosenBand;
    }

    /**
     * Mencari pita kandidat untuk algoritma Zidouri.
     *
     * @return Daftar pita kandidat.
     */
    private ArrayList<Pita> zidouriFindBandCandidate() {
        final int width = thinnedWord.getWidth();
        final int height = thinnedWord.getHeight();
        //Penyimpan pita-pita yang ditemukan
        ArrayList<Pita> bandCandidate = new ArrayList<>();

        //Mencari kandidat pita potong
        for (int h = 0; h < height; h++) {
            int hp = 0;
            boolean foundBlack, beforeBlack = false;
            Point rightSideOfTheBand = new Point(9999, 9999);

            for (int w = width - 1; w >= 0; w--) {
                foundBlack = w == width ? false : thinnedWord.isBlack(w, h);
                // Perpindahan dari putih ke hitam
                if (!beforeBlack && foundBlack) {
                    rightSideOfTheBand = new Point(w, h);
                }
                // Perpindahan dari hitam ke putih
                if (beforeBlack && !foundBlack) {
                    Point leftSideOfTheBand = new Point(w, h);

                    int bandWidth = rightSideOfTheBand.x - leftSideOfTheBand.x;
                    // Jika panjang rentetan piksel lebih besar dari Ls
                    if (bandWidth >= Ls) {
                        //thinnedWord.drawLine(leftSideOfTheBand.x, false, Color.CYAN);
                        //thinnedWord.drawLine(rightSideOfTheBand.x, false, Color.pink);

                        // jumlah piksel pada setiap axis dari pita rentetan piksel tadi
                        int[] npix = zidouriCountPixel(thinnedWord, leftSideOfTheBand.x + 1, rightSideOfTheBand.x);
                        int countLoneDot = 0, npLeftLoc = 0, npRightLoc = 0;
                        boolean foundLoneDot = false;
                        boolean isBandCandidate = false;
                        // iterasi dari kiri ke kanan rentetan piksel untuk 
                        // mengecek jumlah piksel pada setiap axis (dan atas-bawahnya)
                        for (int np = 0; np < npix.length; np++) {
                            if (foundLoneDot && npix[np] == 1) {
                                //Saat lonedot
                                countLoneDot++;

                                if (np == npix.length - 1) {
                                    isBandCandidate = countLoneDot >= this.Ls ? true : false;
                                    npRightLoc = np;
                                }
                            } else if (!foundLoneDot && npix[np] == 1) {
                                //Ujung kiri lonedot
                                npLeftLoc = np;
                                foundLoneDot = true;
                                countLoneDot = 1;
                            } else if (foundLoneDot && npix[np] > 1) {
                                //Ujung kanan lonedot
                                isBandCandidate = countLoneDot >= this.Ls ? true : false;
                                if (isBandCandidate) {
                                    npRightLoc = np;
                                    break;
                                } else {
                                    foundLoneDot = false;
                                    countLoneDot = 0;
                                }
                            }
                        }

                        // jika memenuhi syarat kandidat, masukkan ke arraylist
                        if (isBandCandidate) {
                            bandCandidate.add(new Pita(leftSideOfTheBand.x + npLeftLoc + 1, leftSideOfTheBand.x + npRightLoc + 1, h));
                        }
                    }

                    //Reset sisi kanan menjadi tak hingga
                    rightSideOfTheBand = new Point(9999, 9999);
                }
                beforeBlack = foundBlack;
            }
        }

        return bandCandidate;
    }

    /**
     * Component algoritma zidouri untuk menghitung jumlah piksel hitam dalam
     * suatu batas leftX dan rightX. Setiap axis X dihitung berapa jumlah piksel
     * hitamnya.
     *
     * @param bin
     * @param leftX
     * @param rightX
     * @return Jumlah piksel hitam pada setiap titik dari leftX ke rightX
     */
    private int[] zidouriCountPixel(BinaryImageShell bin, int leftX, int rightX) {
        int[] npix = new int[rightX - leftX + 1];

        for (int y = 0; y < bin.getHeight(); y++) {
            for (int x = leftX; x <= rightX; x++) {
                if (bin.isBlack(x, y)) {
                    npix[x - leftX] = npix[x - leftX] + 1;
                }
            }
        }
        return npix;
    }

    /**
     * Mencari pita dengan lebar terbanyak.
     *
     * @param bands Koleksi pita yang ingin dicari paling lebarnya.
     * @return
     */
    private int findWidestBandIndex(ArrayList<Pita> bands) {
        int highestRelativeWidthIndex = -1;
        int highestRelativeWidth = -1;

        //Mencari pita terlebar
        for (int p = 0; p < bands.size(); ++p) {
            Pita pita = bands.get(p);
            //Cek width terhadap width yg paling besar sekarang
            if (pita.F1() > highestRelativeWidth) {
                highestRelativeWidth = pita.F1();
                highestRelativeWidthIndex = p;
            }
        }
        return highestRelativeWidthIndex;
    }

    /**
     * Mengecek daftar pita yang sudah dibangkitkan dan segmen kanan pita yang
     * akan dihasilkan dengan pita tersebut. Jika ada yang jelek dengan kriteria
     * sbb: 1. Banyak piksel kurang dari panjang segmen hasil, 2. tinggi amplop
     * segmen kurang daru 10 dan lebar kurang dari Lsa, maka pita tersebut
     * dihapus.
     *
     * @param chosenBand
     */
    private void examineSegments(ArrayList<Pita> chosenBand) {
        ArrayList<Pita> removedBand = new ArrayList<>();

        int lastLeftPoint = 0;
        //Cek setiap citra segmen sebelah kiri pita
        for (Pita pita : chosenBand) {
            //segmen gambar bagian kiri pita
            BinaryImageShell cs = thinnedWord.crop(new Rectangle(lastLeftPoint, 0, pita.F5() - lastLeftPoint, thinnedWord.getHeight()));
            cs.cropEnvelope();
            boolean removed = false;
            //CEK 1: banyaknya piksel harus lebih dari panjang segmen
            //ini untuk mengurangi segmen yang cuma garis doang, kayak ujung huruf dal
            int npiksel = FeatureExtraction.cumulativeVerticalProjection(cs)[cs.getWidth() - 1];
            if (npiksel <= cs.getWidth()) {
                //hapus pita pemotongan ini
                removedBand.add(pita);
                removed = true;
            }

            //CEK 2: tinggi dari amplop skeleton citra
            if (!removed && cs.getHeight() < 10 && cs.getWidth() < this.Lsa) {
                //hapus pita pemotongan ini
                removedBand.add(pita);
                removed = true;
            }
            //System.out.println(cs.getWidth()+":"+npiksel);
            //titik ujung kiri sebelumnya
            lastLeftPoint = pita.F5();
        }

        chosenBand.removeAll(removedBand);
    }

    // ******************************   GETTER   ******************************
    /**
     * Mengembalikan citra kata/subkata polos yang dimasukkan ke objek ini.
     *
     * @return Citra badan utama dari kata/subkata
     */
    public BinaryImageShell getInputImage_plain() {
        return originalWord;
    }

    /**
     * Mengembalikan citra kata/subkata bersecondary object yang dimasukkan ke
     * objek ini.
     *
     * @return Citra kata/subkata bersecondary oject
     */
    public BinaryImageShell getInputImage_withSecondary() {
        return originalWord_withSecondary;
    }

    /**
     * Mengembalikan citra kata/subkata tertipiskan yang dimasukkan ke objek
     * ini.
     *
     * @return Citra skeleton dari kata/subkata
     */
    public BinaryImageShell getInputImage_thin() {
        return thinnedWord;
    }

    /**
     * Megembalikan jumlah segment hasil pemotongan kelas ini.
     *
     * @return Jumlah segment
     */
    public int getSegmentSize() {
        return this.plainChars.length;
    }

    /**
     * Mengembalikan citra segment huruf dalam bentuk telah ditipiskan.
     *
     * @param index Posisi segment dalam array.
     * @return Citra segmen huruf tipis
     */
    public BinaryImageShell getChar_thin(int index) {
        return this.thinCars[index];
    }

    /**
     * Mengembalikan citra segment huruf dalam bentuk asli TANPA secondary
     * object.
     *
     * @param index Posisi segment dalam array.
     * @return Citra segmen huruf biasa
     */
    public BinaryImageShell getChar_plain(int index) {
        return this.plainChars[index];
    }

    /**
     * Mengembalikan citra segment huruf dalam bentuk asli DENGAN secondary
     * object.
     *
     * @param index Posisi segment dalam array.
     * @return Citra segmen huruf biasa DENGAN secondary object.
     */
    public BinaryImageShell getChar_withSecondary(int index) {
        return this.fullChars[index];
    }

    /**
     * Mengembalikan blok yang menyimpan objek sekunder pada huruf dengan nomor
     * urut tertentu.
     *
     * @param letteriIndex Posisi segmen huruf dalam array
     * @return Daftar blok objek sekunder yang terkait huruf tersebut.
     */
    public ArrayList<RectAndBlackPoints> getSecondaries(int letteriIndex) {
        return this.secondaryOnChars.get(letteriIndex);
    }

    // ****************************** CLASS PITA ******************************
    /**
     * Kelas yang menangani pita yang menjadi titik potong antar huruf.
     */
    class Pita implements Comparable<Pita> {

        private int xl, xr;
        private int bandFoundInY;
        private int f1, f2, f3, f4, f5;

        /**
         * Membuat kandidat pita potong zidouri yang baru.
         *
         * @param xl Axis paling kiri pita
         * @param xr Axis paling kanan pita
         * @param y Lokasi ditemukannya garis dasar pita
         */
        public Pita(int xl, int xr, int y) {
            this.xl = xl;
            this.xr = xr;
            this.bandFoundInY = y;
            //titik tengah pita
            this.f5 = (xl + xr) / 2;
        }

        public int xLeft() {
            return this.xl;
        }

        public int xRight() {
            return this.xr;
        }

        public int width() {
            return xr - xl + 1;
        }

        public int ypos() {
            return bandFoundInY;
        }

        public void setF1(int rawF1) {
            this.f1 = rawF1;
        }

        public void setF2(int rawF2) {
            this.f2 = rawF2;
        }

        public void setF3(int rawF3) {
            this.f3 = rawF3;
        }

        public void setF4(int rawF4) {
            this.f4 = rawF4;
        }

        /**
         * Lebar pita.
         *
         * @return Nilai sama dengan width.
         */
        public int F1() {
            return this.f1;
        }

        /**
         * Jarak pita ke pita sblh kanannya.
         *
         * @return Jarak dari sisi kanan pita ini ke sisi kiri pita sebelah
         * kanannya. Nilai 0 jika pita ini pita paling kanan.
         */
        public int F2() {
            return this.f2;
        }

        /**
         * Jarak pita ke pita kedua sblh kanannya.
         *
         * @return Jarak dari sisi kanan pita ini ke sisi kiri dua pita sebelah
         * kanannya. Nilai 0 jika pita ini pita paling kanan atau pita paling
         * kanan kedua.
         */
        public int F3() {
            return this.f3;
        }

        /**
         * Posisi ditemukannya garis dasar pita ini.
         *
         * @return 1 jika di atas baseline, 0 jika di bawah
         */
        public int F4() {
            return this.f4;
        }

        /**
         * Titik tengah dari pita.
         *
         * @return nilai (xr - xl)/2
         */
        public int F5() {
            return this.f5;
        }

        /**
         * Compare object in term of its horizontal position in an image.
         *
         * @param other Object to compare
         * @return Positive means this object is more left to other object
         */
        @Override
        public int compareTo(Pita other) {
            int compareQuantity = ((Pita) other).xr;

            //descending order
            return this.xr - compareQuantity;
        }
    }
}
