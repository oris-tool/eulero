package org.oristool.eulero.graph;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class SEQ extends DAG{
    private final List<Activity> activities;
    protected SEQ(String name, List<Activity> activities){
        super(name);
        setEFT(this.low());
        setLFT(this.upp());
        //setC(activities.stream().max(Comparator.comparing(Activity::C)).get().C());
        //setR(activities.stream().max(Comparator.comparing(Activity::R)).get().R());
        this.activities = activities;
    }

    public List<Activity> activities() {
        return activities;
    }

    @Override
    public boolean isWellNested() {
        boolean isWellNested = true;
        for (Activity block: activities) {
            isWellNested = isWellNested && block.isWellNested();
        }
        return isWellNested;
    }

    /*public double[] getNumericalCDF(BigDecimal timeLimit, BigDecimal step) {
        if(!this.isWellNested()){
            throw new RuntimeException("Block is not well-nested...");
        }

        if (activities.size() == 1)
            return activities.get(0).getNumericalCDF(timeLimit, step);

        double[] cdf = new double[timeLimit.divide(step).intValue()];
        for(Activity act: activities){
            if (act.equals(activities.get(0))){
                cdf = act.getNumericalCDF(timeLimit, step);
            } else {
                double[] convolution = new double[cdf.length];
                double[] activityCDF = act.getNumericalCDF(timeLimit, step);

                for (int x = 1; x < cdf.length; x++) {
                    for (int u = 1; u <= x; u++)
                        convolution[x] += (cdf[u] - cdf[u - 1]) * (activityCDF[x - u + 1] + activityCDF[x - u]) * 0.5;
                }

                cdf = convolution;
            }
        }

        return cdf;
    }*/
}
