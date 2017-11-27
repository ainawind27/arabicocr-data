package com.milowski.hmm;

import java.io.*;
import java.util.*;
import org.xml.sax.*;


/**
 * A reader for processing the XML form of the HMM model specification.
 * @author R. Alexander Milowski
 */
public class XMLModelReader {

   /**
    * The namespace string of the XML document.
    */
   public static final String HMM_NAMESPACE="urn:publicid:IDN+milowski.com:schemas:math:hmm:2004:us";

   XMLReader xmlReader;

   static class State {
      Map transitions;
      Map emissions;
      String name;

      State() {
         transitions = new HashMap();
         emissions = new HashMap();
      }
   }

   static class HMM {
      String name;
      State startState;
      State finalState;
      List states;
      List alphabet;
      Map name2Index;
      Map alpha2Index;
      double error;
      HMM() {
         states = new ArrayList();
         alphabet = new ArrayList();
         name2Index = new HashMap();
         alpha2Index = new HashMap();
         startState = new State();
         finalState = new State();
      }
   }

   class HMMContentHandler implements ContentHandler {

      static final int ALPHABET_STAGE = 0;
      static final int START_STAGE    = 1;
      static final int STATE_STAGE    = 2;
      static final int FINAL_STAGE    = 3;


      HMM hmm;
      int level;
      int skipLevel;
      int stage;
      String error;
      String stateName;
      State currentState;
      boolean inEmissions;

      HMMContentHandler(HMM hmm) {
         this.hmm = hmm;
         this.error = null;
      }

      String getError() {
         return error;
      }

      boolean hasErrors() {
         return error!=null;
      }

      public void startDocument() {
         level = 0;
         stage = 0;
         skipLevel = 0;
         error = null;
         stateName = null;
         currentState = null;
         inEmissions = false;
      }

      public void endDocument() {
      }

