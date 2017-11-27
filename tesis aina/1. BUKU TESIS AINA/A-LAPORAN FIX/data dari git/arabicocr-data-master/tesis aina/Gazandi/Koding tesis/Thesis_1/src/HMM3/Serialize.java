package HMM3;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Serialize {
    public static void serializeModel(HiddenMarkov hmm, String url) throws Exception {
        FileOutputStream fout = new FileOutputStream(url);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(hmm);
        
        if (fout != null) {
            fout.close();
        }
        
        if (oos != null) {
            oos.close();
        }
    }
    
    public static HiddenMarkov deserializeObject(String filename) throws Exception {
        FileInputStream fin = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fin);
        HiddenMarkov hmm = (HiddenMarkov) ois.readObject();
        
        if (fin != null) {
            fin.close();
        }
        
        if (ois != null) {
            ois.close();
        }
        
        return hmm;
    }
}
