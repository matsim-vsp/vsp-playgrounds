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

package playground.vsp.cadyts.marginals;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;

import javax.inject.Inject;
import java.net.URL;

/**
 * Created by amit on 21.02.18.
 */

public class RunExample {

    public static void main(String[] args, URL configUrl) {
        String configFile = "../../git/matsim-code-examples/scenarios/equil-mixedTraffic/config-with-mode-vehicles.xml";
        String outputDir = "";
        double cadytsWt = 2500.;
        int lastIt =20;
        Config config;

        if (args.length>0){
            outputDir = args[0];
            cadytsWt = Double.valueOf(args[1]);
            lastIt = Integer.valueOf(args[2]);

            config = ConfigUtils.loadConfig(configUrl);
        } else {
            config = ConfigUtils.loadConfig(configFile);
        }
        double beelineDistanceFactorForNetworkModes = 1.0;

        Scenario scenario = prepareScenario(outputDir, lastIt, config);        
        DistanceDistribution inputDistanceDistribution = getInputDistanceDistribution(beelineDistanceFactorForNetworkModes);

        Controler controler = new Controler(scenario);

        addCadytsModules(cadytsWt, config, inputDistanceDistribution, controler);
        controler.run();
    }

	/**
	 * @param outputDir
	 * @param lastIt
	 * @param config
	 * @return
	 */
	public static Scenario prepareScenario(String outputDir, int lastIt, Config config) {
		prepareConfig(outputDir, lastIt, config);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        addPersonsWithShortTrips(scenario);
		return scenario;
	}

	/**
	 * @param cadytsWt
	 * @param config
	 * @param inputDistanceDistribution
	 * @param controler
	 */
	private static void addCadytsModules(double cadytsWt, Config config, DistanceDistribution inputDistanceDistribution,
			Controler controler) {
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

						final CadytsScoring<Id<DistanceDistribution.DistanceBin>> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(),
                                config,
                                cContext);
                        scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
                        sumScoringFunction.addScoringFunction(scoringFunction);

                        return sumScoringFunction;
                    }
                });
            
                	// optional:
        			this.bind(AgentFilter.class).to(RunExampleAgentFilter.class);
        		
            }
            
        });
	}

	/**
	 * @param outputDir
	 * @param lastIt
	 * @param config
	 */
	private static void prepareConfig(String outputDir, int lastIt, Config config) {
		config.controler().setOutputDirectory(outputDir);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setLastIteration(lastIt);
        config.counts().setWriteCountsInterval(1);
        config.counts().setAverageCountsOverIterations(4);

        // add mode choice to it
        StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
        //don't use subTourModeChoice here which does not change mode if plan has only one trip.
        modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode);
        modeChoice.setWeight(0.15);
        config.strategy().addStrategySettings(modeChoice);
        config.changeMode().setModes(new String[]{"car", "bicycle"});
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

        config.planCalcScore().getOrCreateModeParams("car").setConstant(-2);
	}

    public static DistanceDistribution getInputDistanceDistribution(double beelineDistanceFactorForNetworkModes){
		DistanceDistribution inputDistanceDistribution = new DistanceDistribution(beelineDistanceFactorForNetworkModes);

		inputDistanceDistribution.add("car", 0.0, 6000., 10000, 2);
		inputDistanceDistribution.add("bicycle", 0.0, 6000., 10000, 8);

		inputDistanceDistribution.add("car", 6000.0, 12000., 10000, 4);
		inputDistanceDistribution.add("bicycle", 6000.0, 12000., 10000, 4);

		inputDistanceDistribution.add("car", 12000.0, 18000., 10000, 5);
		inputDistanceDistribution.add("bicycle", 12000.0, 18000., 10000, 2);

		inputDistanceDistribution.add("car", 18000.0, 86000., 10000, 20);
		inputDistanceDistribution.add("bicycle", 18000.0, 86000., 10000, 0);

		return inputDistanceDistribution;
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
        createPersonAndPlan(population, network, 9, 18, 3); //node10-node12 = 4.98 km

        createPersonAndPlan(population, network, 5, 14, 3); //node6-node12 = 4.99 km

        createPersonAndPlan(population, network, 23, 1, 4); //node1-node2 = 5 km

        createPersonAndPlan(population, network, 1, 6, 4); //node2-node7 = 10 km

        createPersonAndPlan(population, network, 15, 21, 4); //node12-node14 = 11.18 km

        createPersonAndPlan(population, network, 1, 15, 3); //node2-node12 = 15 km

        createPersonAndPlan(population, network, 1, 2, 4); //node2-node3 = 17 km
    }

    private static void createPersonAndPlan(Population population, Network network, int startLinkId, int endLinkId, int numberOfPlans) {
        for (int i = 0; i<numberOfPlans; i++){
            Id<Person> personId = Id.createPersonId(population.getPersons().size() + 1);
            Person person = population.getFactory().createPerson(personId);
            Plan plan = population.getFactory().createPlan();
            //link23 to link 1
            Activity h = population.getFactory()
                                   .createActivityFromCoord("h",
                                           network.getLinks().get(Id.createLinkId(startLinkId)).getCoord());
            h.setEndTime(6. * 3600. + MatsimRandom.getRandom().nextInt(startLinkId*60));
            plan.addActivity(h);
            plan.addLeg(population.getFactory().createLeg("car"));
            Activity w = population.getFactory()
                                   .createActivityFromCoord("w",
                                           network.getLinks().get(Id.createLinkId(endLinkId)).getCoord());
            w.setEndTime(16. * 3600. + MatsimRandom.getRandom().nextInt(endLinkId*60));
            plan.addActivity(w);
            person.addPlan(plan);
            population.addPerson(person);
        }
    }
}