      public void startElement(String namespaceURI,String localName,String qName,Attributes atts) {
         level++;
         if (skipLevel>0 && level>skipLevel) {
            return;
         }

         switch (level) {
         case 1: // root element
         {
            if (!namespaceURI.equals(HMM_NAMESPACE) || !localName.equals("hmm")) {
               skipLevel = level;
               error = "Unrecognized root element '"+localName+"' in namespace '"+namespaceURI+"'";
               return;
            }

            int attIndex = atts.getIndex("","name");
            if (attIndex<0) {
               skipLevel = level;
               error = "Missing 'name' attribute on root element.";
               return;
            }
            hmm.name = atts.getValue(attIndex);

            attIndex = atts.getIndex("","error");
            if (attIndex<0) {
               skipLevel = level;
               error = "Missing 'name' attribute on root element.";
               return;
            }
            hmm.error = Double.parseDouble(atts.getValue(attIndex));
         }
            break;
         case 2: // alphabet, start, state, final
         {
            if (!namespaceURI.equals(HMM_NAMESPACE)) {
               // skip other namespaces one level down
               skipLevel = level;
               return;
            }
            if (localName.equals("description")) {
               // skip the description
               skipLevel = level;
               return;
            }
            switch (stage) {
            case ALPHABET_STAGE: // want alphabet only
            {
               if (!localName.equals("alphabet")) {
                  skipLevel = level;
                  error = "Expecting 'alphabet' element and got '"+localName+"'";
                  return;
               }
               int attIndex = atts.getIndex("","lexicon");
               if (attIndex<0) {
                  skipLevel = 1; // skip the whole doc
                  error = "Missing 'lexicon' attribute on 'alphabet' element.";
                  return;
               }
               String lexicon = atts.getValue(attIndex);
               for (int i=0; i<lexicon.length(); i++) {
                  char alpha = lexicon.charAt(i);
                  Character key = new Character(alpha);
                  if (hmm.alpha2Index.get(key)!=null) {
                     skipLevel = 1; // skip the whole doc
                     error = "Character '"+alpha+"' repeated in lexicon.";
                     return;
                  }
                  hmm.alpha2Index.put(key,new Integer(i));
                  hmm.alphabet.add(key);
               }
               stage = START_STAGE;
               break;
            }
            case START_STAGE: // want start or switch to state
               if (localName.equals("start")) {
                  int attIndex = atts.getIndex("","at");
                  if (attIndex<0) {
                     skipLevel = 1;
                     error = "Missing 'at' attribute of 'start' element.";
                     return;
                  }

                  String name = atts.getValue(attIndex);

                  attIndex = atts.getIndex("","probability");
                  if (attIndex<0) {
                     skipLevel = 1;
                     error = "Missing 'probability' attribute of 'start' element.";
                     return;
                  }
                  
                  double probability = Double.parseDouble(atts.getValue(attIndex));
                  Double pvalue = new Double(probability);

                  hmm.startState.transitions.put(name,pvalue);

                  return;

               } else if (!localName.equals("state")) {
                  skipLevel = 1;
                  error = "Element "+localName+" not allowed after 'alphabet' or before 'state'.";
                  return;
               } else {
                  stage = STATE_STAGE;
               }
            case STATE_STAGE: // want state or switch to final
               if (localName.equals("state")) {

                  int attIndex = atts.getIndex("","id");
                  if (attIndex<0) {
                     skipLevel = 1;
                     error = "Missing 'id' attribute of 'state' element.";
                     return;
                  }

                  stateName = atts.getValue(attIndex);
                  currentState = new State();
                  currentState.name = stateName;

               } else if (hmm.states.size()==0) {
                  skipLevel = 1;
                  error = "Missing 'state' elements.";
                  return;
               } else if (!localName.equals("final")) {
                  skipLevel = 1;
                  error = "Element "+localName+" not allowed after 'alphabet' or before 'state'.";
                  return;
               } else {
                  stage = FINAL_STAGE;
               }
            case FINAL_STAGE: // want final only
               if (localName.equals("final")) {
                  int attIndex = atts.getIndex("","at");
                  if (attIndex<0) {
                     skipLevel = 1;
                     error = "Missing 'at' attribute of 'final' element.";
                     return;
                  }

                  String name = atts.getValue(attIndex);

                  attIndex = atts.getIndex("","probability");
                  if (attIndex<0) {
                     skipLevel = 1;
                     error = "Missing 'probability' attribute of 'final' element.";
                     return;
                  }
                  
                  double probability = Double.parseDouble(atts.getValue(attIndex));
                  Double pvalue = new Double(probability);

                  hmm.finalState.transitions.put(name,pvalue);

               } else if (!localName.equals("state")) {
                  skipLevel = 1;
                  error = "Element "+localName+" not allowed after 'state'.";
                  return;
               }
            }
         }
            break;
         case 3: // transition, emissions 
         {
            if (!namespaceURI.equals(HMM_NAMESPACE)) {
               // we shouldn't have foreign namespaces at this level
               skipLevel = 1;
               error = "Namespace '"+namespaceURI+"' is not allowed at this level.";
               return;
            }

            if (stage!=STATE_STAGE) {
               skipLevel = 1;
               error = "Child "+localName+" in namespace '"+namespaceURI+"' is not allowed at this level.";
               return;
            }

            if (localName.equals("transition")) {
               int attIndex = atts.getIndex("","to");
               if (attIndex<0) {
                  skipLevel = 1;
                  error = "Missing 'to' attribute of 'state' element.";
                  return;
               }

               String name = atts.getValue(attIndex);

               attIndex = atts.getIndex("","probability");
               if (attIndex<0) {
                  skipLevel = 1;
                  error = "Missing 'probability' attribute of 'state' element.";
                  return;
               }
                  
               double probability = Double.parseDouble(atts.getValue(attIndex));
               Double pvalue = new Double(probability);

               currentState.transitions.put(name,pvalue);

            } else if (localName.equals("emissions")) {
               // nothing to do...
               inEmissions = true;
            } else {
               skipLevel = 1;
               error = "Child "+localName+" in namespace '"+namespaceURI+"' is not allowed at this level.";
               return;
            }
            
         }
            break;
         case 4: // symbol
         {
            if (inEmissions && namespaceURI.equals(HMM_NAMESPACE) && localName.equals("symbol")) {
               int attIndex = atts.getIndex("","char");
               if (attIndex<0) {
                  skipLevel = 1;
                  error = "Missing 'char' attribute of 'symbol' element.";
                  return;
               }

               String charString = atts.getValue(attIndex);
               charString = charString.trim();
               if (charString.length()!=1) {
                  skipLevel = 1;
                  error = "The 'char' attribute may only contain a character and not '"+charString+"'.";
                  return;
               }
               Character alpha = new Character(charString.charAt(0));

               attIndex = atts.getIndex("","probability");
               if (attIndex<0) {
                  skipLevel = 1;
                  error = "Missing 'probability' attribute of 'symbol' element.";
                  return;
               }
                  
               double probability = Double.parseDouble(atts.getValue(attIndex));
               Double pvalue = new Double(probability);

               currentState.emissions.put(alpha,pvalue);

            } else {
               skipLevel = 1;
               error = "Child "+localName+" in namespace '"+namespaceURI+"' is not allowed at this level.";
               return;
            }
         }
            break;
         }
      }

