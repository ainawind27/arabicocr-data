package com.milowski.hmm.tools;

import java.io.*;

import javax.xml.parsers.*;
import org.xml.sax.*;

import com.milowski.hmm.*;

public class Forward {

   public static short [] load(Reader input,Model.Translator translator) 
      throws java.io.IOException
   {
      short [] sequence = null;
      char [] buffer = new char[10240];
      int len;
      while ((len=input.read(buffer))>0) {
         for (int i=len-1; i>=0; i--) {
            if (Character.isWhitespace(buffer[i])) {
               if (i==(len-1)) {
                  len--;
               } else {
                  System.arraycopy(buffer,i+1,buffer,i,len-i-1);
               }
            }
         }
         int start;
         if (sequence==null) {
            sequence = new short[len];
            start = 0;
         } else {
            start = sequence.length;
            short [] newseq = new short[sequence.length+len];
            System.arraycopy(sequence,0,newseq,0,sequence.length);
            sequence = newseq;
         }
         for (int i=0; i<len; i++) {
            sequence[start+i] = translator.translate(buffer[i]);
            if (sequence[start+i]<0) {
               throw new IOException("Sequence character "+buffer[i]+" is not in the model's lexicon.");
            }
         }
      }
      return sequence;
   }

   public static void main(String args[]) {

      if (args.length!=2) {
         System.err.println("Usage: Predict model-file sequence-file");
         System.exit(1);
      }

      try {
         SAXParserFactory saxFactory = SAXParserFactory.newInstance();
         saxFactory.setNamespaceAware(true);
         SAXParser saxParser = saxFactory.newSAXParser();
         XMLReader xmlReader = saxParser.getXMLReader();

         XMLModelReader modelReader = new XMLModelReader(xmlReader);
         FileReader modelInput = new FileReader(args[0]);
         InputSource source = new InputSource(modelInput);

         Model model = modelReader.load(source);

         modelInput.close();

         model.check();

         Engine hmmEngine = new Engine();
         hmmEngine.loadModel(model);

         Model.Translator translator = model.getTranslator();

         FileReader sequenceInput = new FileReader(args[1]);
         
         short [] sequence = load(sequenceInput,translator);

         sequenceInput.close();

         double [][] result = hmmEngine.forwards(sequence,null);
         double total = 0;
         for (int i=1; i<result.length; i++) {
            double value = Math.exp(result[i][sequence.length-1]);
            total += value;
            System.out.println(i+": "+value);
         }
         System.out.println("Total: "+total);

      } catch (java.io.IOException ex) {
         ex.printStackTrace();
      } catch (org.xml.sax.SAXException ex) {
         System.err.println(ex.getMessage());
      } catch (javax.xml.parsers.ParserConfigurationException ex) {
         ex.printStackTrace();
      }
   }
}