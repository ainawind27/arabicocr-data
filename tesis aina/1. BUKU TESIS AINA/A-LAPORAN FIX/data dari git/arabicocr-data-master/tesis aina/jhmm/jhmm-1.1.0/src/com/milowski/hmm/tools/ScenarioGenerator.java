/*
 * ScenarioGenerator.java
 *
 * Created on October 29, 2004, 9:27 AM
 */

package com.milowski.hmm.tools;

import java.text.*;
import java.util.*;

import com.milowski.hmm.*;
import org.infoset.xml.Element;
import org.infoset.xml.ItemConstructor;
import org.infoset.xml.ItemDestination;
import org.infoset.xml.XMLException;
import org.infoset.xml.util.XMLConstructor;

/**
 *
 * @author  alex
 */
public class ScenarioGenerator
{

   static class Scenario {
      List counts;
      double limit;
      double [][]s;
      double [][]t;
      Scenario(List counts,double limit,double [][]s,double [][]t) {
         this.counts = counts;
         this.limit = limit;
         this.s = s;
         this.t = t;
      }
   }
   
   Model model;
   List scenarios;
   /** Creates a new instance of ScenarioGenerator */
   public ScenarioGenerator(Model model)
   {
      scenarios = new ArrayList();
      this.model = model;
   }
   
   public void addScenario(List compactSequenceCounts,double limit,double [][] s, double [][] t) {
      scenarios.add(new Scenario(compactSequenceCounts,limit,s,t));
   }
   
   public void addScenario(List compactSequenceCounts,double [][] s, double [][] t) {
      scenarios.add(new Scenario(compactSequenceCounts,0,s,t));
   }
   
   public void addScenario(List compactSequenceCounts,double [][] s) {
      scenarios.add(new Scenario(compactSequenceCounts,0,s,null));
   }
   
   public void addScenario(List compactSequenceCounts) {
      scenarios.add(new Scenario(compactSequenceCounts,0,null,null));
   }
   
   public void toXML(ItemDestination dest) 
      throws XMLException
   {
      ItemConstructor itemConstructor = ScenarioDriver.DEFAULT_INFOSET.createItemConstructor();
      XMLConstructor xmlConstructor = new XMLConstructor(itemConstructor,dest);

      dest.send(itemConstructor.createDocument());
      
      Element e = xmlConstructor.createElement(ScenarioDriver.TSCENARIOS_NM);
      dest.send(e);
      dest.send(itemConstructor.createCharacters("\n"));

      List lexicon = model.getLexicon();
      
      NumberFormat formatter = NumberFormat.getInstance();
      formatter.setMaximumFractionDigits(8);
      formatter.setMinimumFractionDigits(2);
      
      int sindex = 1;
      for (Iterator toOutput = scenarios.iterator(); toOutput.hasNext(); sindex++) {
         Scenario s = (Scenario)toOutput.next();
         Element se = xmlConstructor.createElement(ScenarioDriver.TSCENARIO_NM);
         se.setAttributeValue("name","s"+sindex);
         if (s.limit!=0) {
            se.setAttributeValue("error",Double.toString(s.limit));
         }
         dest.send(se);
         dest.send(itemConstructor.createCharacters("\n"));
         e = xmlConstructor.createElement(ScenarioDriver.ODATA_NM);
         dest.send(e);
         StringBuffer output = new StringBuffer();
         output.append('\n');
         for (Iterator counts = s.counts.iterator(); counts.hasNext(); ) {
            Train.SequenceCount count = (Train.SequenceCount)counts.next();
            short [] sequence = count.getSequence();
            for (int i=0; i<sequence.length; i++) {
               output.append((Character)lexicon.get(sequence[i]));
            }
            output.append(',');
            output.append(Integer.toString(count.getCount()));
            output.append('\n');
         }
         dest.send(itemConstructor.createCharacters(output.toString()));
         xmlConstructor.flushToElement(e);
         dest.send(itemConstructor.createCharacters("\n"));
         if (s.s!=null) {
            e = xmlConstructor.createElement(ScenarioDriver.TRANSITIONS_NM);
            dest.send(e);
            output = new StringBuffer();
            output.append('\n');
            for (int from=0; from<s.s.length; from++) {
               for (int to=0; to<s.s[from].length; to++) {
                  output.append(' ');
                  output.append(formatter.format(s.s[from][to]));
               }
               output.append('\n');
            }
            dest.send(itemConstructor.createCharacters(output.toString()));
            xmlConstructor.flushToElement(e);
            dest.send(itemConstructor.createCharacters("\n"));
         }
         if (s.t!=null) {
            e = xmlConstructor.createElement(ScenarioDriver.EMISSIONS_NM);
            dest.send(e);
            output = new StringBuffer();
            output.append('\n');
            for (int from=0; from<s.t.length; from++) {
               for (int to=0; to<s.t[from].length; to++) {
                  output.append(' ');
                  output.append(formatter.format(s.t[from][to]));
               }
               output.append('\n');
            }
            dest.send(itemConstructor.createCharacters(output.toString()));
            xmlConstructor.flushToElement(e);
            dest.send(itemConstructor.createCharacters("\n"));
         }
         xmlConstructor.flushToElement(se);
         dest.send(itemConstructor.createCharacters("\n"));
      }
      
      xmlConstructor.flush();
      dest.send(itemConstructor.createDocumentEnd());
   }
   
}
