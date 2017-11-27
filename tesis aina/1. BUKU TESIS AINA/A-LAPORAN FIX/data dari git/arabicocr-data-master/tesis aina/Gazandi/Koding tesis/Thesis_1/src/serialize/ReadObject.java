package serialize;

import HMM2.HMM;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class ReadObject {
    public static HMM deserializeObject(String filename) throws Exception {
        FileInputStream fin = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fin);
        HMM hmm = (HMM) ois.readObject();
        
        if (fin != null) {
            fin.close();
        }
        
        if (ois != null) {
            ois.close();
        }
        
        return hmm;
    }
}
