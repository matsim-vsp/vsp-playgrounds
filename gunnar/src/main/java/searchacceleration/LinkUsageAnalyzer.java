package searchacceleration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import searchacceleration.datastructures.IndicatorNumerics;
import searchacceleration.datastructures.SpaceTimeIndicatorVectorListbased;
import searchacceleration.examples.matsimdummy.DummyPSim;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkUsageAnalyzer implements IterationStartsListener {

	// -------------------- MEMBERS --------------------

	private final LinkUsageListener physicalMobsimUsageListener;

	private final double meanLambda;

	private final double delta;

	private final Set<Id<Person>> replannerIds = new LinkedHashSet<>();

	private final Map<Id<Person>, Plan> personId2newPlan = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public LinkUsageAnalyzer(final LinkUsageListener physicalMobsimLinkUsageListener, final double meanLambda,
			final double delta) {
		this.physicalMobsimUsageListener = physicalMobsimLinkUsageListener;
		this.meanLambda = meanLambda; // TODO this should be allowed to vary
										// during runtime
		this.delta = delta;
	}

	// -------------------- FUNCTIONALITY --------------------

	public boolean isAllowedToReplan(final Id<Person> personId) {
		return this.replannerIds.contains(personId);
	}

	public Plan getNewPlan(final Id<Person> personId) {
		return this.personId2newPlan.get(personId);
	}

	// --------------- IMPLEMENTATION OF IterationStartsListener ---------------

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {

		// This data structure represents what happened in the network during
		// the most recent real network loading.
		final Map<Id<Vehicle>, SpaceTimeIndicatorVectorListbased<Id<Link>>> vehId2physicalLinkUsage = this.physicalMobsimUsageListener
				.getAndResetIndicators();

		/*
		 * PSEUDOSIM
		 * 
		 * Let every agent re-plan once. Memorize the new plan.
		 */

		final DummyPSim pSim = new DummyPSim();

		this.personId2newPlan.clear();
		this.personId2newPlan.putAll(pSim.getNewPlanForAllAgents());

		/*
		 * PSEUDOSIM
		 * 
		 * Execute all new plans in the pSim.
		 */

		final LinkUsageListener pSimLinkUsageListener = new LinkUsageListener(
				this.physicalMobsimUsageListener.getTimeDiscretization());
		pSim.executePlans(this.personId2newPlan, pSimLinkUsageListener);

		// This data structure represents what happened in the network during
		// the most recent real network loading.
		final Map<Id<Vehicle>, SpaceTimeIndicatorVectorListbased<Id<Link>>> vehId2pSimLinkUsage = pSimLinkUsageListener
				.getAndResetIndicators();

		/*
		 * At this point, one has (i) the link usage statistics from the
		 * previous MATSim network loading (vehId2physLinkUsage), and (ii) the
		 * hypothetical link usage statistics that would result from a 100%
		 * re-planning rate if network congestion did not change
		 * (vehId2pSimLinkUsage).
		 *
		 * Now, the actual search acceleration is run. It decides which newly
		 * generated plans are selected in the next MATSim iteration.
		 */

		final DynamicData<Id<Link>> currentCounts = IndicatorNumerics
				.newCounts(this.physicalMobsimUsageListener.getTimeDiscretization(), vehId2physicalLinkUsage.values());
		final DynamicData<Id<Link>> upcomingCounts = IndicatorNumerics
				.newCounts(pSimLinkUsageListener.getTimeDiscretization(), vehId2physicalLinkUsage.values());

		final double currentCountsSumOfSquares = IndicatorNumerics.sumOfSquareCounts(currentCounts);
		final double deltaCountsSumOfSquares = IndicatorNumerics.sumOfSquareDeltaCounts(currentCounts, upcomingCounts);
		final double w = this.meanLambda / (1.0 - this.meanLambda) * (deltaCountsSumOfSquares + this.delta)
				/ currentCountsSumOfSquares;

		final DynamicData<Id<Link>> interactionResiduals = IndicatorNumerics.newInteractionResidual(currentCounts,
				upcomingCounts, this.meanLambda);
		final DynamicData<Id<Link>> inertiaResiduals = IndicatorNumerics.newInertiaResidual(currentCounts,
				this.meanLambda);
		double regularizationResidual = this.meanLambda * currentCountsSumOfSquares;

		final Set<Id<Vehicle>> allVehicleIds = new LinkedHashSet<>(vehId2physicalLinkUsage.keySet());
		allVehicleIds.addAll(vehId2pSimLinkUsage.keySet());
		final Set<Id<Vehicle>> replanningVehicleIds = new LinkedHashSet<>();
		for (Id<Vehicle> vehId : allVehicleIds) {

			final IndicatorNumerics<Id<Link>> numerics = new IndicatorNumerics<>(vehId2physicalLinkUsage.get(vehId),
					vehId2pSimLinkUsage.get(vehId), this.meanLambda, currentCounts, currentCountsSumOfSquares, w,
					this.delta, interactionResiduals, inertiaResiduals, regularizationResidual);

			// TODO randomization option; consider delta -> inf !

			final double newLambda;
			if (numerics.getScoreChangeIfOne() <= numerics.getScoreChangeIfZero()) {
				newLambda = 1.0;
				replanningVehicleIds.add(vehId);
			} else {
				newLambda = 0.0;
			}

			numerics.updateDynamicDataResiduals(newLambda);
			regularizationResidual = numerics.getRegularizationResidual();
		}

		/*
		 * Now, it needs to be memorized which agents get to switch to their
		 * newly generated plans, and which must stick to their most recently
		 * selected plan. For this, the vehicle-specific link usage information
		 * from the mobility simulation(s) needs to be related to the
		 * person-specific re-planning information, meaning that one somehow
		 * needs to figure out who the drivers of all vehicles were.
		 */

		this.replannerIds.clear();
		// TODO ... and add selected re-planners based on replanningVehicleIds.

	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("Started ...");

		final Config config = ConfigUtils.loadConfig("./testdata/berlin_2014-08-01_car_1pct/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);

		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		final LinkUsageListener linkUsageListener = new LinkUsageListener(timeDiscr);
		controler.getEvents().addHandler(linkUsageListener);

		final double meanLambda = 0.1;
		final double delta = 1.0;
		final LinkUsageAnalyzer linkUsageAnalyzer = new LinkUsageAnalyzer(linkUsageListener, meanLambda, delta);
		controler.addControlerListener(linkUsageAnalyzer);

		controler.run();

		System.out.println("... done.");
	}
}
