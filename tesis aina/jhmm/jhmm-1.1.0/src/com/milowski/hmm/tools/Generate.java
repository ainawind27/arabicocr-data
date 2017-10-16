package com.milowski.hmm.tools;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import org.xml.sax.*;

import com.milowski.hmm.*;

public class Generate {

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
   
   public static class SequenceCount {
      int [] sequence;
      int count;
      SequenceCount(int [] sequence) {
         this.sequence = sequence;
         this.count = 1;
      }
      
      void increment() {
         count++;
      }
      
      public int getCount() {
         return count;
      }
      
      public int [] getSequence() {
         return sequence;
      }
   }
   
   public static class ListOfSequences extends ArrayList {
      
      public void addSequence(int [] sequence) {
         int index = find(sequence);
         if (index<0) {
            add(new SequenceCount(sequence));
         } else {
            SequenceCount seqCount = (SequenceCount)get(index);
            seqCount.increment();
         }
      }
      public int find(int [] sequenceToFind) {
         for (int i=0; i<size(); i++) {
            SequenceCount seqCount = (SequenceCount)get(i);
            int [] seq = seqCount.getSequence();
            if (seq.length!=sequenceToFind.length) {
               continue;
            }
            int j;
            for (j=0; j<seq.length; j++) {
               if (seq[j]!=sequenceToFind[j]) {
                  break;
               }
            }
            if (j==seq.length) {
               return i;
            }
         }
         return -1;
      }
   }

   public static void main(String args[]) {

      if (args.length<3) {
         System.err.println("Usage: Generate model-file ( -s length | -c count length )");
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

         if (args[1].equals("-s")) {
            int [][] result = model.generateSequence(Integer.parseInt(args[2]));

            List lexicon = model.getLexicon();
            for (int i=0; i<result[0].length; i++) {
               System.out.print((Character)lexicon.get(result[0][i]));
            }
            System.out.println();
            int numberOfStates = model.getNumberOfStates();
            if (numberOfStates>9) {
               for (int i=0; i<result[1].length; i++) {
                  System.out.print(result[1][i]);
                  System.out.print(',');
               }
            } else {
               for (int i=0; i<result[1].length; i++) {
                  System.out.print(result[1][i]);
               }
            }
            System.out.println();
            for (int i=1; i<numberOfStates; i++) {
               System.out.println(i+" = "+model.getStateName(i));
            }
         } else if (args[1].equals("-c")) {
            List lexicon = model.getLexicon();
            int count = Integer.parseInt(args[2]);
            int length = Integer.parseInt(args[3]);
            ListOfSequences uniqueSeqs = new ListOfSequences();
            for (int i=0; i<count; i++) {
               int [][] result = model.generateSequence(length);
               uniqueSeqs.addSequence(result[0]);
            }
            Iterator seqs = uniqueSeqs.iterator();
            while (seqs.hasNext()) {
               SequenceCount seq = (SequenceCount)seqs.next();
               int [] key = seq.getSequence();
               for (int i=0; i<key.length; i++) {
                  System.out.print((Character)lexicon.get(key[i]));
               }
               System.out.print(',');
               System.out.println(seq.getCount());
            }
            
         }

      } catch (java.io.IOException ex) {
         ex.printStackTrace();
      } catch (org.xml.sax.SAXException ex) {
         System.err.println(ex.getMessage());
      } catch (javax.xml.parsers.ParserConfigurationException ex) {
         ex.printStackTrace();
      }
   }
}