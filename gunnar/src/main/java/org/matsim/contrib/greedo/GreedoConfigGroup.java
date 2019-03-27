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

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class GreedoConfigGroup extends ReflectiveConfigGroup {

	// -------------------- CONSTANTS --------------------

	public static final String GROUP_NAME = "greedo";

	// -------------------- CONSTRUCTION --------------------

	public GreedoConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- CONFIGURATION --------------------

	// members that are set through configure(..)

	private Integer populationSize = null;

	private Integer firstIteration = null;

	private ConcurrentMap<Id<Link>, Double> concurrentLinkWeights = null;

	private ConcurrentMap<Id<Vehicle>, Double> concurrentTransitVehicleWeights = null;

	private double[] replanningRates = null;

	// the (necessary) configuration

	public void configure(final Scenario scenario) {
		this.configure(scenario, scenario.getNetwork().getLinks().keySet(),
				scenario.getTransitVehicles().getVehicles().keySet());
	}

	public void configure(final Scenario scenario, final Set<Id<Link>> capacitatedLinkIds,
			final Set<Id<Vehicle>> capacitatedTransitVehicleIds) {

		this.populationSize = scenario.getPopulation().getPersons().size();
		this.firstIteration = scenario.getConfig().controler().getFirstIteration();

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

		this.concurrentTransitVehicleWeights = new ConcurrentHashMap<>();
		if (capacitatedTransitVehicleIds != null) {
			for (Id<Vehicle> vehicleId : capacitatedTransitVehicleIds) {
				final Vehicle transitVehicle = scenario.getTransitVehicles().getVehicles().get(vehicleId);
				final VehicleCapacity capacity = transitVehicle.getType().getCapacity();
				final double cap_persons = capacity.getSeats() + capacity.getStandingRoom();
				if (cap_persons < 1e-3) {
					throw new RuntimeException("vehicle " + transitVehicle.getId() + " has capacity of " + cap_persons
							+ " < 0.001 persons.");
				}
				this.concurrentTransitVehicleWeights.put(transitVehicle.getId(), 1.0 / cap_persons);
			}
		}

		// Pre-compute re-planning rates. To have consistent results when randomizing
		// per iteration.
		final RandomGenerator rng = new Well19937c(MatsimRandom.getRandom().nextLong());
		this.replanningRates = new double[scenario.getConfig().controler().getLastIteration()
				- scenario.getConfig().controler().getFirstIteration() + 1];
		for (int it = 0; it < this.replanningRates.length; it++) {
			double rate = this.getInitialMeanReplanningRate()
					* Math.pow(1.0 + it, this.getReplanningRateIterationExponent());
			if (this.getBinomialNumberOfReplanners()) {
				final BinomialDistribution distr = new BinomialDistribution(rng, this.populationSize, rate);
				rate = ((double) distr.sample()) / this.populationSize;
			}
			this.replanningRates[it] = rate;
		}
	}

	// getters

	public TimeDiscretization newTimeDiscretization() {
		return new TimeDiscretization(this.getStartTime_s(), this.getBinSize_s(), this.getBinCnt());
	}

	public Map<Id<Link>, Double> getConcurrentLinkWeights() {
		return this.concurrentLinkWeights;
	}

	public Map<Id<Vehicle>, Double> getConcurrentTransitVehicleWeights() {
		return this.concurrentTransitVehicleWeights;
	}

	public double getReplanningRate(int iteration) {
		return this.replanningRates[iteration - this.firstIteration];
	}

	public double getAgeWeight(final double age) {
		return Math.pow(1.0 / (1.0 + age), this.getAgeWeightExponent());
	}

	// -------------------- mode --------------------

	public static enum ModeType {
		off, sample, accelerate, mah2007, mah2009
	};

	private ModeType modeTypeField = ModeType.accelerate;

	@StringGetter("mode")
	public ModeType getModeTypeField() {
		return this.modeTypeField;
	}

	@StringSetter("mode")
	public void setModeTypeField(final ModeType modeTypeField) {
		this.modeTypeField = modeTypeField;
	}

	// -------------------- startTime_s --------------------

	private int startTime_s = 0;

	@StringGetter("startTime_s")
	public int getStartTime_s() {
		return this.startTime_s;
	}

	@StringSetter("startTime_s")
	public void setStartTime_s(int startTime_s) {
		this.startTime_s = startTime_s;
	}

	// -------------------- binSize_s --------------------

	private int binSize_s = 3600;

	@StringGetter("binSize_s")
	public int getBinSize_s() {
		return this.binSize_s;
	}

	@StringSetter("binSize_s")
	public void setBinSize_s(int binSize_s) {
		this.binSize_s = binSize_s;
	}

	// -------------------- binCnt_s --------------------

	private int binCnt = 24;

	@StringGetter("binCnt")
	public int getBinCnt() {
		return this.binCnt;
	}

	@StringSetter("binCnt")
	public void setBinCnt(int binCnt) {
		this.binCnt = binCnt;
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

	// -------------------- meanReplanningRate --------------------

	private double initialMeanReplanningRate = 0.2;

	@StringGetter("initialMeanReplanningRate")
	public double getInitialMeanReplanningRate() {
		return this.initialMeanReplanningRate;
	}

	@StringSetter("initialMeanReplanningRate")
	public void setInitialMeanReplanningRate(double initialMeanReplanningRate) {
		this.initialMeanReplanningRate = initialMeanReplanningRate;
	}

	// -------------------- replanningRateIterationExponent --------------------

	private double replanningRateIterationExponent = 0.0;

	@StringGetter("replanningRateIterationExponent")
	public double getReplanningRateIterationExponent() {
		return this.replanningRateIterationExponent;
	}

	@StringSetter("replanningRateIterationExponent")
	public void setReplanningRateIterationExponent(double replanningRateIterationExponent) {
		this.replanningRateIterationExponent = replanningRateIterationExponent;
	}

	// -------------------- binomialNumberOfReplanners --------------------

	private boolean binomialNumberOfReplanners = false;

	@StringGetter("binomialNumberOfReplanners")
	public boolean getBinomialNumberOfReplanners() {
		return this.binomialNumberOfReplanners;
	}

	@StringSetter("binomialNumberOfReplanners")
	public void setBinomialNumberOfReplanners(final boolean binomialNumberOfReplanners) {
		this.binomialNumberOfReplanners = binomialNumberOfReplanners;
	}

	// -------------------- detailedLogging --------------------

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

	// --------------- cheapStrategies, expensiveStrategies ---------------

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

	private List<String> cheapStrategies = Arrays.asList("TimeAllocationMutator");

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

	private List<String> expensiveStrategies = Arrays.asList("ReRoute", "TimeAllocationMutator_ReRoute",
			"ChangeLegMode", "ChangeTripMode", "ChangeSingleLegMode", "SubtourModeChoice");

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
