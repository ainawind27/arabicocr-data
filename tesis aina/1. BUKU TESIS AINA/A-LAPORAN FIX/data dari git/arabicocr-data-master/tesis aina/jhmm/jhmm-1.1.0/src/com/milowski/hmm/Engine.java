package com.milowski.hmm;

import java.util.*;
import java.util.logging.*;

/**
 * The HMM algorithms engine.
 * @author  R. Alexander Milowski
 */
public class Engine {
   
   public static class Prediction {
      double score;
      short [] result;
      public Prediction(double score,short [] result) {
         this.result = result;
         this.score = score;
      }

      public double getScore() {
         return score;
      }

      public short [] getResult() {
         return result;
      }
   }
   
   public interface TrainingTracer {
      void notifyStart(double [][] transitions,double [][] emit);
      void notifyChange(double [][] oldTransitions,double [][] newTransitions, double [][] oldEmit, double [][] newEmit);
      void notifyLastLogLikelihood(double value);
   }


   /**
    * Interface for notification of algorithm iteration
    */
   public interface WorkingNotifier {
      /**
       * This method is called with the iteration count.  If a false value is returned the algorithm processor will stop.
       */
      public boolean continueWorking(int interation);
   }
   
   // Base class for notification adapter
   public static class WorkingNotifierAdapter implements WorkingNotifier {
      
      public boolean continueWorking(int interation) {
         return true;
      }
      
   }
   
   public interface LikelyNotifier {
      public void likely(int index,double d21,double d11,double d12,double d22);
   }
   
   public static class LikelyNotifierAdapter implements LikelyNotifier {
      
      public void likely(int index, double d21, double d11, double d12, double d22) {
      }
      
   }

   // Notifier for iterations
   WorkingNotifier workingNotifier;
   Logger log;

   int alphabetSize;
   double [][] p;
   double [][] emit;
   double error;
   boolean trainStart;
   boolean trainTransitions;
   boolean trainEmissions;
   TrainingTracer trainTracer;
   
   /** Creates a new instance of Model */
   public Engine() {
      this(Logger.getAnonymousLogger());
   }
   
   public Engine(Logger log) {
      this.log = log;
      this.workingNotifier = null;
      this.trainStart = true;
      this.trainTransitions = true;
      this.trainEmissions = true;
      this.trainStart = false;
      this.trainTracer = null;
   }
   
   public void setTraining(boolean startStates,boolean transitions,boolean emissions) {
      trainStart = startStates;
      trainTransitions = transitions;
      trainEmissions = emissions;
   }
   
   public void setTrainingTracer(TrainingTracer tracer) {
      trainTracer = tracer;
   }
   
   public void setLogger(Logger log) {
      this.log = log;
   }
   
   public Logger getLogger() {
      return log;
   }
   
   public void setWorkingNotifier(WorkingNotifier notifier) {
      this.workingNotifier = notifier;
   }

   public void loadModel(Model model) {

      this.error = model.getError();

      this.alphabetSize = model.getAlphabetSize();

      double [][] stateTransitions = model.getStateTransitions();
      double [][] emissions = model.getStateEmissions();

      p = new double[stateTransitions.length][];
      for (int from=0; from<p.length; from++) {
         p[from] = new double[p.length];
         for (int to=0; to<p[from].length; to++) {
            p[from][to] = Math.log(stateTransitions[from][to]);
         }
      }

      emit = new double[emissions.length][];
      for (int state=0; state<emissions.length; state++) {
         emit[state] = new double[alphabetSize];
         for (int t=0; t<emit[state].length; t++) {
            emit[state][t] = Math.log(emissions[state][t]);
         }
      }

   }

   public void updateModel(Model model) {

      for (int from=0; from<p.length; from++) {
         for (int to=0; to<p[from].length; to++) {
            model.setStateTransition(from,to,Math.exp(p[from][to]));
         }
      }

      for (int state=0; state<emit.length; state++) {
         for (int t=0; t<emit[state].length; t++) {
            model.setStateEmission(state,t,Math.exp(emit[state][t]));
         }
      }

   }
   
   /**
    * Generates the most likely state predictions for the sequence
    * @param sequence The sequence for which to generate predictions
    * @return A sequence on integers representing the most likely state for each position in the sequence.
    */
   public Prediction mostLikely(short [] sequence) {
      return mostLikely(sequence,null);
   }
   
   /**
    * Generates the most likely state predictions for the sequence
    * @param sequence The sequence for which to generate predictions
    * @param notifier The notifier for progress indication
    * @return A sequence on integers representing the most likely state for each position in the sequence.
    */
   public Prediction mostLikely(short [] sequence,LikelyNotifier notifier) {
      // Results
      short [] result = new short[sequence.length];
      return mostLikely(sequence,result,notifier);
   }
   
