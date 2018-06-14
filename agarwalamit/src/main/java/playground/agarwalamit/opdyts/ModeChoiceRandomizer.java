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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import floetteroed.opdyts.DecisionVariableRandomizer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;

/**
 * @author amit
 */

public final class ModeChoiceRandomizer implements DecisionVariableRandomizer<ModeChoiceDecisionVariable> {
    private static final Logger log = Logger.getLogger(ModeChoiceRandomizer.class);

    public enum ASCRandomizerStyle {
        axial_randomVariation, // combinations like (0,+), (0,-), (+,0), (-,0)
        grid_randomVariation, // combinations like (+,+), (+,-), (-,+), (-,-)
        /**
         * A consequence of fixed variation is that if trial of a decision variable does not improve objective function,
         * same set of decision variables will be re-created. Amit Dec'17
         */
        axial_fixedVariation,
        grid_fixedVariation
    }

    private final Scenario scenario;
    private final Random rnd;
    private final String subPopName;
    private final OpdytsScenario opdytsScenario;

    private final OpdytsConfigGroup opdytsConfigGroup;
    private final Collection<String> considerdModes ;

    private final ASCRandomizerStyle ascRandomizerStyle;

    public ModeChoiceRandomizer(final Scenario scenario, final RandomizedUtilityParametersChoser randomizedUtilityParametersChoser,
             final OpdytsScenario opdytsScenario, final String subPopName, final Collection<String> considerdModes, final ASCRandomizerStyle ascRandomizerStyle) {

        this.scenario = scenario;
        this.opdytsConfigGroup = (OpdytsConfigGroup) scenario.getConfig().getModules().get(OpdytsConfigGroup.GROUP_NAME);

//        this.rnd = new Random(opdytsConfigGroup.getRandomSeedToRandomizeDecisionVariable());
        //        this.rnd = new Random(4711);
        // this will create an identical sequence of candidate decision variables for each experiment where a new ModeChoiceRandomizer instance is created.
        // That's not good; the parametrized runs are then all conditional on the 4711 random seed.
        // (careful with using matsim-random since it is always the same sequence in one createDiagonalCombinations)
        this.rnd = MatsimRandom.getRandom(); // random seed of matsim random should be changed in each run.

        if ( ! randomizedUtilityParametersChoser.equals(RandomizedUtilityParametersChoser.ONLY_ASC) ) {
            throw new RuntimeException("not implemented yet.");
        }

        this.subPopName = subPopName;
        this.opdytsScenario = opdytsScenario;
        this.considerdModes = considerdModes;
        this.ascRandomizerStyle = ascRandomizerStyle;

        if (this.ascRandomizerStyle.equals(ASCRandomizerStyle.axial_fixedVariation) || this.ascRandomizerStyle.equals(ASCRandomizerStyle.grid_fixedVariation)) {
            log.warn("A consequence of the fixed variation: if trial of a decision variable does not improve \n" +
                    "the overall objective function, the next set of decision variables is likely to be same. \n" +
                    "This means, eventually, there will not be any improvements afterwards.");
        }

        // a higher population size would matter for fixed variation styles even if they start with same simulation state,
        // however, matsim random is sequential and thus, they can end up in a different traffic pattern. Amit Oct'17
    }

    public ModeChoiceRandomizer(final Scenario scenario, final RandomizedUtilityParametersChoser randomizedUtilityParametersChoser,
                                final OpdytsScenario opdytsScenario, final String subPopName, final Collection<String> considerdModes) {
        this(scenario,randomizedUtilityParametersChoser, opdytsScenario, subPopName, considerdModes, ASCRandomizerStyle.grid_randomVariation);
    }

    @Override
    public List<ModeChoiceDecisionVariable> newRandomVariations(ModeChoiceDecisionVariable decisionVariable) {
        List<ModeChoiceDecisionVariable> result ;

        final PlanCalcScoreConfigGroup oldScoringConfig = decisionVariable.getScoreConfig();
        PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet = oldScoringConfig.getScoringParametersPerSubpopulation().get(this.subPopName);

        int totalNumberOfCombination = (int) Math.pow(2, (this.considerdModes.size()-1)); // exclude car
        List<PlanCalcScoreConfigGroup> allCombinations = new ArrayList<>(totalNumberOfCombination);
        List<String> remainingModes = new ArrayList<>(this.considerdModes);

        { // create one planCalcScoreConfigGroup with all starting params.
            PlanCalcScoreConfigGroup configGroupWithStartingModeParams = copyOfPlanCalcScore(oldParameterSet);
            allCombinations.add(configGroupWithStartingModeParams);
            remainingModes.remove(TransportMode.car);
        }

        double randomVariationOfStepSize =  rnd.nextDouble();
        log.warn("creating randomVariation combinations of decision variable as "+ this.ascRandomizerStyle);
        switch (this.ascRandomizerStyle) {
            case axial_randomVariation:
                allCombinations  = createAxialCombinations(oldParameterSet, randomVariationOfStepSize);
                break;
            case axial_fixedVariation:
                allCombinations  = createAxialCombinations(oldParameterSet, 1);
                break;
            case grid_randomVariation:
                createDiagonalCombinations(oldParameterSet, allCombinations, remainingModes, randomVariationOfStepSize);
                break;
            case grid_fixedVariation:
                createDiagonalCombinations(oldParameterSet, allCombinations, remainingModes, 1);
                break;
            default:
                throw new RuntimeException("not implemented yet.");
        }

        result = allCombinations.parallelStream().map(e -> new ModeChoiceDecisionVariable(e, this.scenario, this.opdytsScenario, this.considerdModes, this.subPopName)).collect(
                Collectors.toList());

        log.warn("input decision variable");
        log.warn(decisionVariable.toString());

        log.warn("giving the following new decision variables to opdyts:");
        for (ModeChoiceDecisionVariable var : result) {
            log.warn(var.toString());
        }
        return result;
    }

