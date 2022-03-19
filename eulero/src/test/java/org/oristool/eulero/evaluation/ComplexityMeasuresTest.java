package org.oristool.eulero.evaluation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.oristool.eulero.evaluation.heuristic.AnalysisHeuristic1;
import org.oristool.eulero.evaluation.heuristic.AnalysisHeuristicStrategy;
import org.oristool.eulero.workflow.*;
import org.oristool.eulero.evaluation.approximator.Approximator;
import org.oristool.eulero.evaluation.approximator.SplineBodyEXPTailApproximation;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class ComplexityMeasuresTest {

    @Test
    void TestSingleActivityComplexityMeasure(){
        StochasticTransitionFeature unif0_10 =
                StochasticTransitionFeature.newUniformInstance(BigDecimal.ZERO, BigDecimal.valueOf(0.8));

        BigInteger C =  BigInteger.valueOf(3);
        BigInteger R =  BigInteger.valueOf(10);
        Approximator approximator = new SplineBodyEXPTailApproximation(3);
        AnalysisHeuristicStrategy analyzer = new AnalysisHeuristic1(C, R, approximator, true);

        Activity model = new Simple("A6", unif0_10);

        // C
        Assertions.assertTrue(model.C().compareTo(C) < 0);
        Assertions.assertEquals(0, model.C().compareTo(BigInteger.ONE));

        // Simplified C
        Assertions.assertTrue(model.simplifiedC().compareTo(C) < 0);
        Assertions.assertEquals(0, model.simplifiedC().compareTo(BigInteger.ONE));
    }

    @Test
    void TestANDComplexityMeasure(){
        StochasticTransitionFeature unif0_10 =
                StochasticTransitionFeature.newUniformInstance(BigDecimal.ZERO, BigDecimal.valueOf(0.8));
        BigInteger C =  BigInteger.valueOf(3);
        BigInteger R =  BigInteger.valueOf(10);

        Activity model = DAG.forkJoin("test",
                new Simple("A6", unif0_10),
                new Simple("A1", unif0_10)
        );

        // C
        Assertions.assertTrue(model.C().compareTo(C) < 0);
        Assertions.assertEquals(0, model.C().compareTo(BigInteger.valueOf(2)));

        // Simplified C
        Assertions.assertTrue(model.simplifiedC().compareTo(C) < 0);
        Assertions.assertEquals(0, model.simplifiedC().compareTo(BigInteger.valueOf(2)));
    }

    @Test
    void TestSEQComplexityMeasure(){
        StochasticTransitionFeature unif0_10 =
                StochasticTransitionFeature.newUniformInstance(BigDecimal.ZERO, BigDecimal.valueOf(0.8));
        BigInteger C =  BigInteger.valueOf(3);
        BigInteger R =  BigInteger.valueOf(10);

        Activity model = DAG.sequence("test",
                new Simple("A6", unif0_10),
                new Simple("A1", unif0_10)
        );

        // C
        Assertions.assertTrue(model.C().compareTo(C) < 0);
        Assertions.assertEquals(0, model.C().compareTo(BigInteger.ONE));

        // Simplified C
        Assertions.assertTrue(model.simplifiedC().compareTo(C) < 0);
        Assertions.assertEquals(0, model.simplifiedC().compareTo(BigInteger.ONE));
    }

    @Test
    void TestXORComplexityMeasure(){
        StochasticTransitionFeature unif0_10 =
                StochasticTransitionFeature.newUniformInstance(BigDecimal.ZERO, BigDecimal.valueOf(0.8));
        BigInteger C =  BigInteger.valueOf(3);
        BigInteger R =  BigInteger.valueOf(10);

        Activity simpleModel = new Xor("test",
                List.of(
                    new Simple("A6", unif0_10),
                    new Simple("A1", unif0_10)
                ),
                List.of(0.3, 0.4)
        );

        Activity complexModel = new Xor("testC",
                List.of(
                        new Simple("A5", unif0_10),
                        DAG.forkJoin("AND",
                                new Simple("A1", unif0_10),
                                new Simple("A2", unif0_10)
                        )
                ),
                List.of(0.3, 0.4)
        );

        Activity veryComplexModel = new Xor("testB",
                List.of(
                        new Simple("A6", unif0_10),
                        DAG.sequence("SEQ",
                                DAG.forkJoin("AND",
                                        new Simple("A1", unif0_10),
                                        DAG.forkJoin("AND_inner",
                                                new Simple("A3", unif0_10),
                                                new Simple("A4", unif0_10)
                                        )
                                ), new Simple("A5", unif0_10)

                        )
                ),
                List.of(0.3, 0.4)
        );

        // C
        Assertions.assertTrue(simpleModel.C().compareTo(C) < 0);
        Assertions.assertEquals(0, simpleModel.C().compareTo(BigInteger.ONE));

        Assertions.assertTrue(complexModel.C().compareTo(C) < 0);
        Assertions.assertEquals(0, complexModel.C().compareTo(BigInteger.valueOf(2)));

        Assertions.assertEquals(0, veryComplexModel.C().compareTo(C));
        Assertions.assertEquals(0, veryComplexModel.C().compareTo(BigInteger.valueOf(3)));

        // Simplified C
        Assertions.assertTrue(simpleModel.simplifiedC().compareTo(C) < 0);
        Assertions.assertEquals(0, simpleModel.simplifiedC().compareTo(BigInteger.ONE));

        Assertions.assertTrue(complexModel.simplifiedC().compareTo(C) < 0);
        Assertions.assertEquals(0, complexModel.simplifiedC().compareTo(BigInteger.ONE));

        Assertions.assertTrue(veryComplexModel.simplifiedC().compareTo(C) < 0);
        Assertions.assertEquals(0, veryComplexModel.simplifiedC().compareTo(BigInteger.ONE));
    }

    @Test
    void TestRepeatComplexityMeasure(){
        StochasticTransitionFeature unif0_10 =
                StochasticTransitionFeature.newUniformInstance(BigDecimal.ZERO, BigDecimal.valueOf(0.8));
        BigInteger C =  BigInteger.valueOf(3);
        BigInteger R =  BigInteger.valueOf(10);

        Activity repeatBody = new Xor("testB",
                List.of(
                        new Simple("A6", unif0_10),
                        DAG.sequence("SEQ",
                                DAG.forkJoin("AND_inner",
                                        new Simple("A3", unif0_10),
                                        new Simple("A4", unif0_10)
                                ),
                                new Simple("A5", unif0_10)
                        )
                ),
                List.of(0.3, 0.4)
        );

        Activity repeat = new Repeat("Repeat", 0.4, repeatBody);

        Activity complexRepeatBody = new Xor("testC",
                List.of(
                        new Simple("A6", unif0_10),
                        DAG.forkJoin("FJ",
                                DAG.forkJoin("AND_inner",
                                        new Simple("A3", unif0_10),
                                        new Simple("A4", unif0_10)
                                ),
                                DAG.forkJoin("AND_inner2",
                                        new Simple("A5", unif0_10),
                                        new Simple("A7", unif0_10)
                                )
                        )
                ),
                List.of(0.3, 0.4)
        );

        Activity complexRepeat = new Repeat("CRepeat", 0.4, complexRepeatBody);

        // C
        Assertions.assertTrue(repeat.C().compareTo(C) < 0);
        Assertions.assertEquals(0, repeat.C().compareTo(BigInteger.valueOf(2)));

        Assertions.assertTrue(complexRepeat.C().compareTo(C) > 0);
        Assertions.assertEquals(0, complexRepeat.C().compareTo(BigInteger.valueOf(4)));

        // Simplified C
        Assertions.assertTrue(repeat.simplifiedC().compareTo(C) < 0);
        Assertions.assertEquals(0, repeat.simplifiedC().compareTo(BigInteger.ONE));

        Assertions.assertTrue(complexRepeat.simplifiedC().compareTo(C) < 0);
        Assertions.assertEquals(0, complexRepeat.simplifiedC().compareTo(BigInteger.ONE));

        // R
        Assertions.assertTrue(repeat.R().compareTo(R) < 0);
        Assertions.assertEquals(0, repeat.R().compareTo(BigInteger.valueOf(2)));

        Assertions.assertTrue(complexRepeat.R().compareTo(R) < 0);
        Assertions.assertEquals(0, complexRepeat.R().compareTo(BigInteger.valueOf(4)));

        // Simplified R
        Assertions.assertTrue(repeat.simplifiedR().compareTo(R) < 0);
        Assertions.assertEquals(0, repeat.simplifiedR().compareTo(BigInteger.valueOf(1)));

        Assertions.assertTrue(complexRepeat.simplifiedR().compareTo(R) < 0);
        Assertions.assertEquals(0, complexRepeat.simplifiedR().compareTo(BigInteger.valueOf(1)));
    }

    @Test
    void TestSimpleDAGComplexityMeasure(){
        StochasticTransitionFeature unif0_10 =
                StochasticTransitionFeature.newUniformInstance(BigDecimal.ZERO, BigDecimal.valueOf(0.8));
        BigInteger C =  BigInteger.valueOf(3);
        BigInteger R =  BigInteger.valueOf(10);

        Simple q = new Simple("Q", unif0_10);
        Simple r = new Simple("R", unif0_10);
        Simple s = new Simple("S", unif0_10);
        Simple u = new Simple("U", unif0_10);
        Simple v = new Simple("V", unif0_10);

        DAG pSimple = DAG.empty("P");
        q.addPrecondition(pSimple.begin());
        r.addPrecondition(pSimple.begin());
        s.addPrecondition(pSimple.begin());
        u.addPrecondition(q, r);
        v.addPrecondition(s, r);
        pSimple.end().addPrecondition(u, v);

        // C
        Assertions.assertEquals(0, pSimple.C().compareTo(C));
        Assertions.assertEquals(0, pSimple.C().compareTo(BigInteger.valueOf(3)));

        // Simplified C
        Assertions.assertEquals(0, pSimple.simplifiedC().compareTo(C));
        Assertions.assertEquals(0, pSimple.simplifiedC().compareTo(BigInteger.valueOf(3)));

        // R
        Assertions.assertTrue(pSimple.R().compareTo(R) < 0);
        Assertions.assertEquals(0, pSimple.R().compareTo(BigInteger.valueOf(8)));

        // Simplified R
        Assertions.assertTrue(pSimple.R().compareTo(R) < 0);
        Assertions.assertEquals(0, pSimple.simplifiedR().compareTo(BigInteger.valueOf(8)));
    }

    @Test
    void TestComplexDAGComplexityMeasure(){
        StochasticTransitionFeature unif0_10 =
                StochasticTransitionFeature.newUniformInstance(BigDecimal.ZERO, BigDecimal.valueOf(1.0));
        BigInteger C =  BigInteger.valueOf(3);
        BigInteger R =  BigInteger.valueOf(10);

        Simple q = new Simple("Q", unif0_10);
        Simple r = new Simple("R", unif0_10);
        Simple s = new Simple("S", unif0_10);
        Simple v = new Simple("V", unif0_10);

        DAG tu = DAG.forkJoin("TU",
                DAG.sequence("T",
                        new Simple("T1", unif0_10),
                        new Simple("T2", unif0_10)
                ), new Simple("U", unif0_10)
        );

        DAG wx = DAG.forkJoin("WX",
                DAG.sequence("X",
                        new Simple("X1", unif0_10),
                        new Simple("X2", unif0_10)
                ),
                new Simple("W", unif0_10)
        );

        DAG pComplex = DAG.empty("P");
        q.addPrecondition(pComplex.begin());
        r.addPrecondition(pComplex.begin());
        s.addPrecondition(pComplex.begin());
        tu.addPrecondition(q, r);
        v.addPrecondition(r);
        wx.addPrecondition(s, r);
        pComplex.end().addPrecondition(tu, v, wx);


        Assertions.assertTrue(pComplex.C().compareTo(C) > 0);
        Assertions.assertEquals(0, pComplex.C().compareTo(BigInteger.valueOf(5)));

        // Simplified C
        Assertions.assertEquals(0, pComplex.simplifiedC().compareTo(C));
        Assertions.assertEquals(0, pComplex.simplifiedC().compareTo(BigInteger.valueOf(3)));

        // R
        Assertions.assertTrue(pComplex.R().compareTo(R) > 0);
        Assertions.assertEquals(0, pComplex.R().compareTo(BigInteger.valueOf(14)));

        // Simplified R
        Assertions.assertTrue(pComplex.R().compareTo(R) > 0);
        Assertions.assertEquals(0, pComplex.simplifiedR().compareTo(BigInteger.valueOf(9)));
    }
}

// TODO check also R, SimplifiedR, SimplifiedC