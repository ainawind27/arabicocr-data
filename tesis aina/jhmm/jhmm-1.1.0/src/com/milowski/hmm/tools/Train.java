package com.milowski.hmm.tools;

import java.io.*;
import java.util.*;
import java.net.*;

import javax.xml.parsers.*;
import org.xml.sax.*;

import com.milowski.hmm.*;
import org.infoset.xml.XMLException;
import org.infoset.xml.util.WriterItemDestination;

public class Train {

   public static class SequenceCount {
      int count;
      short [] sequence;
      public SequenceCount(int count,short [] sequence) {
         this.count = count;
         this.sequence = sequence;
      }
      
      public int getCount() {
         return count;
      }
      
      public short [] getSequence() {
         return sequence;
      }
   }
   
   public static List loadSequenceCounts(Reader input,Model.Translator translator) 
      throws java.io.IOException
   {
      return loadSequenceCounts(input,translator,false);
   }
   
   public static List loadSequenceCounts(Reader input,Model.Translator translator,boolean compact) 
      throws java.io.IOException
   {
      List sequences = new ArrayList();
      // TODO: this isn't so efficient
      BufferedReader breader = new BufferedReader(input);
      String line;
      while ((line = breader.readLine())!=null) {
         line = line.trim();
         if (line.length()==0) {
            continue;
         }
         int comma = line.indexOf(',');
         if (comma<1) {
            comma = line.length();
         }

         short [] sequence = new short[comma];
         for (int pos=0; pos<comma; pos++) {
            char ch = line.charAt(pos);
            sequence[pos] = translator.translate(ch);
            if (sequence[pos]<0) {
               throw new IOException("Sequence character "+ch+" is not in the model's lexicon.");
            }
         }
         int count = comma==line.length() ? 1 : Integer.parseInt(line.substring(comma+1));
         if (compact) {
            sequences.add(new SequenceCount(count,sequence));
         } else {
            for (int i=0; i<count; i++) {
               sequences.add(sequence);
            }
         }
      }
      return sequences;
      
   }
   
   public static void loadStateTransitionMatrix(Model model,Reader input) 
      throws IOException
   {
      int row = 0;
      BufferedReader breader = new BufferedReader(input);
      String line;
      int numberOfStates = model.getNumberOfStates();
      while ((line = breader.readLine())!=null && row<numberOfStates) {
         line = line.trim();
         if (line.length()==0) {
            continue;
         }
         String [] numbers = line.split("\\s+");
         if (numbers.length!=numberOfStates) {
            throw new IOException("Bad matrix line at row "+row+".  Number of columns is "+numbers.length+" but it should be "+numberOfStates);
         }
         for (int to=1; to<numberOfStates; to++) {
            model.setStateTransition(row,to, Double.parseDouble(numbers[to]));
         }
         if (row!=0) {
            model.setStateTransition(row,0,Double.parseDouble(numbers[0]));
         }
         row++;
      }
   }
   
   public static void loadStateEmissionMatrix(Model model,Reader input) 
      throws IOException
   {
      int row = 0;
      BufferedReader breader = new BufferedReader(input);
      String line;
      int numberOfStates = model.getNumberOfStates();
      int numberOfCharacters = model.getLexicon().size();
      while ((line = breader.readLine())!=null && row<numberOfStates) {
         line = line.trim();
         if (line.length()==0) {
            continue;
         }
         String [] numbers = line.split("\\s+");
         if (numbers.length!=numberOfCharacters) {
            throw new IOException("Bad matrix line at row "+row+".  Number of columns is "+numbers.length+" but it should be "+numberOfCharacters);
         }
         for (int ch=0; ch<numberOfCharacters; ch++) {
            model.setStateEmission(row, ch, Double.parseDouble(numbers[ch]));
         }
         row++;
      }
   }
   
   static private short [] load(String name,Reader input,Model.Translator translator) 
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
               throw new java.io.IOException("Invalid character '"+buffer[i]+"' in sequence file "+name);
            }
         }
      }
      return sequence;
   }

   public static void main(String args[]) {

      if (args.length<4) {
         System.err.println("Usage: Predict model-file threshold ( -c sequences-count-file | -f sequence-file1 sequence-file2 ... | -s scenario-xml output-file-xml )");
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

         double delta = Double.parseDouble(args[1]);

         Model.Translator translator = model.getTranslator();
         List sequences = null;
         if (args[2].equals("-c")) {
            // Sequence counts
            FileReader sequenceInput = new FileReader(args[3]);
            sequences = loadSequenceCounts(sequenceInput,translator);
            
            hmmEngine.train(sequences,delta,null);

            hmmEngine.updateModel(model);

            XMLModelWriter modelWriter = new XMLModelWriter(new OutputStreamWriter(System.out));
            modelWriter.write(model);

         } else if (args[2].equals("-s")) {
            // A training scenario document
            URL location;
            if (args[3].startsWith("http:") || args[3].startsWith("file:")) {
               location = new URL(args[3]);
            } else if (args[3].charAt(0)=='!') {
               location = new URL(args[3].substring(1));
            } else {
               File f = new File(args[3]);
               location = f.toURL();
            }
            ScenarioDriver driver = new ScenarioDriver(model);
            FileOutputStream out = new FileOutputStream(args[4]);
            driver.run(location, new WriterItemDestination(new OutputStreamWriter(out,"UTF-8"),"UTF-8",true));
            
         } else if (args[2].equals("-f")) {
            // Sequences from files

            sequences = new ArrayList();

            for (int i=3; i<args.length; i++) {

               FileReader sequenceInput = new FileReader(args[i]);

               short [] sequence = load(args[i],sequenceInput,translator);

               sequenceInput.close();

               sequences.add(sequence);

            }
            
            hmmEngine.train(sequences,delta,null);

            hmmEngine.updateModel(model);

            XMLModelWriter modelWriter = new XMLModelWriter(new OutputStreamWriter(System.out));
            modelWriter.write(model);

         } else {
            System.err.println("Unknown option: "+args[2]);
            System.exit(1);
         }

      } catch (java.io.IOException ex) {
         ex.printStackTrace();
      } catch (XMLException ex) {
         ex.printStackTrace();
      } catch (org.xml.sax.SAXException ex) {
         System.err.println(ex.getMessage());
      } catch (javax.xml.parsers.ParserConfigurationException ex) {
         ex.printStackTrace();
      }
   }
}