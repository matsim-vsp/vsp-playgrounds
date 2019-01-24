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
import java.util.ArrayList;
import java.util.Collection;
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
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.greedo.datastructures.Ages;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.datastructures.Utilities;
import org.matsim.contrib.greedo.listeners.SlotUsageListener;
import org.matsim.contrib.greedo.logging.AgesPercentile;
import org.matsim.contrib.greedo.logging.AverageAge;
import org.matsim.contrib.greedo.logging.AverageAgeWeight;
import org.matsim.contrib.greedo.logging.DriversInPhysicalSim;
import org.matsim.contrib.greedo.logging.DriversInPseudoSim;
import org.matsim.contrib.greedo.logging.EffectiveReplanningRate;
import org.matsim.contrib.greedo.logging.ExpectedDeltaUtilityAccelerated;
import org.matsim.contrib.greedo.logging.ExpectedDeltaUtilityUniform;
import org.matsim.contrib.greedo.logging.LogDataWrapper;
import org.matsim.contrib.greedo.logging.MeanReplanningRate;
import org.matsim.contrib.greedo.logging.RealizedDeltaUtility;
import org.matsim.contrib.greedo.logging.RealizedUtilitySum;
import org.matsim.contrib.greedo.logging.UnweightedCountDifferences2;
import org.matsim.contrib.greedo.logging.UnweightedUtilityChangeSum;
import org.matsim.contrib.greedo.logging.WeightedCountDifferences2;
import org.matsim.contrib.greedo.logging.WeightedUtilityChangeSum;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSim;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.contrib.pseudosimulation.transit.TransitEmulator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
@Singleton
public class WireGreedoIntoMATSimListener implements StartupListener, IterationEndsListener, LinkEnterEventHandler,
		VehicleEntersTrafficEventHandler, PersonEntersVehicleEventHandler, VehicleLeavesTrafficEventHandler {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(Controler.class);

	// -------------------- INJECTED MEMBERS --------------------

	@Inject
	private MatsimServices services;

	/*
	 * We know if we are in a pSim iteration or in a "real" iteration. The
	 * MobsimSwitcher is updated at iterationStarts, i.e. always *before* the mobsim
	 * (or psim) is executed. The SearchAccelerator, on the other hand, is invoked
	 * at iterationEnds, i.e. *after* the corresponding mobsim (or psim) run.
	 * 
	 */
	@Inject
	private MobSimSwitcher mobsimSwitcher;

	@Inject
	private TravelTime linkTravelTimes;

	@Inject
	private TransitEmulator transitEmulator;

	@Inject
	private GreedoProgressListener greedoProgressListener;

	// -------------------- NON-INJECTED MEMBERS --------------------

	private Set<Id<Person>> replanners = null;

	private PopulationState hypotheticalPopulationState = null;

	private Double expectedUtilityChangeSumAccelerated = null;

	private Double expectedUtilityChangeSumUniform = null;

	private Double realizedUtilityChangeSum = null;

	private Double realizedUtilitySum = null;

	// >>> created upon startup >>>

	private SlotUsageListener slotUsageListener;

	private StatisticsWriter<LogDataWrapper> statsWriter;

	private Utilities utilities;

	private Ages ages;

	// <<< created upon startup <<<

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public WireGreedoIntoMATSimListener() {
	}

	// -------------------- GETTERS, MAINLY FOR LOGGING --------------------

	public Integer getDriversInPhysicalSim() {
		if (this.lastPhysicalSlotUsages != null) {
			return this.lastPhysicalSlotUsages.size();
		} else {
			return null;
		}
	}

	public Double getEffectiveReplanningRate() {
		if (this.replanners == null) {
			return null;
		} else {
			return ((double) this.replanners.size()) / this.services.getScenario().getPopulation().getPersons().size();
		}
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

	// --------------- IMPLEMENTATION OF StartupListener ---------------

	private GreedoConfigGroup greedoConfig = null;

	@Override
	public void notifyStartup(final StartupEvent event) {

		this.greedoProgressListener.callToNotifyStartup_greedo(event);

		this.greedoConfig = ConfigUtils.addOrGetModule(this.services.getConfig(), GreedoConfigGroup.class);
		this.utilities = new Utilities();
		this.ages = new Ages(this.services.getScenario().getPopulation().getPersons().keySet());
		this.slotUsageListener = new SlotUsageListener(this.greedoConfig.getTimeDiscretization(),
				this.ages.getPersonWeights(), this.greedoConfig.getLinkWeights(),
				this.greedoConfig.getTransitVehicleWeights());

		this.statsWriter = new StatisticsWriter<>(
				new File(this.services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(),
				false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());

		this.statsWriter.addSearchStatistic(new DriversInPhysicalSim());
		this.statsWriter.addSearchStatistic(new DriversInPseudoSim());

		this.statsWriter.addSearchStatistic(new MeanReplanningRate());
		this.statsWriter.addSearchStatistic(new EffectiveReplanningRate());

		this.statsWriter.addSearchStatistic(new AverageAge());
		this.statsWriter.addSearchStatistic(new AverageAgeWeight());

		this.statsWriter.addSearchStatistic(new UnweightedCountDifferences2());
		this.statsWriter.addSearchStatistic(new WeightedCountDifferences2());

		this.statsWriter.addSearchStatistic(new UnweightedUtilityChangeSum());
		this.statsWriter.addSearchStatistic(new WeightedUtilityChangeSum());

		this.statsWriter.addSearchStatistic(new RealizedUtilitySum());
		this.statsWriter.addSearchStatistic(new RealizedDeltaUtility());
		this.statsWriter.addSearchStatistic(new ExpectedDeltaUtilityUniform());
		this.statsWriter.addSearchStatistic(new ExpectedDeltaUtilityAccelerated());

		this.statsWriter.addSearchStatistic(new AgesPercentile(1));
		this.statsWriter.addSearchStatistic(new AgesPercentile(5));
		this.statsWriter.addSearchStatistic(new AgesPercentile(50));
		this.statsWriter.addSearchStatistic(new AgesPercentile(95));
		this.statsWriter.addSearchStatistic(new AgesPercentile(99));
	}

	// -------------------- IMPLEMENTATION OF EventHandlers --------------------

	@Override
	public void reset(final int iteration) {
		this.greedoProgressListener.callToReset_greedo(iteration);
		this.slotUsageListener.reset(iteration);
		// this.matsimIVMobsimUsageListener.reset(iteration);
		// this.matsimOVMobsimUsageListener.reset(iteration);
		// this.stopInteractionListener.reset(iteration);
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.slotUsageListener.handleEvent(event);
			// this.matsimIVMobsimUsageListener.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.slotUsageListener.handleEvent(event);
			// this.matsimIVMobsimUsageListener.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.slotUsageListener.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.slotUsageListener.handleEvent(event);
		}
	}

	// --------------- IMPLEMENTATION OF IterationEndsListener ---------------

	private PopulationState lastPhysicalPopulationState = null;
	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastPhysicalSlotUsages = null;

	private int pseudoSimIterationCnt = 0;

	// A safeguard.
	private boolean nextMobsimIsExpectedToBePhysical = true;

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		this.greedoProgressListener.callToNotifyIterationEnds_greedo(event);

		if (this.mobsimSwitcher.isQSimIteration()) {
			log.info("physical mobsim run in iteration " + event.getIteration() + " ends");
			if (!this.nextMobsimIsExpectedToBePhysical) {
				throw new RuntimeException("Did not expect a physical mobsim run!");
			}
			this.lastPhysicalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
			this.greedoProgressListener.extractedLastPhysicalPopulationState(event.getIteration());
			this.lastPhysicalSlotUsages = this.slotUsageListener.getNewIndicatorView();
			this.pseudoSimIterationCnt = 0;
		} else {
			log.info("pseudoSim run in iteration " + event.getIteration() + " ends");
			if (this.nextMobsimIsExpectedToBePhysical) {
				throw new RuntimeException("Expected a physical mobsim run!");
			}
			this.pseudoSimIterationCnt++;
		}

		if (this.pseudoSimIterationCnt == (ConfigUtils.addOrGetModule(this.services.getConfig(), PSimConfigGroup.class)
				.getIterationsPerCycle() - 1)) {

			final int greedoIt = this.greedoConfig.getGreedoIteration(event.getIteration());
			this.greedoProgressListener.observedLastPSimIterationWithinABlock(event.getIteration());

			/*
			 * Extract, for each agent, the expected (hypothetical) score change and do some
			 * book-keeping.
			 */

			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				final double realizedUtility = this.lastPhysicalPopulationState.getSelectedPlan(person.getId())
						.getScore();
				final double expectedUtility = person.getSelectedPlan().getScore();
				this.utilities.update(person.getId(), realizedUtility, expectedUtility);
			}
			final Utilities.SummaryStatistics utilityStatsBeforeReplanning = this.utilities.newSummaryStatistics();

			/*
			 * Book-keeping.
			 */

			if (utilityStatsBeforeReplanning.previousDataValid) {

				this.realizedUtilitySum = utilityStatsBeforeReplanning.previousRealizedUtilitySum;
				this.realizedUtilityChangeSum = utilityStatsBeforeReplanning.currentRealizedUtilitySum
						- utilityStatsBeforeReplanning.previousRealizedUtilitySum;
				this.expectedUtilityChangeSumUniform = this.greedoConfig.getReplanningRate(greedoIt)
						* (utilityStatsBeforeReplanning.previousExpectedUtilitySum
								- utilityStatsBeforeReplanning.previousRealizedUtilitySum);

				this.expectedUtilityChangeSumAccelerated = 0.0;
				for (Id<Person> replannerId : this.replanners) {
					this.expectedUtilityChangeSumAccelerated += this.utilities.getUtilities(replannerId)
							.getPreviousExpectedUtilityChange();
				}
			}

			/*
			 * Extract hypothetical selected plans.
			 */

			final Collection<Plan> selectedHypotheticalPlans = new ArrayList<>(
					this.services.getScenario().getPopulation().getPersons().size());
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				selectedHypotheticalPlans.add(person.getSelectedPlan());
			}

			/*
			 * Execute one pSim with the full population.
			 * 
			 * TODO This is probably not necessary.
			 */

			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastPseudoSimSlotUsages;
			{
				final SlotUsageListener pSimSlotUsageListener = new SlotUsageListener(
						this.greedoConfig.getTimeDiscretization(), this.ages.getPersonWeights(),
						this.greedoConfig.getLinkWeights(), this.greedoConfig.getTransitVehicleWeights());
				final EventsManager eventsManager = EventsUtils.createEventsManager();
				eventsManager.addHandler(pSimSlotUsageListener);
				final PSim pSim = new PSim(this.services.getScenario(), eventsManager, selectedHypotheticalPlans,
						this.linkTravelTimes, this.transitEmulator);
				pSim.run();
				lastPseudoSimSlotUsages = pSimSlotUsageListener.getNewIndicatorView();
			}

			/*
			 * Memorize the most recent hypothetical population state and re-set the
			 * population to its most recent physical state.
			 */

			this.hypotheticalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
			this.lastPhysicalPopulationState.set(this.services.getScenario().getPopulation());

			/*
			 * DECIDE WHO GETS TO RE-PLAN.
			 * 
			 * At this point, one has (i) the link usage statistics from the last physical
			 * MATSim network loading (lastPhysicalLinkUsages), and (ii) the hypothetical
			 * link usage statistics that would result from a 100% re-planning rate if
			 * network congestion did not change (lastPseudoSimLinkUsages).
			 * 
			 * Now solve an optimization problem that aims at balancing simulation
			 * advancement (changing link usage patterns) and simulation stabilization
			 * (keeping link usage patterns as they are). Most of the code below prepares
			 * the (heuristic) solution of this problem.
			 * 
			 */

			final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(this.greedoConfig, greedoIt,
					this.lastPhysicalSlotUsages, lastPseudoSimSlotUsages, this.services.getScenario().getPopulation(),
					utilityStatsBeforeReplanning.personId2currentDeltaUtility, this.ages);
			this.replanners = replannerIdentifier.drawReplanners();
			this.ages.update(this.replanners, this.greedoConfig.getAgeWeights(greedoIt + 1));
			this.slotUsageListener.updatePersonWeights(this.ages.getPersonWeights());

			this.statsWriter.writeToFile(new LogDataWrapper(this, replannerIdentifier, lastPseudoSimSlotUsages.size()));

			this.greedoProgressListener.madeReplanningDecisions(event.getIteration());

			this.nextMobsimIsExpectedToBePhysical = true;
			this.setWeightOfHypotheticalReplanning(1e9);

		} else {

			this.nextMobsimIsExpectedToBePhysical = false;
			this.setWeightOfHypotheticalReplanning(0);

		}
	}

	// -------------------- REPLANNING FUNCTIONALITY --------------------

	private void setWeightOfHypotheticalReplanning(final double weight) {
		this.greedoProgressListener.setWeightOfHypotheticalReplanning(weight);
		final StrategyManager strategyManager = this.services.getStrategyManager();
		for (GenericPlanStrategy<Plan, Person> strategy : strategyManager.getStrategies(null)) {
			if (strategy instanceof AcceptIntendedReplanningStrategy) {
				strategyManager.changeWeightOfStrategy(strategy, null, weight);
			}
		}
	}

	public void replan(final HasPlansAndId<Plan, Person> person) {
		if ((this.replanners != null) && this.replanners.contains(person.getId())) {
			this.hypotheticalPopulationState.set(person);
		}
	}
}
