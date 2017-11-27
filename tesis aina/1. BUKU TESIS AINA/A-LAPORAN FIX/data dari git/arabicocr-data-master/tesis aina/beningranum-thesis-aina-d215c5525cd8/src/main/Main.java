package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import model.Point;
import utils.ImageSegmentation;
import utils.ImageTransformation;
import utils.Thinning;
import utils.segment.zidouri.Segmentation;

public class Main {

    public static void main(String[] args) throws Exception {
        String mainLocation = "D:\\tesis\\";
        String imageFileName = "dsawal-without dots.jpg";

        // load image file into BufferedImage
        BufferedImage mainImage = ImageIO.read(new File(mainLocation + imageFileName));
        // transform it into black-white image
        ImageTransformation imageTrans = new ImageTransformation(mainImage);
        BufferedImage bwImage = imageTrans.doBinary();
        // do line segmentation
        List<BufferedImage> lineSegments, subWordSegments, characterSegments;
        lineSegments = ImageSegmentation.lineSegmentation(bwImage);
        // iterate through all the horizontal segments
        for (BufferedImage horSegment : lineSegments) {
            // do sub-word segmentation
            subWordSegments = ImageSegmentation.subWordSegmentation(horSegment);
            // iterate through all the vertical segments
            for (BufferedImage verSegment : subWordSegments) {
                // create white image based on segment
                BufferedImage emptySegment = ImageTransformation.createWhiteImage(verSegment.getWidth(), verSegment.getHeight());
                // skeletonize the segment
                Thinning thin = new Thinning(verSegment);
                // search for the candidates of cutting point
                List<Point> pointBands = Segmentation.characterSegmentation(thin.doZhangSuen());
                // draw guide bands based on cutting point candidates
                pointBands.stream().forEach((p) -> {
                    for (int y = 0; y < emptySegment.getHeight(); y++) {
                        emptySegment.setRGB(p.getX(), y, 0xff000000);
                    }
                });
                // find out every guide bands in the image
                List<int[]> guideBands = Segmentation.bandRange(emptySegment);
                // extract all features based on guide bands
                List<int[]> features = Segmentation.extractFeatures(emptySegment);
                // make decisions for every guide band candidates
                if (features != null) {
                    List<Integer> removedIndex = new ArrayList<>();
                    for (int i = 0; i < features.size(); i++) {
                        int[] feature = features.get(i);

                        if (!Segmentation.guideBandClassification(feature, features.size(), i + 1)) {
//                            guideBands.remove(i);
                            removedIndex.add(i);
                        }
                    }
                    for (int idx : removedIndex) {
                        guideBands.remove(idx);
                    }
                }
                // create new image filled with fixed guide bands
                BufferedImage fixedGuideBands = ImageTransformation.createWhiteImage(verSegment.getWidth(), verSegment.getHeight());
                for (int[] guideBand : guideBands) {
                    int start = guideBand[0];
                    int end = guideBand[1];

                    for (int x = end; end <= start; x++) {
                        for (int y = 0; y < fixedGuideBands.getHeight(); y++) {
                            fixedGuideBands.setRGB(x, y, 0xff000000);
                        }
                    }
                }

                ImageIO.write(fixedGuideBands, "bmp", new File(mainLocation + "fixed.bmp"));
            }
        }
    }
}
