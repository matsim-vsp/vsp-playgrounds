/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.nemo.calibration.locationChoice;

import javax.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
 * Created by amit on 03.11.17.
 */

public class NemoLocationChoiceCalibration {

    public static void main(String[] args) {

        String configFile = "data/locationChoice/run1/input/config.xml";
        String plansFile = "data/input/matsim_initial_plans/nrw_plans_1pct.xml.gz";
        String networkFile = "data/input/network/allWaysNRW/tertiaryNemo_Network_31102017filteredcleaned_network.xml.gz";
        String outputDir = "data/locationChoice/run1/output/";
        String runId = "run1";
        double flowCapFactor = 0.01;
        double storageCapFactor = 0.03;
        int lastIt = 300;
        double cadytsWt = 0.15;

        if (args.length>0){
            configFile = args[0];
            plansFile = args[1];
            networkFile = args[2];
            outputDir = args[3];
            runId = args[4];
            flowCapFactor = Double.valueOf(args[5]);
            storageCapFactor = Double.valueOf(args[6]);
            lastIt = Integer.valueOf(args[7]);
            cadytsWt = Double.valueOf(args[8]);
        }

        Config config = ConfigUtils.loadConfig(configFile);

        config.network().setInputFile(networkFile);
        config.plans().setInputFile(plansFile);
        config.controler().setOutputDirectory(outputDir);
        config.controler().setRunId(runId);
        config.controler().setLastIteration(lastIt);
        config.qsim().setFlowCapFactor(flowCapFactor);
        config.qsim().setStorageCapFactor(storageCapFactor);

        Scenario scenario = ScenarioUtils.createScenario(config);
        ScenarioUtils.loadScenario(scenario);

        Controler controler = new Controler(scenario);

        final double cadytsScoringWeight = cadytsWt * config.planCalcScore().getBrainExpBeta();

        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Inject
            private CadytsContext cadytsContext;
            @Inject
            ScoringParametersForPerson parameters;
            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                SumScoringFunction sumScoringFunction = new SumScoringFunction();

                final ScoringParameters params = parameters.getScoringParameters(person);
                sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
                sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
                sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, cadytsContext);
                scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
                sumScoringFunction.addScoringFunction(scoringFunction);

                return sumScoringFunction;
            }
        });
        controler.run();
    }
}
