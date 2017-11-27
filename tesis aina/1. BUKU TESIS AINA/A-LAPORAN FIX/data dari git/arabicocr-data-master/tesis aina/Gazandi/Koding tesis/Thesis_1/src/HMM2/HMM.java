package HMM2;

import java.io.Serializable;

public class HMM implements Serializable {

    /**
     * Every serializable classes must have this
     */
    private static final long serialVersionUID = 1L;
    /**
     * number of states
     */
    public static int numStates;
    /**
     * size of output vocabulary
     */
    public static int sigmaSize;
    /**
     * initial state probabilities
     */
    public double pi[];
    /**
     * transition probabilities
     */
    public double a[][];
    /**
     * emission probabilities
     */
    public double b[][];
    /**
     * Just because
     */
    public double c[];

    /**
     * Empty constructor for serializing
     */
    public HMM() {

    }

    /**
     * initializes an HMM.
     *
     * @param numStates number of states
     * @param sigmaSize size of output vocabulary
     */
    public HMM(int numStates, int sigmaSize) {
        HMM.numStates = numStates;
        HMM.sigmaSize = sigmaSize;

        pi = new double[numStates];
        a = new double[numStates][numStates];
        b = new double[numStates][sigmaSize];

        init();
    }

    /**
     * Initialize pi, a and b
     */
    private void init() {
        // init pi
        for (int i = 0; i < numStates; i++) {
            pi[i] = 1.0 / numStates;
        }
        // init a
        for (int i = 0; i < numStates; i++) {
            for (int j = 0; j < numStates; j++) {
                a[i][j] = 1.0 / numStates;
            }
        }
        // init b
        for (int i = 0; i < numStates; i++) {
            for (int j = 0; j < sigmaSize; j++) {
                b[i][j] = 1.0 / sigmaSize;
            }
        }
    }

    /**
     * Forward algorithm
     *
     * @param o
     * @param alpha
     * @return
     */
    public double[][] alphaPass(int[] o, double[][] alpha) {
        // init alpha and c
        c[0] = 0;
        // compute alpha[0][i]
        for (int i = 0; i < numStates; i++) {
            alpha[0][i] = pi[i] * b[i][o[0]];
            c[0] += alpha[0][i];
        }
        // scale the alpha[0][i]
        c[0] = 1.0 / c[0];
        for (int i = 0; i < numStates; i++) {
            alpha[0][i] *= c[0];
        }
        // compute alpha[t][i]
        for (int t = 1; t < o.length; t++) {
            c[t] = 0;
            for (int i = 0; i < numStates; i++) {
                alpha[t][i] = 0;
                for (int j = 0; j < numStates; j++) {
                    alpha[t][i] += alpha[t - 1][j] * a[j][i];
                }
                alpha[t][i] = alpha[t][i] * b[i][o[t]];
                c[t] += alpha[t][i];
            }
            // scale alpha[t][i]
            c[t] = 1.0 / c[t];
            for (int i = 0; i < numStates; i++) {
                alpha[t][i] *= c[t];
            }
        }
        return alpha;
    }

    /**
     * Backward algorithm
     *
     * @param o
     * @param beta
     * @return
     */
    public double[][] betaPass(int[] o, double[][] beta) {
        // let beta[t-1][i] = 1, scaled by c[t-1]
        for (int i = 0; i < numStates; i++) {
            beta[o.length - 1][i] = c[o.length - 1];
        }
        // beta-pass
        for (int t = o.length - 2; t >= 0; t--) {
            for (int i = 0; i < numStates; i++) {
                beta[t][i] = 0;
                for (int j = 0; j < numStates; j++) {
                    beta[t][i] += a[i][j] * b[j][o[t + 1]] * beta[t + 1][j];
                }
                // scale beta[t][i] with same scale factor as alpha[t][i]
                beta[t][i] *= c[t];
            }
        }
        return beta;
    }

