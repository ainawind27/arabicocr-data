package HMM3;

/**
 * This HMM library is from here:
 * 
 * https://github.com/Sciss/SpeechRecognitionHMM/blob/master/src/main/java/org/ioe/tprsa/classify/speech/HiddenMarkov.java
 */

import java.io.Serializable;

public class HiddenMarkov implements Serializable{
    /**
     * Every serializable classes must have this
     */
    private static final long serialVersionUID = 1L;
    /**
     * minimum probability
     */
    final double MIN_PROBABILITY = 0.0001;
    /**
     * length of observation sequence
     */
    protected int len_obSeq;
    /**
     * number of state in the model example: number of urns
     */
    protected int num_states;
    /**
     * number of observation symbols per state example: how many different
     * colour balls there are
     */
    protected int num_symbols;
    /**
     * number of states the model is allowed to jump
     */
    protected final int delta = 2;
    /**
     * discrete set of observation symbols example: sequence of colour of balls
     */
    protected int obSeq[][];
    /**
     * current observation sequence
     */
    protected int currentSeq[];
    /**
     * number of observation sequence
     */
    protected int num_obSeq;
    /**
     * state transition probability example: probability from one state to another state
     */
    protected double transition[][];
    /**
     * discrete output probability example: probability of a specific output from a state
     */
    protected double output[][];
    /**
     * initial state distribution example: which state is the starting state
     */
    protected double pi[];
    /**
     * forward variable alpha
     */
    protected double alpha[][];
    /**
     * backward variable beta
     */
    protected double beta[][];
    /**
     * Scale Coefficient
     */
    protected double scaleFactor[];
    /**
     * best state sequence
     */
    public int q[];
    
    /**
     * Empty constructor for serializing
     */
    public HiddenMarkov() {

    }
    
    /**
     * viterbi algorithm used to get best state sequence and probability<br>
     * calls: none<br> called by: volume
     *
     * @param testSeq test sequence
     * @return probability
     */
    public double viterbi(int testSeq[]) {
        //System.out.println("tes seq");
        //ArrayWriter.printIntArrayToConole(testSeq);
        setObSeq(testSeq);
        double phi[][] = new double[len_obSeq][num_states];     //frame x 6
        
        //variable for viterbi algorithm
        int[][] psi = new int[len_obSeq][num_states];   //frame x 6
        q = new int[len_obSeq]; //frame
        
        // initial
        for (int i = 0; i < num_states; i++) { //state = 6
            double temp = pi[i];
            if (temp == 0) {
                temp = MIN_PROBABILITY;
            }
            //System.out.println("Out "+output[i][currentSeq[0]]+" cur"+currentSeq[0]);
            phi[0][i] = Math.log(temp) + Math.log(output[i][currentSeq[0]]);
            psi[0][i] = 0;
        }
        //System.out.println("");
        //ArrayWriter.print2DTabbedDoubleArrayToConole(phi);
        
        // find best sequence
        for (int t = 1; t < len_obSeq; t++) { //frame
            for (int j = 0; j < num_states; j++) { //6 state
                double max = phi[t - 1][0] + Math.log(transition[0][j]);
                double temp;
                int index = 0;

                for (int i = 1; i < num_states; i++) {  //6 state
                    temp = phi[t - 1][i] + Math.log(transition[i][j]);
                    if (temp > max) {
                        max = temp;
                        index = i;
                    }
                }
                //System.out.println(index+" | max :"+max +"| "+Math.log(output[j][currentSeq[t]]));
                phi[t][j] = max +  Math.log(output[j][currentSeq[t]]);  //fill phi value
                psi[t][j] = index; //fill best index state
            }
            //System.out.println("");
        }
        //ArrayWriter.print2DTabbedDoubleArrayToConole(phi);
        double max = phi[len_obSeq - 1][0];
        double temp;
        int index = 0;
        // find best index from last phi
        // terminate method
        for (int i = 1; i < num_states; i++) { //6 state
            temp = phi[len_obSeq - 1][i];
            if (temp > max) {
                max = temp;
                index = i;
            }
        }
        //System.out.println("Indeks :"+index);

        q[len_obSeq - 1] = index; //best last
        // backward moving
        for (int t = len_obSeq - 2; t >= 0; t--) {
            q[t] = psi[t + 1][q[t + 1]];
            
        }
        //ArrayWriter.printIntArrayToConole(q);
        return max;
    }

    /**
     * rescale backward variable beta to prevent underflow<br> calls: none<br>
     * called by: HiddenMarkov
     *
     * @param t index number of backward variable beta
     */
    private void rescaleBeta(int t) {
        for (int i = 0; i < num_states; i++) {
            beta[t][i] *= scaleFactor[t];
        }
    }

