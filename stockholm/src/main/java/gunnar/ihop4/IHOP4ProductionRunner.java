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
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.AbsoluteLinkEntryCountDeviationObjectiveFunction;
import org.matsim.contrib.pseudosimulation.searchacceleration.Greedo;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.scenario.ScenarioUtils;

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

	static void run(boolean useGreedo, boolean useData) {

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

		if (useData) {

			// Toll zone link flows.

			final TollZoneMeasurementReader measReader = new TollZoneMeasurementReader(config);
			measReader.run();
			for (AbstractModule module : measReader.getAllDayMeasurements().getModules()) {
				controler.addOverridingModule(module);
			}
			for (AbstractModule module : measReader.getOnlyTollTimeMeasurements().getModules()) {
				controler.addOverridingModule(module);
			}

			// Checking if this really works:

			final int testIteration = 10;

			BeforeMobsimListener testListener = new BeforeMobsimListener() {
				@Override
				public void notifyBeforeMobsim(BeforeMobsimEvent event) {
					if (event.getIteration() == testIteration) {
						for (AbsoluteLinkEntryCountDeviationObjectiveFunction objFct : measReader
								.getAllDayMeasurements().getObjectiveFunctions()) {
							System.out.println();
							System.out.println(objFct);
						}
						for (AbsoluteLinkEntryCountDeviationObjectiveFunction objFct : measReader
								.getOnlyTollTimeMeasurements().getObjectiveFunctions()) {
							System.out.println();
							System.out.println(objFct);
						}
						System.out.println();
						System.out.println("TERMINATING IN ITERATION " + event.getIteration());
						System.exit(0);
					}
				}
			};

			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addControlerListenerBinding().toInstance(testListener);
				}
			});

		}

		// ... and run.

		controler.run();
	}

	// static class MyStateFactory extends MATSimStateFactoryImpl<OpeningTimes,
	// MATSimState>
	// implements AfterMobsimListener {
	//
	// @Inject
	// private LegHistogram legHist;
	//
	// private LegHistogramObjectiveFunction.StateComponent legHistogramData = null;
	//
	// @Override
	// public void notifyAfterMobsim(final AfterMobsimEvent event) {
	// this.legHistogramData = new LegHistogramObjectiveFunction.StateComponent();
	// for (String mode : this.legHist.getLegModes()) {
	// this.legHistogramData.mode2departureData.put(mode,
	// this.legHist.getDepartures(mode));
	// this.legHistogramData.mode2arrivalData.put(mode,
	// this.legHist.getArrivals(mode));
	// }
	// }
	//
	// @Override
	// protected void addComponents(final MATSimState state) {
	// state.putComponent(LegHistogramObjectiveFunction.StateComponent.class,
	// this.legHistogramData);
	// }
	// }

	// static void optimize(final boolean useGreedo) {
	//
	// OpdytsGreedoProgressListener progressListener = new
	// OpdytsGreedoProgressListener("progress.log");
	//
	// final Greedo greedo = (useGreedo ? new Greedo() : null);
	// if (greedo != null) {
	// greedo.setAdjustStrategyWeights(true);
	// greedo.setGreedoProgressListener(progressListener);
	// }
	//
	// final Config config = ConfigUtils
	// .loadConfig("/Users/GunnarF/NoBackup/data-workspace/ihop4/production-scenario/config.xml");
	// config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
	// if (greedo != null) {
	// greedo.meet(config);
	// }
	//
	// OpdytsConfigGroup opdytsConfig = ConfigUtils.addOrGetModule(config,
	// OpdytsConfigGroup.class);
	// opdytsConfig.setBinCount(24 * 12);
	// opdytsConfig.setBinSize(3600 / 12);
	// opdytsConfig.setInertia(0.9);
	// opdytsConfig.setInitialEquilibriumGapWeight(0.0);
	// opdytsConfig.setInitialUniformityGapWeight(0.0);
	// opdytsConfig.setMaxIteration(100);
	// opdytsConfig.setMaxMemoryPerTrajectory(Integer.MAX_VALUE);
	// opdytsConfig.setMaxTotalMemory(Integer.MAX_VALUE);
	// opdytsConfig.setMaxTransition(Integer.MAX_VALUE);
	// opdytsConfig.setNoisySystem(true);
	// opdytsConfig.setNumberOfIterationsForAveraging(5); // TODO REMOVE
	// opdytsConfig.setNumberOfIterationsForConvergence(10); // TODO REMOVE
	// opdytsConfig.setSelfTuningWeightScale(1.0);
	// opdytsConfig.setStartTime(0);
	// opdytsConfig.setUseAllWarmUpIterations(true);
	// opdytsConfig.setWarmUpIterations(1);
	//
	// // TODO NEW:
	// if (greedo != null) {
	// opdytsConfig.setEnBlockSimulationIterations(
	// ConfigUtils.addOrGetModule(config,
	// PSimConfigGroup.class).getIterationsPerCycle());
	// }
	//
	// // OBJECTIVE FUNCTION
	//
	// Set<String> modes = new LinkedHashSet<>(Arrays.asList("car"));
	//
	// double[] realDepartures = new double[362];
	// final ObjectiveFunction<MATSimState> dptObjFct =
	// LegHistogramObjectiveFunction.newDepartures(modes,
	// realDepartures);
	// final WeightedSumObjectiveFunction<MATSimState> objFct = new
	// WeightedSumObjectiveFunction<>();
	// objFct.add(dptObjFct, 1.0);
	//
	// // DECISION VARIABLE RANDOMIZER
	//
	// int maxNumberOfVariedElements = 4; // work and other open and close
	// double initialSearchRange_s = 600;
	// double searchStageExponent = 0;
	// final DecisionVariableRandomizer<OpeningTimes> randomizer = new
	// OpeningTimesRandomizer(
	// maxNumberOfVariedElements, initialSearchRange_s, searchStageExponent);
	//
	// // STATE FACTORY
	//
	// MyStateFactory stateFactory = new MyStateFactory();
	//
	// // WIRE EVERYTHING TOGETHER
	//
	// Scenario scenario = ScenarioUtils.loadScenario(config);
	// keepOnlyStrictCarUsers(scenario);
	// greedo.meet(scenario);
	//
	// MATSimOpdytsRunner<OpeningTimes, MATSimState> runner = new
	// MATSimOpdytsRunner<>(scenario, stateFactory);
	// runner.addOverridingModule(new AbstractModule() {
	// @Override
	// public void install() {
	// bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
	// this.addControlerListenerBinding().toInstance(stateFactory);
	// }
	// });
	// if (greedo != null) {
	// // runner.addWantsControlerReferenceBeforeInjection(greedo);
	// runner.addOverridingModule(greedo);
	// }
	// runner.setOpdytsProgressListener(progressListener);
	// // runner.setConvergenceCriterion(new AR1ConvergenceCriterion(2.5));
	// runner.run(randomizer, new OpeningTimes(config), objFct);
	// }

	public static void main(String[] args) {
		final boolean useGreedo = true;
		final boolean useData = false;
		run(useGreedo, useData);
		// optimize(useGreedo);
	}
}
