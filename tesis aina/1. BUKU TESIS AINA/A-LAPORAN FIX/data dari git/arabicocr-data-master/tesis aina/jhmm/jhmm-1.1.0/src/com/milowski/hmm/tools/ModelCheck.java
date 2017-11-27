package com.milowski.hmm.tools;

import java.io.*;

import javax.xml.parsers.*;
import org.xml.sax.*;

import com.milowski.hmm.*;

public class ModelCheck {
   public static void main(String args[]) {

      if (args.length!=1) {
         System.err.println("Usage: ModelCheck file");
         System.exit(1);
      }

      try {
         SAXParserFactory saxFactory = SAXParserFactory.newInstance();
         saxFactory.setNamespaceAware(true);
         SAXParser saxParser = saxFactory.newSAXParser();
         XMLReader xmlReader = saxParser.getXMLReader();

         XMLModelReader modelReader = new XMLModelReader(xmlReader);
         InputSource source = new InputSource(new FileReader(args[0]));

         Model model = modelReader.load(source);

         model.check();

         XMLModelWriter modelWriter = new XMLModelWriter(new OutputStreamWriter(System.out));
         modelWriter.write(model);

      } catch (java.io.IOException ex) {
         ex.printStackTrace();
      } catch (org.xml.sax.SAXException ex) {
         System.err.println(ex.getMessage());
      } catch (javax.xml.parsers.ParserConfigurationException ex) {
         ex.printStackTrace();
      }
   }
}