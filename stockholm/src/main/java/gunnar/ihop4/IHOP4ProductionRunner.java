/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package gunnar.ihop4;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.opdyts.MATSimOpdytsRunner;
import org.matsim.contrib.opdyts.OpdytsConfigGroup;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes.ActivityTimesUtils;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariable;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.OneAtATimeRandomizer;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.utils.EveryIterationScoringParameters;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.microstate.MATSimStateFactory;
import org.matsim.contrib.opdyts.microstate.MATSimStateFactoryImpl;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunction;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunctionSum;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.contrib.pseudosimulation.searchacceleration.Greedo;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

import gunnar.ihop4.tollzonepassagedata.TollZoneMeasurementReader;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class IHOP4ProductionRunner {

	private static final Logger log = Logger.getLogger(Greedo.class);

	static void keepOnlyStrictCarUsers(final Scenario scenario) {
		final Set<Id<Person>> remove = new LinkedHashSet<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement planEl : person.getSelectedPlan().getPlanElements()) {
				if (planEl instanceof Leg && !TransportMode.car.equals(((Leg) planEl).getMode())) {
					remove.add(person.getId());
				}
			}
		}
		log.info("before strict-car filter: " + scenario.getPopulation().getPersons().size());
		for (Id<Person> removeId : remove) {
			scenario.getPopulation().getPersons().remove(removeId);
		}
		log.info("after strict-car filter: " + scenario.getPopulation().getPersons().size());
	}

	static void run(boolean useGreedo) {

		final Greedo greedo = (useGreedo ? new Greedo() : null);
		if (greedo != null) {
			greedo.setAdjustStrategyWeights(true);
		}

		// Config.

		final Config config = ConfigUtils
				.loadConfig("/Users/GunnarF/NoBackup/data-workspace/ihop4/production-scenario/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		if (greedo != null) {
			greedo.meet(config);
		}

		// Scenario.

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		keepOnlyStrictCarUsers(scenario);
		if (greedo != null) {
			greedo.meet(scenario);
		}

		// Controler.

		final Controler controler = new Controler(scenario);
		if (greedo != null) {
			controler.addOverridingModule(greedo);
		}

		// if (calibrate) {
		//
		// final MATSimObjectiveFunctionSum<MATSimState> overallObjectiveFunction = new
		// MATSimObjectiveFunctionSum<>();
		//
		// /*
		// * (1) Load sensor data and build objective function.
		// */
		//
		// final TollZoneMeasurementReader measReader = new
		// TollZoneMeasurementReader(config);
		// measReader.run();
		//
		// for (MATSimObjectiveFunction<MATSimState> objectiveFunctionComponent :
		// measReader.getAllDayMeasurements().getObjectiveFunctions()) {
		// overallObjectiveFunction.add(objectiveFunctionComponent, 1.0);
		// }
		//
		// for (AbstractModule module : measReader.getAllDayMeasurements().getModules())
		// {
		// controler.addOverridingModule(module);
		// }
		// for (AbstractModule module :
		// measReader.getOnlyTollTimeMeasurements().getModules()) {
		// controler.addOverridingModule(module);
		// }
		//
		// /*
		// * (2) Create decision variable.
		// */
		//
		//
		//
		// // Checking if this really works:
		//
		// final int testIteration = 10;
		//
		// BeforeMobsimListener testListener = new BeforeMobsimListener() {
		// @Override
		// public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// if (event.getIteration() == testIteration) {
		// for (AbsoluteLinkEntryCountDeviationObjectiveFunction objFct : measReader
		// .getAllDayMeasurements().getObjectiveFunctions()) {
		// System.out.println();
		// System.out.println(objFct);
		// }
		// for (AbsoluteLinkEntryCountDeviationObjectiveFunction objFct : measReader
		// .getOnlyTollTimeMeasurements().getObjectiveFunctions()) {
		// System.out.println();
		// System.out.println(objFct);
		// }
		// System.out.println();
		// System.out.println("TERMINATING IN ITERATION " + event.getIteration());
		// System.exit(0);
		// }
		// }
		// };
		//
		// controler.addOverridingModule(new AbstractModule() {
		// @Override
		// public void install() {
		// this.addControlerListenerBinding().toInstance(testListener);
		// }
		// });
		//
		// }

		// ... and run.

		controler.run();
	}

	static void calibrate(boolean useGreedo) {

		final OpdytsGreedoProgressListener progressListener = new OpdytsGreedoProgressListener("progress.log");

		final Greedo greedo = (useGreedo ? new Greedo() : null);
		if (greedo != null) {
			greedo.setAdjustStrategyWeights(true);
			greedo.setGreedoProgressListener(progressListener);
		}

		// Config.

		final Config config = ConfigUtils
				.loadConfig("/Users/GunnarF/NoBackup/data-workspace/ihop4/production-scenario/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		if (greedo != null) {
			greedo.meet(config);
		}

		// Opdyts configuration.

		final OpdytsConfigGroup opdytsConfig = ConfigUtils.addOrGetModule(config, OpdytsConfigGroup.class);
		opdytsConfig.setBinCount(24 * 12);
		opdytsConfig.setBinSize(3600 / 12);
		opdytsConfig.setInertia(0.9);
		opdytsConfig.setInitialEquilibriumGapWeight(0.0);
		opdytsConfig.setInitialUniformityGapWeight(0.0);
		opdytsConfig.setMaxIteration(100);
		opdytsConfig.setMaxMemoryPerTrajectory(Integer.MAX_VALUE);
		opdytsConfig.setMaxTotalMemory(Integer.MAX_VALUE);
		opdytsConfig.setMaxTransition(Integer.MAX_VALUE);
		opdytsConfig.setNoisySystem(true);
		opdytsConfig.setNumberOfIterationsForAveraging(5); // TODO REMOVE
		opdytsConfig.setNumberOfIterationsForConvergence(10); // TODO REMOVE
		opdytsConfig.setSelfTuningWeightScale(1.0);
		opdytsConfig.setStartTime(0);
		opdytsConfig.setUseAllWarmUpIterations(true);
		opdytsConfig.setWarmUpIterations(1);

		if (greedo != null) {
			opdytsConfig.setEnBlockSimulationIterations(
					ConfigUtils.addOrGetModule(config, PSimConfigGroup.class).getIterationsPerCycle());
		}

		// Scenario.

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		keepOnlyStrictCarUsers(scenario);
		if (greedo != null) {
			greedo.meet(scenario);
		}

		// // Controler.
		//
		// final Controler controler = new Controler(scenario);
		// if (greedo != null) {
		// controler.addOverridingModule(greedo);
		// }

		// Decision variables.

		final double timeVariationStepSize_s = 15 * 60;
		final double searchStageExponent = 0.0;
		final CompositeDecisionVariable allActivityTimesDecisionVariable = ActivityTimesUtils
				.newAllActivityTimesDecisionVariable(config, timeVariationStepSize_s, searchStageExponent);

		// Decision variable randomizer.

		final OneAtATimeRandomizer decisionVariableRandomizer = new OneAtATimeRandomizer();

		// State factory.

		final MATSimStateFactory<CompositeDecisionVariable, MATSimState> stateFactory = new MATSimStateFactoryImpl<>();

		// Objective function.

		final MATSimObjectiveFunctionSum<MATSimState> overallObjectiveFunction = new MATSimObjectiveFunctionSum<>();

		final TollZoneMeasurementReader measReader = new TollZoneMeasurementReader(config);
		measReader.run();

		for (MATSimObjectiveFunction<MATSimState> objectiveFunctionComponent : measReader.getAllDayMeasurements()
				.getObjectiveFunctions()) {
			overallObjectiveFunction.add(objectiveFunctionComponent, 1.0);
		}

		// Opdyts runner.

		final MATSimOpdytsRunner<CompositeDecisionVariable, MATSimState> runner = new MATSimOpdytsRunner<>(scenario,
				stateFactory);
		runner.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
			}
		});
		for (AbstractModule module : measReader.getAllDayMeasurements().getModules()) {
			runner.addOverridingModule(module);
		}
		for (AbstractModule module : measReader.getOnlyTollTimeMeasurements().getModules()) {
			runner.addOverridingModule(module);
		}
		if (greedo != null) {
			// runner.addWantsControlerReferenceBeforeInjection(greedo);
			runner.addOverridingModule(greedo);
		}
		runner.setOpdytsProgressListener(progressListener);
		// runner.setConvergenceCriterion(new AR1ConvergenceCriterion(2.5));
		runner.run(decisionVariableRandomizer, allActivityTimesDecisionVariable, overallObjectiveFunction);
	}

	public static void main(String[] args) {
		final boolean useGreedo = true;
		calibrate(useGreedo);
	}
}