   /**
    * Generates the most likely state predictions for the sequence
    * @param sequence The sequence for which to generate predictions
    * @param result The array to use for the result.
    * @return A sequence on integers representing the most likely state for each position in the sequence.
    */
   public Prediction mostLikely(short [] sequence,short [] result,LikelyNotifier notifier) {
      // Pointer matrix
      short [][] ptrs = new short[p.length][];
      for (int state=0; state<p.length; state++) {
         ptrs[state] = new short[sequence.length];
      }

      // Start with initial probabilities
      double [] last = new double[p.length];
      last[0] = 0;
      for (int state = 1; state<last.length; state++) {
         last[state] = Double.NEGATIVE_INFINITY;
      }

      double max [] = new double[p.length];

      // Iterate through the rest of the sequence
      for (int i=0; i<sequence.length; i++) {

         // Calculate all combinations

         for (int to=0; to<p.length; to++) {
            ptrs[to][i] = -1;
            max[to] = Double.NEGATIVE_INFINITY;
            for (int from=0; from<p.length; from++) {
               // Calculate the probability from this state
               double state_max;
               if (last[from]==Double.NEGATIVE_INFINITY || p[from][to]==Double.NEGATIVE_INFINITY) {
                  state_max = Double.NEGATIVE_INFINITY;
               } else {
                  state_max = last[from] + p[from][to];
               }
               // Keep the greatest for each state
               if (state_max>max[to]) {
                  max[to] = state_max;
                  ptrs[to][i] = (short)from;
               }
            }

            max[to] = max[to] + emit[to][sequence[i]];
            //System.out.println(i+","+to+" -> "+ptrs[to][i]+" from "+max[to]);

         }

         // Assign max as last for next iteration
         for (int to=0; to<p.length; to++) {
            last[to] = max[to];
         }

      }

      // transition to the end state
      for (int from=0; from<p.length; from++) {
         if (last[from]==Double.NEGATIVE_INFINITY || p[from][0]==Double.NEGATIVE_INFINITY) {
            last[from] = Double.NEGATIVE_INFINITY;
         } else {
            last[from] = last[from]+p[from][0];
         }
      }

      // backtrack to get result from pointers 

      // pick the maximum transition to the final state
      int state = 0;
      double maxValue = Double.NEGATIVE_INFINITY;
      for (int to=1; to<p.length; to++) {
         if (last[to]>maxValue) {
            maxValue = last[to];
            state = to;
         }
      }

      // do the backtrack assigning the state as the result
      for (int i=sequence.length-1; i>=0; i--) {
         result[i] = (short)state;
         if (state<0) {
            // Model can't produce sequence.
            maxValue = Double.NEGATIVE_INFINITY;
            break;
         }
         state = ptrs[state][i];
      }
      
      return new Prediction(maxValue,result);
   }
   
   /**
    * Generates a sequence of rolls from the model with the states of used to emit each roll.
    * @param length The length of the sequence to generate.
    * @return A double array of integers where the first array is the sequence of rolls and the second array is the states used to generate the rolls.
    */
   public short [][] generateSequence(int length) {

      throw new UnsupportedOperationException("Not implemented.");

      /*
      // Setup result
      short [][] result = new short[STATE_MAX][];
      result[STATE_D1] = new short[length];
      result[STATE_D2] = new short[length];
      
      double start_d1 = Math.exp(initial[STATE_D1]);
      double stayDice1 = Math.exp(p[STATE_D1][STATE_D1]);
      double stayDice2 = Math.exp(p[STATE_D2][STATE_D2]);
      
      // Choose initial state
      int dice = Math.random()<=start_d1 ? STATE_D1 : STATE_D2;
      
      // Calculate probability spectrum of D1
      double [] d1_spectrum = new double[emit[STATE_D1].length];
      d1_spectrum[0] = Math.exp(emit[STATE_D1][0]);
      for (int i=1; i<emit[STATE_D1].length; i++) {
         d1_spectrum[i] = d1_spectrum[i-1]+Math.exp(emit[STATE_D1][i]);
      }
      if ((d1_spectrum[d1_spectrum.length-1]-1.0)>ROUND_ERROR) { // handles rounding errors
         throw new RuntimeException("Invalid dice 1 emission parameters.  They do not sum to one: "+d1_spectrum[d1_spectrum.length-1]);
      }
      
      // Calculate probability spectrum of D2
      double [] d2_spectrum = new double[emit[STATE_D2].length];
      d2_spectrum[0] = Math.exp(emit[STATE_D2][0]);
      for (int i=1; i<emit[STATE_D2].length; i++) {
         d2_spectrum[i] = d2_spectrum[i-1]+Math.exp(emit[STATE_D2][i]);
      }
      if ((d2_spectrum[d2_spectrum.length-1]-1.0)>ROUND_ERROR) {
         throw new RuntimeException("Invalid dice 2 emission parameters.  They do not sum to one: "+d2_spectrum[d1_spectrum.length-1]);
      }
      
      for (int i=0; i<length; i++) {
         // Calculate roll;
         double rollProb = Math.random();
         int roll = 0;
         for (; rollProb>(dice==STATE_D1 ? d1_spectrum[roll] : d2_spectrum[roll]); roll++);
         result[0][i] = roll+1;
         result[1][i] = dice;
         
         // Calculate transition
         if (dice==STATE_D1) {
            dice = Math.random()<=stayDice1 ? STATE_D1 : STATE_D2;
         } else {
            dice = Math.random()<=stayDice2 ? STATE_D2 : STATE_D1;
         }
      }
      
      return result;
      */
   }
   
