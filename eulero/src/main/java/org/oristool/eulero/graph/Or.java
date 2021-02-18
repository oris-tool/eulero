/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oristool.eulero.graph;

import java.util.Arrays;

/**
 * OR: completes when any of the dependencies has completed. 
 */
public class Or implements Activity {
    private int min;
    private int max;
    private double[] cdf;   
    private Activity[] dependencies;
    
    public Or(Activity... dependencies) {
        min = Arrays.stream(dependencies).mapToInt(s -> s.min()).min().getAsInt(); // min of mins
        max = Arrays.stream(dependencies).mapToInt(s -> s.max()).min().getAsInt(); // min of maxs
        
        cdf = new double[max-min-1]; 
        for (int x = 0; x < cdf.length; x++) {
            // CDF of min is 1-(1-F(x))*(1-G(x))
            int t = min + 1 + x;
            cdf[x] = 1.0;
            for (int i = 0; i < dependencies.length; i++)
                cdf[x] *= 1 - dependencies[i].CDF(t);
            cdf[x] = 1 - cdf[x];
        }
        
        this.dependencies = dependencies;
    }
    
    @Override
    public int min() {
        return min;
    }
    
    @Override
    public int max() {
        return max;
    }

    @Override
    public double CDF(int time) {
        if (time <= min)
            return 0;
        else if (time >= max)
            return 1;
        else
            return cdf[time-min-1];
    }
    
    @Override
    public Activity[] dependencies() {
        return dependencies;
    }
}
