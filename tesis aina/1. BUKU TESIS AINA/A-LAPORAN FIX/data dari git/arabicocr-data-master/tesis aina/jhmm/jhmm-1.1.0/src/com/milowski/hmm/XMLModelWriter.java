package com.milowski.hmm;

import java.io.*;
import java.util.*;

/**
 * A writer for output HMM models in XML syntax.
 * @author R. Alexander Milowski
 */
public class XMLModelWriter {

   Writer writer;

   public XMLModelWriter(Writer writer) {
      this.writer = writer;
   }

   public void write(Model model) 
      throws java.io.IOException
   {
      writer.write("<hmm xmlns='"+XMLModelReader.HMM_NAMESPACE+"' name='"+model.getName()+"' error='"+Double.toString(model.getError())+"'>\n\n");

      double [][] transitions = model.getStateTransitions();
      for (int state=1; state<transitions[0].length; state++) {
         writer.write("<start at='"+model.getStateName(state)+"' probability='"+Double.toString(transitions[0][state])+"'/>\n");
      }
      writer.write("\n");

      List lexicon = model.getLexicon();

      double [][] emissions = model.getStateEmissions();

      for (int from=1; from<transitions.length; from++) {
         writer.write("<state id='"+model.getStateName(from)+"'>\n");
         for (int to=1; to<transitions[from].length; to++) {
            writer.write("<transition to='"+model.getStateName(to)+"' probability='"+Double.toString(transitions[from][to])+"'/>\n");
         }
         writer.write("<emissions>\n");
         for (int t=0; t<emissions[from].length; t++) {
            writer.write("<symbol char='"+lexicon.get(t)+"' probability='"+Double.toString(emissions[from][t])+"'/>\n");
         }
         writer.write("</emissions>\n");
         writer.write("</state>\n\n");
      }

      for (int state=1; state<transitions.length; state++) {
         writer.write("<final at='"+model.getStateName(state)+"' probability='"+Double.toString(transitions[state][0])+"'/>\n");
      }

      writer.write("\n</hmm>\n");

      writer.flush();
   }
}