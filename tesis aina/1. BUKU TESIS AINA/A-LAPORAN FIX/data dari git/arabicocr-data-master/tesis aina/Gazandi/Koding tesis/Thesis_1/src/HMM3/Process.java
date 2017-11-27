package HMM3;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import utils.ChainCode;
import utils.ImageSegmentation;
import utils.ImageTransformation;
import utils.Thinning;
import utils.segment.zidouri2.Segmentation;

public class Process {
    public static void train() throws Exception {
        // create model directory for each data
        File dataset = new File(Const.TRAIN_DATA);
        System.out.println(dataset.getAbsolutePath());
        System.out.println(dataset.listFiles());
        for (File file : dataset.listFiles()) {
            if (file.isDirectory()) {
                String name = file.getName();
                File model = new File(Const.MODEL_DATA + name);
                if (!model.exists()) {
                    model.mkdir();
                }
            }
        }
        // write model to each destined folder
        for (File file : dataset.listFiles()) {
            if (file.isDirectory()) {
                String name = file.getName();
                for (File fImage : file.listFiles()) {
                    if (!fImage.isDirectory()) {
                        // get image's name for naming model
                        String iName = fImage.getName();
                        System.out.println(iName);
                        // load the model image
                        BufferedImage tempImage = ImageIO.read(fImage);
                        tempImage = whiteBorder(tempImage);
                        // convert the image into black and white
                        ImageTransformation imageTrans = new ImageTransformation(tempImage);
                        BufferedImage bwImage = imageTrans.doBinary();
                        // thinning
                        Thinning thinning = new Thinning(bwImage);
                        BufferedImage thinImage = thinning.doZhangSuen();
                        // chain code
                        int[][] arrayImage = ChainCode.imageToArray(thinImage);
                        List<String> chains = ChainCode.multipleChain(arrayImage);
                        String allChain = "";
                        for (String chain : chains) {
                            allChain += chain;
                        }
                        int[] temp = new int[allChain.length()];
                        for (int i = 0; i < temp.length; i++) {
                            temp[i] = Integer.parseInt(allChain.substring(i, i + 1)) - 1;
                        }
                        int[][] o = {temp};
                        // train
                        HiddenMarkov hmm = new HiddenMarkov(temp.length, Const.NUM_SYMBOLS);
                        hmm.setTrainSeq(o);
                        hmm.train();
                        // write object
                        Serialize.serializeModel(hmm, Const.MODEL_DATA + name + File.separator + iName + ".ser");
                    }
                }
            }
        }
    }
    
    public static void test(String dirImage) throws Exception {
        File imageFile = new File(dirImage);
        // load the model image
        BufferedImage tempImage = ImageIO.read(imageFile);
        // convert the image into black and white
        ImageTransformation imageTrans = new ImageTransformation(tempImage);
        BufferedImage bwImage = imageTrans.doBinary();
        // do line segmentation based on horizontal projection
        List<BufferedImage> lines = ImageSegmentation.lineSegmentation(bwImage);
        // do character segmentation
        for (int i = 0; i < lines.size(); i++) {
            ImageIO.write(lines.get(i), "bmp", new File(Const.TEST_IMAGES + Const.lineName + imageFile.getName() + i + ".bmp"));
            BufferedImage imageChar = Segmentation.characterSegmentation(lines.get(i));
            ImageIO.write(imageChar, "bmp", new File(Const.TEST_IMAGES + Const.charName + i + ".bmp"));
            List<BufferedImage> charSegments = ImageSegmentation.subWordSegmentation(imageChar);
            // iterate through every image on the list
            System.out.println("Gambar ke-" + (i + 1));
            for (int j = 0; j < charSegments.size(); j++) {
                ImageIO.write(charSegments.get(j), "bmp", new File(Const.TEST_IMAGES + Const.segmentName + j + ".bmp"));
                // create white border around image
                BufferedImage segment = charSegments.get(j);
                segment = whiteBorder(segment);
                // thinning
                Thinning thinning = new Thinning(segment);
                BufferedImage thinImage = thinning.doZhangSuen();
                ImageIO.write(thinImage, "bmp", new File(Const.TEST_IMAGES + Const.thinSegmentName + j + ".bmp"));
                // chain code
                int[][] arrayImage = ChainCode.imageToArray(thinImage);
                List<String> chains = ChainCode.multipleChain(arrayImage);
                String allChain = "";
                for (String chain : chains) {
                    allChain += chain;
                }
                System.out.println(allChain);
                // preparing chain
                int[] o = new int[allChain.length()];
                for (int iterate = 0; iterate < o.length; iterate++) {
                    o[iterate] = Integer.parseInt(allChain.substring(iterate, iterate + 1)) - 1;
                }
                // init similarity
                double highest = Double.NEGATIVE_INFINITY;
                String currentSim = "";
                // load every model that has been created earlier
                File models = new File(Const.MODEL_DATA);
                for (File model : models.listFiles()) {
                    if (model.isDirectory()) {
                        // take the name of the folder
                        String name = model.getName();
                        // load every file inside that folder
                        for (File modelFile : model.listFiles()) {
                            // compute similarity
                            if (!modelFile.isDirectory()) {
                                String fullPath = modelFile.getAbsolutePath();
                                HiddenMarkov currentModel = Serialize.deserializeObject(fullPath);
                                double viterbi = currentModel.viterbi(o);
                                if (viterbi > highest) {
                                    highest = viterbi;
                                    currentSim = name;
                                }
                            }
                        }
                    }
                }
                // prints the answer
                System.out.println(currentSim);
            }
        }
    }
    
    private static BufferedImage whiteBorder(BufferedImage input) {
        int height = input.getHeight();
        int width = input.getWidth();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x == 0 || y == 0 || y == height - 1 || x == width - 1) {
                    input.setRGB(x, y, 0xffffffff);
                }
            }
        }

        return input;
    }
    
    public static void printHMM() throws Exception {
        File modelFiles = new File(Const.MODEL_DATA);
        for (File modelFolder : modelFiles.listFiles()) {
            if (modelFolder.isDirectory()) {
                System.out.println(modelFolder.getName());
                for (File modelFile : modelFolder.listFiles()) {
                    System.out.println(modelFile.getName());
                    HiddenMarkov currentModel = Serialize.deserializeObject(modelFile.getAbsolutePath());
                    currentModel.print();
                }
            }
        }
    }
}