    /**
     * rescale forward variable alpha to prevent underflow<br> calls: none<br>
     * called by: HiddenMarkov
     *
     * @param t index number of forward variable alpha
     */
    private void rescaleAlpha(int t) {
        // calculate scale coefficients
        for (int i = 0; i < num_states; i++) {
            scaleFactor[t] += alpha[t][i];
        }

        scaleFactor[t] = 1 / scaleFactor[t];

        // apply scale coefficients
        for (int i = 0; i < num_states; i++) {
            alpha[t][i] *= scaleFactor[t];
        }
    }

    /**
     * calculate forward variable alpha<br> calls: none<br> called by:
     * HiddenMarkov
     *
     * @return probability
     */
    protected double computeAlpha() {
        // Pr(obSeq | model); Probability of the observation sequence given the hmm model
        double probability = 0;

        // reset scaleFactor[]
        for (int t = 0; t < len_obSeq; t++) {
            scaleFactor[t] = 0;
        }

        // Initialization: Calculating all alpha variables at time = 0
        for (int i = 0; i < num_states; i++) {
            // System.out.println("current  "+i+" crr  "+currentSeq[0]);
            alpha[0][i] = pi[i] * output[i][currentSeq[0]];
        }
        rescaleAlpha(0);

        // Induction:
        for (int t = 0; t < len_obSeq - 1; t++) {
            for (int j = 0; j < num_states; j++) {

                // Sum of all alpha[t][i] * transition[i][j]
                double sum = 0;

                // Calculate sum of all alpha[t][i] * transition[i][j], 0 <= i < num_states
                for (int i = 0; i < num_states; i++) {
                    sum += alpha[t][i] * transition[i][j];
                }

                alpha[t + 1][j] = sum * output[j][currentSeq[t + 1]];
            }
            rescaleAlpha(t + 1);
        }
        // Termination: Calculate Pr(obSeq | model)
        for (int i = 0; i < num_states; i++) {
            probability += alpha[len_obSeq - 1][i];
        }

        probability = 0;
        // double totalScaleFactor = 1;
        for (int t = 0; t < len_obSeq; t++) {
            // System.out.println("s: " + Math.log(scaleFactor[t]));
            probability += Math.log(scaleFactor[t]);
            // totalScaleFactor *= scaleFactor[t];
        }
        return -probability;
        // return porbability / totalScaleFactor;
    }

    /**
     * calculate backward variable beta for later use with Re-Estimation
     * method<br> calls: none<br> called by: HiddenMarkov
     */
    protected void computeBeta() {
        /**
         * Initialization: Set all beta variables to 1 at time = len_obSeq - 1
         */
        for (int i = 0; i < num_states; i++) {
            beta[len_obSeq - 1][i] = 1;
        }
        rescaleBeta(len_obSeq - 1);

        /**
         * Induction:
         */
        for (int t = len_obSeq - 2; t >= 0; t--) {
            for (int i = 0; i < num_states; i++) {
                for (int j = 0; j < num_states; j++) {
                    beta[t][i] += transition[i][j] * output[j][currentSeq[t + 1]] * beta[t + 1][j];
                }
            }
            rescaleBeta(t);
        }
    }

    /**
     * set training sequences for re-estimation step<br> calls: none<br> called
     * by: trainHMM
     *
     * @param trainSeq training sequences
     */
    public void setTrainSeq(int trainSeq[][]) {
        num_obSeq = trainSeq.length;
        obSeq = new int[num_obSeq][];// /ADDED
        // System.out.println("num obSeq << setTrainSeq()    "+num_obSeq);
        System.arraycopy(trainSeq, 0, obSeq, 0, num_obSeq);
        
        System.out.println("----obseq----"+obSeq.length+"x"+obSeq[0].length);
        //ArrayWriter.print2DTabbedIntArrayToConole(obSeq);
    }

    /**
     * train the hmm model until no more improvement<br> calls: none<br> called
     * by: trainHMM
     */
    public void train() {
        // re-estimate 25 times
        // NOTE: should be changed to re-estimate until no more improvement
        
        double oldTransition[][];
        double oldOutput[][];
        int i = 0;
        while (i<25) {
            i++;
            System.out.println("reestimating ke" + i);
            oldTransition = transition;
            oldOutput = output;
            reestimate();
        }
    }

