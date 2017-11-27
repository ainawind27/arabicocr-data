package com.milowski.hmm;

import java.util.*;

/**
 * This class represents a HMM model.  It provides the basis to
 * manipulate the alphabet, states, transitions and emission probabilities.
 * @author  R. Alexander Milowski
 */
public class Model {

   /**
    * A translator for each character of the alphabet into a character code.
    */
   public interface Translator {
      short translate(char c);
   }

   static private class LexiconTranslator implements Translator {
      char [] alphabet;

      LexiconTranslator(List lexicon) {
         alphabet = new char[lexicon.size()];
         for (int i=0; i<lexicon.size(); i++) {
            alphabet[i] = ((Character)lexicon.get(i)).charValue();
         }
      }

      public short translate(char c) {
         for (short i=0; i<alphabet.length; i++) {
            if (alphabet[i]==c) {
               return i;
            }
         }
         return -1;
      }

   }

   List lexicon;
   List stateNames;
   String name;
   int alphabetSize;
   double [][] stateTransitions;
   double [][] emissions;
   double error;

   /**
    * Constructs a model with a default number of states.
    * @param name The name of the HMM.
    * @param alphabetSize The alphabet size.
    * @param numberOfStates The number of states.
    * @param error The precision of the model (for training purposes).
    */
   public Model(String name,int alphabetSize,int numberOfStates,double error) {
      this.name = name;
      this.error = error;
      this.alphabetSize = alphabetSize;
      this.lexicon = null;
      if (alphabetSize==0) {
         throw new IllegalArgumentException("The alphabet size cannot be zero.");
      }
      if (numberOfStates==0) {
         throw new IllegalArgumentException("The number of states cannot be zero.");
      }
      if (alphabetSize>Short.MAX_VALUE) {
         throw new IllegalArgumentException("The alphabet size cannot be greater than "+Short.MAX_VALUE);
      }
      if (numberOfStates>(Short.MAX_VALUE-1)) {
         throw new IllegalArgumentException("The alphabet size cannot be greater than "+(Short.MAX_VALUE-1));
      }
      this.emissions = new double[numberOfStates+1][];
      // We've got the initial and final state transitions in this matrix too
      this.stateTransitions = new double[numberOfStates+1][];
      for (int i=0; i<stateTransitions.length; i++) {
         emissions[i] = new double[alphabetSize];
         for (int j=0; j<emissions[i].length; j++) {
            emissions[i][j] = 0;
         }
         stateTransitions[i] = new double[numberOfStates+1];
         for (int j=0; j<stateTransitions[i].length; j++) {
            stateTransitions[i][j] = 0;
         }
      }
      this.stateNames = new ArrayList();
      stateNames.add("");
      for (int i=0; i<numberOfStates; i++) {
         stateNames.add("");
      }
   }

   public String getName() {
      return name;
   }

   public int getAlphabetSize() {
      return alphabetSize;
   }

   public void setLexicon(List values) {
      this.lexicon = values;
   }

   public List getLexicon() {
      return lexicon;
   }

   public Translator getTranslator() {
      return new LexiconTranslator(lexicon);
   }

   public int getNumberOfStates() {
      return stateTransitions.length;
   }

   public double [][] getStateTransitions() {
      return stateTransitions;
   }

   public void setStateName(int state,String name) {
      stateNames.set(state,name);
   }

   public String getStateName(int state) {
      return (String)stateNames.get(state);
   }

   public void setStateTransition(int from, int to,double value) {
      stateTransitions[from][to] = value;
   }

   public double [][] getStateEmissions() {
      return emissions;
   }

   public void setStateEmission(int state,int achar,double value) {
      emissions[state][achar] = value;
   }
   
   public void randomize(boolean doInitial,boolean doStates, boolean doEmissions) {
      if (doInitial) {
         // Handle initial states
         double remaining = 1;
         for (int to=1; to<stateTransitions[0].length; to++) {
            stateTransitions[0][to] = (to+1)==stateTransitions[0].length ? remaining : Math.random()*remaining;
            remaining -= stateTransitions[0][to];
         }
         remaining = 1;
         for (int from=1; from<stateTransitions.length; from++) {
            stateTransitions[from][0] = (from+1)==stateTransitions.length ? remaining : Math.random()*remaining;
            remaining -= stateTransitions[from][0];
         }
      }
      if (doStates) {
         for (int from=1; from<stateTransitions.length; from++) {
            double remaining = 1;
            for (int to=1; to<stateTransitions[from].length; to++) {
               stateTransitions[from][to] = (to+1)==stateTransitions[from].length ? remaining : Math.random()*remaining;
               remaining -= stateTransitions[from][to];
            }
         }
      }
      
      if (doEmissions) {
         for (int from=1; from<emissions.length; from++) {
            double remaining = 1;
            for (int ch=0; ch<emissions[from].length; ch++) {
               emissions[from][ch] = (ch+1)==emissions[from].length ? remaining : Math.random()*remaining;
               remaining -= emissions[from][ch];
            }
         }
      }
   }

