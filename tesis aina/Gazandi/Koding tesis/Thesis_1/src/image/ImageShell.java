package image;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

/**
 * This class hold as a shell of image (buffered image) loaded from a file.
 * Notes: RGB is written as AARRGGBB and white value is 255
 *
 * @author Albadr
 * @author Gazandi
 */
public class ImageShell extends Component {

    BufferedImage image;
    int width_x_height;

    public ImageShell(BufferedImage image) {
        this.image = image;
        width_x_height = image.getHeight() * image.getWidth();
    }

    /**
     * Membuat citra berdasarkan file.
     *
     * @param file File citra
     */
    public ImageShell(String fileName) {
        try {
            image = ImageIO.read(new File(fileName));
            width_x_height = image.getHeight() * image.getWidth();
        } catch (IOException e) {
            Logger.getLogger(ImageShell.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Membuat citra berdasarkan path ke file.
     *
     * @param fileName Alamat file diletakkan di komputer, relatif ke program
     */
    public ImageShell(File file) {
        try {
            image = ImageIO.read(file);
            width_x_height = image.getHeight() * image.getWidth();
        } catch (IOException e) {
            Logger.getLogger(ImageShell.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Membuat sebuah citra kosong berwarna putih dengan ukuran width x height.
     *
     * @param width Lebar citra
     * @param height Tinggi citra
     */
    public ImageShell(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        width_x_height = width * height;
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(image, 0, 0, null);
    }

    /**
     * Mengambil variabel citra dari cangkang citra ini. Biasanya untuk save.
     *
     * @return citra yang disimpan
     */
    public BufferedImage getBufferedImage() {
        return image;
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) {
            return new Dimension(100, 100);
        } else {
            return new Dimension(image.getWidth(null), image.getHeight(null));
        }
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    public int getWxH() {
        return width_x_height;
    }

    /**
     * Mengambil nilai Red, Green, dan Blue masing-masing dari BufferedImage.
     *
     * @param x posisi koordinat x (horizontal) dari gambar
     * @param y posisi koordinat y (vertikal) dari gambar
     * @return array integer {red,green,blue} dari gambar dengan nilai
     * masing-masing 0-255
     */
    public int[] getRGB(int x, int y) {
        int rawRGB = this.image.getRGB(x, y) - (0xff << 24);
        int red = rawRGB >> 16;
        int green = (rawRGB >> 8) % 256;
        int blue = rawRGB % 256;

        return new int[]{red, green, blue};
    }

    /**
     * Mengambil nilai Red, Green, dan Blue masing-masing dari BufferedImage.
     *
     * @param x posisi koordinat x (horizontal) dari gambar
     * @param y posisi koordinat y (vertikal) dari gambar
     * @return array integer {alpha,red,green,blue} dari gambar dengan nilai
     * masing-masing 0-255
     */
    public int[] getARGB(int x, int y) {
        int rawRGB = this.image.getRGB(x, y);
        int alpha = rawRGB >>> 24;
        int red = (rawRGB >>> 16) % 256;
        int green = (rawRGB >>> 8) % 256;
        int blue = (rawRGB - (0xff << 24)) % 256;

        return new int[]{alpha, red, green, blue};
    }

    /**
     * Mengeset nilai Red, Green, dan Blue masing-masing ke BufferedImage.
     *
     * @param x posisi koordinat x (horizontal) dari gambar
     * @param y posisi koordinat y (vertikal) dari gambar
     * @param red Nilai red dari gambar dengan nilai 0-255
     * @param green Nilai green dari gambar dengan nilai 0-255
     * @param blue Nilai blue dari gambar dengan nilai 0-255
     */
    public void setRGB(int x, int y, int red, int green, int blue) {
        int newRGB = blue + (green << 8) + (red << 16) + (0xff << 24);
        this.image.setRGB(x, y, newRGB);
    }

    /**
     * Mengubah citra ini menjadi grayscale.
     */
    public void convertToGrayscale() {
        BufferedImageOp op = new ColorConvertOp(
                ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        image = op.filter(image, null);
    }

    /**
     * Menggambar persegi panjang rect pada citra ini dengan warna acak.
     *
     * @param rect Persegi yang digambar. Set posisi dan ukuran persegi disini.
     */
    public void drawRectangle(Rectangle rect) {
        Random rand = new Random(Calendar.getInstance().getTimeInMillis() * 17);
        this.drawRectangle(rect, Color.getHSBColor(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()));
    }

    /**
     * Menggambar persegi panjang rect pada citra ini.
     *
     * @param rect Persegi yang digambar. Set posisi dan ukuran persegi disini.
     * @param color Warna persegi
     */
    public void drawRectangle(Rectangle rect, Color color) {
        Graphics2D g = image.createGraphics();
        Random rand = new Random(Calendar.getInstance().getTimeInMillis());

        g.setColor(color);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * Menggambar garis lurus pada citra ini. Warna acak.
     *
     * @param position Posisi garis tegak/mendatar yang akan digambar.
     * @param horizontal True jika garis mendatar, false jika tegak.
     */
    public void drawLine(int position, boolean horizontal) {
        Random rand = new Random(Calendar.getInstance().getTimeInMillis());
        Color color = Color.getHSBColor(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        this.drawLine(position, horizontal, color);
    }

    /**
     * Menggambar garis lurus pada citra ini.
     *
     * @param position Posisi garis tegak/mendatar yang akan digambar.
     * @param horizontal True jika garis mendatar, false jika tegak.
     * @param color Warna garis
     */
    public void drawLine(int position, boolean horizontal, Color color) {
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        if (horizontal) {
            g.drawLine(0, position, this.getWidth(), position);
        } else {
            g.drawLine(position, 0, position, this.getHeight());
        }
    }

    /**
     * Menggambar garis di antara dua titik pada citra dengan warna tertentu.
     *
     * @param start Titik awal garis
     * @param end Titik akhir garis
     * @param color Warna garis yang ingi digambar
     */
    public void drawLine(Point start, Point end, Color color) {
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.drawLine(start.x, start.y, end.x, end.y);
    }

    /**
     * Menggambar garis di antara dua titik pada citra dengan warna acak.
     *
     * @param start Titik awal garis
     * @param end Titik akhir garis
     */
    public void drawLine(Point start, Point end) {
        Random rand = new Random(Calendar.getInstance().getTimeInMillis());
        Color color = Color.getHSBColor(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        this.drawLine(start, end, color);
    }

    /**
     * Convert this shell of image into its binary counterpart.
     *
     * @return
     */
    public BinaryImageShell toBinaryShell() {
        BinaryImageShell bin = new BinaryImageShell(image);
        return bin;
    }

    // *************************************************************************************************
    /**
     * Do a median filter to this image. Te median of each pixel from its value
     * and its neighbor value is counted. The pixel value is then replaced with
     * this median value. It is done for all 3 color channel.
     */
    public void medianFilter() {
        int[][] offset = new int[][]{{0, 0}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};

        /*Get pixels from image*/
        int filterWidth = this.getWidth() - 1;
        int filterHeight = this.getHeight() - 1;
        int[] srgb = new int[this.width_x_height];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), srgb, 0, image.getWidth());

        /*Pixel Mapping*/
        int[] redLayer = new int[this.width_x_height];
        int[] greenLayer = new int[this.width_x_height];
        int[] blueLayer = new int[this.width_x_height];

        for (int i = 0; i < this.width_x_height; i++) {
            int rawRGB = srgb[i] - (0xff << 24);
            int red = (rawRGB >> 16) % 256;
            redLayer[i] = red;
            int green = (rawRGB >> 8) % 256;
            greenLayer[i] = green;
            int blue = rawRGB % 256;
            blueLayer[i] = blue;
        }


        /*Init*/
        int[] temp = new int[this.getWxH()];

        /*Do Red*/
        for (int j = 0; j < this.getHeight(); ++j) {
            for (int i = 0; i < this.getWidth(); ++i) {
                if (i == 0 || i == filterWidth || j == 0 || j == filterHeight) {
                    temp[j * image.getWidth() + i] += 255 << 16;
                    continue;
                }

                /*Fetch neighbor*/
                int[] neighbor = new int[9];
                for (int off = 0; off < offset.length; ++off) {
                    int xoff = i - offset[off][0];
                    int yoff = j - offset[off][1];
                    neighbor[off] = redLayer[yoff * image.getWidth() + xoff];
                }
                /*Find median*/
                Arrays.sort(neighbor);
                int median = neighbor[5];
                /*replace*/
                temp[j * image.getWidth() + i] += median << 16;
            }
        }
        /*Do Green*/
        for (int j = 0; j < this.getHeight(); ++j) {
            for (int i = 0; i < this.getWidth(); ++i) {
                if (i == 0 || i == filterWidth || j == 0 || j == filterHeight) {
                    temp[j * image.getWidth() + i] += 255 << 8;
                    continue;
                }
                /*Fetch neighbor*/
                int[] neighbor = new int[9];
                for (int off = 0; off < offset.length; ++off) {
                    int xoff = i - offset[off][0];
                    int yoff = j - offset[off][1];
                    neighbor[off] = greenLayer[yoff * image.getWidth() + xoff];
                }
                /*Find median*/
                Arrays.sort(neighbor);
                int median = neighbor[5];
                /*replace*/
                temp[j * image.getWidth() + i] += median << 8;
            }
        }
        /*Do Blue*/
        for (int j = 0; j < this.getHeight(); ++j) {
            for (int i = 0; i < this.getWidth(); ++i) {
                if (i == 0 || i == filterWidth || j == 0 || j == filterHeight) {
                    temp[j * image.getWidth() + i] += 255;
                    continue;
                }
                /*Fetch neighbor*/
                int[] neighbor = new int[9];
                for (int off = 0; off < offset.length; ++off) {
                    int xoff = i - offset[off][0];
                    int yoff = j - offset[off][1];
                    neighbor[off] = blueLayer[yoff * image.getWidth() + xoff];
                }
                /*Find median*/
                Arrays.sort(neighbor);
                int median = neighbor[5];
                /*replace*/
                temp[j * image.getWidth() + i] += median;
            }
        }

        this.imageSetRGB(temp);
        this.repaint();
    }

    private void imageSetRGB(int[] newRGBs) {
        image.setRGB(0, 0, this.getWidth(), this.getHeight(), newRGBs, 0, this.getWidth());
    }

    // *************************************************************************************************
    /**
     * Save this image in the specified path. The buffered image property of
     * this object will be saved as png default format. If in color, it will be
     * saved as is.
     *
     * @param savePath The path to save this image.
     */
    public void saveImage(String savePath) {
        try {
            if (!savePath.endsWith(".png")) {
                savePath += ".png";
            }

            BufferedImage bi = this.getBufferedImage();
            File file = new File(savePath);

            if (!file.exists()) {
                file.mkdirs();
            }

            ImageIO.write(bi, "png", file);
        } catch (IOException ex) {
            Logger.getLogger(ImageShell.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test the binary image by creating a jpanel.
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame f = new JFrame("Load Image Sample");
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        java.awt.ScrollPane scrollPane = new java.awt.ScrollPane();
        f.add(scrollPane);
        ImageShell imageShell = new ImageShell("D:\\white.png");
        scrollPane.add(imageShell);
        f.pack();
        f.setVisible(true);
        System.out.println(imageShell.getWidth());
        System.out.println(imageShell.getHeight());
        System.out.println(imageShell.getWxH());

        int rgb[] = imageShell.getRGB(1, 0);
        int red = rgb[0];
        int green = rgb[1];
        int blue = rgb[2];

        System.out.println("Red=" + red + ", green=" + green + ", blue=" + blue);
    }
}