   /**
    * Generates the forward algorithm to the end of the sequence.
    * @param sequence The sequence upon which to base the calculations.
    * @param result The result array to use.  One will be allocated if null is passed.
    * @return A double array of forward algorithm probabilities--one for each state and position.  The first index is the state (one of STATE_D1 or STATE_D2) and the second is the position.
    */
   public double [][] forwards(short [] sequence,double [][] result) {
      return forwards(sequence,result,sequence.length);
   }
   
   /**
    * Generates the forward algorithm to the end of the sequence.
    * @param sequence The sequence upon which to base the calculations.
    * @param result The result array to use.  One will be allocated if null is passed.
    * @param limit The position up to which the forward value should be calculated.
    * @return A double array of forward algorithm probabilities--one for each state and position.  The first index is the state (one of STATE_D1 or STATE_D2) and the second is the position.
    */
   public double [][] forwards(short [] sequence,double [][] result,int limit) {

      // TODO: this is horribly inefficient
      if (result==null || result[0].length!=limit) {
         result = new double[p.length][];
         for (int state=0; state<p.length; state++) {
            result[state] = new double[limit];
         }
      }

      for (int to=0; to<p.length; to++) {
         if (p[0][to]==Double.NEGATIVE_INFINITY || emit[to][sequence[0]]==Double.NEGATIVE_INFINITY) {
            result[to][0] = Double.NEGATIVE_INFINITY;
         } else {
            result[to][0] = p[0][to]+emit[to][sequence[0]];
         }
      }

      for (int i=1; i<limit; i++) {

         for (int to=0; to<result.length; to++) {

            if (emit[to][sequence[i]]==Double.NEGATIVE_INFINITY) {
               result[to][i] = Double.NEGATIVE_INFINITY;
            } else {
               int start = 1;
               for (; start<p.length && 
                       (p[start][to]==Double.NEGATIVE_INFINITY || result[start][i-1]==Double.NEGATIVE_INFINITY); 
                    start++);
               if (start==result.length) {
                  result[to][i] = Double.NEGATIVE_INFINITY;
               } else {
                  result[to][i] = result[start][i-1] + p[start][to];
               }

               for (int from=start+1; from<p.length; from++) {
                  if (p[from][to]!=Double.NEGATIVE_INFINITY && result[to][i-1]!=Double.NEGATIVE_INFINITY) {

                     result[to][i] = result[to][i] + 
                        Math.log(1+Math.exp(result[from][i-1]+p[from][to]-result[to][i]));
                  }
               }

               result[to][i] = result[to][i] + emit[to][sequence[i]];

            }
            if (result[to][i]==Double.NaN) {
               throw new RuntimeException("forwards algorithm failed at index "+i+" for state "+to);
            }
         }
      }
      return result;
   }
   
    /**
    * Generates the backwards algorithm to the end of the sequence.
    * @param sequence The sequence upon which to base the calculations.
    * @param result The result array to use.  One will be allocated if null is passed.
    * @return A double array of backwards algorithm probabilities--one for each state and position.  The first index is the state (one of STATE_D1 or STATE_D2) and the second is the position.
    */
   public double [][] backwards(short [] sequence,double [][]result) {
      return backwards(sequence,result,sequence.length);
   }
   
    /**
    * Generates the backwards algorithm to the end of the sequence.
    * @param sequence The sequence upon which to base the calculations.
    * @param result The result array to use.  One will be allocated if null is passed.
    * @param limit The position up to which the backwards values will be calculated.
    * @return A double array of backwards algorithm probabilities--one for each state and position.  The first index is the state (one of STATE_D1 or STATE_D2) and the second is the position.
    */
   public double [][] backwards(short [] sequence,double [][]result, int limit) {
      if (result==null || result[0].length!=limit) {
         result = new double[p.length][];
         for (int from=0; from<p.length; from++) {
            result[from] = new double[limit];
         }
      }

      int diff = sequence.length-limit;
      for (int from=0; from<p.length; from++) {
         result[from][limit-1] = p[from][0];
      }
      for (int i=limit-2; i>=0; i--) {
         for (int from=0; from<result.length; from++) {

            int start = 1;
            for (; start<p.length && 
                    (p[from][start]==Double.NEGATIVE_INFINITY || 
                     emit[start][sequence[i+1]]==Double.NEGATIVE_INFINITY ||
                     result[start][i+1]==Double.NEGATIVE_INFINITY); start++);
            if (start==result.length) {
               result[from][i] = Double.NEGATIVE_INFINITY;
            } else {
               result[from][i] = p[from][start]+emit[start][sequence[i+1]]+result[start][i+1];
            }

            for (int to=start+1; to<p.length; to++) {
               if (p[from][to]!=Double.NEGATIVE_INFINITY && 
                   emit[to][sequence[i+1]]!=Double.NEGATIVE_INFINITY &&
                   result[to][i+1]!=Double.NEGATIVE_INFINITY) {

                  result[from][i] = result[from][i] + 
                     Math.log(1+Math.exp(p[from][to]+emit[to][sequence[i+1]]+result[to][i+1]-result[from][i]));
               }
            }
            if (result[from][i]==Double.NaN) {
               throw new RuntimeException("backwards algorithm failed at index "+i+" for state "+from);
            }
         }
      }
      return result;
   }
   
