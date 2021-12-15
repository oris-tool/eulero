package org.oristool.eulero.models.qest21;

import org.oristool.eulero.graph.*;
import org.oristool.eulero.models.ModelBuilder;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;

import java.util.List;

public class TestDBuilder extends ModelBuilder {
    public TestDBuilder(StochasticTransitionFeature feature) {
        super(feature);
    }

    @Override
    public Activity buildModel() {
        StochasticTransitionFeature feature = this.getFeature();

        // Computing M, M', N' and K
        Analytical q_0 = new Analytical("SimDAG_0_Q", feature);
        Analytical r_0 = new Analytical("SimDAG_0_R", feature);
        Analytical s_0 = new Analytical("SimDAG_0_S", feature);
        Analytical u_0 = new Analytical("SimDAG_0_U", feature);
        Analytical v_0 = new Analytical("SimDAG_0_V", feature);
        Analytical w_0 = new Analytical("SimDAG_0_W", feature);

        DAG tu_0 = DAG.forkJoin("SimDAG_0_TU",
                DAG.sequence("SimDAG_0_T",
                        new Analytical("SimDAG_0_T1", feature),
                        new Analytical("SimDAG_0_T2", feature)
                ), u_0
        );

        DAG wx_0 = DAG.forkJoin("SimDAG_0_WX",
                DAG.sequence("SimDAG_0_X",
                        new Analytical("SimDAG_0_X1", feature),
                        new Analytical("SimDAG_0_X2", feature)
                ),
                w_0
        );

        DAG simDag_0 = DAG.empty("SimDAG_0");
        q_0.addPrecondition(simDag_0.begin());
        r_0.addPrecondition(simDag_0.begin());
        s_0.addPrecondition(simDag_0.begin());
        tu_0.addPrecondition(q_0, r_0);
        v_0.addPrecondition(r_0);
        wx_0.addPrecondition(s_0, r_0);

        simDag_0.end().addPrecondition(tu_0, v_0, wx_0);
        simDag_0.setEFT(simDag_0.low());
        simDag_0.setLFT(simDag_0.upp());


        Repeat bRepetition = new Repeat("NR_B", 0.2, simDag_0);

        Analytical q_1 = new Analytical("SimDAG_1_Q", feature);
        Analytical r_1 = new Analytical("SimDAG_1_R", feature);
        Analytical s_1 = new Analytical("SimDAG_1_S", feature);
        Analytical u_1 = new Analytical("SimDAG_1_U", feature);
        Analytical v_1 = new Analytical("SimDAG_1_V", feature);
        Analytical w_1 = new Analytical("SimDAG_1_W", feature);

        DAG tu_1 = DAG.forkJoin("SimDAG_1_TU",
                DAG.sequence("SimDAG_1_T",
                        new Analytical("SimDAG_1_T1", feature),
                        new Analytical("SimDAG_1_T2", feature)
                ), u_1
        );

        DAG wx_1 = DAG.forkJoin("SimDAG_1_WX",
                DAG.sequence("SimDAG_1_X",
                        new Analytical("SimDAG_1_X1", feature),
                        new Analytical("SimDAG_1_X2", feature)
                ),
                w_1
        );

        DAG simDag_1 = DAG.empty("SimDAG_1");
        q_1.addPrecondition(simDag_1.begin());
        r_1.addPrecondition(simDag_1.begin());
        s_1.addPrecondition(simDag_1.begin());
        tu_1.addPrecondition(q_1, r_1);
        v_1.addPrecondition(r_1);
        wx_1.addPrecondition(s_1, r_1);

        simDag_1.end().addPrecondition(tu_1, v_1, wx_1);
        simDag_1.setEFT(simDag_1.low());
        simDag_1.setLFT(simDag_1.upp());

        Analytical q_2 = new Analytical("SimDAG_2_Q", feature);
        Analytical r_2 = new Analytical("SimDAG_2_R", feature);
        Analytical s_2 = new Analytical("SimDAG_2_S", feature);
        Analytical u_2 = new Analytical("SimDAG_2_U", feature);
        Analytical v_2 = new Analytical("SimDAG_2_V", feature);
        Analytical w_2 = new Analytical("SimDAG_2_W", feature);

        DAG tu_2 = DAG.forkJoin("SimDAG_2_TU",
                DAG.sequence("SimDAG_2_T",
                        new Analytical("SimDAG_2_T1", feature),
                        new Analytical("SimDAG_2_T2", feature)
                ), u_2
        );

        DAG wx_2 = DAG.forkJoin("SimDAG_2_WX",
                DAG.sequence("SimDAG_2_X",
                        new Analytical("SimDAG_2_X1", feature),
                        new Analytical("SimDAG_2_X2", feature)
                ),
                w_2
        );

        DAG simDag_2 = DAG.empty("SimDAG_2");
        q_2.addPrecondition(simDag_2.begin());
        r_2.addPrecondition(simDag_2.begin());
        s_2.addPrecondition(simDag_2.begin());
        tu_2.addPrecondition(q_2, r_2);
        v_2.addPrecondition(r_2);
        wx_2.addPrecondition(s_2, r_2);

        simDag_2.end().addPrecondition(tu_2, v_2, wx_2);
        simDag_2.setEFT(simDag_2.low());
        simDag_2.setLFT(simDag_2.upp());

        Analytical q_3 = new Analytical("SimDAG_3_Q", feature);
        Analytical r_3 = new Analytical("SimDAG_3_R", feature);
        Analytical s_3 = new Analytical("SimDAG_3_S", feature);
        Analytical u_3 = new Analytical("SimDAG_3_U", feature);
        Analytical v_3 = new Analytical("SimDAG_3_V", feature);
        Analytical w_3 = new Analytical("SimDAG_3_W", feature);

        DAG tu_3 = DAG.forkJoin("SimDAG_3_TU",
                DAG.sequence("SimDAG_3_T",
                        new Analytical("SimDAG_3_T1", feature),
                        new Analytical("SimDAG_3_T2", feature)
                ), u_3
        );

        DAG wx_3 = DAG.forkJoin("SimDAG_3_WX",
                DAG.sequence("SimDAG_3_X",
                        new Analytical("SimDAG_3_X1", feature),
                        new Analytical("SimDAG_3_X2", feature)
                ),
                w_3
        );

        DAG simDag_3 = DAG.empty("SimDAG_3");
        q_3.addPrecondition(simDag_3.begin());
        r_3.addPrecondition(simDag_3.begin());
        s_3.addPrecondition(simDag_3.begin());
        tu_3.addPrecondition(q_3, r_3);
        v_3.addPrecondition(r_3);
        wx_3.addPrecondition(s_3, r_3);

        simDag_3.end().addPrecondition(tu_3, v_3, wx_3);
        simDag_3.setEFT(simDag_3.low());
        simDag_3.setLFT(simDag_3.upp());

        Repeat nestedRepetition = new Repeat("NestedRepetition", 0.15,
                DAG.sequence("NR_A",
                        bRepetition,
                        new Analytical("NR_AD", feature),
                        DAG.forkJoin("NR_ComAND",
                                DAG.sequence("NR_ComAND_A",
                                        new Analytical("NR_ComAND_B", feature),
                                        DAG.forkJoin("NR_ComAND_C",
                                                new Analytical("NR_ComAND_D", feature),
                                                new Analytical("NR_ComAND_E", feature)
                                        )
                                ),
                                DAG.sequence("NR_ComAND_F",
                                        new Analytical("NR_ComAND_G", feature),
                                        DAG.forkJoin("NR_ComAND_H", simDag_1, simDag_2)
                                )
                        )
                )
        );

        DAG main = DAG.forkJoin("MAIN",
                DAG.forkJoin("A",
                        DAG.sequence("B",
                                new Analytical("C", feature),
                                new Analytical("D", feature),
                                new Analytical("E", feature),
                                new Analytical("F", feature)
                        ),
                        nestedRepetition
                ),
                DAG.sequence("H",
                        DAG.forkJoin("I",
                                new Analytical("J", feature),
                                DAG.sequence("K",
                                        new Analytical("L", feature),
                                        new Analytical("M", feature)
                                )
                        ),
                        DAG.forkJoin("N",
                                DAG.sequence("O",
                                        new Analytical("P", feature),
                                        new Analytical("Q'", feature)
                                ),
                                new Xor("R",
                                        List.of(
                                                new Analytical("S", feature),
                                                new Analytical("T", feature)
                                        ),
                                        List.of(0.3, 0.7)
                                )
                        ),
                        DAG.forkJoin("U",
                                DAG.sequence("V",
                                        new Analytical("W", feature),
                                        new Analytical("X", feature),
                                        new Analytical("Y", feature)
                                ), simDag_3
                        )
                )
        );

        return main;
    }
}