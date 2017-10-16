package image;

/**
 * Mengamdung konvensi pewarnaan biner dan ambang batas hitam putih piksel.
 * 
 * @author Albadr
 * @author Gazandi
 */
public class BWC {

    /** Batas hitam dan putih. Di bawah ambang berarti hitam, di atas putih. */
    public static int BW_THRESHOLD = 127;
    /** Warna putih dalam range HEX 0-255. Bernilai 255. */
    public static int WHITE_COLOR = 255;
    /** Warna abu-abu dalam range HEX 0-255. Bernilai 127. */
    public static int GRAY_COLOR = 127;
    /** Warna hitam dalam range HEX 0-255. Bernilai 0. */
    public static int BLACK_COLOR = 0;
    
    /** Nilai integer yang menyatakan background (putih). */
    public static int WHITE_INT = 0; 
    /** Nilai integer yang menyatakan titik asalnya hitam dan siap dihilangkan. */
    public static int GRAY_INT = -1;     
    /** Nilai integer yang menyatakan warna badan huruf (hitam). */
    public static int BLACK_INT = 1;    
    
    /** Nilai boolean yang menyatakan background (putih). */
    public static boolean WHITE_BOOL = false;
    /** Nilai boolean yang menyatakan warna tubuh (hitam). */
    public static boolean BLACK_BOOL = true;
}