   /**
    * Trains the model one a set of sequences using the Baum-Welsch estimation method.  The model will retain the new model parameters from the training.
    * @param sequences A array of sequences to train upon.
    * @param delta The delta change value at which the training will end.
    * @param pseudoCounts Pseudo counts for the transitions
    */
   public void train(List sequences,double delta,int [][] pseudoCounts) {
      double change;

      /*
      System.out.println("Initial     : "+Math.exp(initial[STATE_D1])+","+Math.exp(initial[STATE_D2]));
      System.out.println("Start       : "+Math.exp(p[STATE_D1][STATE_D2])+","+Math.exp(p[STATE_D1][STATE_D1])+","+Math.exp(p[STATE_D2][STATE_D1])+","+Math.exp(p[STATE_D2][STATE_D2]));
      System.out.println("log(Start)  : "+p[STATE_D1][STATE_D2]+","+p[STATE_D1][STATE_D1]+","+p[STATE_D2][STATE_D1]+","+p[STATE_D2][STATE_D2]);
      */


      if (trainTracer!=null) {
         trainTracer.notifyStart(p, emit);
      }
      
      if (sequences.size()<1) {
         throw new RuntimeException("There must be at least one sequence.");
      }
      double [][] log_pseudo = new double[p.length][];
      for (int from=0; from<p.length; from++) {
         log_pseudo[from] = new double[p.length];
         for (int to=0; to<p.length; to++) {
            log_pseudo[from][to] = pseudoCounts==null ? 0 : Math.log(pseudoCounts[from][to]);
         }
      }
      
      double [][] forward = null;
      double [][] backward = null;
      int count = 0;

      // Allocate state matrix for calculations
      double new_p[][] = new double[p.length][];
      for (int from=0; from<p.length; from++) {
         new_p[from] = new double[p.length];
      }

      double g[][] = new double[p.length][];
      for (int from=0; from<p.length; from++) {
         g[from] = new double[p.length];
      }

      double [][] new_emit = new double[p.length][];
      for (int from=0; from<p.length; from++) {
         new_emit[from] = new double[emit[from].length];
      }
      for (int i=0; i<new_emit[0].length; i++) {
         new_emit[0][i] = Double.NEGATIVE_INFINITY;
      }

      double [][] emit_g = new double[p.length][];
      for (int from=0; from<p.length; from++) {
         emit_g[from] = new double[emit[from].length];
      }
      
      //log.setLevel(Level.FINE);

      if (log.isLoggable(Level.FINE)) {
         log.info("Starting probabilities: ");
         for (int from=0; from<p.length; from++) {
            for (int to=0; to<p.length; to++) {
               double value = p[from][to]==Double.NEGATIVE_INFINITY ? 0 : Math.exp(p[from][to]);
               log.info("P "+from+"->"+to+" "+value+" "+p[from][to]);
            }
            for (int t=0; t<emit[from].length; t++) {
               double value = emit[from][t]==Double.NEGATIVE_INFINITY ? 0 : Math.exp(emit[from][t]);
               log.info(from+": E["+t+"] = "+value);
            }
         }
         log.info("Training: ");
      }

      double lastLogLikelihood = Double.NEGATIVE_INFINITY;
      
      int lastSequenceIndex = sequences.size()-1;
      do {

         double logLikelihood = 0;
         
         for (int j=lastSequenceIndex; j>=0; j--) {

            short [] sequence = (short [])sequences.get(j);

            int end = sequence.length-1;
            if (true || log.isLoggable(Level.FINE)) {
               log.fine("Training on sequence: "+(j+1));
            }
            
            // Pre-calculate the forwards and backwards algorithm values
            forward = forwards(sequence,forward);
            backward = backwards(sequence,backward);

            // Calculate sequence probability
            int endIndex = sequence.length-1;
            int start = 1;
            for (; start<p.length && (forward[start][endIndex]==Double.NEGATIVE_INFINITY || p[start][0]==Double.NEGATIVE_INFINITY); start++);
            double seqprob = Double.NEGATIVE_INFINITY;
            if (start!=p.length) {
               seqprob = forward[start][endIndex]+p[start][0];
            }
            for (int state=start+1; state<p.length; state++) {
               if (forward[state][endIndex]!=Double.NEGATIVE_INFINITY &&
                   p[state][0]!=Double.NEGATIVE_INFINITY) {
                  seqprob = seqprob + Math.log(1+Math.exp(forward[state][endIndex]+p[state][0]-seqprob));
               }
            }
            if (log.isLoggable(Level.FINE)) {
               log.fine("log(P("+(j+1)+"))   : "+seqprob);            
            }
            if (seqprob==Double.NEGATIVE_INFINITY) {
               throw new RuntimeException("Sequence "+(j+1)+" cannot be produced by this model.");
            }
            
            logLikelihood += seqprob;

            // TODO: This doesn't work for the training the transitions to the final state
            // as emit[to][*] is zero and backwards[to][*] probably is too.
            
            // Calculate the initial transitions 0->n;
            for (int to=1; to<p.length; to++) {
               if (p[0][to]==Double.NEGATIVE_INFINITY) {
                  g[0][to] = Double.NEGATIVE_INFINITY;
                  //System.out.println("set g["+from+"]["+to+"]="+g[from][to]);
               } else {
                  g[0][to] = 
                     p[0][to]
                     + emit[to][sequence[0]]
                     + backward[to][1];
               }
            }

            // Calculate the initial transitions n->0;
            for (int from=1; from<p.length; from++) {
               if (p[from][0]==Double.NEGATIVE_INFINITY || forward[from][end-1]==Double.NEGATIVE_INFINITY) {
                  g[from][0] = Double.NEGATIVE_INFINITY;
                  //System.out.println("set g["+from+"]["+to+"]="+g[from][to]);
               } else {
                  g[from][0] = 
                     p[from][0]
                     + forward[from][end-1];
               }
            }

            // calculate sequence transition probability for k->l where k,l!=0
            for (int from=1; from<p.length; from++) {
               for (int to=1; to<p.length; to++) {
                  if (log.isLoggable(Level.FINE)) {
                     log.info("Calculating "+from+"->"+to);
                  }
                  if (p[from][to]==Double.NEGATIVE_INFINITY) {
                     g[from][to] = Double.NEGATIVE_INFINITY;
                     //System.out.println("set g["+from+"]["+to+"]="+g[from][to]);
                  } else {
                     int startIndex = end-1;
                     for (; startIndex>=0 && 
                            (forward[from][startIndex]==Double.NEGATIVE_INFINITY ||
                             emit[to][sequence[startIndex+1]]==Double.NEGATIVE_INFINITY ||
                             backward[to][startIndex+1]==Double.NEGATIVE_INFINITY); startIndex--) {
                        if (log.isLoggable(Level.FINE)) {
                           log.info("skipping f="+forward[from][startIndex]+", p="+p[from][to]+", e="+emit[to][sequence[startIndex+1]]+", b="+backward[to][startIndex+1]);
                        }
                     }
                     g[from][to] = Double.NEGATIVE_INFINITY;
                     if (startIndex>=0) {
                        g[from][to] = 
                           forward[from][startIndex]
                           + p[from][to]
                           + (to==0 ? 0 : emit[to][sequence[startIndex+1]])
                           + backward[to][startIndex+1];
                        if (log.isLoggable(Level.FINE)) {
                           log.info("At pos="+startIndex+" -> "+sequence[startIndex]);
                           log.info("from f="+forward[from][startIndex]+", p="+p[from][to]+", e="+emit[to][sequence[startIndex+1]]+", b="+backward[to][startIndex+1]);
                        }
                        
                     } else {
                        throw new RuntimeException("Sequence is too short to make it through the model for all states (i.e. some transitions cannot be estimated).");
                     }
                     if (log.isLoggable(Level.FINE)) {
                        log.info("first g["+from+"]["+to+"]="+g[from][to]);
                     }
                     for (int i=startIndex-1; i>=0; i--) {
                        if (log.isLoggable(Level.FINE)) {
                           log.info("At pos="+i+" -> "+sequence[i]);
                        }
                        if (forward[from][i]!=Double.NEGATIVE_INFINITY &&
                            emit[to][sequence[i+1]]!=Double.NEGATIVE_INFINITY &&
                            backward[to][i+1]!=Double.NEGATIVE_INFINITY) {
                           double s = 
                              forward[from][i]
                              + p[from][to]
                              + (to==0 ? 0 : emit[to][sequence[i+1]])
                              + backward[to][i+1];
                           g[from][to] = g[from][to] + Math.log(1+Math.exp(s-g[from][to]));

                           if (log.isLoggable(Level.FINE)) {
                              log.info(i+": g["+from+"]["+to+"]="+g[from][to]);
                           }
                           //if (g[from][to]==Double.POSITIVE_INFINITY) {
                           //   System.out.println("s="+s);
                           //}
                        } else if (log.isLoggable(Level.FINE)) {
                           log.info("Skipping at pos="+i+" -> f="+forward[from][i]+", e="+emit[to][sequence[i+1]]+", b="+backward[to][i+1]);
                        }
                     }
                  }
                  //System.out.println("g["+from+"]["+to+"]="+g[from][to]);
               }
            }

            // calculate emission probabilities for each state
            for (int state=0; state<p.length; state++) {
               for (int t=0; t<emit_g[state].length; t++) {
                  emit_g[state][t] = Double.NEGATIVE_INFINITY;
               }
               for (int i=end; i>=0; i--) {
                  if (forward[state][i]!=Double.NEGATIVE_INFINITY &&
                      backward[state][i]!=Double.NEGATIVE_INFINITY) {
                     //System.out.println("forward["+state+"]["+i+"]="+forward[state][i]+",backward["+state+"]["+i+"]="+backward[state][i]);
                     if (emit_g[state][sequence[i]]==Double.NEGATIVE_INFINITY) {
                        emit_g[state][sequence[i]] = forward[state][i]+backward[state][i];
                     } else {
                        emit_g[state][sequence[i]] = emit_g[state][sequence[i]] + Math.log(1+Math.exp(forward[state][i]+backward[state][i]-emit_g[state][sequence[i]]));
                     }
                     //System.out.println("emit_g["+state+"]["+sequence[i]+"]="+emit_g[state][sequence[i]]);
                  }
               }
            }


            // Divide by sequence probability add contribution to final values
            for (int from=0; from<p.length; from++) {
               for (int to=1; to<p.length; to++) {

                  // calculate transition contributions
                  if (j==lastSequenceIndex || new_p[from][to]==Double.NEGATIVE_INFINITY) {
                     if (g[from][to]==Double.NEGATIVE_INFINITY) {
                        new_p[from][to] = Double.NEGATIVE_INFINITY;
                     } else {
                        new_p[from][to] = g[from][to] - seqprob;
                     }
                  } else if (g[from][to]!=Double.NEGATIVE_INFINITY) {
                     new_p[from][to] = g[from][to] - seqprob + 
                        Math.log(1+Math.exp(new_p[from][to]-g[from][to]+seqprob));
                  }

                  //System.out.println("new_p["+from+"]["+to+"]="+new_p[from][to]);

                  // calculate emission contributions
                  if (from!=0) {
                     for (int t=0; t<new_emit[from].length; t++) {
                        if (j==lastSequenceIndex || new_emit[from][t]==Double.NEGATIVE_INFINITY) {
                           if (emit_g[from][t]==Double.NEGATIVE_INFINITY) {
                              new_emit[from][t] = Double.NEGATIVE_INFINITY;
                           } else {
                              new_emit[from][t] = emit_g[from][t]-seqprob;
                           }
                        } else if (emit_g[from][t]!=Double.NEGATIVE_INFINITY) {
                           new_emit[from][t] = emit_g[from][t] - seqprob + 
                              Math.log(1+Math.exp(new_emit[from][t]-emit_g[from][t]+seqprob));                       
                        }
                     }
                  }

               }
            }

            // Divide by sequence probability for k->0 as the go the other way
            for (int from=1; from<p.length; from++) {
               // calculate transition contributions
               if (j==lastSequenceIndex || new_p[from][0]==Double.NEGATIVE_INFINITY) {
                  if (g[from][0]==Double.NEGATIVE_INFINITY) {
                     new_p[from][0] = Double.NEGATIVE_INFINITY;
                  } else {
                     new_p[from][0] = g[from][0] - seqprob;
                  }
               } else if (g[from][0]!=Double.NEGATIVE_INFINITY) {
                  new_p[from][0] = g[from][0] - seqprob + 
                     Math.log(1+Math.exp(new_p[from][0]-g[from][0]+seqprob));
               }
            }
         }
         
/* TODO: fix pseudo counts????  This needs to come *after* the sum is calcuated otherwise
         bad things happen to large unscaled counts (those transitions with near 1 probability).
         for (int from=0; from<p.length; from++) {
            for (int to=1; to<p.length; to++) {

               // Add in pseudo counts
               if (log_pseudo[from][to]!=Double.NEGATIVE_INFINITY) {
                  if (new_p[from][to]==Double.NEGATIVE_INFINITY) {
                     new_p[from][to] = log_pseudo[from][to];
                  } else {
                     new_p[from][to] = new_p[from][to]+Math.log(1+Math.exp(log_pseudo[from][to]-new_p[from][to]));
                  }
                  System.out.println("pseudo count new_p["+from+"]["+to+"]="+new_p[from][to]);
               }

            }
         }
*/

         // Normalize so they sum to one
         for (int from=0; from<p.length; from++) {
            int start = 1;
            double sum = Double.NEGATIVE_INFINITY;
            for (; start<p.length && new_p[from][start]==Double.NEGATIVE_INFINITY; start++);
            if (start!=p.length) {
               sum = new_p[from][start];
            }
            //System.out.println("sum["+from+"]="+sum);
            for (int to=start+1; to<p.length; to++) {
               if (new_p[from][to]!=Double.NEGATIVE_INFINITY) {
                  sum = sum + Math.log(1+Math.exp(new_p[from][to]-sum));
               }
            }
            if (sum==Double.NEGATIVE_INFINITY) {
               throw new RuntimeException("Fatal error: Zero state transitions in training.");
            }
            //System.out.println("sum["+from+"]="+sum);
            if (sum==Double.NaN) {
               throw new RuntimeException("Fatal error: Sum is NaN.");
            }
            for (int to=1; to<p.length; to++) {
               if (new_p[from][to]==Double.NaN) {
                  throw new RuntimeException("Fatal error: Transition "+from+" -> "+to+" is NaN.");
               }
               if (new_p[from][to]!=Double.NEGATIVE_INFINITY) {
                  new_p[from][to] = new_p[from][to] - sum;
               }
               if (new_p[from][to]==Double.NaN) {
                  throw new RuntimeException("Fatal error: Transition "+from+" -> "+to+" is NaN.");
               }
            }

            if (from!=0) {
               sum = Double.NEGATIVE_INFINITY;
               for (int t=0; t<new_emit[from].length; t++) {
                  //System.out.println("new_emit["+from+"]["+t+"]="+new_emit[from][t]);
                  if (new_emit[from][t]!=Double.NEGATIVE_INFINITY) {
                     if (sum==Double.NEGATIVE_INFINITY) {
                        sum = new_emit[from][t];
                     } else {
                        sum = sum + Math.log(1+Math.exp(new_emit[from][t]-sum));
                     }
                     //System.out.println("sum is now "+sum);
                  }
               }
               //System.out.println("Emission sum for "+from+" is "+sum);
               //if (sum==Double.NEGATIVE_INFINITY) {
               //   throw new RuntimeException("Fatal error: Zero emissions in training for state "+from);
               //}
               if (sum==Double.NaN) {
                  throw new RuntimeException("Fatal error: Sum is NaN.");
               }
               for (int t=0; t<new_emit[from].length; t++) {
                  if (new_emit[from][t]!=Double.NEGATIVE_INFINITY) {
                     new_emit[from][t] = new_emit[from][t] - sum;
                  }
               }
            }
         }

         // Do the same for n->0 transitions as they go a different way on the matrix
         // Calculate new model paraemters and normalize so they sum to one
         {
            int start = 1;
            double sum = Double.NEGATIVE_INFINITY;
            for (; start<p.length && new_p[start][0]==Double.NEGATIVE_INFINITY; start++);
            if (start!=p.length) {
               sum = new_p[start][0];
            }
            //System.out.println("sum["+from+"]="+sum);
            for (int from=start+1; from<p.length; from++) {
               if (new_p[from][0]!=Double.NEGATIVE_INFINITY) {
                  sum = sum + Math.log(1+Math.exp(new_p[from][0]-sum));
               }
            }
            if (sum==Double.NEGATIVE_INFINITY) {
               throw new RuntimeException("Fatal error: Zero state transitions in training.");
            }
            //System.out.println("sum["+from+"]="+sum);
            if (sum==Double.NaN) {
               throw new RuntimeException("Fatal error: Sum is NaN.");
            }
            // Normalize
            for (int from=1; from<p.length; from++) {
               if (new_p[from][0]==Double.NaN) {
                  throw new RuntimeException("Fatal error: Transition "+from+" -> 0 is NaN.");
               }
               if (new_p[from][0]!=Double.NEGATIVE_INFINITY) {
                  new_p[from][0] = new_p[from][0] - sum;
               }
               if (new_p[from][0]==Double.NaN) {
                  throw new RuntimeException("Fatal error: Transition "+from+" -> 0 is NaN.");
               }
            }
         }
         
         /* TODO: This was already done above, right?!?!?!
         // Normalize emissions for each state
         for (int from=0; from<new_p.length; from++) {
            double sum = Double.NEGATIVE_INFINITY;
            for (int to=1; to<new_p.length; to++) {
               if (new_p[from][to]!=Double.NEGATIVE_INFINITY) {
                  if (sum==Double.NEGATIVE_INFINITY) {
                     sum = new_p[from][to];
                  } else {
                     sum = sum + Math.log(1+Math.exp(new_p[from][to]-sum));
                  }
               }               
            }
            //System.out.println("Total transition prob for "+from+" is "+Math.exp(sum));
            for (int to=1; to<p.length; to++) {
               new_p[from][to] -= sum;
            }
            
            if (from!=0) {
               sum = Double.NEGATIVE_INFINITY;
               for (int t=0; t<new_emit[from].length; t++) {
                  if (new_emit[from][t]!=Double.NEGATIVE_INFINITY) {
                     if (sum==Double.NEGATIVE_INFINITY) {
                        sum = new_emit[from][t];
                     } else {
                        sum = sum + Math.log(1+Math.exp(new_emit[from][t]-sum));
                     }
                  }
               }
               //System.out.println("Total emission prob for "+from+" is "+Math.exp(sum));
               if (sum!=Double.NEGATIVE_INFINITY) {
                  for (int t=0; t<emit[from].length; t++) {
                     new_emit[from][t] -= sum;
                  }
               }
            }
         }*/
         
         // This never gets used but set it to zero for reporting 
         new_p[0][0] = Double.NEGATIVE_INFINITY;
         
         if (trainTracer!=null) {
            trainTracer.notifyLastLogLikelihood(logLikelihood);
         }
         
         change = lastLogLikelihood==Double.NEGATIVE_INFINITY ? -logLikelihood : logLikelihood-lastLogLikelihood;

         if (change>=delta) {
            if (trainTracer!=null) {
               // TODO: we aren't training the n->0 transitions and
               // so this fixes the output

               // End states
               /*
               for (int from=1; from<new_p.length; from++) {
                  new_p[from][0] = p[from][0];
               }*/
               // Start states
               /*
               for (int to=1; to<new_p[0].length; to++) {
                  new_p[0][to] = p[0][to];
               }*/
               trainTracer.notifyChange(p,new_p, emit, new_emit);
            }

            // Calculate change & assign new values
            // TODO: work in log space
            //change = 0;

            // initial and final states
            if (trainStart) {
               // start transitions
               for (int to=1; to<p.length; to++) {
                  // calculate change for state transition
                  //double next_change = Math.abs(Math.exp(p[0][to])-Math.exp(new_p[0][to]));
                  //if (next_change>change) {
                  //   change = next_change;
                  //}

                  // assign new value
                  p[0][to] = new_p[0][to];

               }
               // end transitions
               /* This values aren't correct... I think...
               for (int from=1; from<p.length; from++) {
                  // calculate change for state transition
                  double next_change = Math.abs(Math.exp(p[from][0])-Math.exp(new_p[from][0]));
                  if (next_change>change) {
                     change = next_change;
                  }

                  // assign new value
                  p[from][0] = new_p[from][0];

               }*/
            }

            // k->n transitions
            for (int from=1; from<p.length; from++) {

               // state transitions
               if (trainTransitions) {
                  for (int to=1; to<p.length; to++) {

                     // calculate change for state transition
                     //double next_change = Math.abs(Math.exp(p[from][to])-Math.exp(new_p[from][to]));
                     //if (next_change>change) {
                     //   change = next_change;
                     //}

                     // assign new value
                     p[from][to] = new_p[from][to];
                  }
               }

               // state emissions
               if (trainEmissions) {
                  for (int t=0; t<emit[from].length; t++) {
                     // calculate change 
                     //double next_change = Math.abs(Math.exp(emit[from][t])-Math.exp(new_emit[from][t]));
                     //if (next_change>change) {
                     //   change = next_change;
                     //}

                     // assign new value
                     emit[from][t] = new_emit[from][t];
                  }
               }
            }

            // adjust probabilities to make sure they all add up to one

            /* we already did this for new_p and new_emit
            for (int from=0; from<p.length; from++) {
               double sum = Double.NEGATIVE_INFINITY;
               for (int to=1; to<p.length; to++) {
                  if (p[from][to]!=Double.NEGATIVE_INFINITY) {
                     if (sum==Double.NEGATIVE_INFINITY) {
                        sum = p[from][to];
                     } else {
                        sum = sum + Math.log(1+Math.exp(p[from][to]-sum));
                     }
                  }               
               }
               //System.out.println("Total transition prob for "+from+" is "+Math.exp(sum));
               for (int to=1; to<p.length; to++) {
                  p[from][to] -= sum;
               }

               if (from!=0) {
                  sum = Double.NEGATIVE_INFINITY;
                  for (int t=0; t<emit[from].length; t++) {
                     if (emit[from][t]!=Double.NEGATIVE_INFINITY) {
                        if (sum==Double.NEGATIVE_INFINITY) {
                           sum = emit[from][t];
                        } else {
                           sum = sum + Math.log(1+Math.exp(emit[from][t]-sum));
                        }
                     }
                  }
                  //System.out.println("Total emission prob for "+from+" is "+Math.exp(sum));
                  if (sum!=Double.NEGATIVE_INFINITY) {
                     for (int t=0; t<emit[from].length; t++) {
                        emit[from][t] -= sum;
                     }
                  }
               }
            }*/
         }

         if (log.isLoggable(Level.FINE)) {
            log.fine("Delta       : "+change);
         }
         
         if (log.isLoggable(Level.FINE)) {
            for (int from=0; from<p.length; from++) {
               for (int to=1; to<p.length; to++) {
                  double value = p[from][to]==Double.NEGATIVE_INFINITY ? 0 : Math.exp(p[from][to]);
                  log.info("P "+from+"->"+to+" "+value+" "+p[from][to]);
               }
               for (int t=0; t<emit[from].length; t++) {
                  double value = emit[from][t]==Double.NEGATIVE_INFINITY ? 0 : Math.exp(emit[from][t]);
                  log.info(from+": E["+t+"] = "+value);
               }
            }
            for (int from=1; from<p.length; from++) {
               double value = p[from][0]==Double.NEGATIVE_INFINITY ? 0 : Math.exp(p[from][0]);
               log.info("P "+from+"->0 "+value+" "+p[from][0]);
            }
         }
         lastLogLikelihood = logLikelihood;
         
      } while (change>delta);
      
   }
   
}