    /**
     * Computes gamma series
     *
     * @param o
     * @param alpha
     * @param beta
     * @return
     */
    public double[][][] gammaSeries(int[] o, double[][] alpha, double[][] beta, double[][][] gamma) {
        // init components
        double denom;
        // compute gamma
        for (int t = 0; t < o.length - 1; t++) {
            denom = 0.0;
            for (int i = 0; i < numStates; i++) {
                for (int j = 0; j < numStates; j++) {
                    denom += alpha[t][i] * a[i][j] * b[j][o[t + 1]] * beta[t + 1][j];
                }
            }
            for (int i = 0; i < numStates; i++) {
                for (int k = 0; k < numStates; k++) {
                    gamma[t][i][k] = 0;
                }
                for (int j = 0; j < numStates; j++) {
                    gamma[t][i][j] = (alpha[t][i] * a[i][j] * b[j][o[t + 1]] * beta[t + 1][j]) / denom;
                    for (int k = 0; k < numStates; k++) {
                        gamma[t][i][k] += gamma[t][i][j];
                    }
                }
            }
        }
        // special case for gamma[t-1][i]
        denom = 0.0;
        for (int i = 0; i < numStates; i++) {
            denom += alpha[o.length - 1][i];
        }
        for (int i = 0; i < numStates; i++) {
            for (int j = 0; j < numStates; j++) {
                gamma[o.length - 1][i][j] = alpha[o.length - 1][i] / denom;
            }
        }
        return gamma;
    }

    /**
     * Trains data
     *
     * @param o
     * @param steps
     */
    public void train(int[] o, int steps) {
        // init components
        double denom, numer;
        double[][] alpha = new double[o.length][numStates];
        double[][] beta = new double[o.length][numStates];
        double[][][] gamma = new double[o.length][numStates][numStates];
        c = new double[o.length];
        // do the steps
        for (int step = 0; step < steps; step++) {
            // forward pass
            alpha = alphaPass(o, alpha);
            // backward pass
            beta = betaPass(o, beta);
            // gamma series
            gamma = gammaSeries(o, alpha, beta, gamma);
            // re-estimate a, b and pi
            //// re-estimate pi
            for (int i = 0; i < numStates; i++) {
                for (int j = 0; j < numStates; j++) {
                    pi[i] = gamma[0][i][j];
                }
            }
            //// re-estimate a
            for (int i = 0; i < numStates; i++) {
                for (int j = 0; j < numStates; j++) {
                    numer = 0.0;
                    denom = 0.0;
                    for (int t = 0; t < o.length - 1; t++) {
                        numer += gamma[t][i][j];
                        for (int k = 0; k < numStates; k++) {
                            denom += gamma[t][i][k];
                        }
                    }
                    a[i][j] = numer / denom;
                }
            }
            //// re-estimate b
            for (int i = 0; i < numStates; i++) {
                for (int j = 0; j < sigmaSize; j++) {
                    numer = 0.0;
                    denom = 0.0;
                    for (int t = 0; t < o.length - 1; t++) {
                        if (o[t] == j) {
                            for (int k = 0; k < numStates; k++) {
                                numer += gamma[t][i][k];
                            }
                        }
                        for (int k = 0; k < numStates; k++) {
                            denom += gamma[t][i][k];
                        }
                    }
                    b[i][j] = numer / denom;
                }
            }
        }
    }

    /**
     * Checks how similar a test data to the models
     *
     * @param pi
     * @param a
     * @param b
     * @param o
     * @return
     */
    public static double similarity(double[] pi, double[][] a, double[][] b, int[] o) {
        int N = a.length;
        int T = o.length;
        double[][] sim = new double[T][N];
        // introduction
        for (int i = 0; i < N; i++) {
            sim[0][i] = pi[i] * b[i][o[1]];
        }
        // recursion
        for (int t = 1; t < T; t++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    sim[t][j] = sim[t - 1][k] * a[k][j] * b[j][o[t]];
                }
            }
        }
        // sum of every row
        double[] sim_row = new double[T];
        for (int i = 0; i < T; i++) {
            for (int j = 0; j < N; j++) {
                sim_row[i] += sim[i][j];
            }
        }
        // product of every row
        double result = 1.0;
        for (int i = 0; i < T; i++) {
            result *= (sim_row[i] != 0) ? sim_row[i] : 1;
        }
        return result;
    }
}
