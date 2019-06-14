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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.greedo.recipes.AccelerationRecipe;
import org.matsim.contrib.greedo.recipes.MSARecipe;
import org.matsim.contrib.greedo.recipes.ReplannerIdentifierRecipe;
import org.matsim.contrib.greedo.recipes.SelfRegulatingMSA;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class GreedoConfigGroup extends ReflectiveConfigGroup {

	// -------------------- CONSTANTS --------------------

	public static final String GROUP_NAME = "greedo";

	// -------------------- MEMBERS, set by configure(..) --------------------

	private ConcurrentMap<Id<Link>, Double> concurrentLinkWeights = null;

	// TODO Still needed? Currently, all of these weights are set to zero.
	private ConcurrentMap<Id<Vehicle>, Double> concurrentTransitVehicleWeights = null;

	// -------------------- CONSTRUCTION & CONFIGURATION --------------------

	public GreedoConfigGroup() {
		super(GROUP_NAME);
	}

	public void configure(final Scenario scenario, final Set<Id<Link>> capacitatedLinkIds,
			final Set<Id<Vehicle>> capacitatedTransitVehicleIds) {

		Logger.getLogger(this.getClass()).info("Using 1/linkCapacityPerTimeBin link weights.");
		this.concurrentLinkWeights = new ConcurrentHashMap<>();
		if (capacitatedLinkIds != null) {
			for (Id<Link> linkId : capacitatedLinkIds) {
				final Link link = scenario.getNetwork().getLinks().get(linkId);
				final double cap_veh_timeBin = link.getFlowCapacityPerSec() * this.getBinSize_s();
				if (cap_veh_timeBin <= 1e-3) {
					throw new RuntimeException("link " + link.getId() + " has capacity of " + cap_veh_timeBin
							+ " < 0.001 veh per " + this.getBinSize_s() + " sec.");
				}
				this.concurrentLinkWeights.put(link.getId(), 1.0 / cap_veh_timeBin);
			}
		}

		Logger.getLogger(this.getClass()).warn("Using flat zero transit vehicle weights.");
		this.concurrentTransitVehicleWeights = new ConcurrentHashMap<>();
		if (capacitatedTransitVehicleIds != null) {
			for (Id<Vehicle> vehicleId : capacitatedTransitVehicleIds) {
				final Vehicle transitVehicle = scenario.getTransitVehicles().getVehicles().get(vehicleId);
				// TODO >>> REVISIT >>>
				// final VehicleCapacity capacity = transitVehicle.getType().getCapacity();
				// final double cap_persons = capacity.getSeats() + capacity.getStandingRoom();
				// if (cap_persons < 1e-3) {
				// throw new RuntimeException("vehicle " + transitVehicle.getId() + " has
				// capacity of " + cap_persons
				// + " < 0.001 persons.");
				// }
				// this.concurrentTransitVehicleWeights.put(transitVehicle.getId(), 1.0 /
				// cap_persons);
				// TODO <<< REVISIT <<<
				this.concurrentTransitVehicleWeights.put(transitVehicle.getId(), 0.0);
			}
		}
	}

	public void configure(final Scenario scenario) {
		this.configure(scenario, scenario.getNetwork().getLinks().keySet(),
				scenario.getTransitVehicles().getVehicles().keySet());
	}

	// -------------------- SUPPLEMENTARY (NON-XML) GETTERS --------------------

	public Map<Id<Link>, Double> getConcurrentLinkWeights() {
		return this.concurrentLinkWeights;
	}

	public Map<Id<Vehicle>, Double> getConcurrentTransitVehicleWeights() {
		return this.concurrentTransitVehicleWeights;
	}

	// TODO Check consistency with article equation (dividing by one plus age).
	public double getAgeWeight(final double age) {
		return Math.pow(1.0 / (1.0 + age), this.getAgeWeightExponent());
	}

	// --------------- adaptiveMSADenominatorIncreaseIfSuccess ---------------

	private double adaptiveMSADenominatorIncreaseIfSuccess = 0.1;

	@StringGetter("adaptiveMSADenominatorIncreaseIfSuccess")
	public double getAdaptiveMSADenominatorIncreaseIfSuccess() {
		return adaptiveMSADenominatorIncreaseIfSuccess;
	}

	@StringSetter("adaptiveMSADenominatorIncreaseIfSuccess")
	public void setAdaptiveMSADenominatorIncreaseIfSuccess(final double adaptiveMSADenominatorIncreaseIfSuccess) {
		this.adaptiveMSADenominatorIncreaseIfSuccess = adaptiveMSADenominatorIncreaseIfSuccess;
	}

	// --------------- adaptiveMSADenominatorIncreaseIfFailure ---------------

	private double adaptiveMSADenominatorIncreaseIfFailure = 1.9;

	@StringGetter("adaptiveMSADenominatorIncreaseIfFailure")
	public double getAdaptiveMSADenominatorIncreaseIfFailure() {
		return adaptiveMSADenominatorIncreaseIfFailure;
	}

	@StringSetter("adaptiveMSADenominatorIncreaseIfFailure")
	public void setAdaptiveMSADenominatorIncreaseIfFailure(final double adaptiveMSADenominatorIncreaseIfFailure) {
		this.adaptiveMSADenominatorIncreaseIfFailure = adaptiveMSADenominatorIncreaseIfFailure;
	}

	// -------------------- acceptNegativeDisappointment --------------------

	// TODO Consider discarding this (and WARN instead if beta gets negative).
	private boolean acceptNegativeDisappointment = true;

	@StringGetter("acceptNegativeDisappointment")
	public boolean getAcceptNegativeDisappointment() {
		return this.acceptNegativeDisappointment;
	}

	@StringSetter("acceptNegativeDisappointment")
	public void setAcceptNegativeDisappointment(final boolean acceptNegativeDisappointment) {
		this.acceptNegativeDisappointment = acceptNegativeDisappointment;
	}

	// -------------------- constrainDeltaToZero --------------------

	// TODO Consider discarding this.
	private boolean constrainDeltaToZero = true;

	@StringGetter("constrainDeltaToZero")
	public boolean getConstrainDeltaToZero() {
		return this.constrainDeltaToZero;
	}

	@StringSetter("constrainDeltaToZero")
	public void setConstrainDeltaToZero(final boolean constrainDeltaToZero) {
		this.constrainDeltaToZero = constrainDeltaToZero;
	}

	// -------------------- replannerIdentifier --------------------

	public static enum ReplannerIdentifierType {
		accelerate, MSA, adaptiveMSA
	}

	private ReplannerIdentifierType replannerIdentifierType = ReplannerIdentifierType.accelerate;

	@StringGetter("replannerIdentifier")
	public ReplannerIdentifierType getReplannerIdentifierType() {
		return this.replannerIdentifierType;
	}

	@StringSetter("replannerIdentifier")
	public void setReplannerIdentifierType(final ReplannerIdentifierType replannerIdentifierType) {
		this.replannerIdentifierType = replannerIdentifierType;
	}

	// Lazily initialized in getter.
	private ReplannerIdentifierRecipe replannerIdentifierRecipe = null;

	public ReplannerIdentifierRecipe getReplannerIdentifierRecipe() {
		if (this.replannerIdentifierRecipe == null) {
			if (ReplannerIdentifierType.accelerate.equals(this.getReplannerIdentifierType())) {
				this.replannerIdentifierRecipe = new AccelerationRecipe();
			} else if (ReplannerIdentifierType.MSA.equals(this.getReplannerIdentifierType())) {
				this.replannerIdentifierRecipe = new MSARecipe(this.getInitialMeanReplanningRate(),
						this.getReplanningRateIterationExponent());
			} else if (ReplannerIdentifierType.adaptiveMSA.equals(this.getReplannerIdentifierType())) {
				this.replannerIdentifierRecipe = new SelfRegulatingMSA(
						this.getAdaptiveMSADenominatorIncreaseIfSuccess(),
						this.getAdaptiveMSADenominatorIncreaseIfFailure());
			} else {
				throw new RuntimeException("Unknown ReplannerIdentifierType: " + this.getReplannerIdentifierType());
			}
		}
		return this.replannerIdentifierRecipe;
	}

	// -------------------- minAbsoluteMemoryLength --------------------

	private int minAbsoluteMemoryLength = 4;

	@StringGetter("minAbsoluteMemoryLength")
	public int getMinAbsoluteMemoryLength() {
		return this.minAbsoluteMemoryLength;
	}

	@StringSetter("minAbsoluteMemoryLength")
	public void setMinAbsoluteMemoryLength(final int minAbsoluteMemoryLength) {
		this.minAbsoluteMemoryLength = minAbsoluteMemoryLength;
	}

	// -------------------- maxAbsoluteMemoryLength --------------------

	private int maxAbsoluteMemoryLength = Integer.MAX_VALUE;

	@StringGetter("maxAbsoluteMemoryLength")
	public int getMaxAbsoluteMemoryLength() {
		return this.maxAbsoluteMemoryLength;
	}

	@StringSetter("maxAbsoluteMemoryLength")
	public void setMaxAbsoluteMemoryLength(final int maxAbsoluteMemoryLength) {
		this.maxAbsoluteMemoryLength = maxAbsoluteMemoryLength;
	}

	// -------------------- maxRelativeMemoryLength --------------------

	private double maxRelativeMemoryLength = 1.0;

	@StringGetter("maxRelativeMemoryLength")
	public double getMaxRelativeMemoryLength() {
		return this.maxRelativeMemoryLength;
	}

	@StringSetter("maxRelativeMemoryLength")
	public void setMaxRelativeMemoryLength(final double maxRelativeMemoryLength) {
		this.maxRelativeMemoryLength = maxRelativeMemoryLength;
	}

	// -------------------- meanReplanningRate --------------------

	private double initialMeanReplanningRate = 1.0;

	@StringGetter("initialMeanReplanningRate")
	public double getInitialMeanReplanningRate() {
		return this.initialMeanReplanningRate;
	}

	@StringSetter("initialMeanReplanningRate")
	public void setInitialMeanReplanningRate(double initialMeanReplanningRate) {
		this.initialMeanReplanningRate = initialMeanReplanningRate;
	}

	// -------------------- replanningRateIterationExponent --------------------

	private double replanningRateIterationExponent = -1.0;

	@StringGetter("replanningRateIterationExponent")
	public double getReplanningRateIterationExponent() {
		return this.replanningRateIterationExponent;
	}

	@StringSetter("replanningRateIterationExponent")
	public void setReplanningRateIterationExponent(double replanningRateIterationExponent) {
		this.replanningRateIterationExponent = replanningRateIterationExponent;
	}

	// -------------------- ageWeightExponent --------------------

	private double ageWeightExponent = 1.0;

	@StringGetter("ageWeightExponent")
	public double getAgeWeightExponent() {
		return this.ageWeightExponent;
	}

	@StringSetter("ageWeightExponent")
	public void setAgeWeightExponent(double ageWeightExponent) {
		this.ageWeightExponent = ageWeightExponent;
	}

	// -------------------- startTime_s, binSize_s, binCnt --------------------

	private int startTime_s = 0;

	@StringGetter("startTime_s")
	public int getStartTime_s() {
		return this.startTime_s;
	}

	@StringSetter("startTime_s")
	public void setStartTime_s(int startTime_s) {
		this.startTime_s = startTime_s;
	}

	private int binSize_s = 3600;

	@StringGetter("binSize_s")
	public int getBinSize_s() {
		return this.binSize_s;
	}

	@StringSetter("binSize_s")
	public void setBinSize_s(int binSize_s) {
		this.binSize_s = binSize_s;
	}

	private int binCnt = 24;

	@StringGetter("binCnt")
	public int getBinCnt() {
		return this.binCnt;
	}

	@StringSetter("binCnt")
	public void setBinCnt(int binCnt) {
		this.binCnt = binCnt;
	}

	public TimeDiscretization newTimeDiscretization() {
		return new TimeDiscretization(this.getStartTime_s(), this.getBinSize_s(), this.getBinCnt());
	}

	// -------------------- detailedLogging --------------------

	// TODO Some of these details may be necessary in ReplannerIdentifier.
	private boolean detailedLogging = true;

	@StringGetter("detailedLogging")
	public boolean getDetailedLogging() {
		return this.detailedLogging;
	}

	@StringSetter("detailedLogging")
	public void setDetailedLogging(final boolean detailedLogging) {
		this.detailedLogging = detailedLogging;
	}

	// -------------------- adjustStrategyWeights --------------------

	private boolean adjustStrategyWeights = true;

	@StringGetter("adjustStrategyWeights")
	public boolean getAdjustStrategyWeights() {
		return this.adjustStrategyWeights;
	}

	@StringSetter("adjustStrategyWeights")
	public void setAdjustStrategyWeights(final boolean adjustStrategyWeights) {
		this.adjustStrategyWeights = adjustStrategyWeights;
	}

	// helpers

	private static String listToString(final List<String> list) {
		final StringBuilder builder = new StringBuilder();
		if (list.size() > 0) {
			builder.append(list.get(0));
		}
		for (int i = 1; i < list.size(); i++) {
			builder.append(',');
			builder.append(list.get(i));
		}
		return builder.toString();
	}

	private static List<String> stringToList(final String string) {
		final ArrayList<String> result = new ArrayList<>();
		for (String part : StringUtils.explode(string, ',')) {
			result.add(part.trim().intern());
		}
		result.trimToSize();
		return result;
	}

	// computationally cheap strategies

	private List<String> cheapStrategies = Arrays.asList(DefaultStrategy.TimeAllocationMutator);

	@StringGetter("cheapStrategies")
	public String getCheapStrategies() {
		return listToString(this.cheapStrategies);
	}

	@StringSetter("cheapStrategies")
	public void setCheapStrategies(final String cheapStrategies) {
		this.cheapStrategies = stringToList(cheapStrategies);
	}

	public List<String> getCheapStrategyList() {
		return this.cheapStrategies;
	}

	// computationally expensive strategies

	private List<String> expensiveStrategies = Arrays.asList(DefaultStrategy.ReRoute,
			DefaultStrategy.TimeAllocationMutator_ReRoute, DefaultStrategy.ChangeSingleTripMode,
			DefaultStrategy.SubtourModeChoice, DefaultStrategy.ChangeTripMode, DefaultStrategy.ChangeLegMode,
			DefaultStrategy.ChangeSingleLegMode, DefaultStrategy.TripSubtourModeChoice);

	@StringGetter("expensiveStrategies")
	public String getExpensiveStrategies() {
		return listToString(this.expensiveStrategies);
	}

	@StringSetter("expensiveStrategies")
	public void setExpensiveStrategies(final String expensiveStrategies) {
		this.expensiveStrategies = stringToList(expensiveStrategies);
	}

	public List<String> getExpensiveStrategyList() {
		return this.expensiveStrategies;
	}
}
