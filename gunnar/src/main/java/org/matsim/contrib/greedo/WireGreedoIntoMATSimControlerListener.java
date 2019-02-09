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
package org.matsim.contrib.greedo;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.Ages;
import org.matsim.contrib.greedo.datastructures.PopulationState;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.datastructures.Utilities;
import org.matsim.contrib.greedo.listeners.SlotUsageListener;
import org.matsim.contrib.greedo.logging.AgePercentile;
import org.matsim.contrib.greedo.logging.AvgAge;
import org.matsim.contrib.greedo.logging.AvgAgeWeight;
import org.matsim.contrib.greedo.logging.AvgExpectedDeltaUtilityAccelerated;
import org.matsim.contrib.greedo.logging.AvgExpectedDeltaUtilityUniform;
import org.matsim.contrib.greedo.logging.AvgNonReplannerSize;
import org.matsim.contrib.greedo.logging.AvgRealizedDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgRealizedUtility;
import org.matsim.contrib.greedo.logging.AvgReplannerSize;
import org.matsim.contrib.greedo.logging.AvgUnweightedNonReplannerUtilityChange;
import org.matsim.contrib.greedo.logging.AvgUnweightedReplannerUtilityChange;
import org.matsim.contrib.greedo.logging.AvgUnweightedUtilityChange;
import org.matsim.contrib.greedo.logging.AvgWeightedNonReplannerUtilityChange;
import org.matsim.contrib.greedo.logging.AvgWeightedReplannerUtilityChange;
import org.matsim.contrib.greedo.logging.AvgWeightedUtilityChange;
import org.matsim.contrib.greedo.logging.Beta;
import org.matsim.contrib.greedo.logging.Delta;
import org.matsim.contrib.greedo.logging.DriversInPhysicalSim;
import org.matsim.contrib.greedo.logging.DriversInPseudoSim;
import org.matsim.contrib.greedo.logging.LambdaBar;
import org.matsim.contrib.greedo.logging.LambdaRealized;
import org.matsim.contrib.greedo.logging.LogDataWrapper;
import org.matsim.contrib.greedo.logging.NormalizedUnweightedCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedUnweightedNonReplannerCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedUnweightedReplannerCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedWeightedCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedWeightedNonReplannerCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedWeightedReplannerCountDifferences2;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.handler.EventHandler;

import com.google.inject.Inject;

