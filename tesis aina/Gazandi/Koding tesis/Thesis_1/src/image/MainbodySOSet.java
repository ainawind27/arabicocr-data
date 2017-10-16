/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image;

import java.util.ArrayList;

/**
 *
 * @author Albadr
 * @author Gazandi
 */
public class MainbodySOSet {

    //Block holder
    RectAndBlackPoints mainbody;
    RectAndBlackPoints mainbody_withSecondary;
    ArrayList<RectAndBlackPoints> secondaries;
    //Image holder
    BinaryImageShell mainbodyImage;
    BinaryImageShell mainbody_withSecondaryImage;
    ArrayList<BinaryImageShell> secondaryImage;
    //Baseline of this subword, extracted from line image
    int baseline = 0;
    int upperY_ofMainBody_relativeToLine = 0;

    public MainbodySOSet(RectAndBlackPoints mainbody) {
        this.mainbody = mainbody;
        this.mainbody_withSecondary = mainbody.clone();
        this.secondaries = new ArrayList<>();
    }

    public MainbodySOSet(RectAndBlackPoints mainbody, ArrayList<RectAndBlackPoints> secondaries) {
        this.mainbody = mainbody;
        this.secondaries.addAll(secondaries);
        for (RectAndBlackPoints secondary : secondaries) {
            this.mainbody_withSecondary.absorb(secondary);
        }
    }

    public void addSecondaryObject(RectAndBlackPoints secondary) {
        this.secondaries.add(secondary);
        this.mainbody_withSecondary.absorb(secondary);
    }

    public ArrayList<RectAndBlackPoints> getSecondaryObject() {
        return secondaries;
    }

    public RectAndBlackPoints getSecondaryObject(int indexOnArray) {
        return secondaries.get(indexOnArray);
    }

    /**
     * Mencari objek sekunder yang berada dalam area x yang diberikan.
     * Diasumsikan titik acuan keduanya sudah sama saat method ini dipanggil.
     *
     * @param xleft
     * @param xright
     * @return
     */
    public ArrayList<RectAndBlackPoints> getSecondaryObjectAtArea(int xleft, int xright) {
        ArrayList<RectAndBlackPoints> candidateSec = new ArrayList<>();

        for (RectAndBlackPoints secondary : secondaries) {
            //panjang secondary objek
            int secondaryWidth = secondary.rect.width;
            //titik x paling kanan dari objek sekunder
            int xrSecondary = secondary.rect.x + secondary.rect.width - 1;
            //titik xr yang paling kecil, untuk menghitung perpotongan
            int xrTaung = xright < xrSecondary ? xright : xrSecondary;
            //titik xl yang paling besar, untuk menghitung perpotongan
            int xlTaung = xleft > secondary.rect.x ? xleft : secondary.rect.x;

            double persenDinaungi = (xrTaung - xlTaung) / (double) secondaryWidth;

            //jika objek sekunder ini masuk ke dalam area, minimal 50%
            //masukkan ke dftar kandidat
            if (persenDinaungi >= 0.50) {
                candidateSec.add(secondary);
            }
        }

        return candidateSec;
    }

    /**
     * Mengeset nilai baseline dari objek subword (manbody-secondaryobject) ini.
     * Baseline objek ini sama dengan baseline citra baris (lineBaseline). Akan
     * tetapi, karena tepi atas citra upakata tidak mesti sama dengan tepi atas
     * citra baris, diperlukan pengetahuan posisi atas objek badan utama ini
     * untuk mengeset baseline relatif terhadap amplop citra badan utama.
     *
     * Perlu dicatat bahwa nilai baseline ini akan diambil relatif terhadap
     * citra badan utama SAJA, bukan citra badan utama plus objek sekunder.
     *
     * @param lineBaseline Posisi baseline dari line
     * @param upperY_ofMainBody_relativeToLine Posisi y mainbody objek ini
     * ditemukan
     */
    public void setBaseline(int lineBaseline) {
        this.upperY_ofMainBody_relativeToLine = mainbody.getRect().y;
        this.baseline = lineBaseline - upperY_ofMainBody_relativeToLine;
    }

    public RectAndBlackPoints getBlock_bodyOnly() {
        return this.mainbody;
    }

    public RectAndBlackPoints getBlock_withSecondary() {
        return this.mainbody_withSecondary;
    }

    public BinaryImageShell getImage_bodyOnly() {
        return mainbody.toBinaryImage();
    }

    public BinaryImageShell getImage_withSecondary() {
        return mainbody_withSecondary.toBinaryImage();
    }

    public BinaryImageShell getImage() {
        return this.getImage_withSecondary();
    }

    public int getBaseline() {
        return this.baseline;
    }
}
