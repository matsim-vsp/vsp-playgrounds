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
package org.matsim.contrib.pseudosimulation.searchacceleration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.core.config.ConfigUtils;
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
public class AccelerationConfigGroup extends ReflectiveConfigGroup {

	// ==================== MATSim-SPECIFICS ====================

	// -------------------- CONSTANTS --------------------

	public static final String GROUP_NAME = "acceleration";

	// -------------------- CONSTRUCTION --------------------

	public AccelerationConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- mode --------------------

	public static enum ModeType {
		off, accelerate, mah2007, mah2009
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

	// -------------------- regularizationThreshold --------------------

	private double regularizationThreshold = 0.05;

	@StringGetter("regularizationThreshold")
	public double getRegularizationThreshold() {
		return this.regularizationThreshold;
	}

	@StringSetter("regularizationThreshold")
	public void setRegularizationThreshold(double regularizationThreshold) {
		this.regularizationThreshold = regularizationThreshold;
	}

	// -------------------- regularizationWeight --------------------

	// private double initialRegularizationWeight = 0.0;
	//
	// @StringGetter("initialRegularizationWeight")
	// public double getInitialRegularizationWeight() {
	// return this.initialRegularizationWeight;
	// }
	//
	// @StringSetter("initialRegularizationWeight")
	// public void setInitialRegularizationWeight(double
	// initialRegularizationWeight) {
	// this.initialRegularizationWeight = initialRegularizationWeight;
	// }

	// -------------------- replanningRateIterationExponent --------------------
	//
	// private double regularizationIterationExponent = 0.0;
	//
	// @StringGetter("regularizationIterationExponent")
	// public double getRegularizationIterationExponent() {
	// return this.regularizationIterationExponent;
	// }
	//
	// @StringSetter("regularizationIterationExponent")
	// public void setRegularizationIterationExponent(double
	// regularizationIterationExponent) {
	// this.regularizationIterationExponent = regularizationIterationExponent;
	// }

	// // -------------------- weighting --------------------
	//
	// public static enum RegularizationType {
	// absolute, relative
	// };
	//
	// private RegularizationType regularizationTypeField = null;
	//
	// @StringGetter("regularizationType")
	// public RegularizationType getRegularizationType() {
	// return this.regularizationTypeField;
	// }
	//
	// @StringSetter("regularizationType")
	// public void setRegularizationType(final RegularizationType
	// regularizationTypeField) {
	// this.regularizationTypeField = regularizationTypeField;
	// }

	// -------------------- weighting --------------------
	//
	// @Deprecated
	// public static enum LinkWeighting {
	// uniform, oneOverCapacity
	// };
	//
	// @Deprecated
	// private LinkWeighting weightingField = LinkWeighting.oneOverCapacity;
	//
	// @Deprecated
	// @StringGetter("linkWeighting")
	// public LinkWeighting getWeighting() {
	// return this.weightingField;
	// }
	//
	// @Deprecated
	// @StringSetter("linkWeighting")
	// public void setWeighting(final LinkWeighting weightingField) {
	// this.weightingField = weightingField;
	// }

	// // -------------------- baselineReplanningRate --------------------
	//
	// private double baselineReplanningRate = Double.NaN;
	//
	// @StringGetter("baselineReplanningRate")
	// public double getBaselineReplanningRate() {
	// return this.baselineReplanningRate;
	// }
	//
	// @StringSetter("baselineReplanningRate")
	// public void setBaselineReplanningRate(final double baselineReplanningRate) {
	// this.baselineReplanningRate = baselineReplanningRate;
	// }

	// // -------------------- randomizeIfNoImprovement --------------------
	//
	// private boolean randomizeIfNoImprovement = false;
	//
	// @StringGetter("randomizeIfNoImprovement")
	// public boolean getRandomizeIfNoImprovement() {
	// return this.randomizeIfNoImprovement;
	// }
	//
	// @StringSetter("randomizeIfNoImprovement")
	// public void setRandomizeIfNoImprovement(final boolean
	// randomizeIfNoImprovement) {
	// this.randomizeIfNoImprovement = randomizeIfNoImprovement;
	// }

