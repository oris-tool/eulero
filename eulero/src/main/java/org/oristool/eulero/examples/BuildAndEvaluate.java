package org.oristool.eulero.examples;

import com.google.common.collect.Lists;
import org.oristool.eulero.evaluation.approximator.Approximator;
import org.oristool.eulero.evaluation.approximator.EXPMixtureApproximation;
import org.oristool.eulero.evaluation.heuristics.AnalysisHeuristics1;
import org.oristool.eulero.evaluation.heuristics.AnalysisHeuristicsStrategy;
import org.oristool.eulero.evaluation.heuristics.EvaluationResult;
import org.oristool.eulero.ui.ActivityViewer;
import org.oristool.eulero.workflow.Activity;
import org.oristool.eulero.workflow.DAG;
import org.oristool.eulero.workflow.Simple;
import org.oristool.eulero.workflow.XOR;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

public class BuildAndEvaluate {
    public static void main(String[] args) {
        StochasticTransitionFeature feature = StochasticTransitionFeature.newUniformInstance("0", "1");

        Activity Q = DAG.sequence("Q",
                DAG.forkJoin("Q1",
                        new Simple("Q1A", feature),
                        new Simple("Q1B", feature)

                ),
                DAG.forkJoin("Q2",
                        new Simple("Q2A", feature),
                        new Simple("Q2B", feature)
                )
        );

        Activity R = DAG.forkJoin("R",
                new XOR("R1",
                        List.of(
                                new Simple("R1A", feature),
                                new Simple("R1b", feature)
                        ),
                        List.of(0.3, 0.7)),
                DAG.sequence("R2",
                        new Simple("R2A", feature),
                        new Simple("R2B", feature)
                )
        );

        Activity S = DAG.forkJoin("S",
                DAG.sequence("S1",
                        new Simple("S1A", feature),
                        new Simple("S1B", feature),
                        new Simple("S1C", feature)
                ),
                DAG.sequence("S2",
                        new Simple("S2A", feature),
                        new Simple("S2B", feature),
                        new Simple("S2C", feature)
                )
        );

        DAG T = DAG.sequence("T",
                DAG.forkJoin("T1",
                        new Simple("T1A", feature),
                        new Simple("T1B", feature),
                        new Simple("T1C", feature)
                ),
                DAG.forkJoin("T2",
                        new Simple("T2A", feature),
                        new Simple("T2B", feature),
                        new Simple("T2C", feature)
                )
        );

        DAG top = DAG.empty("TOP");
        Q.addPrecondition(top.begin());
        R.addPrecondition(top.begin());
        T.addPrecondition(R);
        S.addPrecondition(R, Q);
        top.end().addPrecondition(T, S);
        top.setEFT(top.getEFTBound(top.end()));
        top.setLFT(top.getLFTBound(top.end()));
        top.setActivities(Lists.newArrayList(Q, R, S, T));

        BigInteger tC = BigInteger.valueOf(3);
        BigInteger tQ = BigInteger.valueOf(7);
        BigDecimal timeLimit = top.LFT();
        BigDecimal step = BigDecimal.valueOf(0.01);
        Approximator approximator = new EXPMixtureApproximation();
        AnalysisHeuristicsStrategy strategy = new AnalysisHeuristics1(tC, tQ, approximator);
        double[] evaluation = strategy.analyze(top, timeLimit.add(BigDecimal.ONE), step);
        EvaluationResult result = new EvaluationResult("Heuristic 1", evaluation, 0, evaluation.length, top.getFairTimeTick().doubleValue(), 0);

        ActivityViewer.CompareResults("Example", List.of("Heuristic 1"), List.of(result));

        double[] cdf = result.cdf();
        double[] pdf = result.pdf();

        StringBuilder cdfString = new StringBuilder();
        StringBuilder pdfString = new StringBuilder();
        for(int j = 0; j < cdf.length; j++){
            BigDecimal x = BigDecimal.valueOf((result.min() + j) * result.step())
                    .setScale(BigDecimal.valueOf(result.step()).scale(), RoundingMode.HALF_DOWN);
            cdfString.append(x.toString()).append(", ").append(cdf[j]).append("\n");
            pdfString.append(x.toString()).append(", ").append(pdf[j]).append("\n");
        }

        try {
            FileWriter cdfWriter = new FileWriter("/Users/riccardoreali/Desktop/Esempio/CDF/" + result.title() + ".txt");
            FileWriter pdfWriter = new FileWriter("/Users/riccardoreali/Desktop/Esempio/PDF/" + result.title() + ".txt");

            cdfWriter.write(cdfString.toString());
            cdfWriter.close();
            pdfWriter.write(pdfString.toString());
            pdfWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }




    }
}
