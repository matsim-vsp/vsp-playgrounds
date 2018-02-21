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

package playground.agarwalamit.cadyts.marginals;

import javax.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
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

/**
 * Created by amit on 21.02.18.
 */

public class RunExample {

    public static void main(String[] args) {

        String configFile = "/Users/amit/Documents/git/matsim-code-examples/scenarios/equil-mixedTraffic/config-with-mode-vehicles.xml";
        double cadytsWt = 0.15;
        Config config = ConfigUtils.loadConfig(configFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        String distanceDistributionFile = "/Users/amit/Documents/gitlab/mercator-nemo/doc/40_documents/validation/distanceDistriEssen.txt";
        DistanceDistribution inputDistanceDistribution = new DistanceDistribution();
        inputDistanceDistribution.fillDistanceDistribution(distanceDistributionFile, DistanceDistributionUtils.DistanceUnit.meter,"/t");

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new ModalDistanceCadytsModule(inputDistanceDistribution));

        final double cadytsScoringWeight = cadytsWt * config.planCalcScore().getBrainExpBeta();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {

                bindScoringFunctionFactory().toInstance(new ScoringFunctionFactory() {
                    @Inject
                    private ScoringParametersForPerson parameters ;
                    @Inject private ModalDistanceCadytsContext cContext;
                    @Inject private Network network;
                    @Inject private Config config;

                    @Override
                    public ScoringFunction createNewScoringFunction(Person person) {
                        SumScoringFunction sumScoringFunction = new SumScoringFunction();

                        final ScoringParameters params = parameters.getScoringParameters( person );

                        sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, network));
                        sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
                        sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                        final CadytsScoring<ModalBin> scoringFunction = new CadytsScoring<>( person.getSelectedPlan(), config, cContext);
                        scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
                        sumScoringFunction.addScoringFunction(scoringFunction);

                        return sumScoringFunction;
                    }
                });
            }
        });
        controler.run();

    }



}
