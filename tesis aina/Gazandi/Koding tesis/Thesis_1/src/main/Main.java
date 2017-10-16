package main;

import HMM2.HMM;
import image.BinaryImageShell;
import image.MainbodySOSet;
import image.segmentator.SegmentatorChar;
import image.segmentator.SegmentatorSubword;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import serialize.ReadObject;
import serialize.WriteObject;
import utils.ChainCode;
import utils.ImageSegmentation;
import utils.ImageTransformation;
import utils.STDChainCode;
import utils.Thinning;
import utils.segment.zidouri2.Segmentation;

public class Main {

    private static final String modelFolder = "D:\\Data\\Kerja\\Individual Project\\Aina\\Test Gambar\\model huruf arab\\";

    // for testing purposes only
    public static void segmentationTest() throws Exception {
//        String localLoc = "D:\\Data\\Kerja\\Individual Project\\Aina\\Test Gambar\\arabic5.png";
//        // load image file into BufferedImage
//        BufferedImage mainImage = ImageIO.read(new File(localLoc));
//        // transform it into black-white image
//        ImageTransformation imageTrans = new ImageTransformation(mainImage);
//        BufferedImage bwImage = imageTrans.doBinary();
//        // do line segmentation based on horizontal projection
//        List<BufferedImage> lines = ImageSegmentation.lineSegmentation(bwImage);
//        // do character segmentation
//        for (int i = 0; i < lines.size(); i++) {
//            BufferedImage imageChar = Segmentation.characterSegmentation(lines.get(i));
//            ImageIO.write(imageChar, "bmp", new File(localLoc + "main_res.bmp"));
//            List<BufferedImage> charSegments = ImageSegmentation.subWordSegmentation(imageChar);
//            for (int j = 0; j < charSegments.size(); j++) {
//                ImageIO.write(charSegments.get(j), "bmp", new File(localLoc + "res" + i + j + ".bmp"));
//            }
//        }
    }

    // for testing purposes only
    public static void testModel() throws Exception {
//        // load image
//        BufferedImage image = ImageIO.read(new File(mainLocation + imageFileName));
//        // transformation image
//        ImageTransformation imageTrans = new ImageTransformation(image);
//        BufferedImage bwImage = imageTrans.doBinary();
//        // thinning
//        Thinning thinning = new Thinning(bwImage);
//        BufferedImage thinImage = thinning.doZhangSuen();
//        // chain code
//        int[][] arrayImage = ChainCode.imageToArray(thinImage);
//        List<String> chains = ChainCode.multipleChain(arrayImage);
//        String allChain = "";
//        for (String chain : chains) {
//            allChain += chain;
//        }
//        // init HMM object
//        HMM hmm = new HMM(28, 8);
//        hmm.initHMM();
//        //// model before train
//        System.out.println("Before training");
//        hmm.print();
//        // train
//        //// init train data
//        int[] o = new int[allChain.length()];
//        for (int i = 0; i < o.length; i++) {
//            o[i] = Integer.parseInt(allChain.substring(i, i + 1)) - 1;
//        }
//        hmm.train(o, 100);
//        System.out.println("");
//        System.out.println("After training");
//        hmm.print();
//        // write object
//        WriteObject.serializeModel(hmm, mainLocation + "model.ser");
//        HMM hmm2 = ReadObject.deserializeObject(mainLocation + "model.ser");
//        System.out.println("This is what hmm2 prints");
//        hmm2.print();
    }