    /**
     * Baum-Welch Algorithm - Re-estimate (iterative update and improvement) of
     * HMM parameters<br> calls: none<br> called by: trainHMM
     */
    private void reestimate() {
        // new probabilities that will be the optimized and replace the older version
        double newTransition[][]    = new double[num_states][num_states];
        double newOutput[][]        = new double[num_states][num_symbols];
        double numerator[]          = new double[num_obSeq];
        double denominator[]        = new double[num_obSeq];

        // calculate new transition probability matrix
        double sumP = 0;

        for (int i = 0; i < num_states; i++) {
            for (int j = 0; j < num_states; j++) {

                if (j < i || j > i + delta) { //same with randomProb()
                    newTransition[i][j] = 0;
                } else {
                    for (int k = 0; k < num_obSeq; k++) { //looping N-file
                        numerator[k] = denominator[k] = 0;
                        setObSeq(obSeq[k]); //from single wav

                        sumP += computeAlpha();
                        computeBeta();
                        for (int t = 0; t < len_obSeq - 1; t++) { //looping each frame
                            numerator[k]    += alpha[t][i] * transition[i][j] * output[j][currentSeq[t + 1]] * beta[t + 1][j];
                            denominator[k]  += alpha[t][i] * beta[t][i]; 
                        }
                    }
                    // numerator & denominator filled
                    double denom = 0;
                    for (int k = 0; k < num_obSeq; k++) { //looping N-file
                        newTransition[i][j] += (1 / sumP) * numerator[k];
                        denom += (1 / sumP) * denominator[k];
                    }
                    newTransition[i][j] /= denom;
                    newTransition[i][j] += MIN_PROBABILITY; // prevent underflow
                }
            }
        }

        // calculate new output probability matrix
        sumP = 0;
        for (int i = 0; i < num_states; i++) { // 6 state
            for (int j = 0; j < num_symbols; j++) {  // 256
                for (int k = 0; k < num_obSeq; k++) { //looping N-file
                    numerator[k] = denominator[k] = 0;
                    setObSeq(obSeq[k]);

                    sumP += computeAlpha();
                    computeBeta();
                    for (int t = 0; t < len_obSeq - 1; t++) { // 53 frame
                        if (currentSeq[t] == j) {
                            numerator[k] += alpha[t][i] * beta[t][i];
                        }
                        denominator[k] += alpha[t][i] * beta[t][i];
                    }
                }

                double denom = 0;
                for (int k = 0; k < num_obSeq; k++) {
                    newOutput[i][j] += (1 / sumP) * numerator[k];
                    denom += (1 / sumP) * denominator[k];
                }

                newOutput[i][j] /= denom;
                newOutput[i][j] += MIN_PROBABILITY;
            }
        }

        // replace old matrices after re-estimate
        transition = newTransition;
        output = newOutput;
    }

    /**
     * set observation sequence<br> calls: none<br> called by: Re-estimate
     * @param observationSeq observation sequence
     */
    public void setObSeq(int observationSeq[]) {
        currentSeq = observationSeq;
        len_obSeq = observationSeq.length;
        // System.out.println("len_obSeq<<setObSeq()   "+len_obSeq);

        alpha = new double[len_obSeq][num_states];
        beta = new double[len_obSeq][num_states];
        scaleFactor = new double[len_obSeq];
    }

    /**
     * class constructor - used to create a left-to-right model with multiple
     * observation sequences for training<br> calls: none<br> called by:
     * trainHMM
     *
     * @param num_states number of states in the model
     * @param num_symbols number of symbols per state
     */
    public HiddenMarkov(int num_states, int num_symbols) {
        this.num_states = num_states;
        this.num_symbols = num_symbols;
        transition = new double[num_states][num_states];
        output = new double[num_states][num_symbols];
        pi = new double[num_states];

        /**
         * in a left-to-right HMM model, the first state is always the initial
         * state. e.g. probability = 1
         */
        pi[0] = 1;
        for (int i = 1; i < num_states; i++) {
            pi[i] = 0;
        }

        // generate random probability for all the other probability matrices
        randomProb();
    }

    /**
     * generates random probabilities for transition, output probabilities<br>
     * calls: none<br> called by: HiddenMarkov corrected by GT
     */
    private void randomProb() {
        for (int i = 0; i < num_states; i++) {
            for (int j = 0; j < num_states; j++) {
                
                if (j < i || j > i + delta) {
                    transition[i][j] = 0;// R-L prob=0 for L-R HMM, and with
                    // Delta
                } else {
                    double randNum = Math.random();
                    transition[i][j] = randNum;
                    // System.out.println("transition init: "+transition[i][j]);
                }
            }
            for (int j = 0; j < num_symbols; j++) {
                double randNum = Math.random();
                output[i][j] = randNum;
                // System.out.println("outputInit: "+output[i][j]);
            }
        }
        //ArrayWriter.print2DTabbedDoubleArrayToConole(transition);
        //System.out.println("------");
        //ArrayWriter.print2DTabbedDoubleArrayToConole(output);
    }
    
    public void print() throws Exception {
        System.out.println("PI");
        for (int x = 0; x < pi.length; x++) {
            System.out.print(pi[x] + " ");
        }
        System.out.println("\nAlpha");
        for (int y = 0; y < alpha.length; y++) {
            for (int x = 0; x < alpha[y].length; x++) {
                System.out.print(alpha[y][x] + " ");
            }
            System.out.println("");
        }
        System.out.println("\nBeta");
        for (int y = 0; y < beta.length; y++) {
            for (int x = 0; x < beta[y].length; x++) {
                System.out.print(beta[y][x] + " ");
            }
            System.out.println("");
        }
        System.out.println("");
    }
}