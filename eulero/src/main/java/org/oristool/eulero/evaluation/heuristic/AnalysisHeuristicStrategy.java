package org.oristool.eulero.evaluation.heuristic;

import org.apache.commons.lang3.tuple.Pair;
import org.oristool.eulero.workflow.*;
import org.oristool.eulero.evaluation.approximator.Approximator;
import org.oristool.eulero.ui.ActivityViewer;
import org.oristool.models.stpn.RewardRate;
import org.oristool.models.stpn.TransientSolution;
import org.oristool.models.stpn.trees.DeterministicEnablingState;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AnalysisHeuristicStrategy {
    private final String heuristicName;
    private final BigInteger CThreshold;
    private final BigInteger SThreshold;
    private final Approximator approximator;
    private final boolean plotIntermediate;
    private final boolean verbose;

    public AnalysisHeuristicStrategy(String heuristicName, BigInteger CThreshold, BigInteger SThreshold, Approximator approximator, boolean verbose, boolean plotIntermediate){
        this.heuristicName = heuristicName;
        this.CThreshold = CThreshold;
        this.SThreshold = SThreshold;
        this.approximator = approximator;
        this.plotIntermediate = plotIntermediate;
        this.verbose = verbose;
    }

    public AnalysisHeuristicStrategy(String heuristicName, BigInteger CThreshold, BigInteger RThreshold, Approximator approximator){
        this(heuristicName, CThreshold, RThreshold, approximator, false, false);
    }

    public AnalysisHeuristicStrategy(String heuristicName, BigInteger CThreshold, BigInteger RThreshold, Approximator approximator, boolean verbose){
        this(heuristicName, CThreshold, RThreshold, approximator, verbose, false);
    }


    public abstract double[] analyze(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars);

    public double[] analyze(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars){
        return analyze(model, timeLimit, step, BigDecimal.ONE, error, tabSpaceChars);
    }

    public double[] analyze(Activity model, BigDecimal timeLimit, BigDecimal step){
        return analyze(model, timeLimit, step, BigDecimal.ONE, BigDecimal.valueOf(0.001), "---");
    }

    public boolean verbose() { return verbose; }

    public BigInteger CThreshold() {
        return CThreshold;
    }

    public BigInteger SThreshold() {
        return SThreshold;
    }

    public Approximator approximator() {
        return approximator;
    }

    public String heuristicName() {
        return heuristicName;
    }

    public double[] analyze(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error) {
        return this.analyze(model, timeLimit, step, forwardReductionFactor, error, "---");
    }

    public double[] numericalXOR(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars, boolean verbose){
        double[] solution = new double[timeLimit.divide(step).intValue() + 1];

        if(verbose)
            System.out.println(tabSpaceChars + " Numerical XOR Analysis of " + model.name());

        long time = System.nanoTime();
        for(Activity act: ((Xor) model).alternatives()){
            double[] activityCDF = analyze(act, timeLimit, step, forwardReductionFactor, error, tabSpaceChars + "---");
            double prob = ((Xor) model).probs().get(((Xor) model).alternatives().indexOf(act));
            for(int t = 0; t < solution.length; t++){
                solution[t] += prob * activityCDF[t];
            }
        }

        if(verbose)
            System.out.println(tabSpaceChars +  " Analysis of " +  model.name() + " done in " + String.format("%.3f seconds",
                    (System.nanoTime() - time)/1e9) + "...");

        return solution;
    }

    public double[] numericalXOR(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        return numericalXOR(model, timeLimit, step, forwardReductionFactor, error, tabSpaceChars, false);
    }

    public double[] numericalAND(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        double[] solution = new double[timeLimit.divide(step).intValue() + 1];

        if(verbose)
            System.out.println(tabSpaceChars + " Numerical AND Analysis of " + model.name());

        long time = System.nanoTime();

        Arrays.fill(solution, 1.0);
        for(Activity act: ((AND) model).activities()){
            double[] activityCDF = analyze(act, timeLimit, step, forwardReductionFactor, error, tabSpaceChars + "---");
            for(int t = 0; t < solution.length; t++){
                solution[t] *= activityCDF[t];
            }
        }

        if(verbose)
            System.out.println(tabSpaceChars +  " Analysis of " +  model.name() + " done in " + String.format("%.3f seconds",
                (System.nanoTime() - time)/1e9) + "...");

        return solution;
    }

    public double[] numericalSEQ(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        double[] solution = new double[timeLimit.divide(step).intValue() + 1];

        if(verbose)
            System.out.println(tabSpaceChars + " Numerical SEQ Analysis of " + model.name());

        long time = System.nanoTime();

        for (Activity act : ((SEQ) model).activities()) {

            if (act.equals(((SEQ) model).activities().get(0))) {
                solution = analyze(act, timeLimit, step, forwardReductionFactor, error, tabSpaceChars + "---");
            } else {
                double[] convolution = new double[solution.length];
                double[] activityCDF = analyze(act, timeLimit, step, forwardReductionFactor, error, tabSpaceChars + "---");

                for (int x = 1; x < solution.length; x++) {
                    for (int u = 1; u <= x; u++)
                        convolution[x] += (solution[u] - solution[u - 1]) * (activityCDF[x - u + 1] + activityCDF[x - u]) * 0.5;
                }

                solution = convolution;
            }
        }

        if(verbose)
            System.out.println(tabSpaceChars +  " Analysis of " +  model.name() + " done in " + String.format("%.3f seconds",
                (System.nanoTime() - time)/1e9) + "...");

        return solution;
    }

    public double[] REPInnerBlockAnalysis(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        Activity repeatBody = ((Repeat) model).repeatBody();

        ArrayList<Pair<BigDecimal, StochasticTransitionFeature>> approximationFeature = approximator().getApproximatedStochasticTransitionFeatures(
                analyze(repeatBody, repeatBody.LFT(), step, forwardReductionFactor, error, tabSpaceChars + "---" ), repeatBody.EFT().doubleValue(), repeatBody.LFT().doubleValue(), step);

        Simple replacingBody = new Simple(repeatBody.name() + "_N",
                approximationFeature.stream().map(Pair::getRight).collect(Collectors.toCollection(ArrayList::new)),
                approximationFeature.stream().map(Pair::getLeft).collect(Collectors.toCollection(ArrayList::new))
        );

        if(verbose)
            System.out.println(repeatBody.name() + " has been approximated");
        if(plotIntermediate){
            TransientSolution<DeterministicEnablingState, RewardRate> simulate = model.simulate(timeLimit.toString(), step.toString(), 1000);
            double[] simulation = new double[simulate.getSolution().length];
            for(int i = 0; i < simulation.length; i++){
                simulation[i] = simulate.getSolution()[i][0][0];
            }

            ((Repeat) model).replaceBody(replacingBody);

            double[] simulation2 = this.analyze(model, timeLimit, step, forwardReductionFactor, error, tabSpaceChars);

            ActivityViewer.CompareResults("REP I.B.: " + model.name(), List.of("Real", "Appr"), List.of(new EvaluationResult("real", simulation, 0, simulation.length, step.doubleValue(), 0), new EvaluationResult("appr", simulation2, 0, simulation2.length, step.doubleValue(), 0)));
        } else {
            ((Repeat) model).replaceBody(replacingBody);
        }

        model.resetComplexityMeasure();
        return this.analyze(model, timeLimit, step, forwardReductionFactor, error, tabSpaceChars);
    }

    public void checkREPinDAG(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        TransientSolution<DeterministicEnablingState, RewardRate> simulate = null;

        if(plotIntermediate){
            simulate = model.simulate(timeLimit.toString(), step.toString(), 1000);
        }

        for(Activity act: ((DAG) model).activitiesBetween(((DAG) model).begin(), ((DAG) model).end())){
            if(act instanceof Repeat){
                if(verbose)
                    System.out.println(tabSpaceChars + " Repetition found! It's " + act.name());

                ArrayList<Pair<BigDecimal, StochasticTransitionFeature>> approximationFeature =  approximator().getApproximatedStochasticTransitionFeatures(
                        analyze(act, timeLimit, step, forwardReductionFactor, error, tabSpaceChars + "---"),
                        act.EFT().doubleValue(), timeLimit.min(act.LFT()).doubleValue(), step);

                Simple replacingActivity = new Simple(act.name() + "_N",
                        approximationFeature.stream().map(Pair::getRight).collect(Collectors.toCollection(ArrayList::new)),
                        approximationFeature.stream().map(Pair::getLeft).collect(Collectors.toCollection(ArrayList::new))
                );

                act.replace(replacingActivity);

                if(plotIntermediate){
                    double[] simulation = new double[simulate.getSolution().length];
                    for(int i = 0; i < simulation.length; i++){
                        simulation[i] = simulate.getSolution()[i][0][0];
                    }

                    TransientSolution<DeterministicEnablingState, RewardRate> simulate2 = model.simulate(timeLimit.toString(), step.toString(), 1000);
                    double[] simulation2 = new double[simulate2.getSolution().length];
                    for(int i = 0; i < simulation2.length; i++){
                        simulation2[i] = simulate2.getSolution()[i][0][0];
                    }

                    ActivityViewer.CompareResults("REP I.B after Check: " + model.name(), List.of("Real", "Appr"), List.of(new EvaluationResult("real", simulation, 0, simulation.length, step.doubleValue(), 0), new EvaluationResult("appr", simulation2, 0, simulation2.length, step.doubleValue(), 0)));
                }
                if(verbose)
                    System.out.println(tabSpaceChars + " Block " + act.name() + " replaced...");

                model.resetComplexityMeasure();
            }
        }
    }

    public double[] DAGInnerBlockAnalysisOld(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        //ArrayList<Activity> innerActivities = ((DAG) model).activitiesBetween(((DAG) model).begin(), ((DAG) model).end())
        //        .stream().filter(t -> (t.C().doubleValue() > 1 || t.S().doubleValue() > 1)).distinct().sorted(Comparator.comparing(Activity::C).thenComparing(Activity::S)).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Activity> innerActivities = ((DAG) model).activities().stream().filter(t -> (t.C().doubleValue() > 1 || t.Q().doubleValue() > 1)).distinct().sorted(Comparator.comparing(Activity::C).thenComparing(Activity::Q)).collect(Collectors.toCollection(ArrayList::new));
        double aux = innerActivities.get(innerActivities.size() - 1).LFT().doubleValue();
        int mag = 1;
        while (aux > 10) {
            mag = mag * 10;
            aux = aux / 10;
        }
        BigDecimal innerActivityStep = BigDecimal.valueOf(mag * Math.pow(10, -2));

        ArrayList<Pair<BigDecimal, StochasticTransitionFeature>> approximationFeature =  approximator().getApproximatedStochasticTransitionFeatures(
                analyze(innerActivities.get(innerActivities.size() - 1), innerActivities.get(innerActivities.size() - 1).LFT().precision() >= 309 ? timeLimit : innerActivities.get(innerActivities.size() - 1).LFT(), innerActivityStep, forwardReductionFactor, error, tabSpaceChars + "---"  ),
                innerActivities.get(innerActivities.size() - 1).EFT().doubleValue(), (innerActivities.get(innerActivities.size() - 1).LFT().precision() >= 309 ? timeLimit : innerActivities.get(innerActivities.size() - 1).LFT()).doubleValue(), innerActivityStep);

        Activity newActivity = new Simple(innerActivities.get(innerActivities.size() - 1).name() + "_N",
                approximationFeature.stream().map(Pair::getRight).collect(Collectors.toCollection(ArrayList::new)),
                approximationFeature.stream().map(Pair::getLeft).collect(Collectors.toCollection(ArrayList::new))
        );

        if(verbose)
            System.out.println(tabSpaceChars + "---"  + " Block Analysis: Choose inner block " + innerActivities.get(innerActivities.size() - 1).name());

        if(plotIntermediate){
            /*TransientSolution<DeterministicEnablingState, RewardRate> simulate = model.simulate(timeLimit.toString(), step.toString(), 5000);
            double[] simulation = new double[simulate.getSolution().length];
            for(int i = 0; i < simulation.length; i++){
                simulation[i] = simulate.getSolution()[i][0][0];
            }*/

            TransientSolution<DeterministicEnablingState, RewardRate> testAct = innerActivities.get(innerActivities.size() - 1).simulate(timeLimit.toString(), step.toString(), 1000);
            double[] testActCDF = new double[testAct.getSolution().length];
            for(int i = 0; i < testActCDF.length; i++){
                testActCDF[i] = testAct.getSolution()[i][0][0];
            }

            TransientSolution<DeterministicEnablingState, RewardRate> newAct = newActivity.analyze(timeLimit.toString(), step.toString(), "0.001");
            double[] newActcdf = new double[newAct.getSolution().length];
            for(int i = 0; i < newActcdf.length; i++){
                newActcdf[i] = newAct.getSolution()[i][0][0];
            }

            innerActivities.get(innerActivities.size() - 1).replace(newActivity);
            /*TransientSolution<DeterministicEnablingState, RewardRate> simulate2 = model.analyze(timeLimit.toString(), step.toString(), "0.001");
            double[] simulation2 = new double[simulate2.getSolution().length];
            for(int i = 0; i < simulation2.length; i++){
                simulation2[i] = simulate2.getSolution()[i][0][0];
            }*/

            ActivityViewer.CompareResults(newActivity.name(), List.of("Real", "Appr"), List.of(new EvaluationResult("real", testActCDF, 0, testActCDF.length, step.doubleValue(), 0), new EvaluationResult("appr", newActcdf, 0, newActcdf.length, step.doubleValue(), 0)));
        } else {
            innerActivities.get(innerActivities.size() - 1).replace(newActivity);
            int activityIndex = ((DAG) model).activities().indexOf(innerActivities.get(innerActivities.size() - 1));
            ((DAG) model).activities().set(activityIndex, newActivity);
        }
        if(verbose)
            System.out.println(tabSpaceChars + "---"  + " Approximated inner block " + innerActivities.get(innerActivities.size() - 1).name());

        model.resetComplexityMeasure();

        return this.analyze(model, timeLimit, step, forwardReductionFactor, error, tabSpaceChars);
    }

    public double[] DAGInnerBlockAnalysis(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        Map<String, Activity> toBeSimplifiedActivityMap = getDeepestComplexDAG(model);
        Activity toBeSimplifiedActivity = toBeSimplifiedActivityMap.get("activity");
        Activity toBeSimplifiedActivityParent = toBeSimplifiedActivityMap.get("parent");
        double aux = toBeSimplifiedActivity.LFT().doubleValue();
        int mag = 1;
        while (aux > 10) {
            mag = mag * 10;
            aux = aux / 10;
        }
        BigDecimal innerActivityStep = BigDecimal.valueOf(mag * Math.pow(10, -2));

        ArrayList<Pair<BigDecimal, StochasticTransitionFeature>> approximationFeature =  approximator().getApproximatedStochasticTransitionFeatures(
                analyze(toBeSimplifiedActivity, toBeSimplifiedActivity.LFT().precision() >= 309 ? timeLimit : toBeSimplifiedActivity.LFT(), innerActivityStep, forwardReductionFactor, error, tabSpaceChars + "---"  ),
                toBeSimplifiedActivity.EFT().doubleValue(), (toBeSimplifiedActivity.LFT().precision() >= 309 ? timeLimit : toBeSimplifiedActivity.LFT()).doubleValue(), innerActivityStep);

        Activity newActivity = new Simple(toBeSimplifiedActivity.name() + "_N",
                approximationFeature.stream().map(Pair::getRight).collect(Collectors.toCollection(ArrayList::new)),
                approximationFeature.stream().map(Pair::getLeft).collect(Collectors.toCollection(ArrayList::new)));

        if(verbose)
            System.out.println(tabSpaceChars + "---"  + " Block Analysis: Choose inner block " + toBeSimplifiedActivity.name());

        if(plotIntermediate){
            /*TransientSolution<DeterministicEnablingState, RewardRate> simulate = model.simulate(timeLimit.toString(), step.toString(), 5000);
            double[] simulation = new double[simulate.getSolution().length];
            for(int i = 0; i < simulation.length; i++){
                simulation[i] = simulate.getSolution()[i][0][0];
            }*/

            TransientSolution<DeterministicEnablingState, RewardRate> testAct = toBeSimplifiedActivity.simulate(timeLimit.toString(), step.toString(), 1000);
            double[] testActCDF = new double[testAct.getSolution().length];
            for(int i = 0; i < testActCDF.length; i++){
                testActCDF[i] = testAct.getSolution()[i][0][0];
            }

            TransientSolution<DeterministicEnablingState, RewardRate> newAct = newActivity.analyze(timeLimit.toString(), step.toString(), "0.001");
            double[] newActcdf = new double[newAct.getSolution().length];
            for(int i = 0; i < newActcdf.length; i++){
                newActcdf[i] = newAct.getSolution()[i][0][0];
            }

            toBeSimplifiedActivity.replace(newActivity);
            /*TransientSolution<DeterministicEnablingState, RewardRate> simulate2 = model.analyze(timeLimit.toString(), step.toString(), "0.001");
            double[] simulation2 = new double[simulate2.getSolution().length];
            for(int i = 0; i < simulation2.length; i++){
                simulation2[i] = simulate2.getSolution()[i][0][0];
            }*/

            ActivityViewer.CompareResults(newActivity.name(), List.of("Real", "Appr"), List.of(new EvaluationResult("real", testActCDF, 0, testActCDF.length, step.doubleValue(), 0), new EvaluationResult("appr", newActcdf, 0, newActcdf.length, step.doubleValue(), 0)));
        } else {
            toBeSimplifiedActivity.replace(newActivity);
            int activityIndex = ((DAG) toBeSimplifiedActivityParent).activities().indexOf(toBeSimplifiedActivity);
            ((DAG) toBeSimplifiedActivityParent).activities().set(activityIndex, newActivity);
            toBeSimplifiedActivityParent.resetComplexityMeasure();
        }
        if(verbose)
            System.out.println(tabSpaceChars + "---"  + " Approximated inner block " + toBeSimplifiedActivity.name());

        model.resetComplexityMeasure();

        return this.analyze(model, timeLimit, step, forwardReductionFactor, error, tabSpaceChars);
    }

    public double[] DAGInnerBlockAnalysis2(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        Map<String, Activity> toBeSimplifiedActivityMap = getDeepestComplexBlock(model);
        Activity toBeSimplifiedActivity = toBeSimplifiedActivityMap.get("activity");
        Activity toBeSimplifiedActivityParent = toBeSimplifiedActivityMap.get("parent");
        double aux = toBeSimplifiedActivity.LFT().doubleValue();
        int mag = 1;
        while (aux > 10) {
            mag = mag * 10;
            aux = aux / 10;
        }
        BigDecimal innerActivityStep = BigDecimal.valueOf(mag * Math.pow(10, -2));

        ArrayList<Pair<BigDecimal, StochasticTransitionFeature>> approximationFeature =  approximator().getApproximatedStochasticTransitionFeatures(
                analyze(toBeSimplifiedActivity, toBeSimplifiedActivity.LFT().precision() >= 309 ? timeLimit : toBeSimplifiedActivity.LFT(), innerActivityStep, forwardReductionFactor, error, tabSpaceChars + "---"  ),
                toBeSimplifiedActivity.EFT().doubleValue(), (toBeSimplifiedActivity.LFT().precision() >= 309 ? timeLimit : toBeSimplifiedActivity.LFT()).doubleValue(), innerActivityStep);

        Activity newActivity = new Simple(toBeSimplifiedActivity.name() + "_N",
                approximationFeature.stream().map(Pair::getRight).collect(Collectors.toCollection(ArrayList::new)),
                approximationFeature.stream().map(Pair::getLeft).collect(Collectors.toCollection(ArrayList::new)));

        if(verbose)
            System.out.println(tabSpaceChars + "---"  + " Block Analysis: Choose inner block " + toBeSimplifiedActivity.name());

        if(plotIntermediate){
            /*TransientSolution<DeterministicEnablingState, RewardRate> simulate = model.simulate(timeLimit.toString(), step.toString(), 5000);
            double[] simulation = new double[simulate.getSolution().length];
            for(int i = 0; i < simulation.length; i++){
                simulation[i] = simulate.getSolution()[i][0][0];
            }*/

            TransientSolution<DeterministicEnablingState, RewardRate> testAct = toBeSimplifiedActivity.simulate(timeLimit.toString(), step.toString(), 1000);
            double[] testActCDF = new double[testAct.getSolution().length];
            for(int i = 0; i < testActCDF.length; i++){
                testActCDF[i] = testAct.getSolution()[i][0][0];
            }

            TransientSolution<DeterministicEnablingState, RewardRate> newAct = newActivity.analyze(timeLimit.toString(), step.toString(), "0.001");
            double[] newActcdf = new double[newAct.getSolution().length];
            for(int i = 0; i < newActcdf.length; i++){
                newActcdf[i] = newAct.getSolution()[i][0][0];
            }

            toBeSimplifiedActivity.replace(newActivity);
            /*TransientSolution<DeterministicEnablingState, RewardRate> simulate2 = model.analyze(timeLimit.toString(), step.toString(), "0.001");
            double[] simulation2 = new double[simulate2.getSolution().length];
            for(int i = 0; i < simulation2.length; i++){
                simulation2[i] = simulate2.getSolution()[i][0][0];
            }*/

            ActivityViewer.CompareResults(newActivity.name(), List.of("Real", "Appr"), List.of(new EvaluationResult("real", testActCDF, 0, testActCDF.length, step.doubleValue(), 0), new EvaluationResult("appr", newActcdf, 0, newActcdf.length, step.doubleValue(), 0)));
        } else {
            toBeSimplifiedActivity.replace(newActivity);
            int activityIndex = ((DAG) toBeSimplifiedActivityParent).activities().indexOf(toBeSimplifiedActivity);
            ((DAG) toBeSimplifiedActivityParent).activities().set(activityIndex, newActivity);
            toBeSimplifiedActivityParent.resetComplexityMeasure();
        }
        if(verbose)
            System.out.println(tabSpaceChars + "---"  + " Approximated inner block " + toBeSimplifiedActivity.name());

        model.resetComplexityMeasure();

        return this.analyze(model, timeLimit, step, forwardReductionFactor, error, tabSpaceChars);
    }

    public Map<String, Activity> getDeepestComplexDAG(Activity model){
        ArrayList<Activity> innerActivities = ((DAG) model).activities().stream().filter(t -> (t.C().doubleValue() > 1 || t.Q().doubleValue() > 1)).distinct().sorted(Comparator.comparing(Activity::C).thenComparing(Activity::Q)).collect(Collectors.toCollection(ArrayList::new));
        Activity mostComplexActivity = innerActivities.get(innerActivities.size() - 1);
        boolean modelIsNotADag = mostComplexActivity instanceof AND || mostComplexActivity instanceof SEQ || mostComplexActivity instanceof Xor || mostComplexActivity instanceof Simple;

        if(!modelIsNotADag && mostComplexActivity.C().compareTo(CThreshold) > 0 && mostComplexActivity.Q().compareTo(SThreshold) > 0){
            return getDeepestComplexDAG(mostComplexActivity);
        }

        return Map.ofEntries(
                Map.entry("parent", model),
                Map.entry("activity", mostComplexActivity)
        );
    }

    public Map<String, Activity> getDeepestComplexBlock(Activity model){
        ArrayList<Activity> innerActivities = ((DAG) model).activities().stream().filter(t -> (t.C().doubleValue() > 1 || t.Q().doubleValue() > 1)).distinct().sorted(Comparator.comparing(Activity::C).thenComparing(Activity::Q)).collect(Collectors.toCollection(ArrayList::new));
        Activity mostComplexActivity = innerActivities.get(innerActivities.size() - 1);

        if(mostComplexActivity.C().compareTo(CThreshold) > 0 || mostComplexActivity.Q().compareTo(SThreshold) > 0){
            return getDeepestComplexBlock(mostComplexActivity);
        }

        return Map.ofEntries(
                Map.entry("parent", model),
                Map.entry("activity", mostComplexActivity)
        );
    }

    public double[] InnerBlockReplicationAnalysis(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        ArrayList<DAG> innerBlocks = new ArrayList<>();
        ArrayList<DAG> sortedInnerBlocks = new ArrayList<>();
        for(Activity activity: ((DAG) model).end().pre()){
            DAG innerBlock = ((DAG) model).copyRecursive(((DAG) model).begin(), activity, "_before_" + activity.name());
            innerBlocks.add(innerBlock);
            innerBlock.C();
            innerBlock.Q();
            sortedInnerBlocks.add(innerBlock);
        }

        sortedInnerBlocks.sort(Comparator.comparing(Activity::C).thenComparing(Activity::Q));

        DAG nestedDAG = ((DAG) model).nest(((DAG) model).end().pre().get(innerBlocks.indexOf(sortedInnerBlocks.get(sortedInnerBlocks.size() - 1))));
        if(verbose)
            System.out.println(tabSpaceChars + "---"  + " Replicated block before " + sortedInnerBlocks.get(sortedInnerBlocks.size() - 1).name().split("_before_")[1]);

        nestedDAG.setEFT(nestedDAG.low());
        nestedDAG.setLFT(nestedDAG.upp());

        if(plotIntermediate){
            TransientSolution<DeterministicEnablingState, RewardRate> simulate = model.simulate(timeLimit.toString(), step.toString(), 5000);
            double[] simulation = new double[simulate.getSolution().length];
            for(int i = 0; i < simulation.length; i++){
                simulation[i] = simulate.getSolution()[i][0][0];
            }

            double[] simulation2 = this.analyze(nestedDAG, timeLimit, step, forwardReductionFactor, error, tabSpaceChars);

            ActivityViewer.CompareResults("Inner Block Replication: " + model.name(), List.of("Real", "Appr"), List.of(new EvaluationResult("real", simulation, 0, simulation.length, step.doubleValue(), 0), new EvaluationResult("appr", simulation2, 0, simulation2.length, step.doubleValue(), 0)));
        }

        model.resetComplexityMeasure();
        return this.analyze(nestedDAG, timeLimit, step, forwardReductionFactor, error, tabSpaceChars);
    }

    public double[] InnerBlockReplicationAnalysisAndApproximation(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal forwardReductionFactor, BigDecimal error, String tabSpaceChars){
        ArrayList<DAG> innerBlocks = new ArrayList<>();
        ArrayList<DAG> sortedInnerBlocks = new ArrayList<>();
        for(Activity activity: ((DAG) model).end().pre()){
            DAG innerBlock = ((DAG) model).copyRecursive(((DAG) model).begin(), activity, "before_" + activity.name());
            innerBlocks.add(innerBlock);
            innerBlock.C();
            innerBlock.Q();
            sortedInnerBlocks.add(innerBlock);
        }

        sortedInnerBlocks.sort(Comparator.comparing(Activity::C).thenComparing(Activity::Q));

        DAG nestedDAG = ((DAG) model).nest(((DAG) model).end().pre().get(innerBlocks.indexOf(sortedInnerBlocks.get(sortedInnerBlocks.size() - 1))));
        for(Activity activity: ((AND) nestedDAG).activities()){
            ArrayList<Pair<BigDecimal, StochasticTransitionFeature>> approximationFeature =  approximator().getApproximatedStochasticTransitionFeatures(
                analyze(activity, activity.LFT().precision() >= 309 ? timeLimit : activity.LFT(), step, forwardReductionFactor, error, tabSpaceChars + "---"),
                activity.EFT().doubleValue(),
                (activity.LFT().precision() >= 309 ? timeLimit : activity.LFT()).doubleValue(),
                step
            );

            Simple newActivity = new Simple(
                activity.name() + "_N",
                approximationFeature.stream().map(Pair::getRight).collect(Collectors.toCollection(ArrayList::new)),
                approximationFeature.stream().map(Pair::getLeft).collect(Collectors.toCollection(ArrayList::new))
            );
            activity.replace(newActivity);
        }
        nestedDAG.setEFT(nestedDAG.low());
        nestedDAG.setLFT(nestedDAG.upp());

        if(plotIntermediate){
            TransientSolution<DeterministicEnablingState, RewardRate> simulate = model.simulate(timeLimit.toString(), step.toString(), 5000);
            double[] simulation = new double[simulate.getSolution().length];
            for(int i = 0; i < simulation.length; i++){
                simulation[i] = simulate.getSolution()[i][0][0];
            }

            double[] simulation2 = this.analyze(nestedDAG, timeLimit, step, forwardReductionFactor, error, tabSpaceChars);

            ActivityViewer.CompareResults("Inner Block Replication: " + model.name(), List.of("Real", "Appr"), List.of(new EvaluationResult("real", simulation, 0, simulation.length, step.doubleValue(), 0), new EvaluationResult("appr", simulation2, 0, simulation2.length, step.doubleValue(), 0)));
        }

        model.resetComplexityMeasure();
        return this.analyze(nestedDAG, timeLimit, step, forwardReductionFactor, error, tabSpaceChars);
    }

    public double[] regenerativeTransientAnalysis(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal sampleFactor, BigDecimal error, String tabSpaceChars){
        if(verbose)
            System.out.println(tabSpaceChars + " Reg Analysis of block " + model.name());

        long time = System.nanoTime();
        TransientSolution<DeterministicEnablingState, RewardRate> transientSolution = model.analyze(timeLimit.toString(), step.divide(sampleFactor).toString(), error.toString());
        if(verbose)
            System.out.println(tabSpaceChars +  " Analysis done in " + String.format("%.3f seconds",
                (System.nanoTime() - time)/1e9) + "...");

        double[] solution = new double[timeLimit.divide(step, RoundingMode.HALF_DOWN).intValue()];
        for(int i = 0; i < solution.length; i++){
            solution[i] = transientSolution.getSolution()[i * sampleFactor.intValue()][0][0];
        }

        return solution;
    }

    public double[] activityCDF(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal sampleFactor, BigDecimal error, String tabSpaceChars){
        long time = System.nanoTime();
        TransientSolution<DeterministicEnablingState, RewardRate> transientSolution = model.analyze(timeLimit.toString(), step.divide(sampleFactor).toString(), error.toString());
        double[] solution = new double[timeLimit.divide(step, RoundingMode.HALF_DOWN).intValue()];
        for(int i = 0; i < solution.length; i++){
            solution[i] = transientSolution.getSolution()[i * sampleFactor.intValue()][0][0];
        }

        return solution;
    }

    public double[] forwardAnalysis(Activity model, BigDecimal timeLimit, BigDecimal step, BigDecimal error, String tabSpaceChars){
        if(verbose)
            System.out.println(tabSpaceChars + " Forward Analysis of block " + model.name());

        long time = System.nanoTime();
        TransientSolution<Marking, RewardRate> transientSolution = model.forwardAnalyze(timeLimit.toString(), step.toString(), error.toString());

        if(verbose)
            System.out.println(tabSpaceChars +  " Analysis done in " + String.format("%.3f seconds",
                (System.nanoTime() - time)/1e9) + "...");

        double[] solution = new double[transientSolution.getSolution().length];
        for(int i = 0; i < solution.length; i++){
            solution[i] = transientSolution.getSolution()[i][0][0];
        }

        return solution;
    }
}