	// -------------------- replanningEfficiencyThreshold --------------------
	//
	// private double replanningEfficiencyThreshold;
	//
	// @StringGetter("replanningEfficiencyThreshold")
	// public double getReplanningEfficiencyThreshold() {
	// return this.replanningEfficiencyThreshold;
	// }
	//
	// @StringSetter("replanningEfficiencyThreshold")
	// public void setReplanningEfficiencyThreshold(final double
	// replanningEfficiencyThreshold) {
	// this.replanningEfficiencyThreshold = replanningEfficiencyThreshold;
	// }

	// --------------------replanningEfficiencyThreshold--------------------
	//
	// private int averageIterations = 1; // TODO revisit
	//
	// @StringGetter("averageIterations")
	// public int getAverageIterations() {
	// return this.averageIterations;
	// }
	//
	// @StringSetter("averageIterations")
	// public void setAverageIterations(final int averageIterations) {
	// this.averageIterations = averageIterations;
	// }

	// -------------------- deltaRecipe --------------------
	//
	// public static enum DeltaRecipeType {
	// linearInPercentile, linearInDelta
	// };
	//
	// private DeltaRecipeType deltaRecipeField = null;
	//
	// @StringGetter("deltaRecipe")
	// public DeltaRecipeType getDeltaRecipeField() {
	// return this.deltaRecipeField;
	// }
	//
	// @StringSetter("deltaRecipe")
	// public void setDeltaRecipeFiled(final DeltaRecipeType deltaRecipeField) {
	// this.deltaRecipeField = deltaRecipeField;

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

	// -------------------- coolingStrategy --------------------
	//
	// public static enum CoolingStrategy {
	// fullPopulationPercentile, nonUniformReplannerPercentile
	// };
	//
	// private CoolingStrategy coolingStrategyField =
	// CoolingStrategy.nonUniformReplannerPercentile;
	//
	// @StringGetter("coolingStrategy")
	// public CoolingStrategy getCoolingStrategy() {
	// return this.coolingStrategyField;
	// }
	//
	// @StringSetter("coolingStrategy")
	// public void setCoolingStrategy(final CoolingStrategy coolingStrategyField) {
	// this.coolingStrategyField = coolingStrategyField;
	// }

	// -------------------- deltaScorePerIterationThreshold --------------------

	@Deprecated
	private double scoreImprovementPerIterationThreshold = 0.0;

	@Deprecated
	@StringGetter("scoreImprovementPerIterationThreshold")
	public double getScoreImprovementPerIterationThreshold() {
		return this.scoreImprovementPerIterationThreshold;
	}

	@Deprecated
	@StringSetter("scoreImprovementPerIterationThreshold")
	public void setScoreImprovementPerIterationThreshold(final double scoreImprovementPerIterationThreshold) {
		this.scoreImprovementPerIterationThreshold = scoreImprovementPerIterationThreshold;
	}

	// -------------------- individualConvergenceIterations --------------------

	@Deprecated
	private int individualConvergenceIterations = Integer.MAX_VALUE;

	@Deprecated
	@StringGetter("individualConvergenceIterations")
	public int getIndividualConvergenceIterations() {
		return this.individualConvergenceIterations;
	}

	@Deprecated
	@StringSetter("individualConvergenceIterations")
	public void setIndividualConvergenceIterations(final int individualConvergenceIterations) {
		this.individualConvergenceIterations = individualConvergenceIterations;
	}

	// -------------------- ageInertia --------------------

	// private double ageInertia = 1.0;

	// @StringGetter("ageInertia")
	// public double getAgeInertia(final int iteration) {
	// return (1.0 - this.getMeanReplanningRate(iteration));
	// }

	// @StringSetter("ageInertia")
	// public void setAgeInertia(final double ageInertia) {
	// this.ageInertia = ageInertia;
	// }

	// -------------------- criticalAge --------------------

	// private int criticalAge = 20;

	// @StringGetter("criticalAge")
	// public double getCriticalAge(final int iteration) {
	// return 1.0 / this.getMeanReplanningRate(iteration);
	// }