import ch.ethz.matsim.ier.IERReplanning;
import ch.ethz.matsim.ier.replannerselection.ReplannerSelector;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
@Singleton
public class WireGreedoIntoMATSimControlerListener implements
		// StartupListener,
		// IterationEndsListener,
		LinkEnterEventHandler, VehicleEntersTrafficEventHandler, PersonEntersVehicleEventHandler,
		VehicleLeavesTrafficEventHandler, ReplannerSelector {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(Controler.class);

	// -------------------- INJECTED MEMBERS --------------------

	@Inject
	private MatsimServices services;

	@Inject
	private IERReplanning ierReplanning;

	// /*
	// * We know if we are in a pSim iteration or in a "real" iteration. The
	// * MobsimSwitcher is updated at iterationStarts, i.e. always *before* the
	// mobsim
	// * (or psim) is executed. The SearchAccelerator, on the other hand, is invoked
	// * at iterationEnds, i.e. *after* the corresponding mobsim (or psim) run.
	// *
	// */
	// @Inject
	// private MobSimSwitcher mobsimSwitcher;
	//
	// @Inject
	// private TravelTime linkTravelTimes;
	//
	// @Inject
	// private TransitEmulator transitEmulator;
	//
	// @Inject
	// private GreedoProgressListener greedoProgressListener;

	// -------------------- NON-INJECTED MEMBERS --------------------

	// private Set<Id<Person>> replanners = null;

	// private PopulationState hypotheticalPopulationState = null;

	private Double expectedUtilityChangeSumAccelerated = null;

	private Double expectedUtilityChangeSumUniform = null;

	private Double realizedUtilityChangeSum = null;

	private Double realizedUtilitySum = null;

	private Integer numberOfReplanners = null;

	// >>> created upon startup >>>

	private SlotUsageListener physicalSlotUsageListener;

	private StatisticsWriter<LogDataWrapper> statsWriter;

	private Utilities utilities;

	private Ages ages;

	// <<< created upon startup <<<

	private GreedoConfigGroup greedoConfig = null;

	private Integer driversInPhysicalSim = null;
	
	private Integer driversInPseudoSim = null;
	
	// -------------------- CONSTRUCTION --------------------

	@Inject
	public WireGreedoIntoMATSimControlerListener() {

		this.greedoConfig = ConfigUtils.addOrGetModule(this.services.getConfig(), GreedoConfigGroup.class);
		this.utilities = new Utilities();
		this.ages = new Ages(this.services.getScenario().getPopulation().getPersons().keySet());
		this.physicalSlotUsageListener = new SlotUsageListener(this.greedoConfig.getTimeDiscretization(),
				this.ages.getPersonWeights(), this.greedoConfig.getLinkWeights(),
				this.greedoConfig.getTransitVehicleWeights());

		this.statsWriter = new StatisticsWriter<>(
				new File(this.services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(),
				false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());

		this.statsWriter.addSearchStatistic(new DriversInPhysicalSim());
		this.statsWriter.addSearchStatistic(new DriversInPseudoSim());

		this.statsWriter.addSearchStatistic(new LambdaRealized());
		this.statsWriter.addSearchStatistic(new LambdaBar());
		this.statsWriter.addSearchStatistic(new Beta());
		this.statsWriter.addSearchStatistic(new Delta());

		this.statsWriter.addSearchStatistic(new AvgAge());
		this.statsWriter.addSearchStatistic(new AvgAgeWeight());

		this.statsWriter.addSearchStatistic(new AvgReplannerSize());
		this.statsWriter.addSearchStatistic(new AvgNonReplannerSize());

		this.statsWriter.addSearchStatistic(new NormalizedUnweightedCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedUnweightedReplannerCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedUnweightedNonReplannerCountDifferences2());

		this.statsWriter.addSearchStatistic(new NormalizedWeightedCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedWeightedReplannerCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedWeightedNonReplannerCountDifferences2());

		this.statsWriter.addSearchStatistic(new AvgUnweightedUtilityChange());
		this.statsWriter.addSearchStatistic(new AvgUnweightedReplannerUtilityChange());
		this.statsWriter.addSearchStatistic(new AvgUnweightedNonReplannerUtilityChange());

		this.statsWriter.addSearchStatistic(new AvgWeightedUtilityChange());
		this.statsWriter.addSearchStatistic(new AvgWeightedReplannerUtilityChange());
		this.statsWriter.addSearchStatistic(new AvgWeightedNonReplannerUtilityChange());

		this.statsWriter.addSearchStatistic(new AvgRealizedUtility());
		this.statsWriter.addSearchStatistic(new AvgRealizedDeltaUtility());
		this.statsWriter.addSearchStatistic(new AvgExpectedDeltaUtilityUniform());
		this.statsWriter.addSearchStatistic(new AvgExpectedDeltaUtilityAccelerated());

		for (int percent = 5; percent <= 95; percent += 5) {
			this.statsWriter.addSearchStatistic(new AgePercentile(percent));
		}
	}

	// -------------------- GETTERS, MAINLY FOR LOGGING --------------------

	public Integer getDriversInPhysicalSim() {
		return this.driversInPhysicalSim;		
//		if (this.lastPhysicalSlotUsages != null) {
//			return this.lastPhysicalSlotUsages.size();
//		} else {
//			return null;
//		}
	}

	public Integer getDriversInPseudoSim() {
		return this.driversInPseudoSim;
	}
	
	public Double getLambdaRealized() {
		return (this.numberOfReplanners.doubleValue()
				/ this.services.getScenario().getPopulation().getPersons().size());
		// if (this.replanners == null) {
		// return null;
		// } else {
		// return ((double) this.replanners.size()) /
		// this.services.getScenario().getPopulation().getPersons().size();
		// }
	}

	public Double getLastExpectedUtilityChangeSumAccelerated() {
		return this.expectedUtilityChangeSumAccelerated;
	}

	public Double getLastExpectedUtilityChangeSumUniform() {
		return this.expectedUtilityChangeSumUniform;
	}

	public Double getLastRealizedUtilityChangeSum() {
		return this.realizedUtilityChangeSum;
	}

	public Double getLastRealizedUtilitySum() {
		return this.realizedUtilitySum;
	}

	public List<Integer> getSortedAgesView() {
		return this.ages.getSortedAgesView();
	}

	public Double getAveragAge() {
		return this.ages.getAverageAge();
	}

	public Double getAverageWeight() {
		return this.ages.getAverageWeight();
	}

	public Integer getPopulationSize() {
		return this.services.getScenario().getPopulation().getPersons().size();
	}

	public Integer getNumberOfReplanners() {
		return this.numberOfReplanners;
		// if (this.replanners == null) {
		// return null;
		// } else {
		// return this.replanners.size();
		// }
	}

	public Integer getNumberOfNonReplanners() {
		if (this.getNumberOfReplanners() == null) {
			return null;
		} else {
			return (this.getPopulationSize() - this.getNumberOfReplanners());
		}
	}

	// // --------------- IMPLEMENTATION OF StartupListener ---------------
	//
	// @Override
	// public void notifyStartup(final StartupEvent event) {
	//
	// // this.greedoProgressListener.callToNotifyStartup_greedo(event);
	//
	// this.greedoConfig = ConfigUtils.addOrGetModule(this.services.getConfig(),
	// GreedoConfigGroup.class);
	// this.utilities = new Utilities();
	// this.ages = new
	// Ages(this.services.getScenario().getPopulation().getPersons().keySet());
	// this.physicalSlotUsageListener = new
	// SlotUsageListener(this.greedoConfig.getTimeDiscretization(),
	// this.ages.getPersonWeights(), this.greedoConfig.getLinkWeights(),
	// this.greedoConfig.getTransitVehicleWeights());
	//
	// this.statsWriter = new StatisticsWriter<>(
	// new File(this.services.getConfig().controler().getOutputDirectory(),
	// "acceleration.log").toString(),
	// false);
	// this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
	//
	// this.statsWriter.addSearchStatistic(new DriversInPhysicalSim());
	// this.statsWriter.addSearchStatistic(new DriversInPseudoSim());
	//
	// this.statsWriter.addSearchStatistic(new LambdaRealized());
	// this.statsWriter.addSearchStatistic(new LambdaBar());
	// this.statsWriter.addSearchStatistic(new Beta());
	// this.statsWriter.addSearchStatistic(new Delta());
	//
	// this.statsWriter.addSearchStatistic(new AvgAge());
	// this.statsWriter.addSearchStatistic(new AvgAgeWeight());
	//
	// this.statsWriter.addSearchStatistic(new AvgReplannerSize());
	// this.statsWriter.addSearchStatistic(new AvgNonReplannerSize());
	//
	// this.statsWriter.addSearchStatistic(new
	// NormalizedUnweightedCountDifferences2());
	// this.statsWriter.addSearchStatistic(new
	// NormalizedUnweightedReplannerCountDifferences2());
	// this.statsWriter.addSearchStatistic(new
	// NormalizedUnweightedNonReplannerCountDifferences2());
	//
	// this.statsWriter.addSearchStatistic(new
	// NormalizedWeightedCountDifferences2());
	// this.statsWriter.addSearchStatistic(new
	// NormalizedWeightedReplannerCountDifferences2());
	// this.statsWriter.addSearchStatistic(new
	// NormalizedWeightedNonReplannerCountDifferences2());
	//
	// this.statsWriter.addSearchStatistic(new AvgUnweightedUtilityChange());
	// this.statsWriter.addSearchStatistic(new
	// AvgUnweightedReplannerUtilityChange());
	// this.statsWriter.addSearchStatistic(new
	// AvgUnweightedNonReplannerUtilityChange());
	//
	// this.statsWriter.addSearchStatistic(new AvgWeightedUtilityChange());
	// this.statsWriter.addSearchStatistic(new AvgWeightedReplannerUtilityChange());
	// this.statsWriter.addSearchStatistic(new
	// AvgWeightedNonReplannerUtilityChange());
	//
	// this.statsWriter.addSearchStatistic(new AvgRealizedUtility());
	// this.statsWriter.addSearchStatistic(new AvgRealizedDeltaUtility());
	// this.statsWriter.addSearchStatistic(new AvgExpectedDeltaUtilityUniform());
	// this.statsWriter.addSearchStatistic(new
	// AvgExpectedDeltaUtilityAccelerated());
	//
	// for (int percent = 5; percent <= 95; percent += 5) {
	// this.statsWriter.addSearchStatistic(new AgePercentile(percent));
	// }
	// }

	// -------------------- IMPLEMENTATION OF EventHandlers --------------------

	@Override
	public void reset(final int iteration) {
		// this.greedoProgressListener.callToReset_greedo(iteration);
		this.physicalSlotUsageListener.reset(iteration);
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		// if (this.mobsimSwitcher.isQSimIteration()) {
		this.physicalSlotUsageListener.handleEvent(event);
		// }
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		// if (this.mobsimSwitcher.isQSimIteration()) {
		this.physicalSlotUsageListener.handleEvent(event);
		// }
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		// if (this.mobsimSwitcher.isQSimIteration()) {
		this.physicalSlotUsageListener.handleEvent(event);
		// }
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		// if (this.mobsimSwitcher.isQSimIteration()) {
		this.physicalSlotUsageListener.handleEvent(event);
		// }
	}

	// -------------------- INTEGRATION WITH IERReplanning --------------------

	private PopulationState lastPhysicalPopulationState = null; // contains last realized utilities!
	private int iteration = 0;

	public void beforeReplanning() {
		this.lastPhysicalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
	}

	private SlotUsageListener hypotheticalSlotUsageListener = null;

	@Override
	public EventHandler getHandlerForHypotheticalNetworkExperience() {
		this.hypotheticalSlotUsageListener = new SlotUsageListener(this.greedoConfig.getTimeDiscretization(),
				this.ages.getPersonWeights(), this.greedoConfig.getLinkWeights(),
				this.greedoConfig.getTransitVehicleWeights());
		this.hypotheticalSlotUsageListener.reset(this.iteration);
		return this.hypotheticalSlotUsageListener;
	}

	public void afterReplanning() {

		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			this.utilities.update(person.getId(),
					this.lastPhysicalPopulationState.getSelectedPlan(person.getId()).getScore(),
					person.getSelectedPlan().getScore());
		}
		final Utilities.SummaryStatistics utilityStatsBeforeReplanning = this.utilities.newSummaryStatistics();

		if (utilityStatsBeforeReplanning.previousDataValid) {

			this.realizedUtilitySum = utilityStatsBeforeReplanning.previousRealizedUtilitySum;
			this.realizedUtilityChangeSum = utilityStatsBeforeReplanning.currentRealizedUtilitySum
					- utilityStatsBeforeReplanning.previousRealizedUtilitySum;
			this.expectedUtilityChangeSumUniform = this.greedoConfig.getReplanningRate(this.iteration)
					* (utilityStatsBeforeReplanning.previousExpectedUtilitySum
							- utilityStatsBeforeReplanning.previousRealizedUtilitySum);

			this.expectedUtilityChangeSumAccelerated = null; // = 0.0;
			// for (Id<Person> replannerId : this.replanners) {
			// this.expectedUtilityChangeSumAccelerated +=
			// this.utilities.getUtilities(replannerId)
			// .getPreviousExpectedUtilityChange();
			// }
		}

		// final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastPseudoSimSlotUsages =
		// this.ierReplanning
		// .getLastHypotheticalSlotUsages();
		// this.hypotheticalPopulationState = new
		// PopulationState(this.services.getScenario().getPopulation());

		final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastPhysicalSimSlotUsages = this.physicalSlotUsageListener
				.getNewIndicatorView();
		final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastPseudoSimSlotUsages = this.hypotheticalSlotUsageListener
				.getNewIndicatorView();

		this.driversInPhysicalSim = lastPhysicalSimSlotUsages.size();
		this.driversInPseudoSim = lastPseudoSimSlotUsages.size();
		
		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(this.greedoConfig, this.iteration,
				lastPhysicalSimSlotUsages, lastPseudoSimSlotUsages,
				this.services.getScenario().getPopulation(), utilityStatsBeforeReplanning.personId2currentDeltaUtility,
				this.ages);
		final Set<Id<Person>> replanners = replannerIdentifier.drawReplanners();
		// Passing (iteration + 1) to ages.update because it will be used in the next
		// Greedo iteration.
		this.ages.update(replanners, this.greedoConfig.getAgeWeights(this.iteration + 1));
		this.physicalSlotUsageListener.updatePersonWeights(this.ages.getPersonWeights());

		this.statsWriter.writeToFile(new LogDataWrapper(this, replannerIdentifier));

		// for (Person person :
		// this.services.getScenario().getPopulation().getPersons().values()) {
		// // TODO Simplify!
		// this.personId2expectedUtilities.put(person.getId(),
		// person.getSelectedPlan().getScore());
		// }

		this.iteration++;
	}
	// @Override
	// public void notifyIterationEnds(final IterationEndsEvent event) {

	// TODO in the 0th iteration, there is not yet an expectation from the
	// replanning.

	// this.greedoProgressListener.callToNotifyIterationEnds_greedo(event);

	// if (this.mobsimSwitcher.isQSimIteration()) {
	// log.info("physical mobsim run in iteration " + event.getIteration() + "
	// ends.");
	// if (!this.nextMobsimIsExpectedToBePhysical) {
	// throw new RuntimeException("Did not expect a physical mobsim run!");
	// }
	// this.lastPhysicalPopulationState = new
	// PopulationState(this.services.getScenario().getPopulation());
	//// this.greedoProgressListener.extractedLastPhysicalPopulationState(event.getIteration());
	// this.lastPhysicalSlotUsages = this.slotUsageListener.getNewIndicatorView();
	// this.pseudoSimIterationCnt = 0;
	// } else {
	// log.info("pseudoSim run in iteration " + event.getIteration() + " ends.");
	// if (this.nextMobsimIsExpectedToBePhysical) {
	// throw new RuntimeException("Expected a physical mobsim run!");
	// }
	// this.pseudoSimIterationCnt++;
	// }

	// if (this.pseudoSimIterationCnt ==
	// (ConfigUtils.addOrGetModule(this.services.getConfig(), PSimConfigGroup.class)
	// .getIterationsPerCycle() - 1)) {

	// final int greedoIt =
	// this.greedoConfig.getGreedoIteration(event.getIteration());
	// this.greedoProgressListener.observedLastPSimIterationWithinABlock(event.getIteration());

	/*
	 * Extract, for each agent, the expected (hypothetical) score change and do some
	 * book-keeping.
	 */

	// for (Person person :
	// this.services.getScenario().getPopulation().getPersons().values()) {
	// final double realizedUtility =
	// this.lastPhysicalPopulationState.getSelectedPlan(person.getId())
	// .getScore();
	// // TODO Simplify!
	// final double expectedUtility =
	// this.personId2expectedUtilities.get(person.getId());
	//// person.getSelectedPlan().getScore();
	// this.utilities.update(person.getId(), realizedUtility, expectedUtility);
	// }
	// final Utilities.SummaryStatistics utilityStatsBeforeReplanning =
	// this.utilities.newSummaryStatistics();

	/*
	 * Book-keeping.
	 */

	// if (utilityStatsBeforeReplanning.previousDataValid) {
	//
	// this.realizedUtilitySum =
	// utilityStatsBeforeReplanning.previousRealizedUtilitySum;
	// this.realizedUtilityChangeSum =
	// utilityStatsBeforeReplanning.currentRealizedUtilitySum
	// - utilityStatsBeforeReplanning.previousRealizedUtilitySum;
	// this.expectedUtilityChangeSumUniform =
	// this.greedoConfig.getReplanningRate(event.getIteration())
	// * (utilityStatsBeforeReplanning.previousExpectedUtilitySum
	// - utilityStatsBeforeReplanning.previousRealizedUtilitySum);
	//
	// this.expectedUtilityChangeSumAccelerated = 0.0;
	// for (Id<Person> replannerId : this.replanners) {
	// this.expectedUtilityChangeSumAccelerated +=
	// this.utilities.getUtilities(replannerId)
	// .getPreviousExpectedUtilityChange();
	// }
	// }

	/*
	 * Extract hypothetical selected plans.
	 */

	// final Collection<Plan> selectedHypotheticalPlans = new ArrayList<>(
	// this.services.getScenario().getPopulation().getPersons().size());
	// for (Person person :
	// this.services.getScenario().getPopulation().getPersons().values()) {
	// selectedHypotheticalPlans.add(person.getSelectedPlan());
	// }

	/*
	 * Execute one pSim with the full population.
	 * 
	 */

	// final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastPseudoSimSlotUsages =
	// this.ierReplanning.getLastHypotheticalSlotUsages();
	// {
	// final SlotUsageListener pSimSlotUsageListener = new SlotUsageListener(
	// this.greedoConfig.getTimeDiscretization(), this.ages.getPersonWeights(),
	// this.greedoConfig.getLinkWeights(),
	// this.greedoConfig.getTransitVehicleWeights());
	// final EventsManager eventsManager = EventsUtils.createEventsManager();
	// eventsManager.addHandler(pSimSlotUsageListener);
	// final PSim pSim = new PSim(this.services.getScenario(), eventsManager,
	// selectedHypotheticalPlans,
	// this.linkTravelTimes, this.transitEmulator);
	// pSim.run();
	// lastPseudoSimSlotUsages = pSimSlotUsageListener.getNewIndicatorView();
	// }

	/*
	 * Memorize the most recent hypothetical population state and re-set the
	 * population to its most recent physical state.
	 */

	// this.hypotheticalPopulationState = new
	// PopulationState(this.services.getScenario().getPopulation());
	// this.lastPhysicalPopulationState.set(this.services.getScenario().getPopulation());

	/*
	 * DECIDE WHO GETS TO RE-PLAN.
	 * 
	 * At this point, one has (i) the link usage statistics from the last physical
	 * MATSim network loading (lastPhysicalLinkUsages), and (ii) the hypothetical
	 * link usage statistics that would result from a 100% re-planning rate if
	 * network congestion did not change (lastPseudoSimLinkUsages).
	 * 
	 * Now (approximately) solve an optimization problem that aims at balancing
	 * simulation advancement (changing link usage patterns) and simulation
	 * stabilization (keeping link usage patterns as they are).
	 * 
	 */

	// final ReplannerIdentifier replannerIdentifier = new
	// ReplannerIdentifier(this.greedoConfig, greedoIt,
	// this.lastPhysicalSlotUsages, lastPseudoSimSlotUsages,
	// this.services.getScenario().getPopulation(),
	// utilityStatsBeforeReplanning.personId2currentDeltaUtility, this.ages);
	// this.replanners = replannerIdentifier.drawReplanners();
	// // Passing (greedoIt + 1) to ages.update because it will be used in the next
	// // Greedo iteration.
	// this.ages.update(this.replanners, this.greedoConfig.getAgeWeights(greedoIt +
	// 1));
	// this.slotUsageListener.updatePersonWeights(this.ages.getPersonWeights());
	//
	// this.statsWriter.writeToFile(new LogDataWrapper(this, replannerIdentifier,
	// lastPseudoSimSlotUsages.size()));

	// this.greedoProgressListener.madeReplanningDecisions(event.getIteration());

	// this.nextMobsimIsExpectedToBePhysical = true;
	// this.setWeightOfHypotheticalReplanning(1e9);

	// } else {
	//
	// this.nextMobsimIsExpectedToBePhysical = false;
	// this.setWeightOfHypotheticalReplanning(0);
	//
	// }
	// }

	// -------------------- REPLANNING FUNCTIONALITY --------------------

	// private void setWeightOfHypotheticalReplanning(final double weight) {
	// this.greedoProgressListener.setWeightOfHypotheticalReplanning(weight);
	// final StrategyManager strategyManager = this.services.getStrategyManager();
	// for (GenericPlanStrategy<Plan, Person> strategy :
	// strategyManager.getStrategies(null)) {
	// if (strategy instanceof AcceptIntendedReplanningStrategy) {
	// strategyManager.changeWeightOfStrategy(strategy, null, weight);
	// }
	// }
	// }

	// public void replan(final HasPlansAndId<Plan, Person> person) {
	// if ((this.replanners != null) && this.replanners.contains(person.getId())) {
	// this.hypotheticalPopulationState.set(person);
	// }
	// }
}