      public void characters(char [] ch,int start,int length) {
      }

      public void endElement(String namespaceURI,String localName,String qName) {
         if (!(skipLevel>0 && level>skipLevel)) {
            if (namespaceURI.equals(HMM_NAMESPACE)) {
               if (stage==STATE_STAGE) {
                  if (localName.equals("state")) {
                     hmm.states.add(currentState);
                     hmm.name2Index.put(stateName,new Integer(hmm.states.size()-1));
                     currentState = null;
                     stateName = null;
                  } else if (localName.equals("emissions")) {
                     inEmissions = false;
                  }
               }
            }
         }
         level--;
         if (skipLevel>level) {
            skipLevel = 0;
         }
      }

      public void startPrefixMapping(String prefix,String uri) {
      }

      public void endPrefixMapping(String prefix) {
      }

      public void ignorableWhitespace(char [] ch,int start,int length) {
      }

      public void processingInstruction(String target,String data) {
      }

      public void skippedEntity(String name) {
      }

      public void setDocumentLocator(Locator loc) {
      }

   }

   /**
    * Instantiates an instance using the XMLReader instance to parse documents.
    */
   public XMLModelReader(XMLReader xmlReader) {
      this.xmlReader = xmlReader;
   }

   /**
    * Loads a document from an source and returns a model.
    */
   public synchronized  Model load(InputSource source) 
      throws java.io.IOException,SAXException
   {
      HMM hmm = new HMM();

      HMMContentHandler handler = new HMMContentHandler(hmm);
      xmlReader.setContentHandler(handler);
      xmlReader.parse(source);

      if (handler.hasErrors()) {
         throw new java.io.IOException("Invalid HMM document: "+handler.getError());
      }

      Model model = new Model(hmm.name,hmm.alphabet.size(),hmm.states.size(),hmm.error);

      // set alphabet

      model.setLexicon(hmm.alphabet);

      // start start state transitions

      Iterator names = hmm.startState.transitions.keySet().iterator();
      while (names.hasNext()) {
         String name = (String)names.next();
         Integer index = (Integer)hmm.name2Index.get(name);
         if (index==null) {
            throw new java.io.IOException("Cannot find state "+name);
         }
         model.setStateTransition(
            0,
            index.intValue()+1,
            ((Double)hmm.startState.transitions.get(name)).doubleValue()
         );
      }

      // set transition parameters and emissions for each state
      for (int from=0; from<hmm.states.size(); from++) {
         State current = (State)hmm.states.get(from);

         model.setStateName(from+1,current.name);

         // set transitions
         names = current.transitions.keySet().iterator();
         while (names.hasNext()) {
            String name = (String)names.next();
            Integer index = (Integer)hmm.name2Index.get(name);
            if (index==null) {
               throw new java.io.IOException("Cannot find state "+name);
            }
            model.setStateTransition(
               from+1,
               index.intValue()+1,
               ((Double)current.transitions.get(name)).doubleValue()
            );
         }

         // set emissions
         Iterator chars = current.emissions.keySet().iterator();
         while (chars.hasNext()) {
            Character alpha = (Character)chars.next();
            Double pvalue = (Double)current.emissions.get(alpha);
            Integer index = (Integer)hmm.alpha2Index.get(alpha);
            model.setStateEmission(from+1,index.intValue(),pvalue.doubleValue());
         }
      }

      // set end state transitions

      names = hmm.finalState.transitions.keySet().iterator();
      while (names.hasNext()) {
         String name = (String)names.next();
         Integer index = (Integer)hmm.name2Index.get(name);
         if (index==null) {
            throw new java.io.IOException("Cannot find state "+name);
         }
         model.setStateTransition(index.intValue()+1,0,((Double)hmm.finalState.transitions.get(name)).doubleValue());
      }

      return model;
   }
}