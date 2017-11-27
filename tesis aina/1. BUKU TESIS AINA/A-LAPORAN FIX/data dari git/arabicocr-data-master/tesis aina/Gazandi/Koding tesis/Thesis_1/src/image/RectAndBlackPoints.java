/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Sebuah kelas yang menggabungkan kotak amplop dari sebuah piksel terhubung
 * (atau bertempelan) dan daftar pikselnya sebagai ArrayList.
 *
 * @author Albadr
 * @author Gazandi
 */
public class RectAndBlackPoints implements Comparable<RectAndBlackPoints> {

    /* Area yang melingkupi points */
    Rectangle rect;
    /* Daftar piksel yang berwarna hitam */
    ArrayList<Point> apo = new ArrayList<>();
    /* Flag penanda kalau objek ini sudah diserap orang shg siap dihapus */
    boolean absorbed = false;
    /* Objek yang telah menyerap ibjek ini*/
    RectAndBlackPoints absorber;

    /**
     * Asumsi, seluruh titik pada Rectangle awal adalah hitam alias masih berupa
     * barisan piksel hitam inline
     *
     * @param originPoint
     * @param dimension
     */
    public RectAndBlackPoints(Point originPoint, Dimension dimension) {
        this.rect = new Rectangle(originPoint, dimension);

        /* Memasukkan seluruh titik pada area awal. Semua dianggap hitam. */
        for (int y = 0; y < dimension.height; y++) {
            for (int x = 0; x < dimension.width; x++) {
                apo.add(new Point(originPoint.x + x, originPoint.y + y));
            }
        }
    }

    public RectAndBlackPoints(Point originPoint, Dimension dimension, boolean initialColor) {
        this.rect = new Rectangle(originPoint, dimension);

        if (initialColor == BWC.BLACK_BOOL) {
            /* Memasukkan seluruh titik pada area awal. Semua dianggap hitam. */
            for (int y = 0; y < dimension.height; y++) {
                for (int x = 0; x < dimension.width; x++) {
                    apo.add(new Point(originPoint.x + x, originPoint.y + y));
                }
            }
        }
    }

    /**
     * Mengembalikan persegi panjang penyimpan amplop objek
     *
     * @return Persegi panjang
     */
    public Rectangle getRect() {
        return this.rect;
    }

    /**
     * Mengembalikan ukuran dari persegi panjang objek ini, yakni panjang x
     * lebar.
     *
     * @return Ukuran objek
     */
    public int getSize() {
        return (int) (this.rect.getHeight() * this.rect.getWidth());
    }

    /**
     * Mengecek apakah objek ini sudah pernah diserap objek lain atau belum.
     *
     * @return Stats pernah diserap
     */
    public boolean isAbsorbed() {
        return this.absorbed;
    }

    /**
     * Mengecek apakah objek ini bersentuhan dengan objek lain yang masih
     * berbentuk satu baris (karena baru ditemukan). Bersentuhan didefinisi- kan
     * sebagai menempelnya antara piksel hitam masing-masing rectangle.
     *
     * @param inline
     * @return Apakah kedua kotak menempel
     */
    public boolean isTouched(RectAndBlackPoints inline) {
        //titik x kiri-kanan eksklusif (satu dipinggir titik terojok) inline
        int xrInlineOut = inline.rect.x + inline.rect.width;
        int xlInlineOut = inline.rect.x - 1;
        int xrBig = rect.x + rect.width - 1;

        boolean outside = (xrBig < xlInlineOut && xrBig < xrInlineOut)
                || (xlInlineOut < this.rect.x && xrInlineOut < this.rect.x);

        boolean touched = false;
        if (!outside) {
            for (int x = xlInlineOut; x <= xrInlineOut; ++x) {
                if (this.apo.contains(new Point(x, inline.rect.y - 1))) {
                    touched = true;
                    break;
                }
            }
        }
        return touched;
    }

    /**
     * Mengecek apakah objek ini menaungi objek secondary yang dipertanyakan.
     * Menaungi berarti objek secondary ada di bawah atau di atas objek mainbody
     * ini sepanjang minimal 60% dari panjang objek secondary.
     *
     * Note: Gunakan metode ini HANYA pada saat objek sudah stabil alias sudah
     * selesai melakukan segmentasi piksel hitam terhubung.
     *
     * @param secondary
     * @return
     */
    public boolean isSecondaryWith(RectAndBlackPoints secondary) {
        //panjang secondary objek
        int secondaryWidth = (int) secondary.getRect().getWidth();
        //titik x paling kanan dari inline
        int xrSecondary = secondary.rect.x + secondary.rect.width - 1;
        //titik x paling kanan objek ini
        int xrMain = rect.x + rect.width - 1;
        //titik xr yang paling kecil, untuk menghitung perpotongan
        int xrTaung = xrMain < xrSecondary ? xrMain : xrSecondary;
        //titik xl yang paling besar, untuk menghitung perpotongan
        int xlTaung = this.rect.x > secondary.rect.x ? this.rect.x : secondary.rect.x;

        double persenDinaungi = (xrTaung - xlTaung) / (double) secondaryWidth;
        return persenDinaungi > 0.50;
    }

