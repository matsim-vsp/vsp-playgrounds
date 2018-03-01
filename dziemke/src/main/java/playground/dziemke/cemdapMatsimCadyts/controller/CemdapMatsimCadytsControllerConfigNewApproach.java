/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.dziemke.cemdapMatsimCadyts.controller;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import playground.dziemke.analysis.SelectedPlansAnalyzer;
import playground.dziemke.analysis.TripAnalyzerV2Extended;
import playground.vsp.cadyts.marginals.AgentFilter;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsContext;
import playground.vsp.cadyts.marginals.ModalDistanceCadytsModule;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.ModalDistanceBinIdentifier;

/**
 * @author dziemke
 */
public class CemdapMatsimCadytsControllerConfigNewApproach {
	
	public static void main(final String[] args) {
		
		final String configFile = args[0];
		final double cadytsCountsWt = Double.parseDouble(args[1]);
		final double cadytsMarginalsWt = Double.parseDouble(args[2]);

		final Config config = ConfigUtils.loadConfig(configFile);				
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		
		// marginals cadyts
        final double beelineDistanceFactorForNetworkModes = 1.3;
        DistanceDistribution inputDistanceDistribution = getInputDistanceDistribution(beelineDistanceFactorForNetworkModes);
		controler.addOverridingModule(new ModalDistanceCadytsModule(inputDistanceDistribution));
        final double cadytsMarginalsScoringWeight = cadytsMarginalsWt * config.planCalcScore().getBrainExpBeta();

        // counts cadyts
        controler.addOverridingModule(new CadytsCarModule());
        
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(AgentFilter.class).to(BerlinAgentFilter.class);
            }
        });
        
        final double cadytsCountsScoringWeight = cadytsCountsWt * config.planCalcScore().getBrainExpBeta();

        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject
            private CadytsContext cadytsContext;
            @Inject
            ScoringParametersForPerson parameters;
            @Inject
            private ModalDistanceCadytsContext marginalCadytsContext;

            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                SumScoringFunction sumScoringFunction = new SumScoringFunction();

                final ScoringParameters params = parameters.getScoringParameters(person);
                sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params,
                        controler.getScenario().getNetwork()));
                sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
                sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                final CadytsScoring<Link> scoringFunctionCounts = new CadytsScoring<Link>(person.getSelectedPlan(),
                        config,
                        cadytsContext);
                scoringFunctionCounts.setWeightOfCadytsCorrection(cadytsCountsScoringWeight);

                final CadytsScoring<ModalDistanceBinIdentifier> scoringFunctionMarginals = new CadytsScoring<>(person.getSelectedPlan(),
                        config,
                        marginalCadytsContext);
                scoringFunctionMarginals.setWeightOfCadytsCorrection(cadytsMarginalsScoringWeight);

                sumScoringFunction.addScoringFunction(scoringFunctionCounts);
                sumScoringFunction.addScoringFunction(scoringFunctionMarginals);

                return sumScoringFunction;
            }
        });	
	}
	
	private static DistanceDistribution getInputDistanceDistribution(double beelineDistanceFactorForNetworkModes){
        DistanceDistribution inputDistanceDistribution = new DistanceDistribution();
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes("car",beelineDistanceFactorForNetworkModes);
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes("bicycle",beelineDistanceFactorForNetworkModes);
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes("walk",beelineDistanceFactorForNetworkModes);

        inputDistanceDistribution.setModeToScalingFactor("car", 5.9); // car + pt
        inputDistanceDistribution.setModeToScalingFactor("bicycle", 10);
        inputDistanceDistribution.setModeToScalingFactor("walk", 10);
        
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(0.,1000.),241339+61162); // car + pt
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(0.,1000.),330404);
        inputDistanceDistribution.addToDistribution("walk", new DistanceBin.DistanceRange(0.,1000.),2245049);

        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(1000.,3000.),700995+370121); // car + pt
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(1000.,3000.),517133);
        inputDistanceDistribution.addToDistribution("walk", new DistanceBin.DistanceRange(1000.,3000.),637301);

        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(3000.,5000.),594879+423928); // car + pt
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(3000.,5000.),234520);
        inputDistanceDistribution.addToDistribution("walk", new DistanceBin.DistanceRange(3000.,5000.),82478);

        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(5000.,10000.),843400+785500); // car + pt
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(5000.,10000.),198907);
        inputDistanceDistribution.addToDistribution("walk", new DistanceBin.DistanceRange(5000.,10000.),22477);
        
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(10000.,20000.),680752+753850); // car + pt
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(10000.,20000.),54255);
        inputDistanceDistribution.addToDistribution("walk", new DistanceBin.DistanceRange(10000.,20000.),2528);
        
        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(20000.,10000000.),338635+335439); // car + pt
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(20000.,10000000.),4781);
        inputDistanceDistribution.addToDistribution("walk", new DistanceBin.DistanceRange(20000.,10000000.),167);
        
        return inputDistanceDistribution;
    }
}