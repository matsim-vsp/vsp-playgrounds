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

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;

/**
 * Created by amit on 11.05.18.
 */

public class OpdytsModeChoiceUtils {
    private static final Logger log = Logger.getLogger(OpdytsModeChoiceUtils.class);

    /*
     * random replanning probability is the probability that
     * a random mode variation is made given that one has chosen a mode-changing strategy
     */
    public static double getProabilityOfRandomReplanning(StrategyConfigGroup strategy){
        double pure_modeChoice_strategy = 0.;
        /*
         * replanning rate is the probability that any strategy is selected that can change the mode
         */
        double replanningProb = 0.;

        for (StrategySettings ss : strategy.getStrategySettings()) {
            if (ss.getStrategyName().equals(DefaultSelector.ChangeExpBeta.toString())) {
                throw new RuntimeException("Don't use "+DefaultSelector.ChangeExpBeta+" for calculation of probability of random re-planning." +
                        DefaultSelector.SelectExpBeta+" or " +DefaultSelector.BestScore+" should be fine.");
            } else if (ss.getStrategyName().equals(DefaultStrategy.ChangeTripMode.toString())) {
                pure_modeChoice_strategy += ss.getWeight();
                replanningProb += ss.getWeight();
            } else if (ss.getStrategyName().equals(DefaultStrategy.SubtourModeChoice.toString())) {
                pure_modeChoice_strategy += ss.getWeight();
                replanningProb += ss.getWeight();
            } else if (ss.getStrategyName().equals(DefaultStrategy.ChangeSingleTripMode.toString())) {
                pure_modeChoice_strategy += ss.getWeight();
                replanningProb += ss.getWeight();
            } else if (ss.getStrategyName().equals(DefaultSelector.BestScore.toString())) {
                replanningProb += ss.getWeight();
            } else if (ss.getStrategyName().equals(DefaultSelector.SelectExpBeta.toString())) {
                replanningProb += ss.getWeight();
            } else{
                log.error("The strategy "+ss.getStrategyName()+" is not considered to calculate probability of random re-planning.");
            }
        }
        return pure_modeChoice_strategy / replanningProb;
    }
}
