/*
 * ScenarioDriver.java
 *
 * Created on October 27, 2004, 2:14 PM
 */

package com.milowski.hmm.tools;

import java.text.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.milowski.hmm.*;
import org.infoset.xml.Characters;
import org.infoset.xml.Element;
import org.infoset.xml.ElementEnd;
import org.infoset.xml.Infoset;
import org.infoset.xml.Item;
import org.infoset.xml.ItemConstructor;
import org.infoset.xml.ItemDestination;
import org.infoset.xml.Name;
import org.infoset.xml.XMLException;
import org.infoset.xml.sax.SAXDocumentLoader;
import org.infoset.xml.util.XMLConstructor;

/**
 *
 * @author  alex
 */
public class ScenarioDriver
{
   static Infoset DEFAULT_INFOSET;
   
   static Name TSCENARIOS_NM;
   static Name TSCENARIO_NM;
   static Name ODATA_NM;
   static Name STEPS_NM;
   static Name STEP_NM;
   static Name TRANSITIONS_NM;
   static Name EMISSIONS_NM;
   static Name ROW_NM;
   static Name E_NM;
   static {
      TSCENARIOS_NM = Name.create("training-scenarios");
      TSCENARIO_NM = Name.create("training-scenario");
      ODATA_NM = Name.create("observation-data");
      STEPS_NM = Name.create("steps");
      STEP_NM = Name.create("step");
      TRANSITIONS_NM = Name.create("transitions");
      EMISSIONS_NM = Name.create("emissions");
      ROW_NM = Name.create("row");
      E_NM = Name.create("e");
   }

   static int DATA = 0;
   static int TRANS = 1;
   static int EMIT = 2;
   
   class ScenarioDestination implements ItemDestination {
      
      ItemDestination output;
      ItemConstructor itemConstructor;
      double error;
      boolean trainInitial;
      boolean trainTrans;
      boolean trainEmit;
      String [] scenario;
      StringBuffer data;
      XMLConstructor xmlConstructor;
      Logger log;
      ScenarioDestination(ItemDestination output) {
         this.output = output;
         this.itemConstructor = infoset.createItemConstructor();
         this.xmlConstructor = new XMLConstructor(itemConstructor,output);
         this.scenario = new String[3];
         this.data = null;
         this.log = Logger.getAnonymousLogger();
      }
      