	// @StringSetter("criticalAge")
	// public void setCriticalAge(final int criticalAge) {
	// this.criticalAge = criticalAge;
	// }

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

	// -------------------- separateOutConvergedAgents --------------------
	//
	// private boolean separateOutConvergedAgents = true;
	//
	// @StringGetter("separateOutConvergedAgents")
	// public boolean getSeparateOutConvergedAgents() {
	// return this.separateOutConvergedAgents;
	// }
	//
	// @StringSetter("separateOutConvergedAgents")
	// public void setSeparateOutConvergedAgents(final boolean
	// separateOutConvergedAgents) {
	// this.separateOutConvergedAgents = separateOutConvergedAgents;
	// }

	// ==================== SUPPLEMENTARY FUNCTIONALITY ====================

	// -------------------- STATIC UTILITIES --------------------

	// @Deprecated
	// public static Map<Id<?>, Double> newUniformLinkWeights(final Network network)
	// {
	// final Map<Id<?>, Double> weights = new LinkedHashMap<>();
	// for (Link link : network.getLinks().values()) {
	// weights.put(link.getId(), 1.0);
	// }
	// return weights;
	// }

	// @Deprecated
	// public static Map<Id<?>, Double> newOneOverCapacityLinkWeights(final Network
	// network) {
	// final Map<Id<?>, Double> weights = new LinkedHashMap<>();
	// for (Link link : network.getLinks().values()) {
	// final double cap_veh_h = link.getFlowCapacityPerSec() *
	// Units.VEH_H_PER_VEH_S;
	// if (cap_veh_h <= 1e-6) {
	// throw new RuntimeException("link " + link.getId() + " has capacity of " +
	// cap_veh_h + " veh/h");
	// }
	// weights.put(link.getId(), 1.0 / cap_veh_h);
	// }
	// return weights;
	// }

	// public static Map<Id<Link>, Double> newNetworkSlotWeights(final Network
	// network) {
	// final Map<Id<Link>, Double> weights = new LinkedHashMap<>();
	// for (Id<Link> linkId : this.capacitatedLinkIds) {
	// // for (Link link : network.getLinks().values()) {
	// final Link link = network.getLinks().get(linkId);
	// final double cap_veh = link.getFlowCapacityPerSec() * this.getBinSize_s();
	// if (cap_veh <= 1e-3) {
	// throw new RuntimeException("link " + link.getId() + " has capacity of " +
	// cap_veh + " < 0.001 veh per "
	// + this.getBinSize_s() + " sec.");
	// }
	// weights.put(link.getId(), 1.0 / cap_veh);
	// }
	// return Collections.unmodifiableMap(weights);
	// }

	// public static Map<Id<Vehicle>, Double> newTransitSlotWeights(final Vehicles
	// transitVehicles) {
	// final Map<Id<Vehicle>, Double> weights = new LinkedHashMap<>();
	// for (Vehicle transitVehicle : transitVehicles.getVehicles().values()) {
	// final VehicleCapacity capacity = transitVehicle.getType().getCapacity();
	// final double cap_persons = capacity.getSeats() + capacity.getStandingRoom();
	// if (cap_persons < 1e-3) {
	// throw new RuntimeException(
	// "vehicle " + transitVehicle.getId() + " has capacity of " + cap_persons + " <
	// 0.001 persons.");
	// }
	// weights.put(transitVehicle.getId(), 1.0 / cap_persons);
	// }
	// return Collections.unmodifiableMap(weights);
	// }

	// -------------------- MEMBERS --------------------

	private Integer pSimIterations = null;

	// private Network network = null; // needs to be explicitly set

	// private Vehicles transitVehicles = null; // needs to be explicitly set

	private Integer populationSize = null; // needs to be explicitly set

	// private Set<Id<Link>> capacitatedLinkIds = null; // needs to be explicitly
	// set

	private TimeDiscretization myTimeDiscretization = null; // lazy initialization

	private Map<Id<Link>, Double> linkWeights = null; // set in configure(..)

	private Map<Id<Vehicle>, Double> transitVehicleWeights = null; // set in configure(..)

	// keeping track of this to avoid a re-randomization
	// private List<Double> meanReplanningRates = null;
	private double[] replanningRates = null;

