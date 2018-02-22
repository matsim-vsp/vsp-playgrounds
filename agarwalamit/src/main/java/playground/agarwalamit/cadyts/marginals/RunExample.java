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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
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
        double cadytsWt = 2000;
        double beta = 1.;
        double beelineDistanceFactorForNetworkModes = 1.0;

        Config config = ConfigUtils.loadConfig(configFile);
        config.planCalcScore().setBrainExpBeta(beta);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setLastIteration(20);
        config.counts().setWriteCountsInterval(1);
        config.counts().setAverageCountsOverIterations(3);

        // add mode choice to it
        StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
        //don't use subTourModeChoice here which does not change mode if plan has only one trip.
        modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name());
        modeChoice.setWeight(0.15);
        config.strategy().addStrategySettings(modeChoice);
        config.changeMode().setModes(new String[]{"car","bicycle"});
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

        config.planCalcScore().getOrCreateModeParams("car").setConstant(-2);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        addPersonsWithShortTrips(scenario);

        DistanceDistribution inputDistanceDistribution = new DistanceDistribution();
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes("car",beelineDistanceFactorForNetworkModes);
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes("bicycle",beelineDistanceFactorForNetworkModes);

        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(0.0,6000.),2);
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(0.0,6000.),8);

        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(6000.0,12000.),4);
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(6000.0,12000.),4);

        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(12000.0,18000.),5);
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(12000.0,18000.),2);

        inputDistanceDistribution.addToDistribution("car", new DistanceBin.DistanceRange(18000.0,86000.),20);
        inputDistanceDistribution.addToDistribution("bicycle", new DistanceBin.DistanceRange(18000.0,86000.),0);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new ModalDistanceCadytsModule(inputDistanceDistribution));

        final double cadytsScoringWeight = cadytsWt * config.planCalcScore().getBrainExpBeta();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindScoringFunctionFactory().toInstance(new ScoringFunctionFactory() {
                    @Inject
                    private ScoringParametersForPerson parameters;
                    @Inject
                    private ModalDistanceCadytsContext cContext;
                    @Inject
                    private Network network;
                    @Inject
                    private Config config;

                    @Override
                    public ScoringFunction createNewScoringFunction(Person person) {
                        SumScoringFunction sumScoringFunction = new SumScoringFunction();

                        final ScoringParameters params = parameters.getScoringParameters(person);

                        sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, network));
                        sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
                        sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

                        final CadytsScoring<ModalBin> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(),
                                config,
                                cContext);
                        scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
                        sumScoringFunction.addScoringFunction(scoringFunction);

                        return sumScoringFunction;
                    }
                });
            }
        });
        controler.run();

    }

    private static void addPersonsWithShortTrips(Scenario scenario) {
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();

        // make sure all existing plans uses car
        population.getPersons()
                  .values()
                  .stream()
                  .flatMap(p -> p.getPlans().stream())
                  .flatMap(p -> p.getPlanElements().stream())
                  .filter(Leg.class::isInstance)
                  .forEach(pe -> ((Leg) pe).setMode("car"));

        // 10 plans (20 trips) already exists--all from link1 to link20 and link 20 to link 1
        // beeline dists for each trip link1 to link20 (or link20 to link1) --->20km

        // short trips
        createPersonAndPlan(population, network, 9, 18, 100); //node10-node12 = 4.98 km
        createPersonAndPlan(population, network, 9, 18, 300); //node10-node12 = 4.98 km
        createPersonAndPlan(population, network, 9, 18, 800); //node10-node12 = 4.98 km

        createPersonAndPlan(population, network, 5, 14, 100); //node6-node12 = 4.99 km
        createPersonAndPlan(population, network, 5, 14, 250); //node6-node12 = 4.99 km
        createPersonAndPlan(population, network, 5, 14, 900); //node6-node12 = 4.99 km

        createPersonAndPlan(population, network, 23, 1, 100); //node1-node2 = 5 km
        createPersonAndPlan(population, network, 23, 1, 800); //node1-node2 = 5 km
        createPersonAndPlan(population, network, 6, 15, 50); //node7-node12 = 5 km
        createPersonAndPlan(population, network, 6, 15, 100); //node7-node12 = 5 km

        createPersonAndPlan(population, network, 1, 6, 90); //node2-node7 = 10 km
        createPersonAndPlan(population, network, 1, 6, 500); //node2-node7 = 10 km
        createPersonAndPlan(population, network, 1, 6, 300); //node2-node7 = 10 km
        createPersonAndPlan(population, network, 1, 6, 1000); //node2-node7 = 10 km

        createPersonAndPlan(population, network, 15, 21, 1000); //node12-node14 = 11.18 km
        createPersonAndPlan(population, network, 15, 21, 200); //node12-node14 = 11.18 km
        createPersonAndPlan(population, network, 15, 21, 900); //node12-node14 = 11.18 km
        createPersonAndPlan(population, network, 15, 21, 550); //node12-node14 = 11.18 km

        createPersonAndPlan(population, network, 1, 15, 600); //node2-node12 = 15 km
        createPersonAndPlan(population, network, 1, 15, 100); //node2-node12 = 15 km
        createPersonAndPlan(population, network, 1, 15, 1000); //node2-node12 = 15 km

        createPersonAndPlan(population, network, 1, 2, 200); //node2-node3 = 17 km
        createPersonAndPlan(population, network, 1, 2, 600); //node2-node3 = 17 km
        createPersonAndPlan(population, network, 1, 2, 900); //node2-node3 = 17 km
        createPersonAndPlan(population, network, 1, 2, 600); //node2-node3 = 17 km
    }

    private static void createPersonAndPlan(Population population, Network network, int startLinkId, int endLinkId, int factor) {
        Id<Person> personId = Id.createPersonId(population.getPersons().size() + 1);
        Person person = population.getFactory().createPerson(personId);
        Plan plan = population.getFactory().createPlan();
        //link23 to link 1
        Activity h = population.getFactory()
                               .createActivityFromCoord("h",
                                       network.getLinks().get(Id.createLinkId(startLinkId)).getCoord());
        h.setEndTime(6. * 3600. + factor);
        plan.addActivity(h);
        plan.addLeg(population.getFactory().createLeg("car"));
        Activity w = population.getFactory()
                               .createActivityFromCoord("w",
                                       network.getLinks().get(Id.createLinkId(endLinkId)).getCoord());
        w.setEndTime(16. * 3600. + factor);
        plan.addActivity(w);
        person.addPlan(plan);
        population.addPerson(person);
    }


}