    /**
     * Menyerap RectAndBlackPoints lain ke dalam diri. Objek lain tersebut lalu
     * dihilangkan daftar pointnya dan ditandai sebagai terserab. Jika objek
     * lain sudah diserab oleh objek lain kedua, objek lain kedua ini yang akan
     * diserab.
     *
     * @param other
     */
    public void absorb(RectAndBlackPoints other) {
        RectAndBlackPoints absorbantRect;
        if (!other.absorbed) {
            absorbantRect = other;
        } else {
            absorbantRect = other.absorber;
        }

        //Serap semua titik
        this.apo.addAll((ArrayList<Point>) absorbantRect.apo.clone());

        //Perlebar amplop
        int xmin = this.rect.x < absorbantRect.rect.x ? this.rect.x : absorbantRect.rect.x;
        int ymin = this.rect.y < absorbantRect.rect.y ? this.rect.y : absorbantRect.rect.y;
        int xmax = (int) (this.rect.getMaxX() > absorbantRect.rect.getMaxX()
                ? this.rect.getMaxX() : absorbantRect.rect.getMaxX());
        int ymax = (int) (this.rect.getMaxY() > absorbantRect.rect.getMaxY()
                ? this.rect.getMaxY() : absorbantRect.rect.getMaxY());

        //Note: getMaxX & getMaxY adalah titik setelah titik terkanan rect.
        //Dengan demikian, width dan height berikut tidak perlu di+1 lagi.
        this.rect = new Rectangle(xmin, ymin, xmax - xmin, ymax - ymin);
        absorbantRect.absorbed = true;
        absorbantRect.absorber = this;
        //reference langsung ke inline
        other.absorber = this;
    }

    /**
     * Mengubah RectAndBlackPoints ini menjadi BinaryImageShell sehingga dapat
     * digambar. Titik-titik tersimpan dalam list digambar relatif terhadap
     * titik ujung kanan dari persegi panang yang melingkupi objek ini.
     */
    public BinaryImageShell toBinaryImage() {
        BinaryImageShell bin = new BinaryImageShell(this.rect.width, this.rect.height);
        int xo = this.rect.x;
        int yo = this.rect.y;

        for (Point p : this.apo) {
            bin.setBiner(p.x - xo, p.y - yo, BWC.BLACK_BOOL);
        }

        bin.binarize();
        return bin;
    }

    /**
     * Make a new copy of itself.
     *
     * @return New object with identical feature
     */
    @Override
    public RectAndBlackPoints clone() {
        RectAndBlackPoints newMe = new RectAndBlackPoints(
                new Point(this.rect.getLocation()), new Dimension(this.rect.getSize()), BWC.WHITE_BOOL);

        for (Point p : apo) {
            newMe.apo.add(new Point(p));
        }

        return newMe;
    }

    /**
     * Compare object in term of its horizontal position in an image
     *
     * @param other Object to compare
     * @return Positive means this object is more right to other object
     */
    @Override
    public int compareTo(RectAndBlackPoints other) {
        double compareQuantity = ((RectAndBlackPoints) other).rect.getMaxX();

        //ascending order
        //return (int) (this.rect.getMaxX() - compareQuantity);

        //descending order
        return (int) (compareQuantity - this.rect.getMaxX());
    }

    /**
     * Menambah sebuah titik ke dalam objek. Bersamaan dengan itu, persegi
     * amplop akan dibesarkan.
     *
     * @param point
     */
    public void add(Point point) {
        this.apo.add(new Point(point));
        this.rect.add(new Rectangle(point.x, point.y, 1, 1));
    }

    /**
     * Mengubah posisi persegi dan seluruh titik dalamnya ke dalam titik
     * referensi baru. Posisi pojok atas kiri persegi akan diset menjadi titik
     * referensi baru dan titik apo akan dipindah terhadap titik pojok yang baru
     * tersebut.
     *
     * @param x_newReference Posisi x titik acuan yang baru
     * @param y_newReference Posisi y titik acuan yang baru
     * @todo Pentranslasian relatif 0,0 rect, masih error di beberapa objek
     * kayaknya
     */
    public void translate(int x_newReference, int y_newReference) {
        //menghitung pergeseran titik pojok kiri atas persegi
        int dx = x_newReference - this.rect.x;
        int dy = y_newReference - this.rect.y;
        //memindahkan persegi
        this.rect.x = x_newReference;
        this.rect.y = y_newReference;
        //memindahkan titik apo
        for (Point p : apo) {
            p.x = p.x + dx;
            p.y = p.y + dy;
        }
    }
}