	// -------------------- INITIALIZATION --------------------

	public void configure(final Scenario scenario, final Set<Id<Link>> capacitatedLinkIds,
			final Set<Id<Vehicle>> capacitatedTransitVehicleIds) {

		// Take over some constants.

		this.populationSize = scenario.getPopulation().getPersons().size();
		this.pSimIterations = ConfigUtils.addOrGetModule(scenario.getConfig(), PSimConfigGroup.class)
				.getIterationsPerCycle();

		// Pre-compute re-planning rates for all Greedo-iterations.

		final RandomGenerator rng = new Well19937c(MatsimRandom.getRandom().nextLong());
		final int lastGreedoIt = this.getGreedoIteration(scenario.getConfig().controler().getLastIteration());
		this.replanningRates = new double[lastGreedoIt + 1];
		for (int greedoIt = 0; greedoIt <= this
				.getGreedoIteration(scenario.getConfig().controler().getLastIteration()); greedoIt++) {
			double rate = this.getInitialMeanReplanningRate()
					* Math.pow(1.0 + greedoIt, this.getReplanningRateIterationExponent());
			if (this.getBinomialNumberOfReplanners()) {
				final BinomialDistribution distr = new BinomialDistribution(rng, this.populationSize, rate);
				rate = ((double) distr.sample()) / this.populationSize;
			}
			this.replanningRates[greedoIt] = rate;
		}

		// Compute link weights from flow capacities.

		this.linkWeights = new LinkedHashMap<>();
		for (Id<Link> linkId : capacitatedLinkIds) {
			final Link link = scenario.getNetwork().getLinks().get(linkId);
			final double cap_veh = link.getFlowCapacityPerSec() * this.getBinSize_s();
			if (cap_veh <= 1e-3) {
				throw new RuntimeException("link " + link.getId() + " has capacity of " + cap_veh + " < 0.001 veh per "
						+ this.getBinSize_s() + " sec.");
			}
			this.linkWeights.put(link.getId(), 1.0 / cap_veh);
		}

		// Compute transit vehicle weights from person capacities.

		this.transitVehicleWeights = new LinkedHashMap<>();
		for (Id<Vehicle> vehicleId : capacitatedTransitVehicleIds) {
			final Vehicle transitVehicle = scenario.getTransitVehicles().getVehicles().get(vehicleId);
			final VehicleCapacity capacity = transitVehicle.getType().getCapacity();
			final double cap_persons = capacity.getSeats() + capacity.getStandingRoom();
			if (cap_persons < 1e-3) {
				throw new RuntimeException(
						"vehicle " + transitVehicle.getId() + " has capacity of " + cap_persons + " < 0.001 persons.");
			}
			this.transitVehicleWeights.put(transitVehicle.getId(), 1.0 / cap_persons);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public int getGreedoIteration(final int matsimIteration) {
		return matsimIteration / this.pSimIterations;
	}

	public Map<Id<Link>, Double> getLinkWeights() {
		return this.linkWeights;
	}

	public Map<Id<Vehicle>, Double> getTransitVehicleWeights() {
		return this.transitVehicleWeights;
	}

	public TimeDiscretization getTimeDiscretization() {
		if (this.myTimeDiscretization == null) {
			this.myTimeDiscretization = new TimeDiscretization(this.getStartTime_s(), this.getBinSize_s(),
					this.getBinCnt());
		}
		return this.myTimeDiscretization;
	}

	public double getMeanReplanningRate(int greedoIteration) {
		return this.replanningRates[greedoIteration];
		// final int outerIteration = matsimIteration / this.pSimIterations;
		// while (outerIteration >= this.meanReplanningRates.size()) {
		// double rate = this.getInitialMeanReplanningRate()
		// * Math.pow(1.0 + matsimIteration / this.pSimIterations,
		// this.getReplanningRateIterationExponent());
		// if (this.getBinomialNumberOfReplanners()) {
		// final RandomGenerator rng = new
		// Well19937c(MatsimRandom.getRandom().nextLong());
		// final BinomialDistribution distr = new BinomialDistribution(rng,
		// this.populationSize, rate);
		// rate = ((double) distr.sample()) / this.populationSize;
		// }
		// this.meanReplanningRates.add(rate);
		// }
		// return this.meanReplanningRates.get(outerIteration);
	}

	// TODO This is extremely inefficient for large populations. Instead of trying
	// to use closed forms (which would reduce flexibility), this may be cached
	// somehow.
	// public double getReplanningStateProbability(int age) {
	// this.getMeanReplanningRate(this.pSimIterations * age); // to make sure that
	// the list is up-to-date
	// double result = 1.0; // multiplicative update!
	// for (int greedoIt = Math.max(0, this.meanReplanningRates.length - age);
	// greedoIt < this.meanReplanningRates
	// length; greedoIt++) {
	// result *= (1.0 - this.meanReplanningRates[greedoIt]);
	// }
	// return result;
	// }

	public double[] getAgeWeights(final int greedoIt) {
		final double[] result = new double[greedoIt + 1];
		result[0] = 1.0;
		for (int age = 1; age < result.length; age++) {
			result[age] = result[age - 1] * (1.0 - this.replanningRates[greedoIt - age]);
		}
		return result;

		// final List<Double> resultList = new ArrayList<>();
		// double proba = 1.0;
		//
		// this.getMeanReplanningRate(this.pSimIterations * age); // to make sure that
		// the list is up-to-date
		// double result = 1.0; // multiplicative update!
		// for (int greedoIt = Math.max(0, this.meanReplanningRates.size() - age);
		// greedoIt < this.meanReplanningRates
		// .size(); greedoIt++) {
		// result *= (1.0 - this.meanReplanningRates.get(greedoIt));
		// }
		// return result;
		//
		// return null;
	}

	// public double getAdaptiveRegularizationWeight(final double
	// currentReplanningEfficiency,
	// final double criticalDelta) {
	// final double boundedFactor = Math.max(0,
	// Math.min(10.0, this.getReplanningEfficiencyThreshold() /
	// currentReplanningEfficiency));
	// return boundedFactor * criticalDelta;
	// }

	// public double getRegularizationWeight(int iteration, Double deltaN2) {
	// double result = Math.pow(1.0 + iteration / this.pSimIterations,
	// this.getRegularizationIterationExponent())
	// * this.getInitialRegularizationWeight();
	// if (this.getRegularizationType() == RegularizationType.absolute) {
	// // Nothing to do, only here to check for unknown regularization types.
	// } else if (this.getRegularizationType() == RegularizationType.relative) {
	// result *= deltaN2;
	// } else {
	// throw new RuntimeException("Unknown regularizationType: " +
	// this.getRegularizationType());
	// }
	// return result;
	// }

	// @Deprecated
	// public Map<Id<?>, Double> getLinkWeightView() {
	// if (this.linkWeights == null) {
	// if (this.weightingField == LinkWeighting.uniform) {
	// this.linkWeights = newUniformLinkWeights(this.network);
	// } else if (this.weightingField == LinkWeighting.oneOverCapacity) {
	// this.linkWeights = newOneOverCapacityLinkWeights(this.network);
	// } else {
	// throw new RuntimeException("unhandled link weighting \"" +
	// this.weightingField + "\"");
	// }
	// this.linkWeights = Collections.unmodifiableMap(this.linkWeights);
	// }
	// return this.linkWeights;
	// }

	// public double getLinkWeight(Object linkId, int bin) {
	// if (this.linkWeights == null) {
	// if (this.weightingField == LinkWeighting.uniform) {
	// this.linkWeights = newUniformLinkWeights(network);
	// } else if (this.weightingField == LinkWeighting.oneOverCapacity) {
	// this.linkWeights = newOneOverCapacityLinkWeights(network);
	// } else {
	// throw new RuntimeException("unhandled link weighting \"" +
	// this.weightingField + "\"");
	// }
	// }
	// if (!(linkId instanceof Id<?>)) {
	// throw new RuntimeException("linkId is of type " +
	// linkId.getClass().getSimpleName());
	// }
	//
	// return this.linkWeights.get(linkId);
	//
	// }

}
