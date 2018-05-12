/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.contrib.analysis.kai.Databins;
import org.matsim.core.config.groups.StrategyConfigGroup;

/**
 * Created by amit on 28/11/2016.
 */

public class ObjectiveFunctionEvaluator {

    private static final Logger log = Logger.getLogger(ObjectiveFunctionEvaluator.class);
    private final SortedMap<String, Double> mode2share = new TreeMap<>();

    public enum ObjectiveFunctionType {
//        SUM_ABS_DIFF,
        SUM_SQR_DIFF, // Q_plain
        SUM_SQR_DIFF_LOG, //Q_lain_log
        SUM_SQR_DIFF_NORMALIZED,
        SUM_SCALED, //Q_scale
        SUM_SCALED_LOG // Q_scale_log
//        SUM_SQRT_DIFF_NORMALIZED
    }

    @Inject(optional=true)
    private final ObjectiveFunctionType objectiveFunctionType = ObjectiveFunctionType.SUM_SQR_DIFF_NORMALIZED;

    @Inject
    private StrategyConfigGroup strategyConfigGroup;

    private final double probOfRandomReplanning;

    public ObjectiveFunctionEvaluator() {
        //injected items should be available during
        log.info("using "+objectiveFunctionType + "objective function.");
        if(this.objectiveFunctionType.equals(ObjectiveFunctionType.SUM_SCALED) || this.objectiveFunctionType.equals(ObjectiveFunctionType.SUM_SCALED_LOG)) {
            this.probOfRandomReplanning = OpdytsModeChoiceUtils.getProabilityOfRandomReplanning(this.strategyConfigGroup);
            log.warn("The probability of random replanning is ="+ this.probOfRandomReplanning);
        } else {
            probOfRandomReplanning = 0.0; // value 0 corresponds to ObjectiveFunctionType.SUM_SQR_DIFF_NORMALIZED.
        }
    }

    public double getObjectiveFunctionValue(final Databins<String> realCounts, final Databins<String> simCounts){
        // databins to maps.

        Map<String, double [] > realCountMap = realCounts.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
        );

        Map<String, double [] > simCountMap = simCounts.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
        );

        return getObjectiveFunctionValue(realCountMap, simCountMap);
    }

    public double getObjectiveFunctionValue(final Map<String, double[]> realCounts, final Map<String, double[]> simCounts){
        double objective = 0. ;
        double realValueSum = 0;
        for ( Map.Entry<String, double[]> theEntry : realCounts.entrySet() ) {
            String mode = theEntry.getKey() ;
            log.info("generating objective function value for mode=" + mode ) ;

            double[] realValue = theEntry.getValue() ;
            double[] simValue = simCounts.get(mode);
            
			log.info("simValue=" + simValue );
            
            final double sum = Arrays.stream(simValue).sum();
            mode2share.put(mode, sum);

            for ( int ii=0 ; ii < realValue.length ; ii++ ) {
                double diff = 0.;

                // realValue cant be null here.
                if (simValue == null) throw new RuntimeException("Sim value cannot be null.");
                else if(realValue.length != simValue.length) {
                    throw new RuntimeException("The length of the real ("+realValue.length+") and sim value ("+simValue.length+ ") arrays for the mode "+mode+" are not same " +
                            "i.e. one of the distance class is missing. The simulation is aborting, because not sure, which bin is missing.");
                }
				realValueSum += realValue[ii];

                switch (this.objectiveFunctionType) {
//                    case SUM_ABS_DIFF:
//                        objective += diff;
//                        break;
                    case SUM_SQR_DIFF:
                    case SUM_SQR_DIFF_NORMALIZED:
                        diff = Math.abs( realValue[ii] - simValue[ii] ) ;
                        objective += diff * diff;
                        break;
                    case SUM_SQR_DIFF_LOG:
                        objective += Math.abs( Math.log( realValue[ii]) - Math.log(simValue[ii] ));
                        break;
//					case SUM_SQRT_DIFF_NORMALIZED:
//						objective += Math.sqrt(Math.abs(diff)) ;
//						break;
                    case SUM_SCALED:
                        double scaledDiff =  ( 1/(1-probOfRandomReplanning) * ( simValue [ii] - probOfRandomReplanning * realValue[ii] / realValue.length ) - realValue[ii]);
                        objective += scaledDiff * scaledDiff;
                        break;
                    case SUM_SCALED_LOG:
                        double scaledDiffLog =  ( Math.log( 1/(1-probOfRandomReplanning) * ( simValue [ii] - probOfRandomReplanning * realValue[ii] / realValue.length )) - Math.log(realValue[ii]));
                        objective += scaledDiffLog * scaledDiffLog;
                        break;
                    default:
                        throw new RuntimeException("not implemented yet.");
                }
            }
        }
        switch (this.objectiveFunctionType) {
//            case SUM_ABS_DIFF:
//                log.error("This should be used with great caution, with Patna, it was not a good experience. See email from GF on 24.11.2016");
//                break;
            case SUM_SQR_DIFF:
            case SUM_SQR_DIFF_LOG:
                break;
            case SUM_SQR_DIFF_NORMALIZED:
                objective /= (realValueSum * realValueSum);
                break;
//			case SUM_SQRT_DIFF_NORMALIZED:
//				objective /= Math.sqrt( realValueSum ) ;
//				break;
            case SUM_SCALED:
            case SUM_SCALED_LOG:
                break;
            default:
                throw new RuntimeException("not implemented yet.");
        }

        log.warn( "objective=" + objective );
        return objective;
    }

    Map<String,Double> getModeToShare(){
        double sumShare = mode2share.values().stream().mapToDouble(Number::doubleValue).sum();
        return mode2share.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() / sumShare ));
    }
}