    private void createDiagonalCombinations(final PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet, final List<PlanCalcScoreConfigGroup> allCombinations, final List<String> remainingModes, final double randomVariationOfStepSize) {
        // create combinations with one mode and call createDiagonalCombinations again
        if (remainingModes.isEmpty()) {
            // don't do anything
        } else {
            String mode = remainingModes.remove(0);
            if (mode.equals(TransportMode.car)) {
                throw new RuntimeException("The parameters of the car remain unchanged. Therefore, car mode should not end up here, it should have been removed in the previous step. ");
            } else {
                PlanCalcScoreConfigGroup.ModeParams sourceModeParam = copyOfModeParam(oldParameterSet.getModes().get(mode));
                {// positive: since this mode is never updated before, update existing one only
                    double newASC =  sourceModeParam.getConstant() + opdytsConfigGroup.getDecisionVariableStepSize() * randomVariationOfStepSize;
                    allCombinations.parallelStream().forEach(e -> e.getOrCreateScoringParameters(this.subPopName).getOrCreateModeParams(mode).setConstant(newASC) );
                }
                { // negative: since this mode is already updated above, first copy existing ones, update values and then add them to main collection
                    List<PlanCalcScoreConfigGroup> tempCombinations = new ArrayList<>();
                    allCombinations.parallelStream().forEach(e -> tempCombinations.add(copyOfPlanCalcScore(e.getScoringParameters(this.subPopName))));
                    double newASC =  sourceModeParam.getConstant() - opdytsConfigGroup.getDecisionVariableStepSize() * randomVariationOfStepSize;
                    tempCombinations.parallelStream().forEach(e -> e.getOrCreateScoringParameters(this.subPopName).getOrCreateModeParams(mode).setConstant(newASC) );
                    allCombinations.addAll(tempCombinations);
                }
            }
            createDiagonalCombinations(oldParameterSet, allCombinations, remainingModes, randomVariationOfStepSize);
        }
    }

    private List<PlanCalcScoreConfigGroup> createAxialCombinations(final PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet, final double randomVariationOfStepSize) {

        final List<PlanCalcScoreConfigGroup> allCombinations = new ArrayList<>();
        for(String mode : this.considerdModes) {
            if ( mode.equals(TransportMode.car)) continue;
            { // positive
                PlanCalcScoreConfigGroup configGroupWithStartingModeParams = copyOfPlanCalcScore(oldParameterSet);
                PlanCalcScoreConfigGroup.ModeParams sourceModeParam = configGroupWithStartingModeParams.getOrCreateScoringParameters(this.subPopName).getModes().get(mode);
                double newASC =  sourceModeParam.getConstant() + opdytsConfigGroup.getDecisionVariableStepSize() * randomVariationOfStepSize;
                sourceModeParam.setConstant(newASC);
                allCombinations.add(configGroupWithStartingModeParams);
            }
            { // negative
                PlanCalcScoreConfigGroup configGroupWithStartingModeParams = copyOfPlanCalcScore(oldParameterSet);
                PlanCalcScoreConfigGroup.ModeParams sourceModeParam = configGroupWithStartingModeParams.getOrCreateScoringParameters(this.subPopName).getModes().get(mode);
                double newASC =  sourceModeParam.getConstant() - opdytsConfigGroup.getDecisionVariableStepSize() *  randomVariationOfStepSize;
                sourceModeParam.setConstant(newASC);
                allCombinations.add(configGroupWithStartingModeParams);
            }
        }
        return allCombinations;
    }

    private PlanCalcScoreConfigGroup.ModeParams copyOfModeParam(final PlanCalcScoreConfigGroup.ModeParams modeParams) {
        PlanCalcScoreConfigGroup.ModeParams newModeParams = new PlanCalcScoreConfigGroup.ModeParams(modeParams.getMode());
        newModeParams.setConstant(modeParams.getConstant());
        newModeParams.setMarginalUtilityOfDistance(modeParams.getMarginalUtilityOfDistance());
        newModeParams.setMarginalUtilityOfTraveling(modeParams.getMarginalUtilityOfTraveling());
        newModeParams.setMonetaryDistanceRate(modeParams.getMonetaryDistanceRate());
        return newModeParams;
    }

    private PlanCalcScoreConfigGroup copyOfPlanCalcScore(final PlanCalcScoreConfigGroup.ScoringParameterSet oldParameterSet){
        PlanCalcScoreConfigGroup configGroupWithStartingModeParams = new PlanCalcScoreConfigGroup();
        for ( String newMode : this.considerdModes) {
            PlanCalcScoreConfigGroup.ModeParams modeParams = oldParameterSet.getModes().get(newMode);
            configGroupWithStartingModeParams.getScoringParametersPerSubpopulation().get(this.subPopName).addModeParams(copyOfModeParam(modeParams) );
        }
        return configGroupWithStartingModeParams;
    }
}