    // generate models for every arabic characters
    public static void createModel() throws Exception {
        HMM hmm;
        String datasetFolder = "D:\\Data\\Kerja\\Individual Project\\Aina\\Test Gambar\\dataset huruf arab\\";
        // create model directory for each data
        File dataset = new File(datasetFolder);
        for (File file : dataset.listFiles()) {
            if (file.isDirectory()) {
                String name = file.getName();
                File model = new File(modelFolder + name);
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
                        // init HMM object
                        hmm = new HMM(28, 8);
                        // train
                        int[] o = new int[allChain.length()];
                        for (int i = 0; i < o.length; i++) {
                            o[i] = Integer.parseInt(allChain.substring(i, i + 1)) - 1;
                        }
                        hmm.train(o, 10);
                        // write object
                        WriteObject.serializeModel(hmm, modelFolder + name + "\\" + iName + ".ser");
                    }
                }
            }
        }
    }

    // end-to-end testing
    public static void testing() throws Exception {
        String imageLocation = "D:\\Data\\Kerja\\Individual Project\\Aina\\Test Gambar\\";
        String imageName = "arabic1.png";
        String clearName = imageName.split("\\.")[0];
        String lineName = "line_";
        String charName = "char_";
        String segmentName = "segment_";
        String thinSegmentName = "thin_";
        // load the model image
        BufferedImage tempImage = ImageIO.read(new File(imageLocation + imageName));
        // convert the image into black and white
        ImageTransformation imageTrans = new ImageTransformation(tempImage);
        BufferedImage bwImage = imageTrans.doBinary();
        // do line segmentation based on horizontal projection
        List<BufferedImage> lines = ImageSegmentation.lineSegmentation(bwImage);
        // do character segmentation
        for (int i = 0; i < lines.size(); i++) {
            ImageIO.write(lines.get(i), "bmp", new File(imageLocation + lineName + clearName + i + ".bmp"));
            BufferedImage imageChar = Segmentation.characterSegmentation(lines.get(i));
            ImageIO.write(imageChar, "bmp", new File(imageLocation + charName + i + ".bmp"));
            List<BufferedImage> charSegments = ImageSegmentation.subWordSegmentation(imageChar);
            // iterate through every image on the list
            System.out.println("Gambar ke-" + i);
            for (int j = 0; j < charSegments.size(); j++) {
                ImageIO.write(charSegments.get(j), "bmp", new File(imageLocation + segmentName + j + ".bmp"));
                // create white border around image
                BufferedImage segment = charSegments.get(j);
                segment = whiteBorder(segment);
                // thinning
                Thinning thinning = new Thinning(segment);
                BufferedImage thinImage = thinning.doZhangSuen();
                ImageIO.write(thinImage, "bmp", new File(imageLocation + thinSegmentName + j + ".bmp"));
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
                double biggestSimilarity = -100.0;
                String currentSim = "";
                // load every model that has been created earlier
                File models = new File(modelFolder);
                for (File model : models.listFiles()) {
                    if (model.isDirectory()) {
                        // take the name of the folder
                        String name = model.getName();
                        // load every file inside that folder
                        for (File modelFile : model.listFiles()) {
                            // compute similarity
                            if (!modelFile.isDirectory()) {
                                String fullPath = modelFile.getAbsolutePath();
                                HMM currentModel = ReadObject.deserializeObject(fullPath);
                                double similarity = HMM.similarity(currentModel.pi, currentModel.a, currentModel.b, o);
                                if (similarity > biggestSimilarity) {
                                    biggestSimilarity = similarity;
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

    public static BufferedImage whiteBorder(BufferedImage input) {
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

    public static void createModelNumber() throws Exception {
        HMM hmm;
        String datasetFolder = "D:\\Data\\Kerja\\Individual Project\\Aina\\dataset angka\\";
        String modelLocal = "D:\\Data\\Kerja\\Individual Project\\Aina\\model angka\\";
        // get all dataset
        File data = new File(datasetFolder);
        for (File d : data.listFiles()) {
            String name = d.getName();
            if (!d.isDirectory()) {
                BufferedImage tempImage = ImageIO.read(new File(d.getAbsolutePath()));
                // convert the image into black and white
                ImageTransformation imageTrans = new ImageTransformation(tempImage);
                BufferedImage bwImage = imageTrans.doBinary();
                // thinning
                Thinning thinning = new Thinning(bwImage);
                BufferedImage thinImage = thinning.doZhangSuen();
                // resize image
                BufferedImage resizedImage = ImageTransformation.resizeImage(thinImage, 100, 100);
                // chain code
                int[][] arrayImage = ChainCode.imageToArray(resizedImage);
                List<String> chains = ChainCode.multipleChain(arrayImage);
                String allChain = "";
                for (String chain : chains) {
                    allChain += chain;
                }
                // init HMM object
                hmm = new HMM(10, 8);
                // train
                int[] o = new int[allChain.length()];
                for (int i = 0; i < o.length; i++) {
                    o[i] = Integer.parseInt(allChain.substring(i, i + 1)) - 1;
                }
                hmm.train(o, 10);
                // write object
                WriteObject.serializeModel(hmm, modelLocal + name + ".ser");
            }
        }
    }

    public static void testModelNumber() throws Exception {
        HMM hmm;
        String datasetFolder = "D:\\Data\\Kerja\\Individual Project\\Aina\\dataset angka\\";
        String modelLocal = "D:\\Data\\Kerja\\Individual Project\\Aina\\model angka\\";
        String nameData = "9.JPG";
        // load image
        BufferedImage testImage = ImageIO.read(new File(datasetFolder + nameData));
        // convert the image into black and white
        ImageTransformation imageTrans = new ImageTransformation(testImage);
        BufferedImage bwImage = imageTrans.doBinary();
        // thinning
        Thinning thinning = new Thinning(bwImage);
        BufferedImage thinImage = thinning.doZhangSuen();
        // resize image
        BufferedImage resizedImage = ImageTransformation.resizeImage(thinImage, 100, 100);
        // chain code
        int[][] arrayImage = ChainCode.imageToArray(resizedImage);
        List<String> chains = ChainCode.multipleChain(arrayImage);
        String allChain = "";
        for (String chain : chains) {
            allChain += chain;
        }
        // HMM prep
        int[] o = new int[allChain.length()];
        for (int i = 0; i < o.length; i++) {
            o[i] = Integer.parseInt(allChain.substring(i, i + 1)) - 1;
        }
        // init similarity
        double biggestSimilarity = -100.0;
        String currentSim = "";
        // load every model that has been created earlier
        File models = new File(modelLocal);
        for (File model : models.listFiles()) {
            if (!model.isDirectory()) {
                HMM currentModel = ReadObject.deserializeObject(model.getAbsolutePath());
                double similarity = HMM.similarity(currentModel.pi, currentModel.a, currentModel.b, o);
                if (similarity > biggestSimilarity) {
                    biggestSimilarity = similarity;
                    currentSim = model.getName();
                }
            }
        }
        System.out.println(currentSim);
    }

    public static void printProbs() throws Exception {
        String datasetFolder = "D:\\Data\\Kerja\\Individual Project\\Aina\\dataset huruf arab\\ain\\";
        File datasetDir = new File(datasetFolder);
        for (File file : datasetDir.listFiles()) {
            if (!file.isDirectory()) {
                String name = file.getName().trim().split("\\.")[0];
                BufferedImage image = ImageIO.read(new File(file.getAbsolutePath()));
                ImageTransformation transform = new ImageTransformation(image);
                BufferedImage bwImage = transform.doBinary();
                bwImage = ImageTransformation.cropObject(bwImage);
                bwImage = ImageTransformation.resizeImage(bwImage, 50, 50);
                bwImage = whiteBorder(bwImage);
                Thinning thinning = new Thinning(bwImage);
                BufferedImage thinImage = thinning.doZhangSuen();
                ImageIO.write(thinImage, "bmp", new File(datasetFolder + name + ".bmp"));
            }
        }
    }
    
    public static void changeDatasetExtension() throws Exception {
        String datasetFolder = "D:\\Data\\Kerja\\Individual Project\\Aina\\dataset huruf arab\\";
        File datasetDir = new File(datasetFolder);
        for (File file : datasetDir.listFiles()) {
            if (file.isDirectory()) {
                for (File file2 : file.listFiles()) {
                    String name = file2.getName().trim().split("\\.")[0];
                    File fileReplacement = new File(file.getAbsolutePath() + "\\" + name + ".png");
                    BufferedImage inImage = ImageIO.read(file2);
                    ImageIO.write(inImage, "png", fileReplacement);
                    file2.delete();
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // WARNING: proses training berjalan sekitar satu jam
        // Hati hati sebelum menjalankan proses ini
//        HMM3.Process.train();
//       String a = "11223344556";
       String file = "ayah bekerja dikantor\\Ayah_bekerja_di_kantor";
       BinaryImageShell image = new BinaryImageShell(HMM3.Const.TEST_IMAGES + file + ".PNG");
       SegmentatorSubword segSubword = new SegmentatorSubword(image);
       segSubword.blockSegment();
       segSubword.groupBlocks();
       MainbodySOSet[] subwordBlocks = segSubword.getAllSegments();
       MainbodySOSet mso = segSubword.getSegment(0);
       
       int i = 0;
       for (MainbodySOSet meso : subwordBlocks) {
            i++;
            SegmentatorChar segCharacter = new SegmentatorChar(meso);
            segCharacter.zidouri();
            BinaryImageShell main = segCharacter.getInputImage_plain();
            BinaryImageShell mainWithSec = segCharacter.getInputImage_withSecondary();
            BinaryImageShell mainThin = segCharacter.getInputImage_thin();
            main.saveImage(HMM3.Const.RESULT_IMAGES + file + "main" + i +".PNG");
            mainWithSec.saveImage(HMM3.Const.RESULT_IMAGES + file + "mainWithSec" + i +".PNG");
            mainThin.saveImage(HMM3.Const.RESULT_IMAGES + file + "mainThin" + i +".PNG");
       }
//      List<String> lst = new ArrayList<>();
//      lst.add(a);
       
//       STDChainCode s = new STDChainCode(15,'8');
//        s.standarizeModel(lst);
//       HMM3.Process.test(HMM3.Const.TEST_IMAGES + "data_test_timesnewroman.PNG");
//       HMM3.Process.test(HMM3.Const.TEST_IMAGES + "qaf_terpisah.gif");
//        HMM3.Process.printHMM();
    }
}