      public void send(Item item) throws XMLException
      {
         switch (item.getType()) {
            case DocumentItem:
               output.send(itemConstructor.createDocument());
               break;
            case DocumentEndItem:
               output.send(itemConstructor.createDocumentEnd());
               break;
            case ElementItem:
            {
               Element e = (Element)item;
               String name = e.getName().getLocalName();
               if (name.equals("training-scenarios")) {
                  output.send(e);
               } else if (name.equals("training-scenario")) {
                  String sname = e.getAttributeValue("name");
                  if (sname!=null) {
                     log.info("Processing scenario "+sname);
                  }
                  String astr = e.getAttributeValue("error");
                  if (astr==null) {
                     error = 0.005;
                  } else {
                     error = Double.parseDouble(astr);
                  }
                  trainInitial = false;
                  astr = e.getAttributeValue("initial");
                  if (astr!=null) {
                     trainInitial = astr.equals("yes");
                  }
                  trainTrans = true;
                  astr = e.getAttributeValue("transitions");
                  if (astr!=null) {
                     trainTrans = astr.equals("yes");
                  }
                  trainEmit = false;
                  astr = e.getAttributeValue("emissions");
                  if (astr!=null) {
                     trainEmit = astr.equals("yes");
                  }
                  output.send(e);
                  scenario[DATA] = null;
                  scenario[TRANS] = null;
                  scenario[EMIT] = null;
               } else if (name.equals("observation-data")) {
                  data = new StringBuffer();
               } else if (name.equals("transitions")) {
                  data = new StringBuffer();
               } else if (name.equals("emissions")) {
                  data = new StringBuffer();
               }
            }
               break;
            case ElementEndItem:
            {
               ElementEnd e = (ElementEnd)item;
               String name = e.getName().getLocalName();
               if (name.equals("training-scenarios")) {
                  output.send(e);
               } else if (name.equals("training-scenario")) {
                  if (scenario[DATA]!=null) {
                     try {
                        log.info("Training data:\n"+scenario[DATA]);
                        List sequences = Train.loadSequenceCounts(new StringReader(scenario[DATA]),model.getTranslator());
                        if (scenario[TRANS]!=null) {
                           Train.loadStateTransitionMatrix(model, new StringReader(scenario[TRANS]));
                        }
                        if (scenario[EMIT]!=null) {
                           Train.loadStateTransitionMatrix(model, new StringReader(scenario[TRANS]));
                        }
                        engine.loadModel(model);
                        TrainingPathRecorder trecorder = new TrainingPathRecorder();
                        engine.setTraining(trainInitial, trainTrans, trainEmit);
                        engine.setTrainingTracer(trecorder);
                        engine.train(sequences, error, null);
                        engine.updateModel(model);
                        
                        // Output data
                        outputResult(trecorder);
                        
                     } catch (IOException ex) {
                        xmlConstructor.createTextElement(infoset.createName("error"),"Cannot process traning scenario due to error: "+ex.getMessage());
                     }
                  }
                  output.send(e);
               } else if (name.equals("observation-data")) {
                  scenario[DATA] = data.toString();
                  data = null;
               } else if (name.equals("transitions")) {
                  scenario[TRANS] = data.toString();
                  data = null;
               } else if (name.equals("emissions")) {
                  scenario[EMIT] = data.toString();
                  data = null;
               }
            }
               break;
            case CharactersItem:
               if (data!=null) {
                  data.append(((Characters)item).getText());
               } else {
                  output.send(item);
               }
         }
      }
      
