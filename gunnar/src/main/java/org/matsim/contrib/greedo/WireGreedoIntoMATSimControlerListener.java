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
import java.util.Set;

import javax.inject.Singleton;

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
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PersonUtils;

import com.google.inject.Inject;

import ch.ethz.matsim.ier.replannerselection.ReplannerSelector;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

/**
 * TODO Wrapping the physicalSlotUsageListener is not ideal.
 * 
 * @author Gunnar Flötteröd
 * 
 */
@Singleton
public class WireGreedoIntoMATSimControlerListener implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler,
		PersonEntersVehicleEventHandler, VehicleLeavesTrafficEventHandler, ReplannerSelector {

	// -------------------- MEMBERS --------------------

	private MatsimServices services;

	private GreedoConfigGroup greedoConfig;

	private StatisticsWriter<LogDataWrapper> statsWriter;

	private Utilities utilities;

	private Ages ages;

	private SlotUsageListener physicalSlotUsageListener;

	private SlotUsageListener hypotheticalSlotUsageListener;

	private PopulationState lastPhysicalPopulationState;

	private Integer iteration;

	// below only for logging

	private Double expectedUtilityChangeSumAccelerated = null;

	private Double expectedUtilityChangeSumUniform = null;

	private Double realizedUtilityChangeSum = null;

	private Double realizedUtilitySum = null;

	private Integer numberOfReplanners = null;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public WireGreedoIntoMATSimControlerListener(final MatsimServices services) {

		this.services = services;
		this.greedoConfig = ConfigUtils.addOrGetModule(this.services.getConfig(), GreedoConfigGroup.class);
		this.utilities = new Utilities();
		this.ages = new Ages(services.getScenario().getPopulation().getPersons().keySet());
		this.physicalSlotUsageListener = new SlotUsageListener(this.greedoConfig.getTimeDiscretization(),
				this.ages.getPersonWeights(), this.greedoConfig.getLinkWeights(),
				this.greedoConfig.getTransitVehicleWeights());

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(), false);

		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());

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

	// -------------------- IMPLEMENTATION OF ReplannerSelector --------------------

	@Override
	public EventHandler getHandlerForHypotheticalNetworkExperience() {
		this.hypotheticalSlotUsageListener = new SlotUsageListener(this.greedoConfig.getTimeDiscretization(),
				this.ages.getPersonWeights(), this.greedoConfig.getLinkWeights(),
				this.greedoConfig.getTransitVehicleWeights());
		this.hypotheticalSlotUsageListener.reset(this.iteration);
		return this.hypotheticalSlotUsageListener;
	}

	@Override
	public void beforeReplanning() {
		
		this.lastPhysicalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
	}

	@Override
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
			this.expectedUtilityChangeSumAccelerated = null; // TODO
		}

		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(this.greedoConfig, this.iteration,
				this.physicalSlotUsageListener.getNewIndicatorView(),
				this.hypotheticalSlotUsageListener.getNewIndicatorView(), this.services.getScenario().getPopulation(),
				utilityStatsBeforeReplanning.personId2currentDeltaUtility, this.ages);
		final Set<Id<Person>> replanners = replannerIdentifier.drawReplanners();

		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			if (!replanners.contains(person.getId())) {
				this.lastPhysicalPopulationState.set(person);
			}
			// PersonUtils.removeUnselectedPlans(person); 
		}
		
		this.numberOfReplanners = replanners.size();
		this.ages.update(replanners, this.greedoConfig.getAgeWeights(this.iteration + 1));
		this.physicalSlotUsageListener.updatePersonWeights(this.ages.getPersonWeights());

		this.statsWriter.writeToFile(new LogDataWrapper(this, replannerIdentifier));
	}

	// -------------------- IMPLEMENTATION OF EventHandlers --------------------

	@Override
	public void reset(final int iteration) {
		this.iteration = iteration;
		this.physicalSlotUsageListener.reset(iteration);
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		this.physicalSlotUsageListener.handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		this.physicalSlotUsageListener.handleEvent(event);
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		this.physicalSlotUsageListener.handleEvent(event);
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		this.physicalSlotUsageListener.handleEvent(event);
	}

	// -------------------- GETTERS, MAINLY FOR LOGGING --------------------

	public Double getLambdaRealized() {
		return (this.numberOfReplanners.doubleValue()
				/ this.services.getScenario().getPopulation().getPersons().size());
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
	}

	public Integer getNumberOfNonReplanners() {
		if (this.getNumberOfReplanners() == null) {
			return null;
		} else {
			return (this.getPopulationSize() - this.getNumberOfReplanners());
		}
	}
}