   public double getError() {
      return error;
   }

   public void check() 
      throws IllegalStateException
   {

      if (stateTransitions[0][0]!=0) {
         throw new IllegalStateException("Transition from 0->0 is not allow to have a probability.");
      }
      for (int from=1; from<stateTransitions.length; from++) {

         double total = 0;
         for (int to=1; to<stateTransitions.length; to++) {
            total += stateTransitions[from][to];
         }
         if ((1.0-total)>error) {
            throw new IllegalStateException("Illegal state transition probability total for state "+from+": "+total+"!=1.0");
         }

         if (from!=0) {
            total = 0;
            for (int t=0; t<emissions[from].length; t++) {
               total += emissions[from][t];
            }
            if ((1.0-total)>error) {
               throw new IllegalStateException("Illegal state emission probability total for state "+from+": "+total+"!=1.0");
            }
         }
         
      }
      
      double total = 0;
      for (int to=1; to<stateTransitions.length; to++) {
         total += stateTransitions[0][to];
      }
      if (total!=(stateTransitions.length-1) && (1.0-total)>error) {
         throw new IllegalStateException("0->n (initial to model) transitions must either all be 1 or sum to 1.");
      }

      total = 0;
      for (int from=1; from<stateTransitions.length; from++) {
         total += stateTransitions[from][0];
      }
      if (total!=(stateTransitions.length-1) && (1.0-total)>error) {
         throw new IllegalStateException("n->0 (final from model) transitions must either all be 1 or sum to 1.");
      }

   }

   /**
    * Generates a sequence of rolls from the model with the states of used to emit each roll.
    * @param length The length of the sequence to generate.
    * @return A double array of integers where the first array is the sequence of rolls and the second array is the states used to generate the rolls.
    */
   public int [][] generateSequence(int length) {
      double stateSpectrum [][] = new double[stateTransitions.length][];
      double emissionSpectrum [][] = new double[stateTransitions.length][];
      stateSpectrum[0] = new double[stateSpectrum.length];
      stateSpectrum[0][0] = 0;
      for (int j=1; j<stateSpectrum[0].length; j++) {
         stateSpectrum[0][j] = stateTransitions[0][j]+stateSpectrum[0][j-1];
         //System.out.println("0->"+j+"="+stateSpectrum[0][j]);
      }
      for (int i=1; i<stateSpectrum.length; i++) {
         stateSpectrum[i] = new double[stateSpectrum.length];
         stateSpectrum[i][1] = stateTransitions[i][1];
         //System.out.println(i+"->1 ="+stateSpectrum[i][1]);
         for (int j=2; j<stateSpectrum[i].length; j++) {
            stateSpectrum[i][j] = stateTransitions[i][j]+stateSpectrum[i][j-1];
            //System.out.println(i+"->"+j+" = "+stateSpectrum[i][j]);
         }
         stateSpectrum[i][stateSpectrum[i].length-1] = 1;
         emissionSpectrum[i] = new double[emissions[i].length];
         emissionSpectrum[i][0] = emissions[i][0];
         //System.out.println("emit("+i+",0)="+emissionSpectrum[i][0]);
         for (int j=1; j<emissions[i].length; j++) {
            emissionSpectrum[i][j] = emissions[i][j]+emissionSpectrum[i][j-1];
            //System.out.println("emit("+i+","+j+")="+emissionSpectrum[i][j]);
         }
         emissionSpectrum[i][emissionSpectrum[i].length-1] = 1;
      }

      int [][] result = new int[2][];
      result[0] = new int[length];
      result[1] = new int[length];
      double startProb = Math.random();
      //System.out.println("start prob="+startProb);
      int state;
      for (state=1; state<stateSpectrum[0].length && startProb>stateSpectrum[0][state]; state++);
      //System.out.println("start state="+state);
      for (int pos=0; pos<length; pos++) {
         // Emit for state
         double emitProb = Math.random();
         //System.out.println("emit prob="+emitProb);
         int emit;
         for (emit=0; emit<emissionSpectrum[state].length && emitProb>emissionSpectrum[state][emit]; emit++);
         result[0][pos] = emit;
         result[1][pos] = state;
         // Change state
         double transProb = Math.random();
         //System.out.println("trans prob="+transProb);
         int current = state;
         for (state=1; state<stateSpectrum[current].length && transProb>stateSpectrum[current][state]; state++);
      }
      
      return result;
   }
}