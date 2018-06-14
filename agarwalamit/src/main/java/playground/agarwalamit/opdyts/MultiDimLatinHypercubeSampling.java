/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.opdyts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.log4j.Logger;

/**
 * @author amit after LatinHypercubeSampling
 */

public class MultiDimLatinHypercubeSampling {

    private static final Logger LOG = Logger.getLogger(MultiDimLatinHypercubeSampling.class);

    private final Random random;

    private List<Double> minList = new ArrayList<>();
    private List<Double> maxList = new ArrayList<>();

    public MultiDimLatinHypercubeSampling(Random random) {
        this.random = random;
    }

    public void addDimension(final double minValue, final double maxValue) {
        this.minList.add(minValue);
        this.maxList.add(maxValue);
    }


    public List<List<Double>> draw(final int sampleAndIntervalCnt) {
        final List<List<Double>> result = new ArrayList<>(sampleAndIntervalCnt);

        for (int index=0; index < sampleAndIntervalCnt ; index++ ){
            List<Double> temp = new ArrayList<>(this.minList.size());
            result.add(temp);
        }

        for (int dim = 0; dim < this.minList.size(); dim++) {
            /*
             * Extract the smallest value (min) and the interval size
             * (intervalSize) along the current dimension dim.
             */
            final double minValue = this.minList.get(dim);
            final double intervalSize = ( this.maxList.get(dim) - minValue ) / sampleAndIntervalCnt;

            /*
             * Split the value range along the current dimension dim into
             * sampleAndIntervalCnt equally large intervals and draw one value
             * uniformly from each interval.
             */
            for (int interval = 0; interval < sampleAndIntervalCnt; interval++) {
                result.get(interval).add(minValue + intervalSize * (interval + this.random.nextDouble()));
            }

            /*
             * Allocate one randomly selected (hence the shuffle) value along
             * the given dimension to each sample.
             */
            Collections.shuffle(result, this.random);
        }
        return result;
    }

    public static void main(String[] args) {

        Random rnd = new Random(444);
        MultiDimLatinHypercubeSampling sampler = new MultiDimLatinHypercubeSampling(rnd);
        sampler.addDimension(0, 1);
        sampler.addDimension(-2, -1);
        sampler.addDimension(2, 4);
        sampler.addDimension(-1, 0.5);

        int sampleSize = (int) Math.pow(2, 4);
        List<List<Double>> result = sampler.draw(sampleSize);

        for (int sample = 0; sample < result.size(); sample++) {
            System.out.println( result.get(sample) );
        }

    }

}
