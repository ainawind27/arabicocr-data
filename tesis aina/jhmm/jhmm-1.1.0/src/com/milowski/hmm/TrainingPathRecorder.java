/*
 * TrainingPathRecorder.java
 *
 * Created on October 12, 2004, 10:50 AM
 */

package com.milowski.hmm;

import java.util.*;

/**
 *
 * @author  alex
 */
public class TrainingPathRecorder implements Engine.TrainingTracer {
   
   int steps;
   List [][] p;
   List [][] emit;
   List logLikelihoods;
   
   /** Creates a new instance of TrainingPathRecorder */
   public TrainingPathRecorder() {
      steps = 0;
      p = null;
      emit = null;
      logLikelihoods = new ArrayList();
   }
   
   public int getSteps() {
      return steps;
   }
   
   public List [][] getTransitionSteps() {
      return p;
   }

   public List [][] getEmissionSteps() {
      return emit;
   }

   List [][] initMatrix(double [][] matrix) {
      List [][] m = new List[matrix.length][];
      for (int from=0; from<m.length; from++) {
         m[from] = new List[matrix[from].length];
         for (int to=0; to<m[from].length; to++) {
            m[from][to] = new ArrayList();
         }
      }
      return m;
   }
   
   void copyValues(List [][] store, double[][]m) {
      for (int from=0; from<m.length; from++) {
         for (int to=0; to<m[from].length; to++) {
            store[from][to].add(new Double(m[from][to]));
         }
      }
   }
   
   public void notifyStart(double[][] transitions, double[][] emitValues) {
      p = initMatrix(transitions);
      emit = initMatrix(emitValues);
      copyValues(p,transitions);
      copyValues(emit,emitValues);
   }
   
   
   public void notifyChange(double[][] oldTransitions, double[][] newTransitions, double[][] oldEmit, double[][] newEmit) {
      steps++;
      copyValues(p,newTransitions);
      copyValues(emit,newEmit);
   }
   
   public void notifyLastLogLikelihood(double value)
   {
      logLikelihoods.add(new Double(value));
   }
   
   public int getLogLikelihoodCount() {
      return logLikelihoods.size();
   }
   
   public double getLogLikelihood(int step) {
      if (step>=logLikelihoods.size()) {
         return Double.NEGATIVE_INFINITY;
      } else {
         return ((Double)logLikelihoods.get(step)).doubleValue();
      }
   }
   
}
