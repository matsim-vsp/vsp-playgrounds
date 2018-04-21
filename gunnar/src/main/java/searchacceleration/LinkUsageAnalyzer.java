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
package searchacceleration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;
import searchacceleration.datastructures.CountIndicatorUtils;
import searchacceleration.datastructures.ScoreUpdater;
import searchacceleration.datastructures.SpaceTimeIndicators;
import searchacceleration.examples.matsimdummy.DummyPSim;

/**
 * Decides, at the beginning of each iteration, which travelers are allowed to
 * re-plan.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkUsageAnalyzer implements IterationStartsListener {

	// -------------------- MEMBERS --------------------

	private final LinkUsageListener physicalMobsimUsageListener;

	private final ReplanningParameterProvider replanningParameters;

	private final Map<Id<Link>, Double> linkWeights;

	private final Map<Id<Person>, Plan> replannerId2newPlan = new LinkedHashMap<>();

	private boolean firstCall = true;

	// -------------------- CONSTRUCTION --------------------

	public LinkUsageAnalyzer(final LinkUsageListener physicalMobsimLinkUsageListener,
			final ReplanningParameterProvider replanningParameters, final Map<Id<Link>, Double> linkWeights) {
		this.physicalMobsimUsageListener = physicalMobsimLinkUsageListener;
		this.replanningParameters = replanningParameters;
		this.linkWeights = linkWeights;
	}

	public static Map<Id<Link>, Double> newUniformLinkWeights(final Network network) {
		final Map<Id<Link>, Double> result = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			result.put(link.getId(), 1.0);
		}
		return result;
	}

	public static Map<Id<Link>, Double> newOneOverCapacityLinkWeights(final Network network) {
		final Map<Id<Link>, Double> result = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			if (link.getCapacity() <= 1e-6) {
				throw new RuntimeException("link " + link.getId() + " has capacity " + link.getCapacity());
			}
			result.put(link.getId(), 1.0 / link.getCapacity());
		}
		return result;
	}

	// -------------------- RESULT ACCESS --------------------

	public boolean isAllowedToReplan(final Id<Person> personId) {
		return this.replannerId2newPlan.containsKey(personId);
	}

	public Plan getNewPlan(final Id<Person> personId) {
		return this.replannerId2newPlan.get(personId);
	}

	// --------------- IMPLEMENTATION OF IterationStartsListener ---------------

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {

		/*
		 * Receive information about what happened in the network during the
		 * most recent real network loading.
		 */
		final Map<Id<Vehicle>, SpaceTimeIndicators<Id<Link>>> vehicleId2physicalLinkUsage = this.physicalMobsimUsageListener
				.getAndClearIndicators();
		final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = this.physicalMobsimUsageListener.getAndClearDrivers();

		/*
		 * The first call to this method occurs before the first physical mobsim
		 * execution, so it is skipped.
		 */
		if (this.firstCall) {
			this.firstCall = false;
			return;
		}

		/*
		 * PSEUDOSIM
		 * 
		 * Let every agent re-plan once. Memorize the new plan. Make sure that
		 * the the actual choice sets and currently chosen plans are NOT
		 * affected by this.
		 */

		final DummyPSim pSim = new DummyPSim();
		this.replannerId2newPlan.clear();
		this.replannerId2newPlan.putAll(pSim.getNewPlanForAllAgents());

		/*
		 * PSEUDOSIM
		 * 
		 * Execute all new plans in the pSim.
		 */

		final LinkUsageListener pSimLinkUsageListener = new LinkUsageListener(
				this.physicalMobsimUsageListener.getTimeDiscretization());
		pSim.executePlans(this.replannerId2newPlan, pSimLinkUsageListener);
		final Map<Id<Vehicle>, SpaceTimeIndicators<Id<Link>>> vehicleId2pSimLinkUsage = pSimLinkUsageListener
				.getAndClearIndicators();

		/*
		 * At this point, one has (i) the link usage statistics from the
		 * previous MATSim network loading (vehId2physLinkUsage), and (ii) the
		 * hypothetical link usage statistics that would result from a 100%
		 * re-planning rate if network congestion did not change
		 * (vehId2pSimLinkUsage).
		 * 
		 */

		// Extract basic statistics.

		final double meanLambda = this.replanningParameters.getMeanLambda(event.getIteration());
		final double delta = this.replanningParameters.getDelta(event.getIteration());

		final DynamicData<Id<Link>> currentWeightedCounts = CountIndicatorUtils.newWeightedCounts(
				this.physicalMobsimUsageListener.getTimeDiscretization(), vehicleId2physicalLinkUsage.values(),
				this.linkWeights);
		final DynamicData<Id<Link>> upcomingWeightedCounts = CountIndicatorUtils.newWeightedCounts(
				pSimLinkUsageListener.getTimeDiscretization(), vehicleId2pSimLinkUsage.values(), this.linkWeights);

		final double sumOfCurrentWeightedCounts2 = CountIndicatorUtils.sumOfEntries2(currentWeightedCounts);
		if (sumOfCurrentWeightedCounts2 < 1e-6) {
			throw new RuntimeException("There is no traffic on the network.");
		}
		final double sumOfWeightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(currentWeightedCounts,
				upcomingWeightedCounts);
		final double w = meanLambda / (1.0 - meanLambda) * (sumOfWeightedCountDifferences2 + delta)
				/ sumOfCurrentWeightedCounts2;

		// Initialize score residuals.

		final DynamicData<Id<Link>> interactionResiduals = CountIndicatorUtils
				.newInteractionResiduals(currentWeightedCounts, upcomingWeightedCounts, meanLambda);
		final DynamicData<Id<Link>> inertiaResiduals = new DynamicData<>(currentWeightedCounts.getStartTime_s(),
				currentWeightedCounts.getBinSize_s(), currentWeightedCounts.getBinCnt());
		for (Id<Link> locObj : currentWeightedCounts.keySet()) {
			for (int bin = 0; bin < currentWeightedCounts.getBinCnt(); bin++) {
				inertiaResiduals.put(locObj, bin, (1.0 - meanLambda) * currentWeightedCounts.getBinValue(locObj, bin));
			}
		}
		double regularizationResidual = meanLambda * sumOfCurrentWeightedCounts2;

		/*
		 * Go through all vehicles and decide which driver gets to re-plan (by
		 * removing the non-replanners from replannerId2newPlan.
		 */

		final Set<Id<Vehicle>> allVehicleIds = new LinkedHashSet<>(vehicleId2physicalLinkUsage.keySet());
		allVehicleIds.addAll(vehicleId2pSimLinkUsage.keySet());

		for (Id<Vehicle> vehId : allVehicleIds) {

			final Id<Person> driverId = vehicleId2personId.get(vehId);
			if (driverId == null) {
				throw new RuntimeException("Vehicle " + vehId + " has no (null) driver!");
			}

			final ScoreUpdater<Id<Link>> scoreUpdater = new ScoreUpdater<>(vehicleId2physicalLinkUsage.get(vehId),
					vehicleId2pSimLinkUsage.get(vehId), meanLambda, currentWeightedCounts, sumOfCurrentWeightedCounts2,
					w, delta, interactionResiduals, inertiaResiduals, regularizationResidual, this.linkWeights);

			final double newLambda;
			if (scoreUpdater.getScoreChangeIfOne() < scoreUpdater.getScoreChangeIfZero()) {
				newLambda = 1.0;
			} else {
				newLambda = 0.0;
				this.replannerId2newPlan.remove(driverId);
			}

			scoreUpdater.updateResiduals(newLambda);
			// Interaction- and inertiaResiduals are updated by reference.
			regularizationResidual = scoreUpdater.getUpdatedRegularizationResidual();
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("Started ...");

		final Config config = ConfigUtils.loadConfig("./testdata/berlin_2014-08-01_car_1pct/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Map<Id<Link>, Double> linkWeights = new LinkedHashMap<>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getCapacity() <= 0.0) {
				throw new RuntimeException("link " + link.getId() + " has capacity " + link.getCapacity());
			}
			linkWeights.put(link.getId(), 1.0 / link.getCapacity());
		}

		final Controler controler = new Controler(scenario);

		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		final LinkUsageListener linkUsageListener = new LinkUsageListener(timeDiscr);
		controler.getEvents().addHandler(linkUsageListener);

		final LinkUsageAnalyzer linkUsageAnalyzer = new LinkUsageAnalyzer(linkUsageListener,
				new ReplanningParameterProvider() {

					@Override
					public double getMeanLambda(int iteration) {
						return 0.1;
					}

					@Override
					public double getDelta(int iteration) {
						return 1.0;
					}
				}, linkWeights);
		controler.addControlerListener(linkUsageAnalyzer);

		controler.run();

		System.out.println("... done.");
	}
}