      void outputResult(TrainingPathRecorder recorder) 
         throws XMLException
      {
         
         NumberFormat formatter = NumberFormat.getInstance();
         formatter.setMaximumFractionDigits(5);
         formatter.setMinimumFractionDigits(2);
         
         double s[][] = model.getStateTransitions();
         double t[][] = model.getStateEmissions();

         output.send(itemConstructor.createCharacters("\n"));
         Element tr = xmlConstructor.createElement(TRANSITIONS_NM);
         output.send(tr);
         output.send(itemConstructor.createCharacters("\n"));
         
         for (int row=0; row<s.length; row++) {
            Element r = xmlConstructor.createElement(ROW_NM);
            //r.setAttribute("from", Integer.toString(row));
            output.send(r);
            
            for (int col=0; col<s[row].length; col++) {
               Element e = xmlConstructor.createElement(E_NM);
               //e.setAttribute("to", Integer.toString(col));
               output.send(e);
               output.send(itemConstructor.createCharacters(formatter.format(s[row][col])));
               xmlConstructor.flushToElement(e);
            }
            xmlConstructor.flushToElement(r);
            output.send(itemConstructor.createCharacters("\n"));
         }
         xmlConstructor.flush();
         output.send(itemConstructor.createCharacters("\n"));

         Element emit = xmlConstructor.createElement(EMISSIONS_NM);
         output.send(emit);
         output.send(itemConstructor.createCharacters("\n"));
         
         for (int row=0; row<t.length; row++) {
            Element r = xmlConstructor.createElement(ROW_NM);
            //r.setAttribute("from", Integer.toString(row));
            output.send(r);
            
            for (int ch=0; ch<t[row].length; ch++) {
               Element e = xmlConstructor.createElement(E_NM);
               //e.setAttribute("to", Integer.toString(ch));
               output.send(e);
               output.send(itemConstructor.createCharacters(formatter.format(t[row][ch])));
               xmlConstructor.flushToElement(e);
            }
            xmlConstructor.flushToElement(r);
            output.send(itemConstructor.createCharacters("\n"));
         }
         xmlConstructor.flush();
         output.send(itemConstructor.createCharacters("\n"));
         
         int stepCount = recorder.getSteps();
         Element steps = xmlConstructor.createElement(STEPS_NM);
         double logLikelihood = recorder.getLogLikelihood(0);
         steps.setAttributeValue("log-likelihood",Double.toString(logLikelihood));
         output.send(steps);
         output.send(itemConstructor.createCharacters("\n"));
         
         List [][] s_list = recorder.getTransitionSteps();
         List [][] t_list = recorder.getEmissionSteps();
         for (int stepIndex=0; stepIndex<stepCount; stepIndex++) {
            Element step = xmlConstructor.createElement(STEP_NM);
            logLikelihood = recorder.getLogLikelihood(stepIndex+1);
            step.setAttributeValue("log-likelihood",Double.toString(logLikelihood));
            output.send(step);
            output.send(itemConstructor.createCharacters("\n"));
         
            tr = xmlConstructor.createElement(TRANSITIONS_NM);
            output.send(tr);
            output.send(itemConstructor.createCharacters("\n"));
            for (int from=0; from<s_list.length; from++) {
               Element r = xmlConstructor.createElement(ROW_NM);
               //r.setAttribute("from", Integer.toString(from));
               output.send(r);

               for (int to=0; to<s_list[from].length; to++) {
                  Element e = xmlConstructor.createElement(E_NM);
                  //e.setAttribute("to", Integer.toString(to));
                  output.send(e);
                  Double value = (Double)s_list[from][to].get(stepIndex);
                  output.send(itemConstructor.createCharacters(formatter.format(Math.exp(value.doubleValue()))));
                  xmlConstructor.flushToElement(e);
               }
               xmlConstructor.flushToElement(r);
               output.send(itemConstructor.createCharacters("\n"));
            }
            xmlConstructor.flushToElement(tr);
            output.send(itemConstructor.createCharacters("\n"));
            
            emit = xmlConstructor.createElement(EMISSIONS_NM);
            output.send(emit);
            output.send(itemConstructor.createCharacters("\n"));
            for (int from=0; from<t_list.length; from++) {
               Element r = xmlConstructor.createElement(ROW_NM);
               //r.setAttribute("from", Integer.toString(from));
               output.send(r);

               for (int to=0; to<t_list[from].length; to++) {
                  Element e = xmlConstructor.createElement(E_NM);
                  //e.setAttribute("to", Integer.toString(to));
                  output.send(e);
                  Double value = (Double)t_list[from][to].get(stepIndex);
                  output.send(itemConstructor.createCharacters(formatter.format(Math.exp(value.doubleValue()))));
                  xmlConstructor.flushToElement(e);
               }
               xmlConstructor.flushToElement(r);
               output.send(itemConstructor.createCharacters("\n"));
            }
            xmlConstructor.flushToElement(emit);
            output.send(itemConstructor.createCharacters("\n"));
            xmlConstructor.flushToElement(step);
            output.send(itemConstructor.createCharacters("\n"));
         }
         xmlConstructor.flush();
         output.send(itemConstructor.createCharacters("\n"));
         
      }
      
      public void setOutputType(Name name) throws XMLException
      {
      }
      
   }
   Infoset infoset;
   Model model;
   Engine engine;
   
   /** Creates a new instance of ScenarioDriver */
   public ScenarioDriver(Model model)
   {
      this(DEFAULT_INFOSET,model);
   }
   
   /** Creates a new instance of ScenarioDriver */
   public ScenarioDriver(Infoset infoset ,Model model)
   {
      this.infoset = infoset;
      this.model = model;
      this.engine = new Engine();
   }
   
   public void run(URL sourceLocation,ItemDestination output) 
      throws IOException,XMLException
   {
      try {
         SAXDocumentLoader dloader = new SAXDocumentLoader();
         dloader.generate(new URI(sourceLocation.toString()), new ScenarioDestination(output));
      } catch (URISyntaxException ex) {
         throw new IOException(ex.getMessage());
      }
   }
   
   public void run(Reader input,ItemDestination output) 
      throws IOException,XMLException
   {
      SAXDocumentLoader dloader = new SAXDocumentLoader();
      dloader.generate(input, new ScenarioDestination(output));
   }
   
}
