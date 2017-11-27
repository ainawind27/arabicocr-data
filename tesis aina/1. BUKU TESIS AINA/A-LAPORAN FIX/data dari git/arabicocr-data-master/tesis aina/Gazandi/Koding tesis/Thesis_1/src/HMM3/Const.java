package HMM3;

import java.io.File;

public class Const {
        public static final Const instance = new Const();
//        public static final String APP_FOLDER = "/home/gazandic" + File.separator + "Aina-Tesis" + File.separator;
//       public static final String TRAIN_DATA = APP_FOLDER +"datasetHurufArab" + File.separator;
//        public static final String MODEL_DATA = APP_FOLDER +"modelHurufArab/";
//        public static final int NUM_SYMBOLS = 8; // 8 karakter chaincode untuk training
//        public static final String TEST_IMAGES = APP_FOLDER + "dataHuruf/";
       
        
      public static final String APP_FOLDER = "D:" + File.separator + "tesis" + File.separator + "Gambar";
	public static final String TRAIN_DATA = APP_FOLDER + File.separator + "dataset huruf arab" + File.separator;
      public static final String MODEL_DATA = APP_FOLDER + File.separator + "model huruf arab" + File.separator;
      public static final int NUM_SYMBOLS = 8; // 8 karakter chaincode untuk training
      public static final String TEST_IMAGES = APP_FOLDER + File.separator + "data huruf" + File.separator;
        
       public static final String RESULT_IMAGES = APP_FOLDER + File.separator + "result"+ File.separator;
        public static final String lineName = "line_";
        public static final String charName = "char_";
        public static final String segmentName = "segment_";
        public static final String thinSegmentName = "thin_";
}