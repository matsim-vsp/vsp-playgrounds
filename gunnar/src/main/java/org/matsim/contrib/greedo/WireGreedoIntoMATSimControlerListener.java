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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.matsim.api.core.v01.Id;
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
import org.matsim.contrib.greedo.logging.AvgNonReplannerUtilityChange;
import org.matsim.contrib.greedo.logging.AvgRealizedDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgRealizedUtility;
import org.matsim.contrib.greedo.logging.AvgReplannerSize;
import org.matsim.contrib.greedo.logging.AvgReplannerUtilityChange;
import org.matsim.contrib.greedo.logging.Beta;
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

import com.google.inject.Inject;
import com.google.inject.Provider;

import ch.ethz.matsim.ier.replannerselection.ReplannerSelector;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
@Singleton
public class WireGreedoIntoMATSimControlerListener implements Provider<EventHandler>, ReplannerSelector {

	// -------------------- MEMBERS --------------------

	private final MatsimServices services;

	private final GreedoConfigGroup greedoConfig;

	private final StatisticsWriter<LogDataWrapper> statsWriter;

	private final Utilities utilities;

	private final Ages ages;

	private final SlotUsageListener physicalSlotUsageListener;

	private final List<SlotUsageListener> hypotheticalSlotUsageListeners = new LinkedList<>();

	private PopulationState lastPhysicalPopulationState = null;

	// below only for logging

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
		this.ages = new Ages(services.getScenario().getPopulation().getPersons().keySet(), this.greedoConfig);
		this.physicalSlotUsageListener = new SlotUsageListener(this.greedoConfig.newTimeDiscretization(),
				this.ages.getPersonWeights(), this.greedoConfig.getConcurrentLinkWeights(),
				this.greedoConfig.getConcurrentTransitVehicleWeights());

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(), false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new LambdaRealized());
		this.statsWriter.addSearchStatistic(new LambdaBar());
		this.statsWriter.addSearchStatistic(new Beta());
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
		this.statsWriter.addSearchStatistic(new AvgReplannerUtilityChange());
		this.statsWriter.addSearchStatistic(new AvgNonReplannerUtilityChange());
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
	public EventHandlerProvider prepareReplanningAndGetEventHandlerProvider() {

		this.lastPhysicalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			this.utilities.updateRealizedUtility(person.getId(),
					this.lastPhysicalPopulationState.getSelectedPlan(person.getId()).getScore());
		}

		this.hypotheticalSlotUsageListeners.clear();
		return new EventHandlerProvider() {
			@Override
			public synchronized EventHandler get(final Set<Id<Person>> personIds) {
				final SlotUsageListener listener = new SlotUsageListener(greedoConfig.newTimeDiscretization(),
						ages.getPersonWeights().entrySet().stream().filter(entry -> personIds.contains(entry.getKey()))
								.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())),
						greedoConfig.getConcurrentLinkWeights(), greedoConfig.getConcurrentTransitVehicleWeights());
				listener.resetOnceAndForAll(iteration());
				hypotheticalSlotUsageListeners.add(listener);
				return listener;
			}
		};
	}

	@Override
	public void afterReplanning() {

		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			this.utilities.updateExpectedUtility(person.getId(), person.getSelectedPlan().getScore());
		}
		final Utilities.SummaryStatistics utilityStats = this.utilities.newSummaryStatistics();

		final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> hypotheticalSlotUsageIndicators = new LinkedHashMap<>();
		for (SlotUsageListener listener : this.hypotheticalSlotUsageListeners) {
			hypotheticalSlotUsageIndicators.putAll(listener.getNewIndicatorView());
		}

		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(this.greedoConfig, this.iteration(),
				this.physicalSlotUsageListener.getNewIndicatorView(), hypotheticalSlotUsageIndicators,
				this.services.getScenario().getPopulation(), utilityStats.personId2currentDeltaUtility);
		final Set<Id<Person>> replannerIds = replannerIdentifier.drawReplanners();
		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			if (!replannerIds.contains(person.getId())) {
				this.lastPhysicalPopulationState.set(person);
			}
		}

		this.numberOfReplanners = replannerIds.size();
		this.ages.update(replannerIds);
		this.physicalSlotUsageListener.updatePersonWeights(this.ages.getPersonWeights());

		if (this.realizedUtilitySum != null) {
			this.realizedUtilityChangeSum = utilityStats.realizedUtilitySum - this.realizedUtilitySum;
		}
		this.realizedUtilitySum = utilityStats.realizedUtilitySum;

		this.statsWriter.writeToFile(new LogDataWrapper(this, replannerIdentifier));

		// These are predictions for the next iteration.
		this.expectedUtilityChangeSumUniform = this.greedoConfig.getReplanningRate(this.iteration())
				* utilityStats.deltaUtilitySum;
	}

	// --------------- IMPLEMENTATION OF Provider<EventHandler> ---------------

	@Override
	public EventHandler get() {
		// Expecting this to be called only once; returning always the same instance.
		return this.physicalSlotUsageListener;
	}

	// -------------------- GETTERS, MAINLY FOR LOGGING --------------------

	private int iteration() {
		return this.physicalSlotUsageListener.getLastResetIteration();
	}

	public Double getLambdaRealized() {
		return (this.numberOfReplanners.doubleValue()
				/ this.services.getScenario().getPopulation().getPersons().size());
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
