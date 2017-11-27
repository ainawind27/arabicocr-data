package serialize;

import HMM2.HMM;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class WriteObject {
    public static void serializeModel(HMM hmm, String url) throws Exception {
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
}