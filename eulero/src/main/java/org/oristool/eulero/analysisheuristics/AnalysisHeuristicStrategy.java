package org.oristool.eulero.analysisheuristics;

import org.oristool.eulero.graph.*;
import org.oristool.eulero.math.approximation.Approximator;
import org.oristool.eulero.ui.ActivityViewer;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AnalysisHeuristicStrategy {
    private final String heuristicName;
    private final BigInteger CThreshold;
    private final BigInteger RThreshold;
    private final Approximator approximator;

    public AnalysisHeuristicStrategy(String heuristicName, BigInteger CThreshold, BigInteger RThreshold, Approximator approximator){
        this.heuristicName = heuristicName;
        this.CThreshold = CThreshold;
        this.RThreshold = RThreshold;
        this.approximator = approximator;
    }

    public abstract double[] analyze(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars);

    public BigInteger CThreshold() {
        return CThreshold;
    }

    public BigInteger RThreshold() {
        return RThreshold;
    }

    public Approximator approximator() {
        return approximator;
    }

    public String heuristicName() {
        return heuristicName;
    }

    public double[] numericalXOR(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars){
        double[] solution = new double[timeLimit.divide(step).intValue() + 1];
        System.out.println(tabSpaceChars + " Numerical XOR Analysis of " + model.name());

        for(Activity act: ((Xor) model).alternatives()){
            double[] activityCDF = analyze(act, timeLimit, step, error, tabSpaceChars + "---");
            double prob = ((Xor) model).probs().get(((Xor) model).alternatives().indexOf(act));
            for(int t = 0; t < solution.length; t++){
                solution[t] += prob * activityCDF[t];
            }
        }

        return solution;
    }

    public double[] analyze(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error) {
        double[] analysisResult = this.analyze(model, timeLimit, step, error, "---");
        return analysisResult;
    }

    public double[] numericalAND(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars){
        double[] solution = new double[timeLimit.divide(step).intValue() + 1];

        System.out.println(tabSpaceChars + " Numerical AND Analysis of " + model.name());
        Arrays.fill(solution, 1.0);
        for(Activity act: ((AND) model).activities()){
            double[] activityCDF = analyze(act, timeLimit, step, error, tabSpaceChars + "---");
            for(int t = 0; t < solution.length; t++){
                solution[t] *= activityCDF[t];
            }
        }
        return solution;
    }

    public double[] numericalSEQ(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars){
        double[] solution = new double[timeLimit.divide(step).intValue() + 1];

        System.out.println(tabSpaceChars + " Numerical SEQ Analysis of " + model.name());

        for (Activity act : ((SEQ) model).activities()) {

            if (act.equals(((SEQ) model).activities().get(0))) {
                solution = analyze(act, timeLimit, step, error, tabSpaceChars + "---");
            } else {
                double[] convolution = new double[solution.length];
                double[] activityCDF = analyze(act, timeLimit, step, error, tabSpaceChars + "---");

                for (int x = 1; x < solution.length; x++) {
                    for (int u = 1; u <= x; u++)
                        convolution[x] += (solution[u] - solution[u - 1]) * (activityCDF[x - u + 1] + activityCDF[x - u]) * 0.5;
                }

                solution = convolution;
            }
        }
        return solution;
    }

    public void REPInnerBlockAnalysis(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars){
        Activity repeatBody = ((Repeat) model).repeatBody();
        Analytical replacingBody = new Analytical(repeatBody.name() + "_new",
                approximator().getApproximatedStochasticTransitionFeature(
                        analyze(repeatBody, /* è giusto? -->*/ repeatBody.LFT(), step, error), repeatBody.EFT().doubleValue(), repeatBody.LFT().doubleValue(), step)
                );

        ((Repeat) model).repeatBody().replace(replacingBody);

        //TODO Place Return here
    }

    public double[] DAGInnerBlockAnalysis(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars){
        // prendo le attività composte con complessità non banale (escludo attività semplici e IMM)
        ArrayList<Activity> innerActivities = ((DAG) model).activitiesBetween(((DAG) model).begin(), ((DAG) model).end())
                .stream().filter(t -> (t.C().doubleValue() > 1 && t.R().doubleValue() > 1)).distinct().sorted(Comparator.comparing(Activity::C).thenComparing(Activity::R)).collect(Collectors.toCollection(ArrayList::new));

        System.out.println(tabSpaceChars + "---"  + " Block Analysis: Choose inner block " + innerActivities.get(innerActivities.size() - 1).name());

        innerActivities.get(innerActivities.size() - 1).replace(
                new Analytical(innerActivities.get(innerActivities.size() - 1).name() + "_N",
                        approximator().getApproximatedStochasticTransitionFeatures(analyze(innerActivities.get(innerActivities.size() - 1), innerActivities.get(innerActivities.size() - 1).LFT(), step, error, tabSpaceChars + "---"  ),
                                innerActivities.get(innerActivities.size() - 1).EFT().doubleValue(), innerActivities.get(innerActivities.size() - 1).LFT().doubleValue(), step),
                        approximator().stochasticTransitionFeatureWeights())
        );

        model.resetComplexityMeasure();

        return this.analyze(model, timeLimit, step, error, tabSpaceChars);
    }

    public double[] InnerBlockReplicationAnalysis(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars){

        ArrayList<DAG> innerBlocks = new ArrayList<>();
        ArrayList<DAG> sortedInnerBlocks = new ArrayList<>();
        int counter = 0;
        for(Activity activity: ((DAG) model).end().pre()){
            DAG innerBlock = ((DAG) model).copyRecursive(((DAG) model).begin(), activity, "_N_act_" + counter);
            innerBlocks.add(innerBlock);
            innerBlock.C();
            innerBlock.R();
            sortedInnerBlocks.add(innerBlock);
            counter++;
        }

        sortedInnerBlocks.sort(Comparator.comparing(Activity::C).thenComparing(Activity::R));

        DAG nestedDAG = ((DAG) model).nest(((DAG) model).end().pre().get(innerBlocks.indexOf(sortedInnerBlocks.get(sortedInnerBlocks.size() - 1))));
        System.out.println(tabSpaceChars + "---"  + " Block Analysis: Choose nested block of " + nestedDAG.end().pre().get(0));
        nestedDAG.setEFT(nestedDAG.low());
        nestedDAG.setLFT(nestedDAG.upp());
        nestedDAG.replace(
                new Analytical(nestedDAG.name() + "_from_" + sortedInnerBlocks.get(sortedInnerBlocks.size() - 1).name(),
                        approximator().getApproximatedStochasticTransitionFeatures(
                                analyze(nestedDAG, nestedDAG.LFT(), step, error, tabSpaceChars + "---"  ),
                                nestedDAG.EFT().doubleValue(),
                                nestedDAG.LFT().doubleValue(),
                                step
                        ),
                        approximator.stochasticTransitionFeatureWeights()
                )
        );

        model.resetComplexityMeasure();
        return this.analyze(model, timeLimit, step, error, tabSpaceChars);
    }

    public double[] regenerativeTransientAnalysis(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars){
        System.out.println(tabSpaceChars + " Regenerative Analysis of the selected block");
        TransientSolution<DeterministicEnablingState, RewardRate> transientSolution = model.analyze(timeLimit.toString(), step.toString(), error.toString());
        System.out.println(tabSpaceChars +  " Analysis done...");
        double[] solution = new double[transientSolution.getSolution().length];
        for(int i = 0; i < solution.length; i++){
            solution[i] = transientSolution.getSolution()[i][0][0];
        }

        return solution;
    }
}
